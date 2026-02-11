@echo off
echo ========================================
echo    AxionBank Server Startup Script
echo ========================================
echo.
echo Starting AxionBank Server...
echo Database: PostgreSQL on localhost:5433
echo Server will be available at: http://localhost:8080
echo.
echo Press Ctrl+C to stop the server
echo.

cd /d "%~dp0.."
call gradlew.bat :server:run

pause