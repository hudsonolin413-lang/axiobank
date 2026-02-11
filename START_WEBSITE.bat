@echo off
cls
echo ========================================
echo   AXION BANK - Website  Server
echo ========================================
echo.
echo Starting the Axion Bank server...
echo.
echo The website will be available at:
echo    http://localhost:8081
echo.
echo The server includes:
echo  - Website (Home page)
echo  - Banking API endpoints
echo  - Database connections
echo.
echo Press Ctrl+C to stop the server
echo ========================================
echo.

cd /d "%~dp0"
call gradlew.bat :server:run

pause
