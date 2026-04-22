/// 백엔드 SSE 타입드 이벤트 `TravelEvent` 의 Dart 미러.
/// event 필드 값: stage | intent | sources.tour | sources.naver | model | token | plan | done | error
class TravelEvent {
  const TravelEvent({
    required this.type,
    required this.stage,
    required this.message,
    this.payload,
  });

  final String type;
  final String stage;
  final String message;
  final Map<String, dynamic>? payload;

  factory TravelEvent.fromJson(Map<String, dynamic> json) {
    return TravelEvent(
      type: (json['type'] ?? '') as String,
      stage: (json['stage'] ?? '') as String,
      message: (json['message'] ?? '') as String,
      payload: (json['payload'] as Map?)?.cast<String, dynamic>(),
    );
  }

  /// token 이벤트 payload 의 누적 텍스트 꺼내기 편의.
  String get tokenDelta => (payload?['delta'] ?? '') as String;
  String get tokenText => (payload?['text'] ?? '') as String;

  @override
  String toString() => 'TravelEvent($type/$stage: $message)';
}
