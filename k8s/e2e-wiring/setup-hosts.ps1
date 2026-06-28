# ============================================================================
# setup-hosts.ps1 — Cấu hình /etc/hosts cho YAS dev E2E trên máy Windows.
# Chạy: PowerShell AS ADMINISTRATOR.
#
# TIEN DE: máy này PHẢI đã trong Tailnet (cài Tailscale + cùng tailnet với cụm),
#          vì 100.98.171.67 là Tailscale IP — máy ngoài tailnet KHÔNG route tới được.
#
# Phát file này cho mỗi thành viên team trước khi demo.
# ============================================================================

param(
    [string]$MasterIP = "100.98.171.67",
    [switch]$Remove   # chạy với -Remove để gỡ các dòng đã thêm
)

$hostsPath = "$env:windir\System32\drivers\etc\hosts"
$marker    = "# YAS dev E2E (managed by setup-hosts.ps1)"
$domains   = @(
    "storefront.yas.local.com",
    "backoffice.yas.local.com",
    "api.yas.local.com",
    "identity.yas.local.com"
)

# --- Kiểm quyền admin ---
$isAdmin = ([Security.Principal.WindowsPrincipal] `
    [Security.Principal.WindowsIdentity]::GetCurrent()
).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin) {
    Write-Host "[x] Cần chạy PowerShell AS ADMINISTRATOR." -ForegroundColor Red
    exit 1
}

# --- Đọc hosts hiện tại, bỏ block YAS cũ (idempotent) ---
$lines = Get-Content $hostsPath
$cleaned = @()
$skip = $false
foreach ($line in $lines) {
    if ($line -eq $marker) { $skip = $true; continue }
    if ($skip -and $line.Trim() -eq "") { $skip = $false; continue }
    if ($skip) { continue }
    $cleaned += $line
}

if ($Remove) {
    Set-Content -Path $hostsPath -Value $cleaned -Encoding ASCII
    Write-Host "[+] Đã gỡ block YAS khỏi hosts." -ForegroundColor Green
    ipconfig /flushdns | Out-Null
    Write-Host "[+] Đã flush DNS cache." -ForegroundColor Green
    exit 0
}

# --- Thêm block mới ---
$cleaned += ""
$cleaned += $marker
foreach ($d in $domains) {
    $cleaned += "$MasterIP`t$d"
}
Set-Content -Path $hostsPath -Value $cleaned -Encoding ASCII

Write-Host "[+] Đã thêm $($domains.Count) domain -> $MasterIP" -ForegroundColor Green
$domains | ForEach-Object { Write-Host "      $_" }

ipconfig /flushdns | Out-Null
Write-Host "[+] Đã flush DNS cache." -ForegroundColor Green

# --- Kiểm tra Tailscale (cảnh báo nếu chưa) ---
$ping = Test-Connection -ComputerName $MasterIP -Count 1 -Quiet -ErrorAction SilentlyContinue
if ($ping) {
    Write-Host "[+] Ping master $MasterIP OK — máy trong tailnet." -ForegroundColor Green
    Write-Host "    Mở: http://storefront.yas.local.com/products" -ForegroundColor Cyan
} else {
    Write-Host "[!] KHÔNG ping được $MasterIP." -ForegroundColor Yellow
    Write-Host "    Kiểm tra: máy đã cài Tailscale + join đúng tailnet chưa?" -ForegroundColor Yellow
    Write-Host "    (hosts đã thêm nhưng sẽ không vào được nếu ngoài tailnet)" -ForegroundColor Yellow
}
