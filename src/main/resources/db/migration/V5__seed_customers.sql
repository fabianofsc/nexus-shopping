INSERT INTO customers (name, document, document_type, status)
VALUES
    ('Benjamin Bryan Duarte', '02648629025', 'CPF', 'ACTIVE'),
    ('Marli Cristiane Marlene Alves', '58119974000', 'CPF', 'ACTIVE'),
    ('Pietra Isabela Maitê da Mata', '58119974001', 'CPF', 'ACTIVE'),
    ('Enzo Kaique Rocha', '18184222230', 'CNH', 'ACTIVE'),
    ('Iago Noah Vieira', '71613090845', 'CNH', 'ACTIVE'),
    ('Cláudia Elaine Eloá Galvão', '378149714', 'RG', 'ACTIVE'),
    ('Marina Giovanna Milena Araújo', '108237126', 'RG', 'ACTIVE'),
    ('Marcos Vinicius André Mário Almeida', '265206510', 'RG', 'ACTIVE'),
    ('Luzia Mariah Rebeca Rodrigues', '34150598000117', 'CNPJ', 'ACTIVE'),
    ('Maitê Yasmin Cardoso', '01879119000149', 'CNPJ', 'ACTIVE');

INSERT INTO customer_contacts (customer_id, email, phone)
VALUES
    (1, 'benjamin-duarte86@lexos.com.br', '+5549983253446'),
    (2, 'marli_alves@brunofaria.com', '+5561998656938'),
    (3, 'pietraisabeladamata@corp.globo.com', '+5541989739924'),
    (4, 'enzo_kaique_rocha@babo.adv.br', '+5519981645847'),
    (5, 'iago_noah_vieira@fcacomputers.com.br', '+5527986188066'),
    (6, 'claudiaelainegalvao@athos.srv.br', '+5579995737583'),
    (7, 'marina.giovanna.araujo@acquire.com.br', '+5585991735780'),
    (8, 'marcos_almeida@pmi.com', '+5551992796027'),
    (9, 'luzia-rodrigues81@likaleal.com.br', '+5521982894808'),
    (10, 'maite.yasmin.cardoso@zoomfoccus.com.br', '+5585998238693');

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
    (1, 'Rua Concórdia', '736', NULL, 'Petrópolis', 'Lages', 'SC', '88505334', 'BR'),
    (2, 'Conjunto SHA Conjunto 5 Chácara 43', '786', NULL, 'Setor Habitacional Arniqueira (Águas Claras)', 'Brasília', 'DF', '71995275', 'BR'),
    (3, 'Rua Rosa Cordeiro Bitencourt', '903', NULL, 'Vila São Vicente', 'Paranaguá', 'PR', '83209208', 'BR'),
    (4, 'Travessa Cristóvão Donati', '669', NULL, 'Piracicamirim', 'Piracicaba', 'SP', '13418605', 'BR'),
    (5, 'Rua Espírito Santo', '987', NULL, 'Areinha', 'Viana', 'ES', '29137147', 'BR'),
    (6, 'Rua Rafael de Aguiar', '557', NULL, 'Pereira Lobo', 'Aracaju', 'SE', '49052220', 'BR'),
    (7, 'Rua 541D', '416', NULL, 'Conjunto Ceará', 'Fortaleza', 'CE', '60531490', 'BR'),
    (8, 'Rua Antônio Saber', '964', NULL, 'Jardim Itu Sabará', 'Porto Alegre', 'RS', '91220640', 'BR'),
    (9, 'Rua Cecília Meireles', '270', NULL, 'Parada Angélica', 'Duque de Caxias', 'RJ', '25272298', 'BR'),
    (10, 'Rua Eugênia', '994', NULL, 'Bom Futuro', 'Fortaleza', 'CE', '60416500', 'BR');
