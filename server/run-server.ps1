Write-Host "========================================" -ForegroundColor Cyan
Write-Host "    AxionBank Server Startup Script" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Starting AxionBank Server..." -ForegroundColor Green
Write-Host "Database: PostgreSQL on localhost:5433" -ForegroundColor Yellow
Write-Host "Server will be available at: http://localhost:8080" -ForegroundColor Yellow
Write-Host ""
Write-Host "Press Ctrl+C to stop the server" -ForegroundColor Red
Write-Host ""

# Change to the parent directory (Axio Bank root)
$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Definition
$parentPath = Split-Path -Parent $scriptPath
Set-Location $parentPath

# Run the server
& ./gradlew.bat :server:run

Write-Host ""
Write-Host "Press any key to continue..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")