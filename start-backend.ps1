# Start occupation-platform backend (PowerShell)
# Usage: cd F:\CSU_Workin\abc; .\start-backend.ps1

Set-Location $PSScriptRoot

$env:DB_URL = "jdbc:mysql://localhost:3307/occupation?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
$env:DB_PASSWORD = "root"

Write-Host "[1/2] Building modules..." -ForegroundColor Cyan
mvn install -pl occupation-web -am -DskipTests -q
if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed." -ForegroundColor Red
    exit 1
}

Write-Host "[2/2] Starting Spring Boot on http://localhost:8080 ..." -ForegroundColor Cyan
mvn spring-boot:run -pl occupation-web -DskipTests
