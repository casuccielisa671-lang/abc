param(
    [switch]$SkipBuildData
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

if (-not $SkipBuildData) {
    Write-Host "Regenerating seed data..."
    node scripts/gen-mock-jobs.js
    node scripts/gen-seed-data.js
}

Write-Host "Resetting Docker development database volume..."
docker compose down -v
docker compose up -d mysql redis zookeeper kafka

Write-Host "Waiting for MySQL health check..."
$deadline = (Get-Date).AddMinutes(3)
do {
    Start-Sleep -Seconds 3
    $status = docker inspect -f "{{.State.Health.Status}}" occupation-mysql 2>$null
    Write-Host "MySQL status: $status"
    if ($status -eq "healthy") {
        Write-Host "Database initialized from occupation-common/src/main/resources/sql/init.sql"
        exit 0
    }
} while ((Get-Date) -lt $deadline)

throw "MySQL did not become healthy within 3 minutes. Check docker logs occupation-mysql."
