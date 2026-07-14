$token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwidGVuYW50SWQiOjEsInJvbGUiOiJBRE1JTiIsImlhdCI6MTc4Mzk1MDY0NCwiZXhwIjoxNzg0MDM3MDQ0fQ.atrvx1f2_fpverXIH9KQ-FxnFmZmkvjHtyfXNEzAGcI"
$headers = @{ Authorization = "Bearer $token" }

Write-Host "=== Teacher Tools ==="
try {
    $r = Invoke-RestMethod 'http://localhost:8080/api/teacher/classes' -Headers $headers
    Write-Host "classes OK:" ($r | ConvertTo-Json -Depth 2 -Compress)
} catch {
    Write-Host "classes ERROR:" $_.Exception.Response.StatusCode.value__ $_.Exception.Message
}

try {
    $r = Invoke-RestMethod 'http://localhost:8080/api/teacher/tools/course-match?courseName=Java' -Headers $headers
    Write-Host "course-match OK:" ($r | ConvertTo-Json -Depth 2 -Compress)
} catch {
    Write-Host "course-match ERROR:" $_.Exception.Response.StatusCode.value__ $_.Exception.Message
}

try {
    $body = '[1,2]'
    $r = Invoke-RestMethod 'http://localhost:8080/api/teacher/tools/compare-classes' -Method Post -Body $body -ContentType 'application/json' -Headers $headers
    Write-Host "compare-classes OK:" ($r | ConvertTo-Json -Depth 2 -Compress)
} catch {
    Write-Host "compare-classes ERROR:" $_.Exception.Response.StatusCode.value__ $_.Exception.Message
}

Write-Host "`n=== HR Tools ==="
try {
    $body = '{"jdText":"Java开发工程师，负责后端开发"}'
    $r = Invoke-RestMethod 'http://localhost:8080/api/hr/tools/optimize-jd' -Method Post -Body $body -ContentType 'application/json' -Headers $headers
    Write-Host "optimize-jd OK:" ($r | ConvertTo-Json -Depth 2 -Compress)
} catch {
    Write-Host "optimize-jd ERROR:" $_.Exception.Response.StatusCode.value__ $_.Exception.Message
}

try {
    $body = '[1,2]'
    $r = Invoke-RestMethod 'http://localhost:8080/api/hr/tools/compare-talents' -Method Post -Body $body -ContentType 'application/json' -Headers $headers
    Write-Host "compare-talents OK:" ($r | ConvertTo-Json -Depth 2 -Compress)
} catch {
    Write-Host "compare-talents ERROR:" $_.Exception.Response.StatusCode.value__ $_.Exception.Message
}

try {
    $r = Invoke-RestMethod 'http://localhost:8080/api/hr/tools/salary-benchmark?jobTitle=Java' -Headers $headers
    Write-Host "salary-benchmark OK:" ($r | ConvertTo-Json -Depth 2 -Compress)
} catch {
    Write-Host "salary-benchmark ERROR:" $_.Exception.Response.StatusCode.value__ $_.Exception.Message
}

Write-Host "`n=== Student Tools ==="
try {
    $body = '[1,2]'
    $r = Invoke-RestMethod 'http://localhost:8080/api/student/tools/compare-jobs' -Method Post -Body $body -ContentType 'application/json' -Headers $headers
    Write-Host "compare-jobs OK:" ($r | ConvertTo-Json -Depth 2 -Compress)
} catch {
    Write-Host "compare-jobs ERROR:" $_.Exception.Response.StatusCode.value__ $_.Exception.Message
}
