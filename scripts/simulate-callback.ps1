# Freedom Pay callback simulator.
# Usage: .\simulate-callback.ps1 -OrderId 123 -PaymentId "987654" -Amount "10000.00" -Secret "your_secret_key"
# Requires: backend running on localhost:8080 with matching FREEDOMPAY_SECRET_KEY

param(
    [Parameter(Mandatory)][long]  $OrderId,
    [Parameter(Mandatory)][string]$PaymentId,
    [Parameter(Mandatory)][string]$Amount,
    [Parameter(Mandatory)][string]$Secret,
    [string]$ScriptName = "freedom-pay",
    [string]$Result     = "1",             # 1=success, 0=failure
    [string]$BaseUrl    = "http://localhost:8080"
)

# Build params in the exact order they'll be sorted (TreeMap order)
$params = [ordered]@{
    pg_amount      = $Amount
    pg_currency    = "KZT"
    pg_description = "Simulated payment"
    pg_order_id    = "$OrderId"
    pg_payment_id  = $PaymentId
    pg_result      = $Result
    pg_salt        = "simulated-salt-1234"
}

# MD5 signature: scriptName;sortedVal1;...;sortedValN;secretKey
# TreeMap sorts alphabetically by key — PowerShell ordered dict above is already sorted.
$sigInput = $ScriptName
foreach ($key in ($params.Keys | Sort-Object)) {
    $sigInput += ";" + $params[$key]
}
$sigInput += ";" + $Secret

$md5    = [System.Security.Cryptography.MD5]::Create()
$bytes  = [System.Text.Encoding]::UTF8.GetBytes($sigInput)
$hash   = $md5.ComputeHash($bytes)
$pg_sig = -join ($hash | ForEach-Object { "{0:x2}" -f $_ })

$params["pg_sig"] = $pg_sig

# Build form body
$formParts = $params.GetEnumerator() | ForEach-Object {
    "$([Uri]::EscapeDataString($_.Key))=$([Uri]::EscapeDataString($_.Value))"
}
$body = $formParts -join "&"

Write-Host ""
Write-Host "=== Freedom Pay Callback Simulator ===" -ForegroundColor Cyan
Write-Host "Order ID:    $OrderId"
Write-Host "Payment ID:  $PaymentId"
Write-Host "Amount:      $Amount KZT"
Write-Host "pg_result:   $Result  (1=success, 0=failure)"
Write-Host "pg_sig:      $pg_sig"
Write-Host ""
Write-Host "Sending callback to $BaseUrl/api/v1/payments/callback/freedom-pay ..." -ForegroundColor Yellow

$response = Invoke-WebRequest `
    -Uri "$BaseUrl/api/v1/payments/callback/freedom-pay" `
    -Method POST `
    -ContentType "application/x-www-form-urlencoded" `
    -Body $body `
    -UseBasicParsing

Write-Host ""
Write-Host "HTTP $($response.StatusCode)" -ForegroundColor Green
Write-Host $response.Content

if ($response.Content -match "<pg_status>ok</pg_status>") {
    Write-Host ""
    Write-Host "[OK] Callback accepted. Check order status:" -ForegroundColor Green
    Write-Host "  GET $BaseUrl/api/v1/order/$OrderId"
} elseif ($response.Content -match "<pg_status>rejected</pg_status>") {
    Write-Host ""
    Write-Host "[REJECTED] See pg_description in response above." -ForegroundColor Red
    Write-Host "  Common causes:"
    Write-Host "  - Wrong FREEDOMPAY_SECRET_KEY in .env vs -Secret param"
    Write-Host "  - FREEDOMPAY_CALLBACK_SCRIPT_NAME mismatch (current default: freedom-pay)"
    Write-Host "  - Order already CONFIRMED (replay)"
}
