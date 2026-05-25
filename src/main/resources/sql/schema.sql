CREATE DATABASE IF NOT EXISTS meetcare
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE meetcare;

CREATE TABLE IF NOT EXISTS users (
    id_users INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    prenom VARCHAR(255) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    avatar VARCHAR(255),
    role VARCHAR(50),
    address VARCHAR(255),
    city VARCHAR(100),
    code_postal VARCHAR(20),
    country VARCHAR(100),
    is_online BOOLEAN DEFAULT FALSE,
    last_seen DATETIME,
    date DATETIME DEFAULT CURRENT_TIMESTAMP,
    first_login BOOLEAN DEFAULT TRUE,
    must_change_password BOOLEAN DEFAULT FALSE,
    account_status VARCHAR(50) DEFAULT 'ACTIVE',
    notify_email BOOLEAN DEFAULT TRUE,
    notify_sms BOOLEAN DEFAULT FALSE,
    notify_new_appointments BOOLEAN DEFAULT TRUE,
    notify_cancellations BOOLEAN DEFAULT TRUE,
    notify_new_messages BOOLEAN DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS doctors (
    id_doctors INT AUTO_INCREMENT PRIMARY KEY,
    speciality VARCHAR(100),
    experience INT,
    consultation_price DECIMAL(10,2),
    biography TEXT,
    id_users INT,
    address VARCHAR(255),
    sector VARCHAR(20),
    accepts_vitale BOOLEAN DEFAULT FALSE,
    teleconsultation BOOLEAN DEFAULT FALSE,
    public_profile_completed BOOLEAN DEFAULT FALSE,
    payment_methods VARCHAR(255),
    spoken_languages VARCHAR(255),
    office_hours TEXT,
    FOREIGN KEY (id_users) REFERENCES users(id_users) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS appointments (
    id_appointments INT AUTO_INCREMENT PRIMARY KEY,
    appointment_date DATE NOT NULL,
    appointment_time TIME NOT NULL,
    status VARCHAR(50),
    notes TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    id_doctors INT,
    id_users INT,
    FOREIGN KEY (id_doctors) REFERENCES doctors(id_doctors) ON DELETE CASCADE,
    FOREIGN KEY (id_users) REFERENCES users(id_users) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS conversations (
    id_conversations INT AUTO_INCREMENT PRIMARY KEY,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS conversation_participants (
    id_conversations INT NOT NULL,
    id_users INT NOT NULL,
    PRIMARY KEY (id_conversations, id_users),
    FOREIGN KEY (id_conversations) REFERENCES conversations(id_conversations) ON DELETE CASCADE,
    FOREIGN KEY (id_users) REFERENCES users(id_users) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS notifications (
    id_notifications INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255),
    message TEXT,
    type VARCHAR(50),
    is_read BOOLEAN DEFAULT FALSE,
    date DATETIME DEFAULT CURRENT_TIMESTAMP,
    id_users INT,
    FOREIGN KEY (id_users) REFERENCES users(id_users) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS invoices (
    id_invoices INT AUTO_INCREMENT PRIMARY KEY,
    amount DECIMAL(10,2),
    description TEXT,
    status VARCHAR(50),
    payment_method VARCHAR(50),
    paid_at DATETIME,
    id_users INT,
    FOREIGN KEY (id_users) REFERENCES users(id_users) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS chatbot_messages (
    id_chatbot_messages INT AUTO_INCREMENT PRIMARY KEY,
    question TEXT,
    response TEXT,
    model VARCHAR(50),
    date DATETIME DEFAULT CURRENT_TIMESTAMP,
    id_conversations INT,
    id_users INT,
    FOREIGN KEY (id_conversations) REFERENCES conversations(id_conversations) ON DELETE CASCADE,
    FOREIGN KEY (id_users) REFERENCES users(id_users) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS messages (
    id_messages INT AUTO_INCREMENT PRIMARY KEY,
    content TEXT,
    message_type VARCHAR(50),
    is_read BOOLEAN DEFAULT FALSE,
    sent_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    id_conversations INT,
    id_users INT,
    FOREIGN KEY (id_conversations) REFERENCES conversations(id_conversations) ON DELETE CASCADE,
    FOREIGN KEY (id_users) REFERENCES users(id_users) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS attachments (
    id_attachments INT AUTO_INCREMENT PRIMARY KEY,
    file_name VARCHAR(255),
    file_path VARCHAR(255),
    file_type VARCHAR(100),
    file_size INT,
    date DATETIME DEFAULT CURRENT_TIMESTAMP,
    id_messages INT,
    FOREIGN KEY (id_messages) REFERENCES messages(id_messages) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS medical_records (
    id_medical_records INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255),
    document_type VARCHAR(100) DEFAULT 'Document',
    description TEXT,
    file_path VARCHAR(255),
    date DATETIME DEFAULT CURRENT_TIMESTAMP,
    id_doctors INT,
    id_users INT,
    FOREIGN KEY (id_doctors) REFERENCES doctors(id_doctors) ON DELETE CASCADE,
    FOREIGN KEY (id_users) REFERENCES users(id_users) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS doctor_availability_slots (
    id_availability_slots INT AUTO_INCREMENT PRIMARY KEY,
    slot_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    status VARCHAR(30) DEFAULT 'DISPONIBLE',
    notes VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    id_doctors INT NOT NULL,
    FOREIGN KEY (id_doctors) REFERENCES doctors(id_doctors) ON DELETE CASCADE
);

INSERT INTO users (username, prenom, email, password, phone, role, address, city, code_postal, country, first_login, must_change_password, account_status)
SELECT 'Administrateur MeetCare', 'Admin', 'admin@meetcare.fr', SHA2('123456', 256), '0102030405', 'ADMIN', 'Siège MeetCare', 'Paris', '75001', 'France', FALSE, FALSE, 'ACTIVE'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@meetcare.fr');

INSERT INTO users (username, prenom, email, password, phone, role, address, city, code_postal, country, first_login, must_change_password, account_status)
SELECT 'Sarah Martin', 'Sarah', 'sarah@mediconnect.fr', SHA2('123456', 256), '0600000001', 'MEDECIN', '15 Avenue des Champs-Elysees', 'Paris', '75008', 'France', TRUE, TRUE, 'ACTIVE'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'sarah@mediconnect.fr');

INSERT INTO users (username, prenom, email, password, phone, role, address, city, code_postal, country, first_login, must_change_password, account_status)
SELECT 'Marc Dubois', 'Marc', 'marc@mediconnect.fr', SHA2('123456', 256), '0600000002', 'MEDECIN', '23 Rue de la Republique', 'Lyon', '69002', 'France', TRUE, TRUE, 'ACTIVE'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'marc@mediconnect.fr');

INSERT INTO users (username, prenom, email, password, phone, role, address, city, code_postal, country, first_login, must_change_password, account_status)
SELECT 'Julie Bernard', 'Julie', 'julie@mediconnect.fr', SHA2('123456', 256), '0600000003', 'MEDECIN', '8 Boulevard Saint-Germain', 'Paris', '75005', 'France', TRUE, TRUE, 'ACTIVE'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'julie@mediconnect.fr');

INSERT INTO users (username, prenom, email, password, phone, role, address, city, code_postal, country, first_login, must_change_password, account_status)
SELECT 'Sophie Durand', 'Sophie', 'sophie@mediconnect.fr', SHA2('123456', 256), '0600000004', 'MEDECIN', '5 Rue Victor Hugo', 'Marseille', '13001', 'France', TRUE, TRUE, 'ACTIVE'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'sophie@mediconnect.fr');

INSERT INTO users (username, prenom, email, password, phone, role, address, city, code_postal, country, first_login, must_change_password, account_status)
SELECT 'Mathieu Bernard', 'Mathieu', 'mathieu@mediconnect.fr', SHA2('123456', 256), '0600000005', 'MEDECIN', '12 Rue Nationale', 'Toulouse', '31000', 'France', TRUE, TRUE, 'ACTIVE'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'mathieu@mediconnect.fr');

INSERT INTO users (username, prenom, email, password, phone, role, address, city, code_postal, country, first_login, must_change_password, account_status)
SELECT 'Camille Leroy', 'Camille', 'camille.leroy@mediconnect.fr', SHA2('123456', 256), '0600000006', 'MEDECIN', '14 Rue Faidherbe', 'Lille', '59000', 'France', TRUE, TRUE, 'ACTIVE'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'camille.leroy@mediconnect.fr');

INSERT INTO users (username, prenom, email, password, phone, role, address, city, code_postal, country, first_login, must_change_password, account_status)
SELECT 'Thomas Morel', 'Thomas', 'thomas.morel@mediconnect.fr', SHA2('123456', 256), '0600000007', 'MEDECIN', '18 Cours Victor Hugo', 'Bordeaux', '33000', 'France', TRUE, TRUE, 'ACTIVE'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'thomas.morel@mediconnect.fr');

INSERT INTO users (username, prenom, email, password, phone, role, address, city, code_postal, country, first_login, must_change_password, account_status)
SELECT 'Ines Chevalier', 'Ines', 'ines.chevalier@mediconnect.fr', SHA2('123456', 256), '0600000008', 'MEDECIN', '7 Rue Crebillon', 'Nantes', '44000', 'France', TRUE, TRUE, 'ACTIVE'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'ines.chevalier@mediconnect.fr');

INSERT INTO users (username, prenom, email, password, phone, role, address, city, code_postal, country, first_login, must_change_password, account_status)
SELECT 'Hugo Renault', 'Hugo', 'hugo.renault@mediconnect.fr', SHA2('123456', 256), '0600000009', 'MEDECIN', '21 Avenue Jean Medecin', 'Nice', '06000', 'France', TRUE, TRUE, 'ACTIVE'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'hugo.renault@mediconnect.fr');

