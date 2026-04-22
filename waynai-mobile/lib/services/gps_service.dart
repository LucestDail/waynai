import '../models/nearby.dart';
import 'api_client.dart';

/// 모바일 GPS 기반 주변 추천.
class GpsService {
  GpsService._internal();
  static final GpsService instance = GpsService._internal();

  Future<NearbyResponse> nearby({
    required double lat,
    required double lng,
    int radiusMeters = 1000,
    String? contentTypeId,
    int limit = 15,
    String? context,
  }) async {
    final body = <String, dynamic>{
      'lat': lat,
      'lng': lng,
      'radiusMeters': radiusMeters,
      'limit': limit,
    };
    if (contentTypeId != null) body['contentTypeId'] = contentTypeId;
    if (context != null) body['context'] = context;

    final res = await ApiClient.instance.dio.post<Map<String, dynamic>>(
      '/api/gps/nearby',
      data: body,
    );
    return NearbyResponse.fromJson(res.data!);
  }
}
