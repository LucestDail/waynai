import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:waynai_mobile/core/theme.dart';

void main() {
  testWidgets('테마 로드 smoke 테스트', (tester) async {
    await tester.pumpWidget(
      ProviderScope(
        child: MaterialApp(
          theme: WaynaiTheme.light(),
          home: const Scaffold(body: Text('WaynAI')),
        ),
      ),
    );
    expect(find.text('WaynAI'), findsOneWidget);
  });
}