INSERT INTO users (username, prenom, email, password, phone, role, address, city, code_postal, country, first_login, must_change_password, account_status)
SELECT 'Clara Fontaine', 'Clara', 'clara.fontaine@mediconnect.fr', SHA2('123456', 256), '0600000010', 'MEDECIN', '9 Quai des Bateliers', 'Strasbourg', '67000', 'France', TRUE, TRUE, 'ACTIVE'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'clara.fontaine@mediconnect.fr');

INSERT INTO users (username, prenom, email, password, phone, role, address, city, code_postal, country, first_login, must_change_password, account_status)
SELECT 'Yasmine Benali', 'Yasmine', 'yasmine.benali@mediconnect.fr', SHA2('123456', 256), '0600000011', 'MEDECIN', '4 Place de la Comedie', 'Montpellier', '34000', 'France', TRUE, TRUE, 'ACTIVE'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'yasmine.benali@mediconnect.fr');

INSERT INTO users (username, prenom, email, password, phone, role, address, city, code_postal, country, first_login, must_change_password, account_status)
SELECT 'Antoine Garnier', 'Antoine', 'antoine.garnier@mediconnect.fr', SHA2('123456', 256), '0600000012', 'MEDECIN', '11 Rue Le Bastard', 'Rennes', '35000', 'France', TRUE, TRUE, 'ACTIVE'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'antoine.garnier@mediconnect.fr');

