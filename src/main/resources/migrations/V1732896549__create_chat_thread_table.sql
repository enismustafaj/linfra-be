create table chat_thread
(
    id          uuid default gen_random_uuid() primary key,
    thread_id   uuid not null,
    role        varchar(255) not null,
    content     text not null,
    created_at  timestamp not null,
    updated_at  timestamp not null
);