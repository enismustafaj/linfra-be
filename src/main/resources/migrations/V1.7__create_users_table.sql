create table users
(
    id                  uuid default random_uuid() primary key,
    full_name            varchar(255) not null,
    email               varchar(255) not null,
    reference_id        varchar(255) not null,
    created_at        timestamp not null,
    last_modified_at  timestamp not null
);