INSERT INTO customers (name, document, document_type, status)
VALUES
    ('Customer 1', '02648629025', 'CPF', 'ACTIVE'),
    ('Customer 2', '58119974000', 'CPF', 'ACTIVE'),
    ('Customer 3', '58119974000', 'CPF', 'ACTIVE'),
    ('Customer 4', '18184222230', 'CNH', 'ACTIVE'),
    ('Customer 5', '71613090845', 'CNH', 'ACTIVE'),
    ('Customer 6', '378149714', 'RG', 'ACTIVE'),
    ('Customer 7', '108237126', 'RG', 'ACTIVE'),
    ('Customer 8', '265206510', 'RG', 'ACTIVE'),
    ('Customer 9', '34150598000117', 'CNPJ', 'ACTIVE'),
    ('Customer 10', '01879119000149', 'CNPJ', 'ACTIVE');

INSERT INTO customer_contacts (customer_id, email, phone)
VALUES
    (1, 'customer1@example.com', '+5511999990001'),
    (2, 'customer2@example.com', '+5511999990002'),
    (3, 'customer3@example.com', '+5511999990003'),
    (4, 'customer4@example.com', '+5511999990004'),
    (5, 'customer5@example.com', '+5511999990005'),
    (6, 'customer6@example.com', '+5511999990006'),
    (7, 'customer7@example.com', '+5511999990007'),
    (8, 'customer8@example.com', '+5511999990008'),
    (9, 'customer9@example.com', '+5511999990009'),
    (10, 'customer10@example.com', '+5511999990010');

INSERT INTO customer_addresses (
    customer_id,
    street,
    number,
    complement,
    neighborhood,
    city,
    state,
    zip_code,
    country
)
VALUES
    (1, 'Rua Customer 1', '101', NULL, 'Centro', 'Sao Paulo', 'SP', '01001001', 'BR'),
    (2, 'Rua Customer 2', '102', NULL, 'Centro', 'Sao Paulo', 'SP', '01001002', 'BR'),
    (3, 'Rua Customer 3', '103', NULL, 'Centro', 'Sao Paulo', 'SP', '01001003', 'BR'),
    (4, 'Rua Customer 4', '104', NULL, 'Centro', 'Sao Paulo', 'SP', '01001004', 'BR'),
    (5, 'Rua Customer 5', '105', NULL, 'Centro', 'Sao Paulo', 'SP', '01001005', 'BR'),
    (6, 'Rua Customer 6', '106', NULL, 'Centro', 'Sao Paulo', 'SP', '01001006', 'BR'),
    (7, 'Rua Customer 7', '107', NULL, 'Centro', 'Sao Paulo', 'SP', '01001007', 'BR'),
    (8, 'Rua Customer 8', '108', NULL, 'Centro', 'Sao Paulo', 'SP', '01001008', 'BR'),
    (9, 'Rua Customer 9', '109', NULL, 'Centro', 'Sao Paulo', 'SP', '01001009', 'BR'),
    (10, 'Rua Customer 10', '110', NULL, 'Centro', 'Sao Paulo', 'SP', '01001010', 'BR');
