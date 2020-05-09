CREATE TABLE "users"
(
    "id"       BIGSERIAL PRIMARY KEY,
    "name"     VARCHAR NOT NULL,
    "email"    VARCHAR NOT NULL UNIQUE,
    "password" VARCHAR NOT NULL
);

-- TODO difference between UNIQUE as filed param and UNIQUE as table constraint?
-- ALTER TABLE users
--     ADD CONSTRAINT user_unique_email UNIQUE (email);
