create table thread
(
    id          uuid default gen_random_uuid() primary key,
    user_id     uuid not null,
    thread_name varchar(255) not null,
    created_at  timestamp not null,
    updated_at  timestamp not null
);