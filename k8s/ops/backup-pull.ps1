# backup-pull.ps1
# Pull BOTH etcd snapshot AND pki (cert) from master to Windows - off-cluster, daily history.
# Uses id_auto key (no passphrase) so it runs without prompting.
# Run manually, or attach to Task Scheduler.

$Key       = "$env:USERPROFILE\.ssh\id_auto"
$MasterIP  = "100.98.171.67"
$LocalDir  = "D:\HK6\DevOps\Projects\Project02\backups"
$Date      = Get-Date -Format "yyyy-MM-dd-HHmm"

if (!(Test-Path $LocalDir)) { New-Item -ItemType Directory -Path $LocalDir | Out-Null }

# 1. etcd snapshot
Write-Host "[*] Pulling etcd snapshot..."
scp -i $Key "root@${MasterIP}:/var/backups/etcd/etcd-snapshot.db" "$LocalDir\etcd-snapshot-$Date.db"

# 2. pki (cert) - needed for full restore onto a fresh master
Write-Host "[*] Pulling pki (cert)..."
scp -i $Key "root@${MasterIP}:/var/backups/etcd/pki.tgz" "$LocalDir\pki-$Date.tgz"

# verify
$etcd = "$LocalDir\etcd-snapshot-$Date.db"
$pki  = "$LocalDir\pki-$Date.tgz"
if ((Test-Path $etcd) -and (Test-Path $pki)) {
    $e = (Get-Item $etcd).Length / 1MB
    $p = (Get-Item $pki).Length / 1KB
    Write-Host ("[OK] etcd: {0:N1} MB | pki: {1:N0} KB" -f $e, $p)
    Write-Host "     Both saved to $LocalDir"
} else {
    Write-Host "[WARN] One or both files missing - check CronJobs have run on master." -ForegroundColor Yellow
}

# remove local backups older than 14 days
Get-ChildItem $LocalDir -Filter "etcd-snapshot-*.db" | Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-14) } | Remove-Item -Force
Get-ChildItem $LocalDir -Filter "pki-*.tgz"          | Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-14) } | Remove-Item -Force
