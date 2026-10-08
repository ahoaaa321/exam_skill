# Final post-cleanup smoke
function Login($u) {
    $r = Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post -ContentType 'application/json' -Body ('{"username":"' + $u + '","password":"123456"}')
    return $r.data.token
}
$pairs = @(
    @('candidate', '/api/system/me'),
    @('admin', '/api/statistics'),
    @('staff', '/api/signins'),
    @('super', '/api/system/users')
)
foreach ($pair in $pairs) {
    $t = Login $pair[0]
    try {
        Invoke-RestMethod -Uri ("http://localhost:8080" + $pair[1]) -Headers @{ Authorization = "Bearer $t" } | Out-Null
        "SMOKE $($pair[0]) $($pair[1]) => 200"
    } catch {
        "SMOKE $($pair[0]) => $([int]$_.Exception.Response.StatusCode)"
    }
}
try {
    Invoke-RestMethod -Uri 'http://localhost:8080/api/public/certificates/verify?no=ZSZN-2026-002907' | Out-Null
    "deleted cert verify => 200 (UNEXPECTED)"
} catch {
    "deleted cert verify => $([int]$_.Exception.Response.StatusCode) (expect 404)"
}
