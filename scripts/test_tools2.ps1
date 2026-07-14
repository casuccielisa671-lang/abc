# 测试工具箱接口
$ErrorActionPreference = "Continue"

# 登录获取 Token
$loginBody = @{username='admin';password='admin123';tenantName='测试学院'} | ConvertTo-Json -Compress
Write-Host "=== 登录 ==="
$loginResp = Invoke-RestMethod 'http://localhost:8080/api/auth/login' -Method Post -Body $loginBody -ContentType 'application/json; charset=utf-8'
$token = $loginResp.data.token
$role = $loginResp.data.role
Write-Host "Token: $token"
Write-Host "Role: $role"
$headers = @{Authorization = "Bearer $token"}

# 1. 测试 /api/teacher/classes
Write-Host "`n=== 1. GET /api/teacher/classes ==="
try {
    $r = Invoke-RestMethod 'http://localhost:8080/api/teacher/classes' -Headers $headers
    Write-Host "OK: $($r | ConvertTo-Json -Depth 2 -Compress)"
} catch {
    Write-Host "FAILED: $($_.Exception.Message)"
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $body = $reader.ReadToEnd()
        Write-Host "Response Body: $body"
    }
}

# 2. 测试 /api/teacher/tools/course-match
Write-Host "`n=== 2. GET /api/teacher/tools/course-match?courseName=Java ==="
try {
    $r = Invoke-RestMethod 'http://localhost:8080/api/teacher/tools/course-match?courseName=Java' -Headers $headers
    Write-Host "OK: $($r | ConvertTo-Json -Depth 3 -Compress)"
} catch {
    Write-Host "FAILED: $($_.Exception.Message)"
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $body = $reader.ReadToEnd()
        Write-Host "Response Body: $body"
    }
}

# 3. 测试 /api/teacher/tools/compare-classes
Write-Host "`n=== 3. POST /api/teacher/tools/compare-classes ==="
$body = '[1,2]'
try {
    $r = Invoke-RestMethod 'http://localhost:8080/api/teacher/tools/compare-classes' -Method Post -Body $body -ContentType 'application/json; charset=utf-8' -Headers $headers
    Write-Host "OK: $($r | ConvertTo-Json -Depth 3 -Compress)"
} catch {
    Write-Host "FAILED: $($_.Exception.Message)"
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $body = $reader.ReadToEnd()
        Write-Host "Response Body: $body"
    }
}

# 4. 测试 /api/teacher/tools/student-alerts
Write-Host "`n=== 4. GET /api/teacher/tools/student-alerts ==="
try {
    $r = Invoke-RestMethod 'http://localhost:8080/api/teacher/tools/student-alerts' -Headers $headers
    Write-Host "OK: $($r | ConvertTo-Json -Depth 3 -Compress)"
} catch {
    Write-Host "FAILED: $($_.Exception.Message)"
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $body = $reader.ReadToEnd()
        Write-Host "Response Body: $body"
    }
}

# 5. 测试 HR 接口 - 需要 HR 角色
Write-Host "`n=== 5. GET /api/hr/tools/salary-benchmark?jobTitle=Java ==="
try {
    $r = Invoke-RestMethod 'http://localhost:8080/api/hr/tools/salary-benchmark?jobTitle=Java' -Headers $headers
    Write-Host "OK: $($r | ConvertTo-Json -Depth 3 -Compress)"
} catch {
    Write-Host "FAILED: $($_.Exception.Message)"
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $body = $reader.ReadToEnd()
        Write-Host "Response Body: $body"
    }
}

# 6. 测试 /api/teacher/overview
Write-Host "`n=== 6. GET /api/teacher/overview ==="
try {
    $r = Invoke-RestMethod 'http://localhost:8080/api/teacher/overview' -Headers $headers
    Write-Host "OK: $($r | ConvertTo-Json -Depth 2 -Compress)"
} catch {
    Write-Host "FAILED: $($_.Exception.Message)"
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $body = $reader.ReadToEnd()
        Write-Host "Response Body: $body"
    }
}
