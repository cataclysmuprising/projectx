/******      Role        ******/
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
(10011,     'projectx','Dashboard',         'dashboard',                'Dashboard page for authenticated Administrator',       'MAIN',     '/web/sec/dashboard',                       '....',0,0,current_timestamp,current_timestamp),
----- Administrator
(10021,     'projectx','Administrator',     'administratorList',        'Administrator Home page',					            'MAIN',     '/web/sec/administrator',                   '....',0,0,current_timestamp,current_timestamp),
(10022,     'projectx','Administrator',     'administratorAdd',         'Administrator register page',			                'SUB',      '/web/sec/administrator/add',               '....',0,0,current_timestamp,current_timestamp),
(10023,     'projectx','Administrator',     'administratorEdit',        'Administrator edit page',					            'SUB',      '/web/sec/administrator/{id}/edit',         '....',0,0,current_timestamp,current_timestamp),
(10024,     'projectx','Administrator',     'administratorRemove',      'To remove existing administrator',			            'SUB',      '/web/sec/administrator/{id}/delete',       '....',0,0,current_timestamp,current_timestamp),
(10025,     'projectx','Administrator',     'administratorDetail',      'View detail information of an administrator',          'SUB',      '/web/sec/administrator/{id}/detail',       '....',0,0,current_timestamp,current_timestamp),
----- Role
(10031,     'projectx','Role',              'roleList',                 'Role Home page',								        'MAIN',     '/web/sec/role',                            '....',0,0,current_timestamp,current_timestamp),
(10032,     'projectx','Role',              'roleAdd',                  'Role register page',								    'SUB',      '/web/sec/role/add',                        '....',0,0,current_timestamp,current_timestamp),
(10033,     'projectx','Role',              'roleEdit',                 'Role edit page',								        'SUB',      '/web/sec/role/{id}/edit',                  '....',0,0,current_timestamp,current_timestamp),
(10034,     'projectx','Role',              'roleRemove',               'To remove existing role',								'SUB',      '/web/sec/role/{id}/delete',                '....',0,0,current_timestamp,current_timestamp);
SELECT setval(
  pg_get_serial_sequence('mjr_action', 'id'),
  (SELECT MAX(id) FROM mjr_action),
  true
);
/******      Role Action        ******/
INSERT INTO mjr_role_x_action
(id, role_id, action_id, created_by, updated_by, created_date, updated_date)
VALUES 
-- ### SUPER-USER role
----- Dashboard
(1, 1, 10011, 0, 0, current_timestamp, current_timestamp),
----- Administrator
(2,     1, 10021, 0, 0, current_timestamp, current_timestamp),
(3,     1, 10022, 0, 0, current_timestamp, current_timestamp),
(4,     1, 10023, 0, 0, current_timestamp, current_timestamp),
(5,     1, 10024, 0, 0, current_timestamp, current_timestamp),
(6,     1, 10025, 0, 0, current_timestamp, current_timestamp),

----- Role
(7,     1, 10031, 0, 0, current_timestamp, current_timestamp),
(8,     1, 10032, 0, 0, current_timestamp, current_timestamp),
(9,     1, 10033, 0, 0, current_timestamp, current_timestamp),
(10,    1, 10034, 0, 0, current_timestamp, current_timestamp),


-- ### ADMINISTRATOR role
(11,    2, 10011, 0, 0, current_timestamp, current_timestamp),
(12,    2, 10021, 0, 0, current_timestamp, current_timestamp),
(13,    2, 10025, 0, 0, current_timestamp, current_timestamp);

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
(id,       admin_id,             role_id,       created_by,updated_by,created_date,updated_date)
VALUES
(1,     1,            1,      0,0,current_timestamp,current_timestamp), -- Alice Carter > SUPER-USER role
(2,     2,            2,      0,0,current_timestamp,current_timestamp), -- Bob Nguyen > ADMINISTRATOR role
(3,     3,            2,      0,0,current_timestamp,current_timestamp); -- Carol Smith > ADMINISTRATOR role

SELECT setval(
  pg_get_serial_sequence('mjr_admin_x_role', 'id'),
  (SELECT MAX(id) FROM mjr_admin_x_role),
  true
);
