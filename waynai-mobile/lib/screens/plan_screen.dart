import 'dart:async';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';

import '../core/theme.dart';
import '../models/travel_event.dart';
import '../services/stream_service.dart';

/// 여행 플랜 SSE 화면.
/// - 상단 타임라인(스테이지 진행)
/// - 하단 토큰 누적 텍스트(스트리밍)
class PlanScreen extends StatefulWidget {
  const PlanScreen({super.key, required this.query});

  final String query;

  @override
  State<PlanScreen> createState() => _PlanScreenState();
}

class _PlanScreenState extends State<PlanScreen> {
  final List<TravelEvent> _stages = [];
  final StringBuffer _text = StringBuffer();
  final CancelToken _cancel = CancelToken();
  StreamSubscription<TravelEvent>? _sub;

  String? _model;
  String? _error;
  bool _done = false;

  @override
  void initState() {
    super.initState();
    _start();
  }

  void _start() {
    final stream = StreamService.instance
        .planStream(widget.query, cancelToken: _cancel);
    _sub = stream.listen(
      _handle,
      onError: (e) {
        setState(() {
          _error = e.toString();
          _done = true;
        });
      },
      onDone: () => setState(() => _done = true),
    );
  }

  void _handle(TravelEvent e) {
    setState(() {
      switch (e.type) {
        case 'stage':
        case 'intent':
        case 'sources.tour':
        case 'sources.naver':
          _stages.add(e);
          break;
        case 'model':
          _model = e.payload?['model'] as String?;
          _stages.add(e);
          break;
        case 'token':
          final delta = e.tokenDelta;
          if (delta.isNotEmpty) {
            _text.write(delta);
          } else if (e.tokenText.isNotEmpty) {
            _text
              ..clear()
              ..write(e.tokenText);
          }
          break;
        case 'plan':
          final text = e.payload?['text'] as String?;
          if (text != null && text.isNotEmpty) {
            _text
              ..clear()
              ..write(text);
          }
          break;
        case 'done':
          _done = true;
          break;
        case 'error':
          _error = e.message;
          _done = true;
          break;
      }
    });
  }

  @override
  void dispose() {
    _sub?.cancel();
    if (!_cancel.isCancelled) _cancel.cancel('dispose');
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(
          widget.query,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          style: const TextStyle(fontSize: 16),
        ),
      ),
      body: Column(
        children: [
          _stageList(),
          const Divider(height: 1),
          Expanded(child: _planView()),
        ],
      ),
    );
  }

  Widget _stageList() {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
      color: WaynaiTheme.cream,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(Icons.timeline,
                  size: 18, color: WaynaiTheme.terra),
              const SizedBox(width: 6),
              const Text(
                '진행 상태',
                style: TextStyle(
                  fontWeight: FontWeight.w600,
                  color: WaynaiTheme.ocean,
                ),
              ),
              const Spacer(),
              if (_model != null)
                Container(
                  padding: const EdgeInsets.symmetric(
                      horizontal: 8, vertical: 4),
                  decoration: BoxDecoration(
                    color: WaynaiTheme.sand,
                    borderRadius: BorderRadius.circular(999),
                  ),
                  child: Text(
                    _model!,
                    style: const TextStyle(
                      fontSize: 11,
                      color: WaynaiTheme.terra,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ),
            ],
          ),
          const SizedBox(height: 8),
          ConstrainedBox(
            constraints: const BoxConstraints(maxHeight: 160),
            child: ListView.builder(
              shrinkWrap: true,
              itemCount: _stages.length,
              itemBuilder: (_, i) {
                final s = _stages[i];
                return Padding(
                  padding: const EdgeInsets.symmetric(vertical: 4),
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Padding(
                        padding: EdgeInsets.only(top: 5, right: 8),
                        child: Icon(Icons.circle,
                            size: 8, color: WaynaiTheme.sage),
                      ),
                      Expanded(
                        child: Text(
                          '${s.stage} · ${s.message}',
                          style: const TextStyle(
                              color: WaynaiTheme.dusk, fontSize: 13),
                        ),
                      ),
                    ],
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }

  Widget _planView() {
    if (_error != null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Text(
            '오류: $_error',
            style: const TextStyle(color: WaynaiTheme.terra),
          ),
        ),
      );
    }
    if (_text.isEmpty && !_done) {
      return const Center(
        child: Padding(
          padding: EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              CircularProgressIndicator(color: WaynaiTheme.ocean),
              SizedBox(height: 12),
              Text('AI 가 준비 중입니다...',
                  style: TextStyle(color: WaynaiTheme.dusk)),
            ],
          ),
        ),
      );
    }
    return Scrollbar(
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: SelectableText(
          _text.toString(),
          style: const TextStyle(
            height: 1.7,
            color: WaynaiTheme.ocean,
            fontSize: 15,
          ),
        ),
      ),
    );
  }
}
