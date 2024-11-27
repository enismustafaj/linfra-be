create table conversation_thread
(
    id          uuid default gen_random_uuid() primary key,
    user_id     uuid not null,
    thread_payload json,
    created_at  timestamp not null,
    updated_at  timestamp not null
);