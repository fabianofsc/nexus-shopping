-- H2 does not advance the IDENTITY sequence when explicit ids are inserted.
-- Restart each sequence well above the seed data range so generated ids
-- never collide with seed rows.
ALTER TABLE brands     ALTER COLUMN id RESTART WITH 100001;
ALTER TABLE categories ALTER COLUMN id RESTART WITH 100001;
ALTER TABLE products   ALTER COLUMN id RESTART WITH 10000001;
