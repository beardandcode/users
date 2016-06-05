-- name: register-user<!
INSERT INTO users (email_address, name, password) VALUES(:email_address, :name, crypt(:password, gen_salt('bf', 8)));

-- name: authenticate-user
SELECT * FROM users WHERE email_address = :email_address AND crypt(:password, password) = password;

-- name: get-user-by-email-address
SELECT * FROM users WHERE email_address = :email_address;

-- name: delete-user-by-id!
DELETE FROM users WHERE id = :id;

-- name: create-token<!
INSERT INTO tokens (token, user_id, type)
  VALUES(
    encode(gen_random_bytes(32), 'hex'),
    :user_id,
    :token_type::token_type
  );

-- name: find-token
SELECT * FROM tokens WHERE token = :token AND type = :token_type::token_type;

-- name: delete-token!
DELETE FROM tokens WHERE token = :token;

-- name: confirm-user<!
UPDATE users
    SET confirmed = TRUE
    FROM (SELECT user_id FROM tokens WHERE type = 'confirmation' AND token = :token) AS token_list
    WHERE users.id = token_list.user_id;

-- name: reset-user-password<!
UPDATE users
    SET password = crypt(:password, gen_salt('bf', 8)), confirmed = TRUE
    FROM (SELECT user_id FROM tokens WHERE type = 'reset' AND token = :token) AS token_list
    WHERE users.id = token_list.user_id;
