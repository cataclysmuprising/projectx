SET search_path TO projectx;

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
(id, app_name, page, action_name, display_name, action_type, access_level, url, description, created_by, updated_by, created_date, updated_date)
VALUES
-- Dashboard
(10011, 'projectx', 'Dashboard',            'dashboard',                   'View dashboard overview',                          'MAIN', 'READ',      '/web/sec/dashboard',                                               'Access the business and operational dashboard overview.', 0, 0, current_timestamp, current_timestamp),

-- Administrator
(10021, 'projectx', 'Administrator',        'administratorList',           'View administrators',                              'MAIN', 'READ',      '/web/sec/administrator',                                           'Browse the administrator directory.', 0, 0, current_timestamp, current_timestamp),
(10022, 'projectx', 'Administrator',        'administratorAdd',            'Create administrator',                             'SUB',  'SENSITIVE', '/web/sec/administrator/add',                                       'Register a new administrator account.', 0, 0, current_timestamp, current_timestamp),
(10023, 'projectx', 'Administrator',        'administratorEdit',           'Edit administrator',                               'SUB',  'SENSITIVE', '/web/sec/administrator/{id}/edit',                                 'Update administrator profile and role assignments.', 0, 0, current_timestamp, current_timestamp),
(10024, 'projectx', 'Administrator',        'administratorRemove',         'Delete administrator',                             'SUB',  'SENSITIVE', '/web/sec/administrator/{id}/delete',                               'Remove an administrator account from the system.', 0, 0, current_timestamp, current_timestamp),
(10025, 'projectx', 'Administrator',        'administratorDetail',         'View administrator detail',                        'SUB',  'READ',      '/web/sec/administrator/{id}/detail',                               'Open the administrator detail screen.', 0, 0, current_timestamp, current_timestamp),

-- Role
(10031, 'projectx', 'Role',                 'roleList',                    'View roles',                                       'MAIN', 'READ',      '/web/sec/role',                                                    'Browse the role catalog.', 0, 0, current_timestamp, current_timestamp),
(10032, 'projectx', 'Role',                 'roleAdd',                     'Create role',                                      'SUB',  'SENSITIVE', '/web/sec/role/add',                                                'Create a new business role.', 0, 0, current_timestamp, current_timestamp),
(10033, 'projectx', 'Role',                 'roleEdit',                    'Edit role',                                        'SUB',  'SENSITIVE', '/web/sec/role/{id}/edit',                                          'Edit an existing business role.', 0, 0, current_timestamp, current_timestamp),
(10034, 'projectx', 'Role',                 'roleRemove',                  'Delete role',                                      'SUB',  'SENSITIVE', '/web/sec/role/{id}/delete',                                        'Delete a custom role.', 0, 0, current_timestamp, current_timestamp),

