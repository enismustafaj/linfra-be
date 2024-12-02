create table wait_list_user
(
    id                  serial primary key,
    user_id             varchar(255) not null unique,
    name                varchar(255) not null,
    email               varchar(255) not null,
    created_date        timestamp null,
    last_modified_date  timestamp null
);
