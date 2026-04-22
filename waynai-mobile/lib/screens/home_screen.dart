import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../core/theme.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  final _controller = TextEditingController();

  static const List<String> _presets = [
    '서울 2박3일 전통과 트렌드 여행',
    '제주 3박4일 자연 힐링 코스',
    '부산 당일치기 바다와 맛집',
    '강릉 1박2일 커피 감성 여행',
    '경주 1박2일 역사 유적 탐방',
  ];

  void _submit(String q) {
    final query = q.trim();
    if (query.isEmpty) return;
    context.pushNamed('plan', queryParameters: {'q': query});
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('WaynAI'),
        actions: [
          IconButton(
            tooltip: '주변 관광지',
            icon: const Icon(Icons.location_on_outlined),
            onPressed: () => context.pushNamed('nearby'),
          ),
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.fromLTRB(20, 8, 20, 40),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _hero(),
            const SizedBox(height: 24),
            _searchCard(),
            const SizedBox(height: 28),
            _sectionTitle('빠른 시작'),
            const SizedBox(height: 12),
            _presetWrap(),
            const SizedBox(height: 28),
            _nearbyTile(),
          ],
        ),
      ),
    );
  }

  Widget _hero() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          padding:
              const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
          decoration: BoxDecoration(
            color: WaynaiTheme.sand,
            borderRadius: BorderRadius.circular(999),
          ),
          child: const Text(
            'AI 여행 설계사',
            style: TextStyle(
              color: WaynaiTheme.terra,
              fontWeight: FontWeight.w600,
            ),
          ),
        ),
        const SizedBox(height: 12),
        const Text(
          '어디로 떠나고 싶으세요?',
          style: TextStyle(
            fontSize: 28,
            fontWeight: FontWeight.w700,
            height: 1.2,
            color: WaynaiTheme.ocean,
          ),
        ),
        const SizedBox(height: 8),
        const Text(
          'AI 가 관광공사 공식 데이터와 네이버 블로그를 병렬로 수집해\n맞춤형 일정을 생성해드립니다.',
          style: TextStyle(
            color: WaynaiTheme.dusk,
            height: 1.5,
          ),
        ),
      ],
    );
  }

  Widget _searchCard() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            TextField(
              controller: _controller,
              onSubmitted: _submit,
              textInputAction: TextInputAction.search,
              decoration: const InputDecoration(
                hintText: '예: 제주 3박4일 바다와 카페 투어',
                prefixIcon: Icon(Icons.search),
              ),
            ),
            const SizedBox(height: 12),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton.icon(
                icon: const Icon(Icons.auto_awesome),
                label: const Text('여행 계획 받기'),
                onPressed: () => _submit(_controller.text),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _sectionTitle(String t) => Text(
        t,
        style: const TextStyle(
          fontSize: 16,
          fontWeight: FontWeight.w600,
          color: WaynaiTheme.ocean,
        ),
      );

  Widget _presetWrap() {
    return Wrap(
      spacing: 8,
      runSpacing: 8,
      children: [
        for (final p in _presets)
          ActionChip(
            label: Text(p),
            onPressed: () {
              _controller.text = p;
              _submit(p);
            },
          ),
      ],
    );
  }

  Widget _nearbyTile() {
    return Card(
      child: ListTile(
        leading: const CircleAvatar(
          backgroundColor: WaynaiTheme.ocean,
          foregroundColor: WaynaiTheme.cream,
          child: Icon(Icons.place_outlined),
        ),
        title: const Text(
          '내 주변 실시간 추천',
          style: TextStyle(fontWeight: FontWeight.w600),
        ),
        subtitle: const Text('GPS 기반 관광지 + 네이버 블로그 병합'),
        trailing: const Icon(Icons.chevron_right),
        onTap: () => context.pushNamed('nearby'),
      ),
    );
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }
}
