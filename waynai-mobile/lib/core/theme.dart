import 'package:flutter/material.dart';

/// WaynAI 모바일 테마 — 웹 디자인 시스템과 동일한 팔레트(ocean/cream/amber/terra).
class WaynaiTheme {
  const WaynaiTheme._();

  static const Color ocean = Color(0xFF0B2B40);
  static const Color dusk = Color(0xFF1E3A52);
  static const Color sky = Color(0xFF5E8CA8);
  static const Color sand = Color(0xFFF1E5D1);
  static const Color cream = Color(0xFFFAF4EA);
  static const Color terra = Color(0xFFBF5B3F);
  static const Color amber = Color(0xFFE8A94C);
  static const Color sage = Color(0xFF7E9B80);

  static ThemeData light() {
    final base = ThemeData.light(useMaterial3: true);
    return base.copyWith(
      scaffoldBackgroundColor: cream,
      colorScheme: ColorScheme.fromSeed(
        seedColor: ocean,
        brightness: Brightness.light,
        primary: ocean,
        onPrimary: cream,
        secondary: amber,
        onSecondary: ocean,
        tertiary: terra,
        surface: cream,
        onSurface: ocean,
      ),
      textTheme: base.textTheme.apply(
        bodyColor: ocean,
        displayColor: ocean,
      ),
      appBarTheme: const AppBarTheme(
        backgroundColor: cream,
        foregroundColor: ocean,
        centerTitle: false,
        elevation: 0,
        scrolledUnderElevation: 0,
        titleTextStyle: TextStyle(
          color: ocean,
          fontSize: 20,
          fontWeight: FontWeight.w600,
          letterSpacing: -0.2,
        ),
      ),
      cardTheme: const CardThemeData(
        color: Colors.white,
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.all(Radius.circular(22)),
          side: BorderSide(color: Color(0xFFE6DFD0)),
        ),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: Colors.white,
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(18),
          borderSide: const BorderSide(color: Color(0xFFE6DFD0)),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(18),
          borderSide: const BorderSide(color: Color(0xFFE6DFD0)),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(18),
          borderSide: const BorderSide(color: ocean, width: 1.5),
        ),
        contentPadding:
            const EdgeInsets.symmetric(horizontal: 18, vertical: 14),
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: ocean,
          foregroundColor: cream,
          padding: const EdgeInsets.symmetric(horizontal: 22, vertical: 16),
          shape: const StadiumBorder(),
          textStyle: const TextStyle(fontWeight: FontWeight.w600),
        ),
      ),
      chipTheme: ChipThemeData(
        backgroundColor: Colors.white,
        labelStyle: const TextStyle(color: ocean),
        side: const BorderSide(color: Color(0xFFE6DFD0)),
        shape: const StadiumBorder(),
      ),
    );
  }
}
