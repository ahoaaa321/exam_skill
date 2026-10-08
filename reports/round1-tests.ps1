# Round-1 dynamic verification (real HTTP calls)
$ErrorActionPreference = 'Continue'
$base = 'http://localhost:8080'

function Req($method, $path, $token, $bodyObj) {
    $headers = @{}
    if ($token) { $headers['Authorization'] = "Bearer $token" }
    $json = $null
    if ($bodyObj -ne $null) { $json = [System.Text.Encoding]::UTF8.GetBytes(($bodyObj | ConvertTo-Json -Compress -Depth 5)) }
    try {
        $r = Invoke-RestMethod -Uri "$base$path" -Method $method -Headers $headers -ContentType 'application/json;charset=utf-8' -Body $json
        return @{ ok = $true; status = 200; data = $r.data }
    } catch {
        $sc = if ($_.Exception.Response) { [int]$_.Exception.Response.StatusCode } else { -1 }
        return @{ ok = $false; status = $sc; msg = $_.ErrorDetails.Message }
    }
}

function Login($u) {
    $r = Req 'POST' '/api/auth/login' $null @{ username = $u; password = '123456' }
    if (-not $r.ok) { throw "login failed: $u $($r.status) $($r.msg)" }
    return $r.data.token
}

$tA = Login 'uifr1a'
$tB = Login 'uifr1b'
$tAdmin = Login 'admin'
$tStaff = Login 'staff'
"LOGIN: tokens acquired"

$r = Req 'POST' '/api/registrations' $tA @{ planId = 7; workYears = 2; education = 'high'; emergencyContact = 'jia'; emergencyPhone = '13700000103' }
"T1 apply            => ok=$($r.ok) status=$($r.status) regStatus=$($r.data.status)"
$regId = $r.data.id
"   regId=$regId"

$r = Req 'PUT' "/api/my/registrations/$regId/cancel" $tA $null
"T2 cancel own       => ok=$($r.ok) status=$($r.status) (expect 200)"

$r = Req 'PUT' "/api/my/registrations/$regId/cancel" $tB $null
"T3 cancel other     => ok=$($r.ok) status=$($r.status) (expect 403)"

$r = Req 'POST' '/api/registrations' $tA @{ planId = 7; workYears = 2; education = 'high'; emergencyContact = 'jia'; emergencyPhone = '13700000103' }
"T4 re-apply cancel  => ok=$($r.ok) status=$($r.status) regStatus=$($r.data.status) (expect 200,1)"

$r = Req 'PUT' "/api/my/registrations/$regId/cancel" $null $null
"T5 cancel no-token  => ok=$($r.ok) status=$($r.status) (expect 401)"

$r = Req 'PUT' "/api/registrations/$regId/audit" $tAdmin @{ approved = $true; reason = ''; secondAudit = $false }
"T6 audit#1          => ok=$($r.ok) status=$($r.status) (expect 200)"
$r = Req 'PUT' "/api/registrations/$regId/audit" $tAdmin @{ approved = $true; reason = ''; secondAudit = $false }
"T7 audit#2 dup      => ok=$($r.ok) status=$($r.status) (expect 400)"

$r = Req 'PUT' "/api/registrations/$regId/pay?payMethod=wechat" $tA $null
"T8 pay#1            => ok=$($r.ok) status=$($r.status) (expect 200)"
$r = Req 'PUT' "/api/registrations/$regId/pay?payMethod=wechat" $tA $null
"T9 pay#2 dup        => ok=$($r.ok) status=$($r.status) (expect 400)"

$r = Req 'PUT' "/api/my/registrations/$regId/cancel" $tA $null
"T10 cancel paid     => ok=$($r.ok) status=$($r.status) (expect 400)"

$r = Req 'PUT' '/api/arrangements/999999' $tStaff @{}
"T11 seat bad body   => ok=$($r.ok) status=$($r.status) (expect 400)"

$long = 'q' * 501
$r = Req 'POST' '/api/system/ai-qa' $null @{ question = $long }
"T12 aiqa gt500      => ok=$($r.ok) status=$($r.status) (expect 400)"
$r = Req 'POST' '/api/system/ai-qa' $null @{ question = 'how to pay' }
"T12b aiqa normal    => ok=$($r.ok) status=$($r.status) category=$($r.data.category) (expect 200)"

$r = Req 'GET' '/api/statistics' $tAdmin $null
$keys = ($r.data.Keys -join ',')
"T13 statistics      => ok=$($r.ok) total=$($r.data.total) keys=[$keys]"

"REGID=$regId"
