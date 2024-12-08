create table resource
(
    id                  uuid default random_uuid() primary key,
    user_id             uuid not null,
    name                varchar(255) not null,
    type                varchar(255) not null,
    properties          json not null,
    created_at        timestamp not null,
    last_modified_at  timestamp not null
);