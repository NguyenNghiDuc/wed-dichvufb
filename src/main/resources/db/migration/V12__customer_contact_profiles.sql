-- Customer-managed contact fields, isolated from users.login credentials/roles.
CREATE TABLE customer_profiles (
    owner_username VARCHAR(100) PRIMARY KEY REFERENCES users(username),
    display_name VARCHAR(80) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL
);
