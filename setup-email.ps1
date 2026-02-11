# Email Configuration for Axio Bank Notifications
# Gmail App Password Setup

$env:SMTP_HOST="smtp.gmail.com"
$env:SMTP_PORT="587"
$env:SMTP_USERNAME="abrocoder@gmail.com"
$env:SMTP_PASSWORD="qbdpvggzgslrqwvz"
$env:SMTP_FROM_NAME="Axio Bank"

Write-Host "✅ Email environment variables configured!" -ForegroundColor Green
Write-Host ""
Write-Host "SMTP Configuration:" -ForegroundColor Cyan
Write-Host "  Host: smtp.gmail.com"
Write-Host "  Port: 587"
Write-Host "  From: abrocoder@gmail.com"
Write-Host "  Name: Axio Bank"
Write-Host ""
Write-Host "Starting server with email notifications enabled..." -ForegroundColor Yellow
Write-Host ""

# Start the server
Set-Location "C:\Users\ADMIN\AxionBank\Axio Bank"
./gradlew run
