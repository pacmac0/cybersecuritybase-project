CREATE TABLE ipblock (
    ip varchar(20) PRIMARY KEY,
    attempts int(20)
);

CREATE TABLE accounts (
    name varchar(200) PRIMARY KEY,
    password varchar(200)
);
