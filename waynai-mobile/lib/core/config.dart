import 'package:flutter/foundation.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';

/// 런타임 설정 (API base URL 등).
/// 웹/iOS 시뮬레이터는 localhost, 안드로이드 에뮬레이터는 10.0.2.2 사용.
class AppConfig {
  const AppConfig._();

  static String get apiBaseUrl {
    final fromEnv = dotenv.maybeGet('API_BASE_URL');
    if (fromEnv != null && fromEnv.isNotEmpty) return fromEnv;

    if (kIsWeb) return 'http://localhost:8080';
    if (defaultTargetPlatform == TargetPlatform.android) {
      return 'http://10.0.2.2:8080';
    }
    return 'http://localhost:8080';
  }

  static Future<void> load() async {
    try {
      await dotenv.load(fileName: '.env');
    } catch (_) {
      // .env 가 없어도 플랫폼 기본 URL 로 폴백.
    }
  }
}
