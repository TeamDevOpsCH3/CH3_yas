#!/usr/bin/env python3
# =============================================================================
# build-realm-json.py — Dựng realm JSON cho Keycloak Bitnami (keycloakConfigCli)
# từ backup operator (.operator.bak), render 2 biến redirect + thêm .dev/.staging.
# Chạy TRÊN MÁY BẠN (worker-hiep) — cần file backup trong repo.
# =============================================================================
import sys, json, re

SRC = "k8s/deploy/keycloak/keycloak.operator.bak/templates/keycloak-yas-realm-import.yaml"
OUT = "k8s/deploy/keycloak/yas-realm.json"

# Giá trị thật (từ cluster-config.yaml — baseline domain)
BACKOFFICE = "http://backoffice.yas.local.com"
STOREFRONT = "http://storefront.yas.local.com"

with open(SRC) as f:
    raw = f.read()

# 1. Render 2 biến Helm {{ }}
raw = raw.replace("{{ .Values.backofficeRedirectUrl }}", BACKOFFICE)
raw = raw.replace("{{ .Values.storefrontRedirectUrl }}", STOREFRONT)
# phòng trường hợp không có space trong {{}}
raw = raw.replace("{{.Values.backofficeRedirectUrl}}", BACKOFFICE)
raw = raw.replace("{{.Values.storefrontRedirectUrl}}", STOREFRONT)

# 2. Parse YAML → lấy spec.realm
try:
    import yaml
except ImportError:
    sys.exit("Cần pyyaml: pip install pyyaml --break-system-packages")

doc = yaml.safe_load(raw)
realm = doc["spec"]["realm"]

# 3. Thêm redirect_uri .dev + .staging cho 2 bff (+ webOrigins)
def add_env_uris(client):
    """Thêm .dev/.staging vào redirectUris + webOrigins của 1 client."""
    ru = client.get("redirectUris", []) or []
    wo = client.get("webOrigins", []) or []
    new_ru, new_wo = list(ru), list(wo)
    for uri in ru:
        # uri kiểu http://storefront.yas.local.com/*  → thêm .dev / .staging
        for env in ("dev", "staging"):
            variant = re.sub(r'\.yas\.local\.com', f'.{env}.yas.local.com', uri)
            if variant != uri and variant not in new_ru:
                new_ru.append(variant)
    for org in wo:
        for env in ("dev", "staging"):
            variant = re.sub(r'\.yas\.local\.com', f'.{env}.yas.local.com', org)
            if variant != org and variant not in new_wo:
                new_wo.append(variant)
    client["redirectUris"] = new_ru
    client["webOrigins"] = new_wo

count = 0
for c in realm.get("clients", []):
    if c.get("clientId") in ("storefront-bff", "backoffice-bff"):
        add_env_uris(c)
        count += 1
        print(f"  ✓ {c['clientId']}: {len(c['redirectUris'])} redirectUris, {len(c['webOrigins'])} webOrigins")

# 4. Xuất JSON
with open(OUT, "w") as f:
    json.dump(realm, f, indent=2, ensure_ascii=False)

print(f"\n✓ Đã dựng {OUT}")
print(f"  clients xử lý: {count}/2")
print(f"  tổng clients: {len(realm.get('clients', []))}")
print(f"  tổng users: {len(realm.get('users', []))}")
# verify redirect_uri .dev có trong output
with open(OUT) as f:
    out = f.read()
print(f"  redirect .dev: {out.count('dev.yas.local.com')} chỗ")
print(f"  redirect .staging: {out.count('staging.yas.local.com')} chỗ")
