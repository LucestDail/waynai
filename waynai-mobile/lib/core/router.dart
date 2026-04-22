import 'package:go_router/go_router.dart';

import '../screens/home_screen.dart';
import '../screens/nearby_screen.dart';
import '../screens/plan_screen.dart';

/// 앱 라우팅.
final GoRouter appRouter = GoRouter(
  initialLocation: '/',
  routes: [
    GoRoute(
      path: '/',
      name: 'home',
      builder: (context, state) => const HomeScreen(),
    ),
    GoRoute(
      path: '/plan',
      name: 'plan',
      builder: (context, state) {
        final q = state.uri.queryParameters['q'] ?? '';
        return PlanScreen(query: q);
      },
    ),
    GoRoute(
      path: '/nearby',
      name: 'nearby',
      builder: (context, state) => const NearbyScreen(),
    ),
  ],
);
