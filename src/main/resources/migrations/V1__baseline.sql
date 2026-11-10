create table tenants
(
    id           uuid primary key,
    name         text        not null,
    api_key_hash text        not null unique, -- sha256 hex of the bearer key; the key itself is never stored
    created_at   timestamptz not null default now()
);

create table intents
(
    id                  uuid primary key,
    tenant_id           uuid        not null references tenants (id),
    state               text        not null check (state in ('PROPOSED', 'PLANNED', 'APPROVED', 'APPLYING', 'SUCCEEDED',
                                                              'REJECTED', 'EXPIRED', 'FAILED', 'NEEDS_ATTENTION')),
    spec                jsonb       not null,
    plan_hash           text,
    approved_at         timestamptz,
    approval_expires_at timestamptz,
    resource_id         text,
    version             bigint      not null default 0,
    created_at          timestamptz not null default now(),
    updated_at          timestamptz not null default now()
);
create index intents_tenant_idx on intents (tenant_id);

create table outbox_jobs
(
    id            bigserial primary key,
    intent_id     uuid        not null unique references intents (id), -- one job per intent: approving twice cannot enqueue twice
    status        text        not null check (status in ('PENDING', 'LEASED', 'DONE', 'DEAD')),
    attempts      int         not null default 0,
    available_at  timestamptz not null default now(),
    lease_owner   text,
    lease_until   timestamptz,
    fencing_token bigint      not null default 0,
    last_error    text,
    created_at    timestamptz not null default now()
);
create index outbox_claimable_idx on outbox_jobs (status, available_at);

create table runs
(
    id            bigserial primary key,
    intent_id     uuid        not null references intents (id),
    job_id        bigint      not null references outbox_jobs (id),
    fencing_token bigint      not null,
    worker_id     text        not null,
    started_at    timestamptz not null,
    finished_at   timestamptz,
    outcome       text,
    unique (job_id, fencing_token)
);
