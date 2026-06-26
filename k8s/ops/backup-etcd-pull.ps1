# backup-etcd-pull.ps1
# Pull etcd snapshot from master (via Tailscale) to Windows - off-cluster copy with daily history.
# Run manually, or attach to Windows Task Scheduler (do NOT set Expire - delete manually after defense).

$MasterTailscaleIP = "100.98.171.67"
$RemoteFile        = "/var/backups/etcd/etcd-snapshot.db"
$LocalDir          = "D:\HK6\DevOps\Projects\Project02\backups"
$Date              = Get-Date -Format "yyyy-MM-dd-HHmm"
$LocalFile         = Join-Path $LocalDir "etcd-snapshot-$Date.db"

if (!(Test-Path $LocalDir)) { New-Item -ItemType Directory -Path $LocalDir | Out-Null }

Write-Host "[*] Pulling etcd snapshot from master ($MasterTailscaleIP)..."
scp "root@${MasterTailscaleIP}:${RemoteFile}" "$LocalFile"

if (Test-Path $LocalFile) {
    $size = (Get-Item $LocalFile).Length / 1MB
    Write-Host ("[OK] Saved: {0} ({1:N1} MB)" -f $LocalFile, $size)
} else {
    Write-Host "[FAIL] Pull failed - check master is up and CronJob has run." -ForegroundColor Red
}

# Optional: remove local backups older than 14 days
Get-ChildItem $LocalDir -Filter "etcd-snapshot-*.db" |
  Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-14) } |
  Remove-Item -Force