----- Actuator
(10180, 'projectx', 'Actuator',             'actuatorSettings',            'View actuator tools',                              'MAIN', 'READ',      '/web/sec/settings/actuator',               'Open the actuator operations page.', 0, 0, current_timestamp, current_timestamp),
(10181, 'projectx', 'Actuator',             'actuatorIndex',               'View actuator index',                              'SUB',  'READ',      '/actuator',                                'Read the actuator index endpoint.', 0, 0, current_timestamp, current_timestamp),
(10182, 'projectx', 'Actuator',             'actuatorHealth',              'View actuator health',                             'SUB',  'READ',      '/actuator/health',                         'Read the actuator health endpoint.', 0, 0, current_timestamp, current_timestamp),
(10183, 'projectx', 'Actuator',             'actuatorHealthProbe',         'View actuator health probes',                      'SUB',  'READ',      '/actuator/health/**',                      'Read actuator liveness and readiness probes.', 0, 0, current_timestamp, current_timestamp),
(10184, 'projectx', 'Actuator',             'actuatorInfo',                'View actuator info',                               'SUB',  'READ',      '/actuator/info',                           'Read the actuator info endpoint.', 0, 0, current_timestamp, current_timestamp),
(10185, 'projectx', 'Actuator',             'actuatorPrometheus',          'View actuator prometheus',                         'SUB',  'READ',      '/actuator/prometheus',                     'Read the Prometheus metrics endpoint.', 0, 0, current_timestamp, current_timestamp),
(10186, 'projectx', 'Actuator',             'actuatorMetrics',             'View actuator metrics',                            'SUB',  'SENSITIVE', '/actuator/metrics',                        'Read the actuator metrics index.', 0, 0, current_timestamp, current_timestamp),
(10187, 'projectx', 'Actuator',             'actuatorMetricDetails',       'View actuator metric detail',                      'SUB',  'SENSITIVE', '/actuator/metrics/**',                     'Read detailed actuator metric output.', 0, 0, current_timestamp, current_timestamp),
(10188, 'projectx', 'Actuator',             'actuatorThreadDump',          'View thread dump',                                 'SUB',  'SENSITIVE', '/actuator/threaddump',                     'Read the actuator thread dump.', 0, 0, current_timestamp, current_timestamp),
(10189, 'projectx', 'Actuator',             'actuatorLoggers',             'View logger settings',                             'SUB',  'SENSITIVE', '/actuator/loggers',                        'Read actuator logger configuration.', 0, 0, current_timestamp, current_timestamp),
(10190, 'projectx', 'Actuator',             'actuatorLoggerDetails',       'View logger detail',                               'SUB',  'SENSITIVE', '/actuator/loggers/**',                     'Read detailed actuator logger configuration.', 0, 0, current_timestamp, current_timestamp),
(10191, 'projectx', 'Actuator',             'actuatorCaches',              'View cache details',                               'SUB',  'SENSITIVE', '/actuator/caches',                         'Read actuator cache details.', 0, 0, current_timestamp, current_timestamp),
(10192, 'projectx', 'Actuator',             'actuatorCacheDetails',        'View cache detail',                                'SUB',  'SENSITIVE', '/actuator/caches/**',                      'Read detailed actuator cache data.', 0, 0, current_timestamp, current_timestamp),
(10193, 'projectx', 'Actuator',             'actuatorScheduledTasks',      'View scheduled tasks',                             'SUB',  'SENSITIVE', '/actuator/scheduledtasks',                 'Read actuator scheduled-task details.', 0, 0, current_timestamp, current_timestamp),
(10195, 'projectx', 'Actuator',             'actuatorMappings',            'View request mappings',                            'SUB',  'SENSITIVE', '/actuator/mappings',                       'Read application request mappings.', 0, 0, current_timestamp, current_timestamp),
(10196, 'projectx', 'Actuator',             'actuatorBeans',               'View spring beans',                                'SUB',  'SENSITIVE', '/actuator/beans',                          'Read Spring bean wiring details.', 0, 0, current_timestamp, current_timestamp),
(10197, 'projectx', 'Actuator',             'actuatorConditions',          'View auto-configuration conditions',               'SUB',  'SENSITIVE', '/actuator/conditions',                     'Read actuator auto-configuration conditions.', 0, 0, current_timestamp, current_timestamp),
(10198, 'projectx', 'Actuator',             'actuatorConfigProps',         'View configuration properties',                    'SUB',  'SENSITIVE', '/actuator/configprops/**',                 'Read actuator configuration-property output.', 0, 0, current_timestamp, current_timestamp),
(10199, 'projectx', 'Actuator',             'actuatorEnv',                 'View environment details',                         'SUB',  'SENSITIVE', '/actuator/env/**',                         'Read environment and property details.', 0, 0, current_timestamp, current_timestamp),
(10201, 'projectx', 'Actuator',             'actuatorProjectX',            'View ProjectX ops summary',                        'SUB',  'SENSITIVE', '/actuator/projectx',                       'Read the ProjectX operational summary endpoint.', 0, 0, current_timestamp, current_timestamp),
(10202, 'projectx', 'Actuator',             'actuatorRedisSummary',        'View Redis summary',                               'SUB',  'SENSITIVE', '/api/web/sec/settings/actuator/redis/summary', 'Read Redis summary data from the actuator page.', 0, 0, current_timestamp, current_timestamp),
(10203, 'projectx', 'Actuator',             'actuatorRedisSearch',         'Search Redis keys',                                'SUB',  'SENSITIVE', '/api/web/sec/settings/actuator/redis/keys', 'Search Redis keys from the actuator page.', 0, 0, current_timestamp, current_timestamp),
(10204, 'projectx', 'Actuator',             'actuatorRedisDelete',         'Delete Redis key',                                 'SUB',  'SENSITIVE', '/api/web/sec/settings/actuator/redis/delete', 'Delete Redis keys from the actuator page.', 0, 0, current_timestamp, current_timestamp);



