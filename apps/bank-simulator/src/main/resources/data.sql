-- Dodajemo jednog klijenta (Prodavca/Merchanta)
INSERT INTO bank_client (id, name, email, merchant_id, merchant_password)
VALUES (1, 'Rent-A-Car Doo', 'office@rentacar.com', 'MERCH_123', 'PASS_123');

-- Dodajemo račun prodavca
INSERT INTO bank_account (id, account_number, balance, currency, client_id)
VALUES (1, '160-0000000000001-01', 0.00, 'RSD', 1);

-- Dodajemo klijenta (Kupca)
INSERT INTO bank_client (id, name, email)
VALUES (2, 'Petar Petrović', 'petar@gmail.com');

-- Dodajemo račun i karticu kupca (sa kojih ćeš skidati novac)
INSERT INTO bank_account (id, account_number, balance, currency, client_id)
VALUES (2, '160-0000000000002-02', 50000.00, 'RSD', 2);

-- KARTICA ZA TESTIRANJE (mora proći Luhn check!)
INSERT INTO bank_card (id, pan, security_code, card_holder_name, expiration_date, account_id)
VALUES (1, '4539123456781235', '123', 'PETAR PETROVIC', '12/26', 2);