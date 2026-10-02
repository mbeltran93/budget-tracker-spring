create table users (
    id bigserial primary key,
    name varchar(100) not null,
    email varchar(255) not null unique,
    password varchar(255) not null
);

create table categories (
    id bigserial primary key,
    name varchar(60) not null unique,
    monthly_limit numeric(12, 2) not null default 0
);

create table expenses (
    id bigserial primary key,
    user_id bigint not null references users(id) on delete cascade,
    category_id bigint not null references categories(id) on delete cascade,
    amount numeric(12, 2) not null,
    description varchar(255),
    date date not null
);

create index idx_expenses_user_id on expenses(user_id);
create index idx_expenses_category_id on expenses(category_id);
create index idx_expenses_date on expenses(date);
