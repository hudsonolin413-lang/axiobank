@echo off
echo ========================================
echo Starting Axion Bank Backend Server
echo ========================================
echo.
echo Server will start on: http://localhost:8081
echo.
echo Press Ctrl+C to stop the server
echo ========================================
echo.

cd /d "%~dp0"
call gradlew.bat :server:run

pause
