

-- Sqlite3

-- add a user
INSERT INTO users(username, email, password, role) VALUES        ('user1', 'user1@localhost', '5baa61e4c9b93f3f0682250b6cf8331b7ee68fd8', 'all');

-- add some clients
INSERT INTO clients (client_id, username, client_secret, redirect_uri, scope, description) 
VALUES 
       ('client1', 'user1', 'secret1', 'http://localhost/9001', 'read write update', 'Client 1'),
       ('client2', 'user1', 'secret2', 'http://localhost/9001', 'read write update', 'Client 2'),
       ('client3', 'user1', 'secret3', 'http://localhost/9001', 'read write update', 'Client 3'),
       ('client4', 'user1', 'secret4', 'http://localhost/9001', 'read write update', 'Client 4')
;


-- insert grant_types in grant_type configuration table
INSERT INTO grant_types (id, grant_type) VALUES (1, 'authorization_code');
INSERT INTO grant_types (id, grant_type) VALUES (2, 'client_credentials');
INSERT INTO grant_types (id, grant_type) VALUES (3, 'password');
INSERT INTO grant_types (id, grant_type) VALUES (4, 'refresh_token');

-- associate some grant_types to client
INSERT INTO client_grant_types(grant_type_id, client_id) 
 VALUES (1, 'client1'), (2, 'client1'), (3, 'client1'), (4, 'client1');


-- generate a sample auth code for client1
INSERT INTO auth_codes(authorization_code, user_id, redirect_uri, scope, client_id, expires_in, created_at) VALUES('authcode1', 1, 'http://localhost:9001/', 'all', 'client1', 3600, date('now'));
