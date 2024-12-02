create table chat_thread
(
    id                  uuid default random_uuid() primary key,
    thread_id           uuid not null,
    role                varchar(255) not null,
    content             text not null,
    created_date        timestamp not null,
    last_modified_date  timestamp not null
);
