# Round-3 end-to-end + edge-case verification (real HTTP, ASCII only)
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
function Login($u) { (Req 'POST' '/api/auth/login' $null @{ username = $u; password = '123456' }).data.token }

$tA = Login 'uifr1a'
$tB = Login 'uifr1b'
$tAdm = Login 'admin'
$tSt = Login 'staff'

# ========== Part A: full chain on reg 2907 (paid, plan 7) ==========
$r = Req 'PUT' '/api/registrations/2907/audit' $tAdm @{ approved = $true; reason = ''; secondAudit = $true }
Check 'A1 second-audit approve' ($r.ok -and $r.data.status -eq 5) "status=$($r.status)/$($r.data.status)"

$r = Req 'POST' '/api/arrangements' $tSt $null
Check 'A2 incremental arrange' ($r.ok -and $r.data.total -ge 1) "total=$($r.data.total)"

$r = Req 'GET' '/api/my/signins' $tA $null
$myArr = $r.data | Where-Object { $_.arrangement_id -and $_.plan_name } | Select-Object -First 1
Check 'A3 candidate signin view' ($r.ok -and $myArr -ne $null) "arr=$($myArr.arrangement_id) ticket=$($myArr.ticket_no)"

$r = Req 'PUT' "/api/my/signins/$($myArr.arrangement_id)/signin" $tA $null
Check 'A4a self signin' ($r.ok) "status=$($r.status)"
$r = Req 'PUT' "/api/my/signins/$($myArr.arrangement_id)/signin" $tA $null
Check 'A4b self signin idempotent' ($r.ok) "status=$($r.status)"
$r = Req 'PUT' "/api/my/signins/999999/signin" $tB $null
Check 'A4c signin others/forbidden' (-not $r.ok -and $r.status -eq 403) "status=$($r.status)"

$r = Req 'GET' '/api/registrations/2907/ticket' $tA $null
Check 'A5 own ticket' ($r.ok -and $r.data.ticketNo) "ticketNo=$($r.data.ticketNo)"

$r = Req 'POST' '/api/scores' $tSt @{ registrationId = 2907; theory = 101; practice = 90 }
Check 'A6a score out of range' (-not $r.ok -and $r.status -eq 400) "status=$($r.status)"
$r = Req 'POST' '/api/scores' $tSt @{ registrationId = 2907; theory = 55; practice = 90 }
Check 'A6b score valid' ($r.ok) "status=$($r.status) comp=$($r.data.comprehensiveScore) result=$($r.data.result)"

$r = Req 'GET' '/api/scores/my' $tA $null
Check 'A7 hidden before publish' ($r.ok -and $r.data.Count -eq 0) "count=$($r.data.Count)"
$r = Req 'PUT' '/api/scores/2907/publish' $tSt $null
Check 'A8a publish score' ($r.ok) "status=$($r.status)"
$r = Req 'PUT' '/api/scores/2907/publish' $tSt $null
Check 'A8b publish duplicate rejected' (-not $r.ok -and $r.status -eq 400) "status=$($r.status)"
$r = Req 'GET' '/api/scores/my' $tA $null
Check 'A8c visible after publish' ($r.ok -and $r.data.Count -eq 1 -and [decimal]$r.data[0].comprehensiveScore -eq 76) "comp=$($r.data[0].comprehensiveScore) result=$($r.data[0].result)"

$r = Req 'GET' '/api/certificates/my' $tA $null
Check 'A9a auto certificate' ($r.ok -and $r.data.Count -eq 1) "count=$($r.data.Count) no=$($r.data[0].certificate_no)"
$certNo = $r.data[0].certificate_no
$r = Req 'GET' "/api/public/certificates/verify?no=$([uri]::EscapeDataString($certNo))" $null $null
Check 'A9b public verify ok' ($r.ok -and $r.data.certificate_no -eq $certNo) "status=$($r.status)"
$r = Req 'GET' '/api/public/certificates/verify?no=NOT-EXIST-999' $null $null
Check 'A9c verify missing 404' (-not $r.ok -and $r.status -eq 404) "status=$($r.status)"

# ========== Part B: edge cases & rules ==========
$r = Req 'POST' '/api/registrations' $tB @{ planId = 3 }
Check 'B1 deadline closed' (-not $r.ok -and $r.status -eq 400) "status=$($r.status)"

