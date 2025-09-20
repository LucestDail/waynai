#!/bin/bash

# 즉시 PATH 수정 스크립트
# 현재 세션에서 바로 실행

echo "🔧 즉시 PATH 수정 중..."

# 현재 PATH에 기본 경로 추가
export PATH="/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:$PATH"

# 명령어 테스트
echo "📋 명령어 테스트:"
if command -v lesspipe >/dev/null 2>&1; then
    echo "✅ lesspipe: $(which lesspipe)"
else
    echo "❌ lesspipe: 명령어를 찾을 수 없음"
    echo "   직접 경로: /usr/bin/lesspipe"
fi

if command -v dircolors >/dev/null 2>&1; then
    echo "✅ dircolors: $(which dircolors)"
else
    echo "❌ dircolors: 명령어를 찾을 수 없음"
    echo "   직접 경로: /bin/dircolors"
fi

echo ""
echo "현재 PATH: $PATH"
echo "✅ PATH 수정 완료!"
