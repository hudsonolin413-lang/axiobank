@echo off
echo ========================================
echo Starting Axio Bank Server with Email
echo ========================================
echo.
echo Configuring email: abrocoder@gmail.com
echo.

set SMTP_HOST=smtp.gmail.com
set SMTP_PORT=587
set SMTP_USERNAME=abrocoder@gmail.com
set SMTP_PASSWORD=qbdpvggzgslrqwvz
set SMTP_FROM_NAME=Axio Bank
set PORT=8081

echo Email configured successfully!
echo Starting server on port 8081...
echo.

gradlew.bat :server:run --no-daemon
