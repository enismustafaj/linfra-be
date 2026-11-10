-- A resource outlives the intent that created it: the reconciler checks it, it can drift, and it gets destroyed.
create table resources
(
    id            uuid primary key,
    tenant_id     uuid        not null references tenants (id),
    intent_id     uuid        not null unique references intents (id), -- one intent creates at most one resource
    provider      text        not null check (provider in ('DOCKER')),  -- extend when a provider is added
    region        text        not null,
    instance_type text        not null,
    cloud_id      text        not null,                                -- hcloud server id / EC2 instance id
    outputs       jsonb       not null,                                -- e.g. public ipv4
    status        text        not null check (status in ('ACTIVE', 'DESTROYED', 'MISSING')),
    last_seen_at  timestamptz,                                         -- set by the reconciler
    created_at    timestamptz not null default now(),
    updated_at    timestamptz not null default now(),
    unique (provider, cloud_id)                                        -- no cloud resource is adopted by two intents
);
create index resources_tenant_idx on resources (tenant_id);

alter table intents drop column resource_id;
