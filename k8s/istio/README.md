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
| `kustomization.yaml` | Applies all manifests in this directory. |

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