INSERT INTO users (username, prenom, email, password, phone, role, address, city, code_postal, country, first_login, must_change_password, account_status)
SELECT 'Salome Perrin', 'Salome', 'salome.perrin@mediconnect.fr', SHA2('123456', 256), '0600000013', 'MEDECIN', '3 Place Grenette', 'Grenoble', '38000', 'France', TRUE, TRUE, 'ACTIVE'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'salome.perrin@mediconnect.fr');

INSERT INTO users (username, prenom, email, password, phone, role, address, city, code_postal, country, first_login, must_change_password, account_status)
SELECT 'Nicolas Faure', 'Nicolas', 'nicolas.faure@mediconnect.fr', SHA2('123456', 256), '0600000014', 'MEDECIN', '26 Rue de Vesle', 'Reims', '51100', 'France', TRUE, TRUE, 'ACTIVE'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'nicolas.faure@mediconnect.fr');

INSERT INTO users (username, prenom, email, password, phone, role, address, city, code_postal, country, first_login, must_change_password, account_status)
SELECT 'Emma Marchand', 'Emma', 'emma.marchand@mediconnect.fr', SHA2('123456', 256), '0600000015', 'MEDECIN', '6 Rue de la Liberte', 'Dijon', '21000', 'France', TRUE, TRUE, 'ACTIVE'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'emma.marchand@mediconnect.fr');