SELECT setval(
  pg_get_serial_sequence('mjr_action', 'id'),
  (SELECT MAX(id) FROM mjr_action),
  true
);

/******      Action Route        ******/
INSERT INTO mjr_action_route
(
    app_name,
    route_pattern,
    http_method,
    route_kind,
    priority,
    active,
    description,
    created_date,
    created_by,
    updated_date,
    updated_by
)
SELECT
    a.app_name,
    a.url,
    'ANY',
    'PRIMARY',
    200,
    TRUE,
    'Primary route for action ' || a.action_name,
    current_timestamp,
    0,
    current_timestamp,
    0
FROM mjr_action a
ORDER BY a.id;

INSERT INTO mjr_action_route_x_action
(
    route_id,
    action_id,
    created_date,
    created_by,
    updated_date,
    updated_by
)
SELECT
    r.id,
    a.id,
    current_timestamp,
    0,
    current_timestamp,
    0
FROM mjr_action a
JOIN mjr_action_route r
  ON r.app_name = a.app_name
 AND r.http_method = 'ANY'
 AND r.route_pattern = a.url
ORDER BY a.id;

CREATE TEMP TABLE tmp_mjr_action_route_seed
(
    route_pattern VARCHAR(250) NOT NULL,
    http_method   VARCHAR(16)  NOT NULL,
    route_kind    VARCHAR(20)  NOT NULL,
    priority      INTEGER      NOT NULL,
    description   VARCHAR(200) NOT NULL,
    action_names  VARCHAR[]    NOT NULL
) ON COMMIT DROP;


INSERT INTO tmp_mjr_action_route_seed
(
    route_pattern,
    http_method,
    route_kind,
    priority,
    description,
    action_names
)
VALUES
    ('/api/web/sec/dashboard/summary', 'ANY', 'PAGE_SUPPORT', 160, 'Dashboard summary API', ARRAY['dashboard']),
    ('/api/web/sec/administrator/search/paging', 'ANY', 'SHARED_LOOKUP', 140, 'Administrator paging API for admin and role setup', ARRAY['administratorList','roleAdd','roleEdit']),
    ('/api/web/sec/role/search/paging', 'ANY', 'PAGE_SUPPORT', 160, 'Role list paging API', ARRAY['roleList']),
     ('/api/web/sec/role/search/all', 'ANY', 'SHARED_LOOKUP', 140, 'Role list all API', ARRAY['administratorAdd','administratorEdit','administratorDetail']),
    ('/api/web/sec/action/search/paging', 'ANY', 'SHARED_LOOKUP', 140, 'Action paging API for role setup', ARRAY['roleAdd','roleEdit']),
    ('/api/web/sec/action/search/keyset', 'ANY', 'SHARED_LOOKUP', 140, 'Action keyset API for role setup', ARRAY['roleAdd','roleEdit']),
    ('/api/web/sec/action/pages', 'ANY', 'SHARED_LOOKUP', 140, 'Action module lookup API for role setup', ARRAY['roleAdd','roleEdit']);



INSERT INTO mjr_action_route
(
    app_name,
    route_pattern,
    http_method,
    route_kind,
    priority,
    active,
    description,
    created_date,
    created_by,
    updated_date,
    updated_by
)
SELECT
    'projectx',
    s.route_pattern,
    s.http_method,
    s.route_kind,
    s.priority,
    TRUE,
    s.description,
    current_timestamp,
    0,
    current_timestamp,
    0
FROM tmp_mjr_action_route_seed s;

