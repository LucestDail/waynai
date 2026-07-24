import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/travel-plan',
      name: 'travel-plan',
      component: () => import('../views/TravelPlanView.vue'),
    },
    {
      path: '/about',
      name: 'about',
      component: () => import('../views/AboutView.vue'),
    },
    // 단순화(2026-07): 관광지 검색/추천 화면 제거 → 홈 바로시작 + 세부 계획에 집중.
    { path: '/tourist-info', redirect: '/' },
    { path: '/recommendations', redirect: '/' },
  ],
})

export default router
