-- Sample data (same data the application loads automatically on first start).
-- Run AFTER the tables exist (start the app once with app.seed-sample-data=false, or run schema.sql):
--   mysql -u root -p task_assignment_db < database/sample_data.sql
--
-- Contains one OVERLOADED employee (Aarav Sharma: 7 active tasks) and one employee exactly
-- AT THE LIMIT (Priya Reddy: 5 active tasks) so you can test the workload rules immediately.

USE task_assignment_db;

INSERT INTO employees (id, name, email, department, designation) VALUES
 (1, 'Aarav Sharma', 'aarav.sharma@example.com', 'Engineering',     'Senior Developer'),
 (2, 'Priya Reddy',  'priya.reddy@example.com',  'Marketing',       'Marketing Manager'),
 (3, 'Rohan Mehta',  'rohan.mehta@example.com',  'Operations',      'Operations Lead'),
 (4, 'Sneha Iyer',   'sneha.iyer@example.com',   'Human Resources', 'HR Executive'),
 (5, 'Karthik Rao',  'karthik.rao@example.com',  'Engineering',     'QA Engineer');

INSERT INTO tasks (title, description, priority, deadline, status, assigned_at, created_at, employee_id) VALUES
 -- Aarav: 7 active tasks -> overloaded
 ('Design database migration plan',  'Plan schema changes for the next release', 'HIGH',   DATE_ADD(CURDATE(), INTERVAL 3 DAY),  'IN_PROGRESS', NOW() - INTERVAL 1 HOUR, NOW(), 1),
 ('Refactor authentication module',  'Split auth into smaller services',         'HIGH',   DATE_ADD(CURDATE(), INTERVAL 5 DAY),  'IN_PROGRESS', NOW() - INTERVAL 2 HOUR, NOW(), 1),
 ('Write API documentation',         'Document all public REST endpoints',       'MEDIUM', DATE_ADD(CURDATE(), INTERVAL 7 DAY),  'PENDING',     NOW() - INTERVAL 3 HOUR, NOW(), 1),
 ('Fix payment gateway timeout',     'Customers see timeouts at checkout',       'HIGH',   DATE_ADD(CURDATE(), INTERVAL 2 DAY),  'IN_PROGRESS', NOW() - INTERVAL 4 HOUR, NOW(), 1),
 ('Set up CI pipeline',              'Build and test on every commit',           'MEDIUM', DATE_ADD(CURDATE(), INTERVAL 10 DAY), 'PENDING',     NOW() - INTERVAL 5 HOUR, NOW(), 1),
 ('Code review for release 2.3',     'Review open pull requests',                'LOW',    DATE_ADD(CURDATE(), INTERVAL 4 DAY),  'PENDING',     NOW() - INTERVAL 6 HOUR, NOW(), 1),
 ('Optimize report queries',         'Slow monthly reports need indexes',        'MEDIUM', DATE_ADD(CURDATE(), INTERVAL 8 DAY),  'PENDING',     NOW() - INTERVAL 7 HOUR, NOW(), 1),
 -- Priya: exactly 5 active tasks -> at the limit
 ('Prepare Q4 marketing report',     'Campaign results and spend',               'MEDIUM', DATE_ADD(CURDATE(), INTERVAL 6 DAY),  'IN_PROGRESS', NOW() - INTERVAL 8 HOUR,  NOW(), 2),
 ('Update brand guidelines',         'Refresh logo usage and colours',           'LOW',    DATE_ADD(CURDATE(), INTERVAL 12 DAY), 'PENDING',     NOW() - INTERVAL 9 HOUR,  NOW(), 2),
 ('Plan product launch event',       'Venue, guests and schedule',               'HIGH',   DATE_ADD(CURDATE(), INTERVAL 9 DAY),  'IN_PROGRESS', NOW() - INTERVAL 10 HOUR, NOW(), 2),
 ('Social media campaign',           'Two-week campaign calendar',               'MEDIUM', DATE_ADD(CURDATE(), INTERVAL 5 DAY),  'PENDING',     NOW() - INTERVAL 11 HOUR, NOW(), 2),
 ('Customer survey analysis',        'Summarise survey answers',                 'MEDIUM', DATE_ADD(CURDATE(), INTERVAL 11 DAY), 'PENDING',     NOW() - INTERVAL 12 HOUR, NOW(), 2),
 -- Rohan: 3 active + 1 completed
 ('Onboard new vendors',             'Collect documents and contracts',          'MEDIUM', DATE_ADD(CURDATE(), INTERVAL 6 DAY),  'PENDING',     NOW() - INTERVAL 13 HOUR, NOW(), 3),
 ('Negotiate office lease',          'Renewal terms for the main office',        'HIGH',   DATE_ADD(CURDATE(), INTERVAL 4 DAY),  'IN_PROGRESS', NOW() - INTERVAL 14 HOUR, NOW(), 3),
 ('Audit expense reports',           'Check last quarter claims',                'LOW',    DATE_ADD(CURDATE(), INTERVAL 14 DAY), 'PENDING',     NOW() - INTERVAL 15 HOUR, NOW(), 3),
 ('Renew software licences',         'Renewals due this month',                  'MEDIUM', DATE_SUB(CURDATE(), INTERVAL 2 DAY),  'COMPLETED',   NOW() - INTERVAL 16 HOUR, NOW(), 3),
 -- Sneha: 1 active + 2 completed
 ('Screen interview candidates',     'Shortlist for the developer role',         'HIGH',   DATE_ADD(CURDATE(), INTERVAL 3 DAY),  'IN_PROGRESS', NOW() - INTERVAL 17 HOUR, NOW(), 4),
 ('Update HR policy handbook',       'Add the new leave policy',                 'LOW',    DATE_SUB(CURDATE(), INTERVAL 5 DAY),  'COMPLETED',   NOW() - INTERVAL 18 HOUR, NOW(), 4),
 ('Organise team workshop',          'Half-day planning workshop',               'MEDIUM', DATE_SUB(CURDATE(), INTERVAL 1 DAY),  'COMPLETED',   NOW() - INTERVAL 19 HOUR, NOW(), 4),
 -- Unassigned
 ('Migrate legacy reports to new BI tool', 'Move 12 old reports',                'MEDIUM', DATE_ADD(CURDATE(), INTERVAL 15 DAY), 'PENDING',     NULL, NOW(), NULL),
 ('Create onboarding checklist',     'One page for new joiners',                 'LOW',    DATE_ADD(CURDATE(), INTERVAL 20 DAY), 'PENDING',     NULL, NOW(), NULL);
