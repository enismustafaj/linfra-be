-- Per-tenant guardrails. Tenants without a row get the configured defaults (linfra.policy.*).
create table policies
(
    tenant_id                  uuid primary key references tenants (id),
    allowed_instance_types     jsonb         not null, -- empty: any catalog type
    allowed_regions            jsonb         not null, -- empty: any catalog region
    monthly_budget_eur         numeric(10, 2) not null, -- active + in-flight resources, including the new one
    auto_approve_max_eur       numeric(10, 2) not null, -- per request; above it a human approves
    require_approval_for_prod  boolean       not null,
    updated_at                 timestamptz   not null default now()
);

-- Why the policy rejected an intent or is waiting for a human.
alter table intents add column decision_reason text;
