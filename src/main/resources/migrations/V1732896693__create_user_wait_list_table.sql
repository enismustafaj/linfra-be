create table user_wait_list
(
    id          uuid default random_uuid() primary key,
    full_name   varchar(255) not null,
    email       varchar(255) not null,
    created_at  timestamp not null,
    updated_at  timestamp not null
);