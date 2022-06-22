create table if not exists posts (
    id serial primary key,
    name text not null,
    description text not null,
    link text unique not null,
    created timestamp not null
);