INSERT INTO mjr_action_route_x_action
(
    route_id,
    action_id,
    created_date,
    created_by,
    updated_date,
    updated_by
)
SELECT
    r.id,
    a.id,
    current_timestamp,
    0,
    current_timestamp,
    0
FROM tmp_mjr_action_route_seed s
JOIN mjr_action_route r
  ON r.app_name = 'projectx'
 AND r.route_pattern = s.route_pattern
 AND r.http_method = s.http_method
JOIN LATERAL unnest(s.action_names) AS seeded_action(action_name)
  ON TRUE
JOIN mjr_action a
  ON a.app_name = 'projectx'
 AND a.action_name = seeded_action.action_name;


/******      Role Action        ******/
-- ### SUPER-USER role
INSERT INTO mjr_role_x_action
(
    role_id,
    action_id,
    created_by,
    updated_by,
    created_date,
    updated_date
)
SELECT
    role_row.id,
    a.id,
    0,
    0,
    current_timestamp,
    current_timestamp
FROM mjr_role role_row
JOIN mjr_action a
  ON a.app_name = role_row.app_name
WHERE role_row.app_name = 'projectx'
  AND role_row.name = 'SUPER-USER';

-- ### ADMINISTRATOR role
INSERT INTO mjr_role_x_action
(
    role_id,
    action_id,
    created_by,
    updated_by,
    created_date,
    updated_date
)
SELECT
    role_row.id,
    a.id,
    0,
    0,
    current_timestamp,
    current_timestamp
FROM
(
    VALUES
        ('dashboard'),
        ('administratorList'),
        ('administratorDetail')
) AS seed(action_name)
JOIN mjr_role role_row
  ON role_row.app_name = 'projectx'
 AND role_row.name = 'ADMINISTRATOR'
JOIN mjr_action a
  ON a.app_name = role_row.app_name
 AND a.action_name = seed.action_name;


SELECT setval(
  pg_get_serial_sequence('mjr_role_x_action', 'id'),
  (SELECT MAX(id) FROM mjr_role_x_action),
  true
);

/******      Administrator        ******/
INSERT INTO mjr_admin
(id,    name,                    login_id,                 password,                                                                           status,   created_by,updated_by,created_date,updated_date)
VALUES
(0,     'SYSTEM_ADMIN',      'Rye7hnyjUmwlipDFpCSwTxk6WEIkPdK.tq',              '$2b$12$P9H0Axn5Z2e0KzXxN1F7sO2Tn0vQn2kG8s5L1r9HfKzW9bJv6kK2u',                'ACTIVE',      0,0,current_timestamp,current_timestamp),
-- alice@SU#P@ss1
(1,     'Alice Carter',      'alice@superuser',        '$2b$12$Czy1Dzq9n5IqwCLVFq.Rye7hnyjUmwlipDFpCSwTxk6WEIkPdK.tq',            'ACTIVE',      0,0,current_timestamp,current_timestamp),
-- bob@SU#P@ss2
(2,     'Bob Nguyen',     'bob@superuser',        '$2b$12$Icajs9ImSzgyDdDpeUFyAuyx3U2h5//AFG1cLH5kaljqAfI8TetD.',                'ACTIVE',      0,0,current_timestamp,current_timestamp),
-- carol@SU#P@ss3
(3,     'Carol Smith',     'carol@superuser',            '$2b$12$.qmLchqedFNuVPUyJzMMKuKQEtqUpVKiIZx.t.qsfm0UjGoz6wNJG',                'ACTIVE',      0,0,current_timestamp,current_timestamp);
-- SYSTEM_ADMIN (non-interactive system actor)


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
(3,     3,            2,      0,0,current_timestamp,current_timestamp), -- Carol Smith > ADMINISTRATOR role
(4,     0,            1,      0,0,current_timestamp,current_timestamp); -- SYSTEM_ADMIN > SUPER-USER role

SELECT setval(
  pg_get_serial_sequence('mjr_admin_x_role', 'id'),
  (SELECT MAX(id) FROM mjr_admin_x_role),
  true
);
