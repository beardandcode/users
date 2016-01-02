
DROP TABLE tokens;
DROP TYPE token_type;

DROP TRIGGER users_insert ON users;
DROP TRIGGER users_update ON users;
DROP INDEX email_on_users;
DROP TABLE users;

DROP FUNCTION on_record_update();
DROP FUNCTION on_record_insert();

DROP EXTENSION citext;
DROP EXTENSION "uuid-ossp";
DROP EXTENSION pgcrypto;
