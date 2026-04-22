import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'core/config.dart';
import 'core/router.dart';
import 'core/theme.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await AppConfig.load();
  runApp(const ProviderScope(child: WaynaiApp()));
}

class WaynaiApp extends StatelessWidget {
  const WaynaiApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp.router(
      title: 'WaynAI',
      debugShowCheckedModeBanner: false,
      theme: WaynaiTheme.light(),
      routerConfig: appRouter,
    );
  }
}
