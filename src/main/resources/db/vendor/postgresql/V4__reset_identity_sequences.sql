-- After the V2 seed inserted explicit ids, the IDENTITY sequences were not
-- advanced. Reset each sequence to max(id) so the next auto-generated value
-- does not collide with seed data.
SELECT setval(pg_get_serial_sequence('brands',     'id'), COALESCE(MAX(id), 0)) FROM brands;
SELECT setval(pg_get_serial_sequence('categories', 'id'), COALESCE(MAX(id), 0)) FROM categories;
SELECT setval(pg_get_serial_sequence('products',   'id'), COALESCE(MAX(id), 0)) FROM products;
