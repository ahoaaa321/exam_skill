# Round-3 retests after generated-key fix
$ErrorActionPreference = 'Continue'
$base = 'http://localhost:8080'
$pass = 0; $fail = 0
function Req($method, $path, $token, $bodyObj) {
    $headers = @{}
    if ($token) { $headers['Authorization'] = "Bearer $token" }
    $json = $null
    if ($bodyObj -ne $null) { $json = [System.Text.Encoding]::UTF8.GetBytes(($bodyObj | ConvertTo-Json -Compress -Depth 6)) }
    try {
        $r = Invoke-RestMethod -Uri "$base$path" -Method $method -Headers $headers -ContentType 'application/json;charset=utf-8' -Body $json
        return @{ ok = $true; status = 200; data = $r.data }
    } catch {
        $sc = if ($_.Exception.Response) { [int]$_.Exception.Response.StatusCode } else { -1 }
        return @{ ok = $false; status = $sc; data = $null }
    }
}
function Check($name, $cond, $detail) {
    if ($cond) { $script:pass++; "PASS  $name $detail" }
    else { $script:fail++; "FAIL  $name $detail" }
}
function Login($u,$p) { (Req 'POST' '/api/auth/login' $null @{ username = $u; password = $p }).data.token }

$tA = Login 'uifr1a' '123456'
$tB = Login 'uifr1b' 'abc12345'
$tAdm = Login 'admin' '123456'

$planBody = @{
    planName = 'E2E-CAP-PLAN2'; planCode = 'E2ECAP0002'; tradeId = 1; levelId = 1;
    registerStartTime = '2026-10-01T00:00:00'; registerEndTime = '2026-10-31T23:59:59';
    examTime = '2026-11-20T09:00:00'; maxCandidates = 1; fee = 0; conditionDesc = 'e2e2'
}
$r = Req 'POST' '/api/admin/plans' $tAdm $planBody
$capPlanId = $r.data.id
Check 'R1 create returns generated id' ($r.ok -and $capPlanId -gt 0) "id=$capPlanId"
$r = Req 'PUT' "/api/admin/plans/$capPlanId/status?status=1" $tAdm $null
Check 'R2 publish via returned id' ($r.ok -and $r.data.status -eq 1) "status=$($r.status)/$($r.data.status)"
$r = Req 'POST' '/api/registrations' $tA @{ planId = $capPlanId }
$capRegA = $r.data.id
Check 'R3 A takes last seat' ($r.ok -and $capRegA -gt 0) "regId=$capRegA"
$r = Req 'POST' '/api/registrations' $tB @{ planId = $capPlanId }
Check 'R4 B over capacity 400' (-not $r.ok -and $r.status -eq 400) "status=$($r.status)"
$r = Req 'PUT' "/api/my/registrations/$capRegA/cancel" $tA $null
Check 'R5 A cancel 200' ($r.ok) "status=$($r.status)"
$r = Req 'POST' '/api/registrations' $tB @{ planId = $capPlanId }
$capRegB = $r.data.id
Check 'R6 B takes freed seat' ($r.ok -and $capRegB -gt 0) "regId=$capRegB"
$r = Req 'PUT' "/api/my/registrations/$capRegB/cancel" $tB $null
Check 'R7 B cancel 200' ($r.ok) "status=$($r.status)"
$r = Req 'GET' "/api/admin/plans" $tAdm $null
$planNow = $r.data | Where-Object { $_.id -eq $capPlanId }
Check 'R8 count back to 0' ($planNow.currentCount -eq 0) "currentCount=$($planNow.currentCount)"

# B5d re-check: timeline reason via raw JSON
$r = Req 'GET' '/api/registrations/2908/timeline' $tB $null
$raw = ($r.data | ConvertTo-Json -Compress -Depth 5)
Check 'R9 timeline contains reject reason' ($r.ok -and $raw -match 'e2e reject reason') "status=$($r.status)"

"RESULT pass=$pass fail=$fail"
"CAP2_PLAN_ID=$capPlanId"
