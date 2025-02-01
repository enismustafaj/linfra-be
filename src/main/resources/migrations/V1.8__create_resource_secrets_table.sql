create table resource_secrets
(
    id                  uuid default random_uuid() primary key,
    resource_id         uuid not null,
    resource_credentials                json not null,
    created_at        timestamp not null,
    last_modified_at  timestamp not null
);