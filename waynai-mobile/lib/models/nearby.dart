/// /api/gps/nearby 응답 모델.
class NearbyResponse {
  const NearbyResponse({
    required this.lat,
    required this.lng,
    required this.radiusMeters,
    required this.spots,
    required this.blogs,
  });

  final double lat;
  final double lng;
  final int radiusMeters;
  final List<NearbySpot> spots;
  final List<NearbyBlog> blogs;

  factory NearbyResponse.fromJson(Map<String, dynamic> json) {
    return NearbyResponse(
      lat: (json['lat'] as num).toDouble(),
      lng: (json['lng'] as num).toDouble(),
      radiusMeters: (json['radiusMeters'] as num).toInt(),
      spots: ((json['spots'] as List?) ?? const [])
          .map((e) => NearbySpot.fromJson(e as Map<String, dynamic>))
          .toList(),
      blogs: ((json['blogs'] as List?) ?? const [])
          .map((e) => NearbyBlog.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }
}

class NearbySpot {
  const NearbySpot({
    this.id,
    required this.title,
    this.address,
    this.category,
    this.contentTypeLabel,
    this.lat,
    this.lng,
    this.distanceMeters,
    this.thumbnail,
    this.tel,
  });

  final String? id;
  final String title;
  final String? address;
  final String? category;
  final String? contentTypeLabel;
  final double? lat;
  final double? lng;
  final int? distanceMeters;
  final String? thumbnail;
  final String? tel;

  factory NearbySpot.fromJson(Map<String, dynamic> json) {
    return NearbySpot(
      id: json['id'] as String?,
      title: (json['title'] ?? '') as String,
      address: json['address'] as String?,
      category: json['category'] as String?,
      contentTypeLabel: json['contentTypeLabel'] as String?,
      lat: (json['lat'] as num?)?.toDouble(),
      lng: (json['lng'] as num?)?.toDouble(),
      distanceMeters: (json['distanceMeters'] as num?)?.toInt(),
      thumbnail: json['thumbnail'] as String?,
      tel: json['tel'] as String?,
    );
  }
}

class NearbyBlog {
  const NearbyBlog({
    required this.title,
    required this.url,
    this.description,
    this.bloggerName,
    this.postdate,
  });

  final String title;
  final String url;
  final String? description;
  final String? bloggerName;
  final String? postdate;

  factory NearbyBlog.fromJson(Map<String, dynamic> json) {
    return NearbyBlog(
      title: (json['title'] ?? '') as String,
      url: (json['url'] ?? '') as String,
      description: json['description'] as String?,
      bloggerName: json['bloggerName'] as String?,
      postdate: json['postdate'] as String?,
    );
  }
}
