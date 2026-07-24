<template>
  <div class="travel-map">
    <div ref="mapEl" class="map-canvas"></div>
    <div class="map-legend" v-if="days.length">
      <span v-for="(d, i) in days" :key="i" class="legend-item">
        <span class="legend-dot" :style="{ background: colorFor(i) }"></span>
        Day {{ d.day ?? i + 1 }}
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

interface Spot { name?: string; latitude?: number; longitude?: number; visitTime?: string }
interface Day { day?: number; title?: string; spots?: Spot[] }

// profile: foot-walking(도보) | driving-car(차량)
const props = defineProps<{ days: Day[]; profile?: string }>();

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

const mapEl = ref<HTMLElement | null>(null);
let map: L.Map | null = null;
let layers: L.Layer[] = [];

const COLORS = ['#2563eb', '#dc2626', '#059669', '#d97706', '#7c3aed', '#0891b2'];
const colorFor = (i: number) => COLORS[i % COLORS.length];

// 백엔드 /api/route 로 실제 도로 경로(geometry) 조회. 실패 시 null → 직선 폴백.
async function fetchRoute(pts: [number, number][]): Promise<[number, number][] | null> {
  try {
    const points = pts.map((p) => `${p[0]},${p[1]}`).join(';');
    const prof = props.profile || 'driving-car';
    const res = await fetch(`${API_BASE}/api/route?profile=${prof}&points=${encodeURIComponent(points)}`);
    if (!res.ok) return null;
    const data = await res.json();
    const geo = data.geometry;
    if (Array.isArray(geo) && geo.length > 1) return geo as [number, number][];
    return null;
  } catch {
    return null;
  }
}

async function render() {
  if (!map) return;
  layers.forEach((l) => map!.removeLayer(l));
  layers = [];
  const allPts: [number, number][] = [];

  for (let di = 0; di < props.days.length; di++) {
    const day = props.days[di];
    const pts: [number, number][] = [];
    (day.spots || []).forEach((s) => {
      if (typeof s.latitude === 'number' && typeof s.longitude === 'number') {
        const p: [number, number] = [s.latitude, s.longitude];
        pts.push(p);
        allPts.push(p);
        const marker = L.circleMarker(p, {
          radius: 7, color: '#ffffff', weight: 2, fillColor: colorFor(di), fillOpacity: 1,
        }).bindTooltip(`${s.visitTime ? s.visitTime + ' · ' : ''}${s.name || ''}`);
        marker.addTo(map!);
        layers.push(marker);
      }
    });
    if (pts.length > 1) {
      // 실제 도로 경로 우선, 없으면 직선(점선).
      const road = await fetchRoute(pts);
      const line = road
        ? L.polyline(road, { color: colorFor(di), weight: 4, opacity: 0.85 })
        : L.polyline(pts, { color: colorFor(di), weight: 3, opacity: 0.6, dashArray: '4 6' });
      if (map) { line.addTo(map); layers.push(line); }
    }
  }

  if (allPts.length && map) {
    map.fitBounds(L.latLngBounds(allPts).pad(0.25));
  }
}

onMounted(() => {
  if (!mapEl.value) return;
  map = L.map(mapEl.value, { scrollWheelZoom: false }).setView([36.5, 127.9], 6);
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '© OpenStreetMap',
    maxZoom: 19,
    crossOrigin: true,
  }).addTo(map);
  nextTick(render);
});

watch(() => props.days, () => nextTick(render), { deep: true });

onBeforeUnmount(() => {
  if (map) {
    map.remove();
    map = null;
  }
});
</script>

<style scoped>
.travel-map {
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid rgba(0, 0, 0, 0.08);
}
.map-canvas {
  width: 100%;
  height: 340px;
}
.map-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding: 10px 12px;
  font-size: 12px;
  color: #444;
  background: rgba(0, 0, 0, 0.02);
}
.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;
}
</style>
