# C14 - Service Discovery Wiring Fix

## Context

The YAS apps were deployed into the `dev` namespace, while shared infrastructure runs in separate namespaces:

- Keycloak: `identity.keycloak`
- Redis: `redis-master.redis`
- PostgreSQL: `postgresql.postgres`

Most service discovery wiring was already working through Kubernetes DNS. However, `storefront-bff` still tried to resolve the legacy Docker Compose hostname `storefront-nextjs`, which caused HTTP 500 on the BFF root route.

## Fix

Create Kubernetes Service aliases:

- `storefront-nextjs` -> `storefront-ui`
- `backoffice-nextjs` -> `backoffice-ui`

This preserves the hostname expected by the BFF while routing traffic to the actual Kubernetes UI services.

## Apply

```bash
kubectl apply -f k8s/c14-service-discovery/nextjs-alias-services.yaml
```

## Verify

```bash
kubectl -n dev get svc storefront-nextjs backoffice-nextjs -o wide
kubectl -n dev get endpoints storefront-nextjs backoffice-nextjs -o wide

kubectl -n dev run curl-test \
  --image=curlimages/curl:8.8.0 \
  --restart=Never \
  --rm -i \
  -- sh -c 'curl -sS -D - -o /dev/null http://storefront-bff/ || true'
```

Expected result:

- `storefront-bff /` returns `HTTP/1.1 200 OK`
- `backoffice-bff /` returns `HTTP/1.1 302 Found` because it redirects to OAuth2 login
    