INSERT INTO doctors (speciality, experience, consultation_price, biography, id_users, address, sector, accepts_vitale, teleconsultation, public_profile_completed)
SELECT 'Cardiologue', 12, 30.00, 'Cardiologue specialisee dans le suivi cardiaque.', u.id_users, '15 Avenue des Champs-Elysees, Paris', 'Secteur 1', TRUE, TRUE, TRUE
FROM users u
WHERE u.email = 'sarah@mediconnect.fr'
  AND NOT EXISTS (SELECT 1 FROM doctors d WHERE d.id_users = u.id_users);

INSERT INTO doctors (speciality, experience, consultation_price, biography, id_users, address, sector, accepts_vitale, teleconsultation, public_profile_completed)
SELECT 'Generaliste', 8, 25.00, 'Medecin generaliste experimente.', u.id_users, '23 Rue de la Republique, Lyon', 'Secteur 1', TRUE, FALSE, TRUE
FROM users u
WHERE u.email = 'marc@mediconnect.fr'
  AND NOT EXISTS (SELECT 1 FROM doctors d WHERE d.id_users = u.id_users);

INSERT INTO doctors (speciality, experience, consultation_price, biography, id_users, address, sector, accepts_vitale, teleconsultation, public_profile_completed)
SELECT 'Dermatologue', 15, 50.00, 'Dermatologue specialisee dans les maladies de la peau.', u.id_users, '8 Boulevard Saint-Germain, Paris', 'Secteur 2', TRUE, TRUE, TRUE
FROM users u
WHERE u.email = 'julie@mediconnect.fr'
  AND NOT EXISTS (SELECT 1 FROM doctors d WHERE d.id_users = u.id_users);

INSERT INTO doctors (speciality, experience, consultation_price, biography, id_users, address, sector, accepts_vitale, teleconsultation, public_profile_completed)
SELECT 'Radiologue', 10, 45.00, 'Specialiste en radiologie et imagerie medicale.', u.id_users, '5 Rue Victor Hugo, Marseille', 'Secteur 2', FALSE, FALSE, TRUE
FROM users u
WHERE u.email = 'sophie@mediconnect.fr'
  AND NOT EXISTS (SELECT 1 FROM doctors d WHERE d.id_users = u.id_users);

INSERT INTO doctors (speciality, experience, consultation_price, biography, id_users, address, sector, accepts_vitale, teleconsultation, public_profile_completed)
SELECT 'Generaliste', 7, 28.00, 'Medecin generaliste oriente teleconsultation.', u.id_users, '12 Rue Nationale, Toulouse', 'Secteur 1', TRUE, TRUE, TRUE
FROM users u
WHERE u.email = 'mathieu@mediconnect.fr'
  AND NOT EXISTS (SELECT 1 FROM doctors d WHERE d.id_users = u.id_users);

INSERT INTO doctors (speciality, experience, consultation_price, biography, id_users, address, sector, accepts_vitale, teleconsultation, public_profile_completed)
SELECT 'Pediatre', 9, 35.00, 'Pediatre attentive au suivi des enfants et nourrissons.', u.id_users, '14 Rue Faidherbe, Lille', 'Secteur 1', TRUE, TRUE, TRUE
FROM users u
WHERE u.email = 'camille.leroy@mediconnect.fr'
  AND NOT EXISTS (SELECT 1 FROM doctors d WHERE d.id_users = u.id_users);

INSERT INTO doctors (speciality, experience, consultation_price, biography, id_users, address, sector, accepts_vitale, teleconsultation, public_profile_completed)
SELECT 'Neurologue', 13, 60.00, 'Specialiste des troubles neurologiques et du sommeil.', u.id_users, '18 Cours Victor Hugo, Bordeaux', 'Secteur 2', TRUE, TRUE, TRUE
FROM users u
WHERE u.email = 'thomas.morel@mediconnect.fr'
  AND NOT EXISTS (SELECT 1 FROM doctors d WHERE d.id_users = u.id_users);

INSERT INTO doctors (speciality, experience, consultation_price, biography, id_users, address, sector, accepts_vitale, teleconsultation, public_profile_completed)
SELECT 'Gynecologue', 11, 50.00, 'Gynecologue experimentee pour le suivi regulier et la prevention.', u.id_users, '7 Rue Crebillon, Nantes', 'Secteur 2', TRUE, FALSE, TRUE
FROM users u
WHERE u.email = 'ines.chevalier@mediconnect.fr'
  AND NOT EXISTS (SELECT 1 FROM doctors d WHERE d.id_users = u.id_users);

