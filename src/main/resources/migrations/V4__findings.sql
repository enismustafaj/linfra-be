-- Problems the reconciler found, kept so a user can act on them and so there is a history of what was done.
create table findings
(
    id                uuid primary key,
    tenant_id         uuid references tenants (id),  -- null: resource can't be attributed (operator-only)
    type              text        not null check (type in ('ORPHAN', 'DUPLICATE', 'MISSING', 'DRIFT', 'STUCK')),
    subject_key       text        not null,          -- what the finding is about, e.g. ORPHAN:<cloud id>
    intent_id         uuid,
    resource_id       uuid,
    provider          text,                          -- set when the finding is about provider resources
    cloud_ids         jsonb       not null,
    details           jsonb       not null,
    status            text        not null check (status in ('OPEN', 'CLEARED', 'RESOLVED')),
    resolution        text,                          -- the action the user took
    resolution_detail text,
    first_seen_at     timestamptz not null,
    last_seen_at      timestamptz not null,
    resolved_at       timestamptz,
    version           bigint      not null default 0
);
-- At most one open finding per subject: the reconciler updates it instead of opening another every minute.
create unique index findings_one_open_per_subject on findings (subject_key) where status = 'OPEN';
create index findings_tenant_status_idx on findings (tenant_id, status);
