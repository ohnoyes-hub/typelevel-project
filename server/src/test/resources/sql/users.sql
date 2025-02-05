CREATE TABLE users (
    email text NOT NULL,
    hashedPassword text NOT NULL,
    firstName text,
    lastName text,
    company text,
    role text NOT NULL
);

ALTER TABLE users
ADD CONSTRAINT pk_users PRIMARY KEY (email);

INSERT INTO users (
    email,
    hashedPassword,
    firstName,
    lastName,
    company,
    role
) VALUES (
    'daniel@rockthejvm.com',
    '$2a$10$RUrc4HAIsSSb908/h7w0nO4gZn.NrGjROXHxFlxY9lX8dQTYxQcLW',
    'Daniel',
    'Ciocirlan',
    'Rock the JVM',
    'ADMIN'
);

INSERT INTO users (
    email,
    hashedPassword,
    firstName,
    lastName,
    company,
    role
) VALUES (
    'riccardo@rockthejvm.com',
    '$2a$10$DDfeZDKeWIJiszswg7ESHurcqD8UtF1M1PeB5PzmcXMfOeVR2twi6',
    'Riccardo',
    'Cardin',
    'Rock the JVM',
    'RECRUITER'
);