INSERT INTO doctors (speciality, experience, consultation_price, biography, id_users, address, sector, accepts_vitale, teleconsultation, public_profile_completed)
SELECT 'ORL', 10, 40.00, 'ORL specialise dans les pathologies de l''oreille, du nez et de la gorge.', u.id_users, '21 Avenue Jean Medecin, Nice', 'Secteur 1', TRUE, TRUE, TRUE
FROM users u
WHERE u.email = 'hugo.renault@mediconnect.fr'
  AND NOT EXISTS (SELECT 1 FROM doctors d WHERE d.id_users = u.id_users);

INSERT INTO doctors (speciality, experience, consultation_price, biography, id_users, address, sector, accepts_vitale, teleconsultation, public_profile_completed)
SELECT 'Endocrinologue', 14, 55.00, 'Endocrinologue pour le suivi du diabete et des troubles hormonaux.', u.id_users, '9 Quai des Bateliers, Strasbourg', 'Secteur 2', TRUE, TRUE, TRUE
FROM users u
WHERE u.email = 'clara.fontaine@mediconnect.fr'
  AND NOT EXISTS (SELECT 1 FROM doctors d WHERE d.id_users = u.id_users);

INSERT INTO doctors (speciality, experience, consultation_price, biography, id_users, address, sector, accepts_vitale, teleconsultation, public_profile_completed)
SELECT 'Psychiatre', 12, 65.00, 'Psychiatre specialisee dans le suivi anxiete et depression.', u.id_users, '4 Place de la Comedie, Montpellier', 'Secteur 2', TRUE, TRUE, TRUE
FROM users u
WHERE u.email = 'yasmine.benali@mediconnect.fr'
  AND NOT EXISTS (SELECT 1 FROM doctors d WHERE d.id_users = u.id_users);

INSERT INTO doctors (speciality, experience, consultation_price, biography, id_users, address, sector, accepts_vitale, teleconsultation, public_profile_completed)
SELECT 'Pneumologue', 15, 58.00, 'Pneumologue experimente pour les pathologies respiratoires.', u.id_users, '11 Rue Le Bastard, Rennes', 'Secteur 2', TRUE, FALSE, TRUE
FROM users u
WHERE u.email = 'antoine.garnier@mediconnect.fr'
  AND NOT EXISTS (SELECT 1 FROM doctors d WHERE d.id_users = u.id_users);

INSERT INTO doctors (speciality, experience, consultation_price, biography, id_users, address, sector, accepts_vitale, teleconsultation, public_profile_completed)
SELECT 'Rhumatologue', 8, 46.00, 'Rhumatologue pour le suivi articulaire et locomoteur.', u.id_users, '3 Place Grenette, Grenoble', 'Secteur 1', TRUE, TRUE, TRUE
FROM users u
WHERE u.email = 'salome.perrin@mediconnect.fr'
  AND NOT EXISTS (SELECT 1 FROM doctors d WHERE d.id_users = u.id_users);

INSERT INTO doctors (speciality, experience, consultation_price, biography, id_users, address, sector, accepts_vitale, teleconsultation, public_profile_completed)
SELECT 'Urologue', 9, 52.00, 'Urologue specialise dans les troubles urinaires et renaux.', u.id_users, '26 Rue de Vesle, Reims', 'Secteur 2', TRUE, FALSE, TRUE
FROM users u
WHERE u.email = 'nicolas.faure@mediconnect.fr'
  AND NOT EXISTS (SELECT 1 FROM doctors d WHERE d.id_users = u.id_users);

INSERT INTO doctors (speciality, experience, consultation_price, biography, id_users, address, sector, accepts_vitale, teleconsultation, public_profile_completed)
SELECT 'Allergologue', 7, 42.00, 'Allergologue pour bilans et suivi des allergies respiratoires et alimentaires.', u.id_users, '6 Rue de la Liberte, Dijon', 'Secteur 1', TRUE, TRUE, TRUE
FROM users u
WHERE u.email = 'emma.marchand@mediconnect.fr'
  AND NOT EXISTS (SELECT 1 FROM doctors d WHERE d.id_users = u.id_users);

-- Les patients, rendez-vous, documents, notes et factures se creent maintenant
-- au fur et a mesure des vraies actions dans l'application.