$planBody = @{
    planName = 'E2E-CAP-PLAN'; planCode = 'E2ECAP0001'; tradeId = 1; levelId = 1;
    registerStartTime = '2026-10-01T00:00:00'; registerEndTime = '2026-10-31T23:59:59';
    examTime = '2026-11-20T09:00:00'; maxCandidates = 1; fee = 0; conditionDesc = 'e2e'
}
$r = Req 'POST' '/api/admin/plans' $tAdm $planBody
$capPlanId = $r.data.id
Check 'B2a create cap plan (draft)' ($r.ok) "id=$capPlanId"
$r = Req 'POST' '/api/registrations' $tB @{ planId = $capPlanId }
Check 'B2b apply draft rejected' (-not $r.ok -and $r.status -eq 400) "status=$($r.status)"
$r = Req 'PUT' "/api/admin/plans/$capPlanId/status?status=1" $tAdm $null
Check 'B2c publish plan' ($r.ok) "status=$($r.status)"
$r = Req 'POST' '/api/registrations' $tA @{ planId = $capPlanId }
Check 'B3a A takes last seat' ($r.ok) "status=$($r.status) regId=$($r.data.id)"
$capRegA = $r.data.id
$r = Req 'POST' '/api/registrations' $tB @{ planId = $capPlanId }
Check 'B3b B over capacity' (-not $r.ok -and $r.status -eq 400) "status=$($r.status)"
$r = Req 'PUT' "/api/my/registrations/$capRegA/cancel" $tA $null
Check 'B4a A cancel frees seat' ($r.ok) "status=$($r.status)"
$r = Req 'POST' '/api/registrations' $tB @{ planId = $capPlanId }
Check 'B4b B takes freed seat' ($r.ok) "status=$($r.status) regId=$($r.data.id)"
$capRegB = $r.data.id
$r = Req 'PUT' "/api/my/registrations/$capRegB/cancel" $tB $null
Check 'B4c B cancel too' ($r.ok) "status=$($r.status)"

$r = Req 'POST' '/api/registrations' $tB @{ planId = 9 }
$b9 = $r.data.id
Check 'B5a B apply plan9' ($r.ok -and $r.data.status -eq 1) "regId=$b9"
$r = Req 'PUT' "/api/registrations/$b9/audit" $tAdm @{ approved = $false; reason = ''; secondAudit = $false }
Check 'B5b reject without reason 400' (-not $r.ok -and $r.status -eq 400) "status=$($r.status)"
$r = Req 'PUT' "/api/registrations/$b9/audit" $tAdm @{ approved = $false; reason = 'e2e reject reason'; secondAudit = $false }
Check 'B5c reject with reason' ($r.ok -and $r.data.status -eq 3) "status=$($r.data.status)"
$r = Req 'GET' "/api/registrations/$b9/timeline" $tB $null
Check 'B5d timeline shows reason' ($r.ok -and ($r.data -join ' ') -match 'e2e reject reason') "status=$($r.status)"
$r = Req 'POST' '/api/registrations' $tB @{ planId = 9 }
Check 'B5e resubmit rejected' ($r.ok -and $r.data.status -eq 1) "status=$($r.data.status)"

$r = Req 'GET' '/api/registrations/2907/ticket' $tB $null
Check 'B6a IDOR ticket 403' (-not $r.ok -and $r.status -eq 403) "status=$($r.status)"
$r = Req 'GET' '/api/registrations/2907/materials' $tB $null
Check 'B6b IDOR materials 403' (-not $r.ok -and $r.status -eq 403) "status=$($r.status)"
$r = Req 'GET' '/api/registrations/2907/audit-records' $tB $null
Check 'B6c audit-records role 403' (-not $r.ok -and $r.status -eq 403) "status=$($r.status)"

$r = Req 'GET' '/api/system/users' $tB $null
Check 'B7a candidate users 403' (-not $r.ok -and $r.status -eq 403) "status=$($r.status)"
$r = Req 'POST' '/api/arrangements' $tB $null
Check 'B7b candidate arrange 403' (-not $r.ok -and $r.status -eq 403) "status=$($r.status)"
$r = Req 'GET' '/api/arrangements' $tB $null
Check 'B7c candidate arrangements list 403' (-not $r.ok -and $r.status -eq 403) "status=$($r.status)"

# B8 approve + pay B's plan9 reg AFTER arrangement step
$r = Req 'PUT' "/api/registrations/$b9/audit" $tAdm @{ approved = $true; reason = ''; secondAudit = $false }
Check 'B8a first-audit B' ($r.ok -and $r.data.status -eq 2) "status=$($r.data.status)"
$r = Req 'PUT' "/api/registrations/$b9/pay?payMethod=alipay" $tB $null
Check 'B8b B pay' ($r.ok -and $r.data.status -eq 4) "status=$($r.data.status)"

# B9 change password auth + validation
$r = Req 'POST' '/api/auth/password/change' $null @{ oldPassword = 'x'; newPassword = 'abc12345' }
Check 'B9a change-pwd no token 401' (-not $r.ok -and $r.status -eq 401) "status=$($r.status)"
$r = Req 'POST' '/api/auth/password/change' $tB @{ oldPassword = 'wrongold1'; newPassword = 'abc12345' }
Check 'B9b wrong old pwd 400' (-not $r.ok -and $r.status -eq 400) "status=$($r.status)"
$r = Req 'POST' '/api/auth/password/change' $tB @{ oldPassword = '123456'; newPassword = 'abc12345' }
Check 'B9c change pwd ok' ($r.ok) "status=$($r.status)"
$r = Req 'POST' '/api/auth/login' $null @{ username = 'uifr1b'; password = 'abc12345' }
Check 'B9d login new pwd' ($r.ok) "status=$($r.status)"

"RESULT pass=$pass fail=$fail"
"CAP_PLAN_ID=$capPlanId"
"B9_REG_ID=$b9"
"CERT_NO=$certNo"
