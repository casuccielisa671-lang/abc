Write-Host "请手动操作：" -ForegroundColor Yellow
Write-Host ""
Write-Host "1. 打开 Docker Desktop" -ForegroundColor White
Write-Host "2. 点击右上角齿轮图标（Settings）" -ForegroundColor White
Write-Host "3. 左侧选择 Docker Engine" -ForegroundColor White
Write-Host "4. 在 JSON 配置中添加 registry-mirrors：" -ForegroundColor White
Write-Host ""
Write-Host '{' -ForegroundColor Green
Write-Host '  "registry-mirrors": [' -ForegroundColor Green
Write-Host '    "https://docker.1ms.run",' -ForegroundColor Green
Write-Host '    "https://docker.xuanyuan.me"' -ForegroundColor Green
Write-Host '  ]' -ForegroundColor Green
Write-Host '}' -ForegroundColor Green
Write-Host ""
Write-Host "5. 点击 Apply & Restart" -ForegroundColor White
Write-Host "6. 等 Docker 重启完成后，告诉我继续" -ForegroundColor White
