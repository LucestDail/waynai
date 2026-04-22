import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';

import '../core/theme.dart';
import '../models/nearby.dart';
import '../services/gps_service.dart';
import '../services/location_service.dart';

class NearbyScreen extends StatefulWidget {
  const NearbyScreen({super.key});

  @override
  State<NearbyScreen> createState() => _NearbyScreenState();
}

class _NearbyScreenState extends State<NearbyScreen> {
  final _contextCtrl = TextEditingController(text: '근처 맛집');
  int _radius = 1000;
  bool _loading = false;
  String? _error;
  NearbyResponse? _result;
  double? _lat;
  double? _lng;

  @override
  void initState() {
    super.initState();
    _refresh();
  }

  Future<void> _refresh() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final pos = await LocationService.instance.getCurrentPosition();
      if (pos == null) {
        setState(() {
          _error = '위치 권한이 없거나 GPS 가 꺼져 있습니다.';
          _loading = false;
        });
        return;
      }
      _lat = pos.latitude;
      _lng = pos.longitude;

      final res = await GpsService.instance.nearby(
        lat: _lat!,
        lng: _lng!,
        radiusMeters: _radius,
        limit: 10,
        context: _contextCtrl.text.trim().isEmpty
            ? null
            : _contextCtrl.text.trim(),
      );
      setState(() {
        _result = res;
        _loading = false;
      });
    } catch (e) {
      setState(() {
        _error = e.toString();
        _loading = false;
      });
    }
  }

  @override
  void dispose() {
    _contextCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('내 주변 실시간 추천'),
        actions: [
          IconButton(
            tooltip: '새로고침',
            icon: const Icon(Icons.refresh),
            onPressed: _loading ? null : _refresh,
          ),
        ],
      ),
      body: Column(
        children: [
          _controls(),
          const Divider(height: 1),
          Expanded(child: _body()),
        ],
      ),
    );
  }

  Widget _controls() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
      child: Column(
        children: [
          TextField(
            controller: _contextCtrl,
            decoration: const InputDecoration(
              hintText: '키워드 (예: 근처 맛집, 카페, 관광지)',
              prefixIcon: Icon(Icons.tune),
            ),
          ),
          const SizedBox(height: 8),
          Row(
            children: [
              const Text('반경', style: TextStyle(color: WaynaiTheme.dusk)),
              Expanded(
                child: Slider(
                  value: _radius.toDouble(),
                  min: 200,
                  max: 5000,
                  divisions: 24,
                  label: '$_radius m',
                  onChanged: (v) => setState(() => _radius = v.round()),
                  onChangeEnd: (_) => _refresh(),
                ),
              ),
              SizedBox(
                width: 72,
                child: Text('$_radius m',
                    textAlign: TextAlign.right,
                    style: const TextStyle(color: WaynaiTheme.ocean)),
              ),
            ],
          ),
          if (_lat != null && _lng != null)
            Align(
              alignment: Alignment.centerLeft,
              child: Text(
                '현재 위치: ${_lat!.toStringAsFixed(5)}, ${_lng!.toStringAsFixed(5)}',
                style: const TextStyle(
                    color: WaynaiTheme.dusk, fontSize: 12),
              ),
            ),
        ],
      ),
    );
  }

  Widget _body() {
    if (_loading) {
      return const Center(child: CircularProgressIndicator());
    }
    if (_error != null) {
      return Padding(
        padding: const EdgeInsets.all(24),
        child: Center(
          child: Text(
            _error!,
            textAlign: TextAlign.center,
            style: const TextStyle(color: WaynaiTheme.terra),
          ),
        ),
      );
    }
    final r = _result;
    if (r == null) return const SizedBox.shrink();
    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
      children: [
        _section('주변 장소 (${r.spots.length})',
            Icons.place_outlined, WaynaiTheme.terra),
        for (final s in r.spots) _spotCard(s),
        const SizedBox(height: 16),
        _section('관련 블로그 (${r.blogs.length})',
            Icons.menu_book_outlined, WaynaiTheme.sage),
        for (final b in r.blogs) _blogTile(b),
      ],
    );
  }

  Widget _section(String t, IconData icon, Color color) {
    return Padding(
      padding: const EdgeInsets.only(top: 4, bottom: 10),
      child: Row(
        children: [
          Icon(icon, size: 18, color: color),
          const SizedBox(width: 6),
          Text(t,
              style: const TextStyle(
                  fontWeight: FontWeight.w600,
                  color: WaynaiTheme.ocean)),
        ],
      ),
    );
  }

  Widget _spotCard(NearbySpot s) {
    return Card(
      margin: const EdgeInsets.only(bottom: 10),
      child: ListTile(
        leading: CircleAvatar(
          backgroundColor: WaynaiTheme.sand,
          foregroundColor: WaynaiTheme.terra,
          child: Text(
            s.distanceMeters == null
                ? '-'
                : '${s.distanceMeters}m',
            style: const TextStyle(fontSize: 11),
          ),
        ),
        title: Text(s.title,
            style: const TextStyle(fontWeight: FontWeight.w600)),
        subtitle: Text(
          [s.contentTypeLabel, s.address]
              .where((e) => e != null && e.isNotEmpty)
              .join(' · '),
          maxLines: 2,
          overflow: TextOverflow.ellipsis,
        ),
        trailing: const Icon(Icons.open_in_new, size: 18),
        onTap: () {
          final q = Uri.encodeComponent(s.title);
          final url = Uri.parse('https://map.naver.com/p/search/$q');
          launchUrl(url, mode: LaunchMode.externalApplication);
        },
      ),
    );
  }

  Widget _blogTile(NearbyBlog b) {
    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      child: ListTile(
        dense: true,
        leading: const Icon(Icons.article_outlined,
            color: WaynaiTheme.sage),
        title: Text(b.title,
            maxLines: 1, overflow: TextOverflow.ellipsis),
        subtitle: Text(
          b.description ?? '',
          maxLines: 2,
          overflow: TextOverflow.ellipsis,
          style: const TextStyle(fontSize: 12),
        ),
        onTap: () => launchUrl(Uri.parse(b.url),
            mode: LaunchMode.externalApplication),
      ),
    );
  }
}
