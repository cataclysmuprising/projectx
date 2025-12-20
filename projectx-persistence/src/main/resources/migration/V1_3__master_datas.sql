/******      Role        ******/
-- BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
INSERT INTO mjr_role
(id, app_name, name, description, type, created_by, updated_by, created_date, updated_date)
VALUES
(1, 'projectx', 'SUPER-USER', 'Master role to manage entire application. This role own special right, advantage, or immunity granted or available.', 'SUPERUSER', 0, 0, current_timestamp, current_timestamp),
(2, 'projectx', 'ADMINISTRATOR', 'Administrator role to manage main functions.', 'BUILT_IN', 0, 0, current_timestamp, current_timestamp);

SELECT setval(
  pg_get_serial_sequence('mjr_role', 'id'),
  (SELECT MAX(id) FROM mjr_role),
  true
);
/******      action        ******/
INSERT INTO mjr_action
(id, app_name, page, action_name, display_name, action_type, url, description, created_by, updated_by, created_date, updated_date)
VALUES 
----- Dashboard
(10011, 'projectx', 'Dashboard', 'dashboard', 'Dashboard page for authenticated User', 'MAIN', '^/sec/dashboard$', 'Control panel page for Sign-in Admin.', 0, 0, current_timestamp, current_timestamp),

----- Users
(10021, 'projectx', 'User', 'userList', 'User Home page', 'MAIN', '^/sec/users$', 'Home page to view all Staff informations.', 0, 0, current_timestamp, current_timestamp),
(10022, 'projectx', 'User', 'userAdd', 'User register page', 'SUB', '^/sec/users/add$', 'This action is for to create new Staff.', 0, 0, current_timestamp, current_timestamp),
(10023, 'projectx', 'User', 'userEdit', 'User edit page', 'SUB', '^/sec/users/[0-9]{1,}/edit$', 'This action can edit personal informations of specific Staff.', 0, 0, current_timestamp, current_timestamp),
(10024, 'projectx', 'User', 'userRemove', 'To remove existing user', 'SUB', '^/sec/users/[0-9]{1,}/delete$', 'To remove a specific Staff from application.', 0, 0, current_timestamp, current_timestamp),
(10025, 'projectx', 'User', 'userDetail', 'View detail information of a user', 'SUB', '^/sec/users/[0-9]{1,}/detail$', 'To view the detail informations of each Staff.', 0, 0, current_timestamp, current_timestamp);
SELECT setval(
  pg_get_serial_sequence('mjr_action', 'id'),
  (SELECT MAX(id) FROM mjr_action),
  true
);
/******      Role Action        ******/
INSERT INTO mjr_role_x_action
(id, role_id, action_id, created_by, updated_by, created_date, updated_date)
VALUES 
----- Super User
----- Dashboard
(1, 1, 10011, 0, 0, current_timestamp, current_timestamp),

----- User
(2, 1, 10021, 0, 0, current_timestamp, current_timestamp),
(3, 1, 10022, 0, 0, current_timestamp, current_timestamp),
(4, 1, 10023, 0, 0, current_timestamp, current_timestamp),
(5, 1, 10024, 0, 0, current_timestamp, current_timestamp),
(6, 1, 10025, 0, 0, current_timestamp, current_timestamp),


----- User
(7, 2, 10021, 0, 0, current_timestamp, current_timestamp),
(8, 2, 10023, 0, 0, current_timestamp, current_timestamp),
(9, 2, 10025, 0, 0, current_timestamp, current_timestamp);

SELECT setval(
  pg_get_serial_sequence('mjr_role_x_action', 'id'),
  (SELECT MAX(id) FROM mjr_role_x_action),
  true
);

/******      Administrator        ******/
INSERT INTO mjr_admin
(id,    name,                    login_id,                 password,                                                                           status,   created_by,updated_by,created_date,updated_date)
VALUES
-- alice@SU#P@ss1
(1,     'Alice Carter',      'alice@superuser',        '$2b$12$Czy1Dzq9n5IqwCLVFq.Rye7hnyjUmwlipDFpCSwTxk6WEIkPdK.tq',            'ACTIVE',      0,0,current_timestamp,current_timestamp),
-- bob@SU#P@ss2
(2,     'Bob Nguyen',     'bob@superuser',        '$2b$12$Icajs9ImSzgyDdDpeUFyAuyx3U2h5//AFG1cLH5kaljqAfI8TetD.',                'ACTIVE',      0,0,current_timestamp,current_timestamp),
-- carol@SU#P@ss3
(3,     'Carol Smith',     'carol@superuser',            '$2b$12$.qmLchqedFNuVPUyJzMMKuKQEtqUpVKiIZx.t.qsfm0UjGoz6wNJG',                'ACTIVE',      0,0,current_timestamp,current_timestamp);

SELECT setval(
  pg_get_serial_sequence('mjr_admin', 'id'),
  (SELECT MAX(id) FROM mjr_admin),
  true
);

/******      Administrator x Roles      ******/
INSERT INTO mjr_admin_x_role
(id,    admin_id,           role_id,    created_by,updated_by,created_date,updated_date)
VALUES
(1,     1,            1,      0,0,current_timestamp,current_timestamp),
(2,     2,            1,      0,0,current_timestamp,current_timestamp),
(3,     3,            1,      0,0,current_timestamp,current_timestamp);

SELECT setval(
  pg_get_serial_sequence('mjr_admin_x_role', 'id'),
  (SELECT MAX(id) FROM mjr_admin_x_role),
  true
);
