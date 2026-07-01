# Istio Service Mesh Security - dev

This directory contains Istio security manifests for the `dev` namespace of the YAS application.

## Purpose

This setup enables:

- STRICT mTLS for workloads in the `dev` namespace.
- Identity-based authorization for the `product` service.
- Only the `cart` service account is allowed to call `product`.
- The `order` service account is denied when calling `product`.

## Files

| File | Purpose |
|---|---|
| `peer-authentication-dev-strict.yaml` | Enables STRICT mTLS for all workloads in namespace `dev`. |
| `authorization-policy-product-allow-cart-only.yaml` | Allows only `cluster.local/ns/dev/sa/cart` to access the `product` workload. |
| `virtual-service-tax-retry.yaml` | Retry policy for `tax`: 3 attempts, 2s perTryTimeout, on 5xx/connect-failure/reset. |
| `virtual-service-product-retry.yaml` | Retry policy for `product`: same config as tax. |
| `virtual-service-tax-fault-inject.yaml` | **Demo-only** — inject 503 on tax to trigger and observe retries. Do not add to kustomization. |
| `kustomization.yaml` | Applies all permanent manifests in this directory. |

## Apply

From the repository root:

```bash
kubectl apply -k k8s/istio
```

## Verify policies

```bash
kubectl get peerauthentication,authorizationpolicy -n dev -o wide
```

Expected result:

```text
NAME                                           MODE
peerauthentication.security.istio.io/default   STRICT

NAME                                                            ACTION
authorizationpolicy.security.istio.io/product-allow-cart-only   ALLOW
```

## Verify app pods and service accounts

```bash
kubectl -n dev get pod \
  -l 'app.kubernetes.io/name in (cart,order,product)' \
  -o custom-columns='POD:.metadata.name,READY:.status.containerStatuses[*].ready,SERVICE_ACCOUNT:.spec.serviceAccountName,NODE:.spec.nodeName'
```

Expected service accounts:

```text
cart    -> cart
order   -> order
product -> product
```

## Verify Istio sidecars

```bash
istioctl proxy-status | grep -E 'cart|order|product'
```

Expected result: `cart`, `order`, and `product` proxies are connected to `istiod`.

## Test real application flow

Get the real application pod names:

```bash
CART_POD=$(kubectl -n dev get pod -l app.kubernetes.io/name=cart -o jsonpath='{.items[0].metadata.name}')
ORDER_POD=$(kubectl -n dev get pod -l app.kubernetes.io/name=order -o jsonpath='{.items[0].metadata.name}')
PRODUCT_POD=$(kubectl -n dev get pod -l app.kubernetes.io/name=product -o jsonpath='{.items[0].metadata.name}')

echo "CART_POD=$CART_POD"
echo "ORDER_POD=$ORDER_POD"
echo "PRODUCT_POD=$PRODUCT_POD"
```

### Test allowed flow: `cart -> product`

```bash
kubectl -n dev exec "$CART_POD" -- sh -c '
  echo "===== cart real pod -> product ====="
  wget -S -O - http://product:8090/actuator/health 2>&1
'
```

Expected result:

```text
HTTP/1.1 200 OK
```

The response should include product health status:

```json
{"status":"UP"}
```

### Test denied flow: `order -> product`

```bash
kubectl -n dev exec "$ORDER_POD" -- sh -c '
  echo "===== order real pod -> product ====="
  wget -S -O - http://product:8090/actuator/health 2>&1 || true
'
```

Expected result:

```text
HTTP/1.1 403 Forbidden
```

This confirms that the `order` service account is blocked by Istio AuthorizationPolicy.

## Summary of expected behavior

| Source  | Target    | Expected result |
| ------- | --------- | --------------- |
| `cart`  | `product` | `200 OK`        |
| `order` | `product` | `403 Forbidden` |

## Optional: verify STRICT mTLS with a plaintext client

Create a temporary pod outside the mesh:

```bash
kubectl -n default run plaintext-client \
  --image=curlimages/curl:8.8.0 \
  --restart=Never \
  --command -- sh -c "sleep 3600"

kubectl -n default wait --for=condition=Ready pod/plaintext-client --timeout=180s
```

Call a service inside the `dev` namespace:

```bash
kubectl -n default exec plaintext-client -- sh -c \
  'curl -sS --max-time 5 -o /tmp/cart-health.txt -w "plaintext-client-to-cart-under-strict %{http_code}\n" http://cart.dev.svc.cluster.local:8090/actuator/health || echo "plaintext-client-to-cart-under-strict curl_failed"'
```

Expected result:

```text
plaintext-client-to-cart-under-strict 000
plaintext-client-to-cart-under-strict curl_failed
```

This shows that plaintext traffic from outside the mesh is rejected under STRICT mTLS.

Clean up the temporary plaintext client:

```bash
kubectl -n default delete pod plaintext-client --ignore-not-found
```

## Rollback

Remove the AuthorizationPolicy only:

```bash
kubectl delete authorizationpolicy product-allow-cart-only -n dev
```

Remove STRICT mTLS:

```bash
kubectl delete peerauthentication default -n dev
```

Remove all manifests from this directory:

```bash
kubectl delete -k k8s/istio/dev
```

---

## C25 — Retry Policy (VirtualService)

Retries are configured for `tax` and `product` in namespace `dev`.
Envoy sidecar automatically retries on transient errors without any app code change.

| Parameter | Value | Reason |
|---|---|---|
| `attempts` | `3` | Max 8s worst case (1 + 3 retries × 2s) |
| `perTryTimeout` | `2s` | Fast fail-fast per attempt |
| `retryOn` | `5xx,connect-failure,reset` | Transient only; never retry 4xx |

### Apply

```bash
kubectl apply -k k8s/istio

# Verify VirtualServices
kubectl get virtualservice -n dev
```

Expected:

```text
NAME              GATEWAYS   HOSTS         AGE
product-retry                ["product"]   5s
tax-retry                    ["tax"]       5s
```

### Demo — capture EV-RETRY evidence

```bash
# 1. Confirm sidecars running (must show 2/2)
kubectl get pods -n dev -l 'app.kubernetes.io/name in (tax,order,product)' \
  --no-headers | awk '{print $1, $2}'

# 2. Apply fault injection (100% → 503 on tax)
kubectl apply -f k8s/istio/virtual-service-tax-fault-inject.yaml

# 3. Trigger traffic from order pod
ORDER_POD=$(kubectl -n dev get pod -l app.kubernetes.io/name=order \
  -o jsonpath='{.items[0].metadata.name}')
kubectl -n dev exec "$ORDER_POD" -- \
  wget -qO- http://tax:8080/actuator/health 2>&1 || true

# 4. Capture retry evidence in istio-proxy log
kubectl logs "$ORDER_POD" -n dev -c istio-proxy --tail=100 \
  | grep -iE "retry|attempt|upstream_reset"

# 5. Check attempt count header
kubectl -n dev exec "$ORDER_POD" -- \
  wget -S -qO- http://tax:8080/actuator/health 2>&1 | grep -i "attempt"
# Expected: x-envoy-attempt-count: 3

# 6. DELETE fault injection after evidence captured
kubectl delete virtualservice tax-fault-inject -n dev

# 7. Confirm permanent retry VirtualServices still present
kubectl get virtualservice -n dev
```

### Rollback retry policies

```bash
kubectl delete virtualservice tax-retry product-retry -n dev
```