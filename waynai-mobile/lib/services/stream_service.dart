import 'dart:async';
import 'dart:convert';

import 'package:dio/dio.dart';

import '../models/travel_event.dart';
import 'api_client.dart';

/// /api/travel/plan/stream SSE 구독 서비스.
/// 백엔드가 text/event-stream 으로 보낸 `event:<type>\ndata:<json>\n\n` 블록을 파싱한다.
class StreamService {
  StreamService._internal();
  static final StreamService instance = StreamService._internal();

  final Dio _dio = ApiClient.instance.dio;

  /// 여행 플랜 스트림.
  /// 구독자는 반환된 Stream 을 listen 하면 TravelEvent 가 순차적으로 흘러나온다.
  Stream<TravelEvent> planStream(String query, {CancelToken? cancelToken}) {
    final controller = StreamController<TravelEvent>();

    Future<void> run() async {
      try {
        final resp = await _dio.get<ResponseBody>(
          '/api/travel/plan/stream',
          queryParameters: {'query': query},
          options: Options(
            responseType: ResponseType.stream,
            headers: {'Accept': 'text/event-stream'},
            receiveTimeout: const Duration(minutes: 5),
          ),
          cancelToken: cancelToken,
        );

        final body = resp.data!;
        final buffer = StringBuffer();
        final utf8Decoder = const Utf8Decoder(allowMalformed: true);

        await for (final chunk in body.stream) {
          buffer.write(utf8Decoder.convert(chunk));
          String text = buffer.toString();

          // SSE 이벤트 경계: 빈 줄(\n\n)
          while (true) {
            final sep = text.indexOf('\n\n');
            if (sep == -1) break;
            final rawEvent = text.substring(0, sep);
            text = text.substring(sep + 2);
            _parseBlock(rawEvent, controller);
          }
          buffer
            ..clear()
            ..write(text);
        }

        // 마지막 남은 버퍼 처리 (혹시 trailing block)
        final tail = buffer.toString().trim();
        if (tail.isNotEmpty) _parseBlock(tail, controller);

        await controller.close();
      } catch (e, st) {
        if (!controller.isClosed) {
          controller.addError(e, st);
          await controller.close();
        }
      }
    }

    // 구독 시작 시 실행
    controller.onListen = () {
      unawaited(run());
    };

    return controller.stream;
  }

  void _parseBlock(String rawEvent, StreamController<TravelEvent> controller) {
    String? eventName;
    final dataLines = <String>[];

    for (final line in rawEvent.split('\n')) {
      if (line.startsWith(':')) continue; // SSE 주석/heartbeat
      if (line.startsWith('event:')) {
        eventName = line.substring(6).trim();
      } else if (line.startsWith('data:')) {
        dataLines.add(line.substring(5).trim());
      }
    }
    if (eventName == null || dataLines.isEmpty) return;

    final dataStr = dataLines.join('\n');
    try {
      final json = jsonDecode(dataStr) as Map<String, dynamic>;
      controller.add(TravelEvent.fromJson(json));
    } catch (_) {
      // 비JSON 이벤트는 타입/스테이지만 담아 내보냄
      controller.add(TravelEvent(
        type: eventName,
        stage: 'raw',
        message: dataStr,
      ));
    }
  }
}
