# Tenant isolation

Each tenant only ever sees and acts on its own conversations and intents.

- Every `/api/*` request needs `Authorization: Bearer <api key>`. `ApiKeyFilter` looks up the key's SHA-256 hash; an
  unknown or missing key returns `401`. The raw key is never stored.
- Controllers get the tenant as a `TenantContext` parameter. The tenant id never comes from the request body.
- Every lookup is scoped (`findByIdAndTenantId`, `requireOwned`). Another tenant's resource returns `404`, not `403`,
  so its existence isn't revealed.
- Intents get their tenant from the session at creation; the LLM can't set it.

Tenants are created directly in the database for now; there's no endpoint yet.
