CREATE EXTENSION pgcrypto;
CREATE EXTENSION "uuid-ossp";
CREATE EXTENSION citext;



CREATE FUNCTION on_record_insert() RETURNS trigger AS $$
  BEGIN
    NEW.id         := uuid_generate_v4();
    NEW.created_at := now();
    NEW.updated_at := now();
    RETURN NEW;
  END;
$$ LANGUAGE plpgsql;

CREATE FUNCTION on_record_update() RETURNS trigger AS $$
  BEGIN
    NEW.id         := OLD.id;
    NEW.created_at := OLD.created_at;
    NEW.updated_at := now();
    RETURN NEW;
  END;
$$ LANGUAGE plpgsql;



CREATE TABLE users (
  id             UUID       PRIMARY KEY,
  email_address  CITEXT     NOT NULL
    CONSTRAINT like_an_email CHECK (email_address ~* '^.+@.+\..+'),
  name           TEXT,
  password       TEXT       NOT NULL,
  confirmed      BOOLEAN    DEFAULT FALSE,
  created_at     TIMESTAMP  NOT NULL,
  updated_at     TIMESTAMP  NOT NULL
);

CREATE UNIQUE INDEX email_on_users
  ON users (email_address);

CREATE TRIGGER users_insert
  BEFORE INSERT ON users
  FOR EACH ROW
  EXECUTE PROCEDURE on_record_insert();

CREATE TRIGGER users_update
  BEFORE UPDATE ON users
  FOR EACH ROW
  EXECUTE PROCEDURE on_record_update();



CREATE TYPE token_type AS ENUM ('confirmation', 'reset');

CREATE TABLE tokens (
  token       TEXT       PRIMARY KEY,
  user_id     UUID       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
  type        token_type NOT NULL,
  created_at  TIMESTAMP  NOT NULL DEFAULT now()
);
