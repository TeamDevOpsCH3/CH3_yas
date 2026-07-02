# Root-cause report — Storefront `/api/*` returns 502

- **Date:** 2026-07-02
- **Environment:** kubeadm cluster, namespace `dev`, Istio mTLS **STRICT** (`PeerAuthentication/default`), deployed by ArgoCD
- **Fix PR:** [#50](https://github.com/TeamDevOpsCH3/CH3_yas/pull/50) — branch `fix/c14-c23-storefront-api-502`
- **Impact tags:** C14 (nginx api-gateway), C23 (retry VirtualServices)
- **Status:** Root cause identified and verified at runtime; fix versioned, pending Hoàng review + merge.

---

## 1. Symptom

`curl http://storefront.dev.yas.local.com/api/product/...` → **HTTP 502** for **every** storefront API endpoint (products, products-es, featured, categories, cart, …). Storefront UI homepage (`/`) served 200, so only the `/api/*` data path was broken.

Initial hypothesis (pre-investigation): storefront-bff routes `/api/product/**` directly to `http://product`, and the `product-retry` VirtualService pins port 8080 while the product Service listens on 80 → 502.

That hypothesis was **partially correct but pointed at the wrong active cause** — see §5.

---

## 2. Trace of the request path

The real chain, reconstructed from Envoy/access logs, was **not** the assumed `edge → bff → product`:

```
edge (ingress)  →  storefront-bff (Spring Cloud Gateway)
                →  nginx  (internal api-gateway, server_name api.yas.local)
                →  backend service (product / cart / tax / …)   ← FAILS HERE
```

- storefront-bff forwarded **all** `/api/*` to the `nginx` Service (not to each backend directly). Confirmed for both `/api/product` and `/api/cart` (same behaviour → nginx is the common hop).
- nginx proxied `/product/…`, `/cart/…`, etc. to the backend Service ClusterIPs.
- The **nginx → backend** hop is where the connection failed.

---

## 3. Hard evidence

### 3.1 Product sidecar (Envoy) inbound — `filter_chain_not_found`
Product pod istio-proxy access log:
```
"- - -" 0 NR filter_chain_not_found - "-" 0 0 0 - "-" "-" "-" "-" "-" - -
   10.244.0.130:80 10.244.2.116:58862 - -
```
- `NR filter_chain_not_found` = a **plaintext** connection hit product's inbound listener, which under **STRICT mTLS** has no matching (plaintext) filter chain → Envoy resets the connection.
- `10.244.0.130:80` = product pod:port. `10.244.2.116` = **nginx pod** → the plaintext source is nginx.

### 3.2 nginx never used its own sidecar for the backend hop — `cx_total::0`
nginx istio-proxy cluster stats for the product upstream:
```
outbound|80||product.dev.svc.cluster.local::10.244.0.130:80::cx_total::0
outbound|80||product.dev.svc.cluster.local::...::health_flags::healthy
```
- `cx_total::0` → traffic to the product cluster **never went through nginx's Envoy**, so it left the nginx pod as raw plaintext (no mTLS origination) → consistent with §3.1.

### 3.3 nginx error log — `recv() ... Connection reset by peer`
```
[error] recv() failed (104: Connection reset by peer) while reading response header
  from upstream, client: 127.0.0.6, server: api.yas.local,
  request: "GET /product/storefront/products-es?keyword= HTTP/1.1",
  upstream: "http://10.97.57.82:80/product/storefront/products-es?keyword=", host: "nginx"
```
- nginx connects to `10.97.57.82:80` (product Service ClusterIP), gets **RST** from product's STRICT-mTLS Envoy (§3.1), and returns **502** to storefront-bff → edge sees 502.

### 3.4 Config precedence — the reason bff went through nginx
- Image ships `spring-cloud-gateway-server-webflux-5.0.1.jar` + `spring-boot-4.0.2.jar`.
  → the bound property key is **`spring.cloud.gateway.server.webflux.routes`**.
- The mounted override (`gateway-routes-config.yaml`, from chart `yas-configuration` → `gatewayRoutesConfig`) defined routes under the **legacy key `spring.cloud.gateway.routes`** → **silently ignored** (wrong key, no binding).
- The jar-baked `prod` profile route therefore won:
  ```yaml
  spring.cloud.gateway.server.webflux.routes:
    - id: api
      uri: http://nginx        # ALL /api/** → nginx api-gateway
      predicates: [Path=/api/**]
      filters: [DedupeResponseHeader..., TokenRelay, StripPrefix=1]
  ```
  `StripPrefix=1` matches the observed rewritten path (`/api/product/...` → `/product/...`).

---

## 4. Two stacked bugs

### Bug 1 — SCG route key mismatch (primary; **C14**)
- **What:** override routes written under `spring.cloud.gateway.routes`, but SCG 5.0.1 binds `spring.cloud.gateway.server.webflux.routes`. Override ignored → baked `/api/** → http://nginx` active.
- **Effect:** every `/api/*` funneled through nginx api-gateway, which delivers plaintext to STRICT-mTLS backends → reset → **502 across the board**.

### Bug 2 — retry VirtualService port (latent; **C23**, Hoàng)
- **What:** `product-retry` / `tax-retry` VirtualServices route to `destination.port.number: 8080`. product/tax Services expose **80** (+8090 metric), **no 8080**.
- **Effect:** **latent** — only exposed *after* Bug 1 is fixed. Once storefront-bff calls product/tax directly in-mesh, the retry VS routes to the non-existent `product:8080` → **503**.

> The two bugs are dependent: fixing Bug 1 alone changes 502 → 503. Both must ship together.

---

## 5. Why the initial hypothesis was insufficient

- Patching `product-retry` 8080 → 80 at runtime, waiting 20s, and re-testing → **still 502**. This ruled out the port as the active cause and redirected the investigation to the nginx hop / mTLS reset (§3).
- The port bug (C23) is real, but latent behind Bug 1.

---

## 6. Verification (runtime, reversible)

1. Rewrote `gateway-routes-config` under the correct key `spring.cloud.gateway.server.webflux.routes` (per-service routes: `product_api → http://product`, etc.); applied to the configmap; `kubectl rollout restart deploy/storefront-bff`.
   - Result: **502 → 503** (nginx hop removed; Bug 2 now exposed).
2. Patched `product-retry` port 8080 → 80.
   - Result: `GET /api/product/storefront/products/featured` → **HTTP 200** with real data (`{"productList":[{"id":1,"name":"iPhone 15",...}]}`).
- Conclusion: both fixes required; storefront product path works end-to-end over **mesh mTLS** with nginx removed from the data path.

---

## 7. Fix (versioned — PR #50)

Config-only, **no image rebuild**:

| File | Change |
|------|--------|
| `k8s/charts/yas-configuration/values.yaml` | Nest `gatewayRoutesConfig` routes under `spring.cloud.gateway.server.webflux.routes` (Bug 1) |
| `k8s/istio/virtual-service-product-retry.yaml` | `port.number: 8080 → 80` (Bug 2) |
| `k8s/istio/virtual-service-tax-retry.yaml` | `port.number: 8080 → 80` (Bug 2, parity) |

Routes already targeted `http://<service>` directly — only the property key was wrong. bff now calls each backend directly in-mesh (mTLS).

---

## 8. Constraints & impact

- **mTLS STRICT preserved.** No `PeerAuthentication` loosened to PERMISSIVE. The fix makes bff → backend use proper mesh mTLS instead of nginx plaintext; STRICT stays intact everywhere.
- **C14 impact:** nginx api-gateway is removed from the storefront data path (bff now bypasses it). Requires Hoàng review.
- **C23 impact:** `product-retry` / `tax-retry` are Hoàng's artifacts. Port change requires Hoàng review.
- **ArgoCD:** dev was hot-patched to the fixed state for verification, but **ArgoCD auto-sync will revert it to the broken state until PR #50 merges**. Review promptly, or temporarily pause auto-sync on `yas-dev`.
- **Do not merge to `develop` without Hoàng's sign-off.**

---

## 9. Residual issues (out of scope for the 502)

- `500` on `/api/product/storefront/category` and `/api/cart/storefront/count` — **application-level** (backend returns 5xx for those specific endpoints; reached directly over mesh mTLS, so infra/routing/mTLS are healthy — `featured` returns 200). Tracked separately from this 502 root cause.
- `thumbnailUrl` in responses points at `http://api.yas.local.com/media/...` (media/frontend URL); relates to the earlier `API_BASE_PATH` missing-`.dev` note but is independent of the 502 root cause. Not addressed here.
