#!/usr/bin/env bash
# =============================================================================
# setup-dnsmasq-master.sh — Cài + config dnsmasq trên yas-master cho DNS nhóm.
# Chạy: TRÊN root@yas-master (sau khi rebuild master, hoặc lần đầu setup).
# Mục đích: cả nhóm resolve *.yas.local.com qua Tailscale Split DNS, không sửa hosts.
#
# SAU KHI CHẠY: nhớ config Tailscale admin → DNS → Split DNS:
#   yas.local.com → 100.98.171.67 (Restrict to domain BẬT)
# =============================================================================
set -euo pipefail

MASTER_TAILSCALE_IP="${MASTER_TAILSCALE_IP:-100.98.171.67}"

echo "==> Cài dnsmasq"
apt-get update -qq
apt-get install -y dnsmasq

echo "==> Ghi config /etc/dnsmasq.d/yas.conf"
cat > /etc/dnsmasq.d/yas.conf <<EOF
# DNS cho client nhóm qua Tailscale — resolve *.yas.local.com → master IP
address=/yas.local.com/${MASTER_TAILSCALE_IP}
listen-address=${MASTER_TAILSCALE_IP}
bind-interfaces
no-resolv
server=8.8.8.8
server=1.1.1.1
EOF

echo "==> Restart + enable dnsmasq"
systemctl restart dnsmasq
systemctl enable dnsmasq

echo "==> Verify (đợi 2s cho dnsmasq bind tailscale0)"
sleep 2
if ss -tulpn | grep -q "${MASTER_TAILSCALE_IP}:53"; then
  echo "  ✓ dnsmasq nghe ${MASTER_TAILSCALE_IP}:53"
else
  echo "  ✗ dnsmasq CHƯA nghe ${MASTER_TAILSCALE_IP}:53 — kiểm tailscale0 up chưa"
  echo "    (dnsmasq cần tailscale0 có IP trước khi start; chạy lại script sau khi Tailscale up)"
  exit 1
fi

echo "==> Test resolve"
which dig >/dev/null 2>&1 || apt-get install -y dnsutils
dig @${MASTER_TAILSCALE_IP} storefront.dev.yas.local.com +short
dig @${MASTER_TAILSCALE_IP} google.com +short | head -1

echo ""
echo "✓ dnsmasq OK. BƯỚC TIẾP (thủ công, 1 lần):"
echo "  Tailscale admin → https://login.tailscale.com/admin/dns"
echo "  Add nameserver (Custom): ${MASTER_TAILSCALE_IP}, Restrict to domain: yas.local.com"
echo "  → cả nhóm (Win/Mac) tự resolve *.yas.local.com, không sửa /etc/hosts."
