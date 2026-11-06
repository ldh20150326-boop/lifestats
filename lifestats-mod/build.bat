@echo off
chcp 65001 >nul
setlocal
where java >nul 2>nul || (echo [오류] Java 21 이상이 필요합니다. & pause & exit /b 1)
where gradle >nul 2>nul || (echo [오류] Gradle이 설치되어 있지 않습니다. Gradle 8.14 이상을 설치하고 PATH에 추가하세요. & pause & exit /b 1)
echo LifeStats - Minecraft Fabric 1.21.8 빌드 시작...
gradle --no-daemon clean build
if errorlevel 1 (
  echo.
  echo [실패] 빌드에 실패했습니다. 위의 오류 메시지를 확인하세요.
  pause
  exit /b 1
)
echo.
echo [완료] build\libs 폴더에서 lifestats-1.0.0.jar 파일을 확인하세요.
pause
