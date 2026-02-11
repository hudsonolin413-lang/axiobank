# Start Server with Email Configuration on Port 8081
$env:SMTP_HOST="smtp.gmail.com"
$env:SMTP_PORT="587"
$env:SMTP_USERNAME="abrocoder@gmail.com"
$env:SMTP_PASSWORD="qbdpvggzgslrqwvz"
$env:SMTP_FROM_NAME="Axio Bank"
$env:PORT="8081"

Write-Host "✅ Email configured: abrocoder@gmail.com via Gmail SMTP" -ForegroundColor Green
Write-Host "✅ Server will start on port 8081 (port 8080 is busy)" -ForegroundColor Yellow
Write-Host "Starting server..." -ForegroundColor Yellow

Set-Location "C:\Users\ADMIN\AxionBank\Axio Bank"
./gradlew :server:run --no-daemon
