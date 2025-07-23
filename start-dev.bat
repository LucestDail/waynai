@echo off
chcp 65001 >nul
echo 🚀 WaynAI 개발 서버를 시작합니다...

REM 현재 디렉토리를 프로젝트 루트로 설정
cd /d "%~dp0"

REM 스프링 백엔드 시작
echo 📦 스프링 백엔드를 시작합니다...
cd waynai-backend
start "Spring Boot Backend" cmd /k "mvnw spring-boot:run"
cd ..

REM 백엔드 서버가 준비될 때까지 대기하는 함수
:wait_for_backend
echo ⏳ 백엔드 서버가 시작될 때까지 대기 중...
set /a attempts=0
set max_attempts=30

:check_backend
curl -s http://localhost:8080/actuator/health >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ 백엔드 서버가 정상적으로 시작되었습니다!
    goto start_frontend
)

set /a attempts+=1
if %attempts% geq %max_attempts% (
    echo ❌ 백엔드 서버 시작 실패 (최대 대기 시간 초과)
    pause
    exit /b 1
)

echo 🔄 백엔드 서버 준비 중... (%attempts%/%max_attempts%)
timeout /t 2 /nobreak >nul
goto check_backend

:start_frontend
REM Vue 프론트엔드 시작
echo 🎨 Vue 프론트엔드를 시작합니다...
cd waynai-frontend
start "Vue Frontend" cmd /k "npm run dev >nul 2>&1"
cd ..

REM 프론트엔드 서버가 준비될 때까지 대기
echo ⏳ 프론트엔드 서버가 시작될 때까지 대기 중...
timeout /t 5 /nobreak >nul

REM 프론트엔드 서버 상태 확인
curl -s http://localhost:5173 >nul 2>&1
if %errorlevel% equ 0 (
    echo.
    echo 🎉 모든 서버가 정상적으로 시작되었습니다!
    echo 📱 프론트엔드: http://localhost:5173
    echo 🔧 백엔드: http://localhost:8080
    echo 📊 백엔드 상태: http://localhost:8080/actuator/health
    echo.
    echo 서버를 종료하려면 각 터미널 창을 닫으세요.
) else (
    echo ⚠️  프론트엔드 서버 시작에 문제가 있을 수 있습니다.
    echo 📱 프론트엔드: http://localhost:5173 (수동으로 확인해주세요)
    echo 🔧 백엔드: http://localhost:8080
)

pause 