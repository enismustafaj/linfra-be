create table conversations
(
    id         uuid primary key,
    tenant_id  uuid        not null references tenants (id),
    created_at timestamptz not null default now()
);
create index conversations_tenant_idx on conversations (tenant_id);

create table messages
(
    id              bigserial primary key,
    conversation_id uuid        not null references conversations (id),
    role            text        not null check (role in ('USER', 'ASSISTANT')), -- only the server writes ASSISTANT
    content         text        not null,
    draft           jsonb,                                                      -- validated requirements + ranked pick
    created_at      timestamptz not null default now()
);
create index messages_conversation_idx on messages (conversation_id, id);

alter table intents
    add column conversation_id   uuid references conversations (id),
    add column source_message_id bigint unique references messages (id);
