create table thread
(
    id                  uuid default random_uuid() primary key,
    user_id             uuid not null,
    thread_name         varchar(255) not null,
    created_at        timestamp not null,
    last_modified_at  timestamp not null
);
