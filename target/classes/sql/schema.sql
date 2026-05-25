CREATE DATABASE IF NOT EXISTS meetcare
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE meetcare;

-- USERS
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
    date DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- DOCTORS
CREATE TABLE doctors (
    id_doctors INT AUTO_INCREMENT PRIMARY KEY,
    speciality VARCHAR(100),
    experience INT,
    consultation_price DECIMAL(10,2),
    biography TEXT,
    id_users INT,
    FOREIGN KEY (id_users) REFERENCES users(id_users) ON DELETE CASCADE
);

-- APPOINTMENTS
CREATE TABLE appointments (
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

-- CONVERSATIONS
CREATE TABLE conversations (
    id_conversations INT AUTO_INCREMENT PRIMARY KEY,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- NOTIFICATIONS
CREATE TABLE notifications (
    id_notifications INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255),
    message TEXT,
    type VARCHAR(50),
    is_read BOOLEAN DEFAULT FALSE,
    date DATETIME DEFAULT CURRENT_TIMESTAMP,
    id_users INT,
    FOREIGN KEY (id_users) REFERENCES users(id_users) ON DELETE CASCADE
);

-- INVOICES
CREATE TABLE invoices (
    id_invoices INT AUTO_INCREMENT PRIMARY KEY,
    amount DECIMAL(10,2),
    description TEXT,
    status VARCHAR(50),
    payment_method VARCHAR(50),
    paid_at DATETIME,
    id_users INT,
    FOREIGN KEY (id_users) REFERENCES users(id_users) ON DELETE CASCADE
);

-- CHATBOT MESSAGES
CREATE TABLE chatbot_messages (
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

-- MESSAGES
CREATE TABLE messages (
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

-- ATTACHMENTS
CREATE TABLE attachments (
    id_attachments INT AUTO_INCREMENT PRIMARY KEY,
    file_name VARCHAR(255),
    file_path VARCHAR(255),
    file_type VARCHAR(100),
    file_size INT,
    date DATETIME DEFAULT CURRENT_TIMESTAMP,
    id_messages INT,
    FOREIGN KEY (id_messages) REFERENCES messages(id_messages) ON DELETE CASCADE
);

-- MEDICAL RECORDS
CREATE TABLE medical_records (
    id_medical_records INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255),
    description TEXT,
    file_path VARCHAR(255),
    date DATETIME DEFAULT CURRENT_TIMESTAMP,
    id_doctors INT,
    id_users INT,
    FOREIGN KEY (id_doctors) REFERENCES doctors(id_doctors) ON DELETE CASCADE,
    FOREIGN KEY (id_users) REFERENCES users(id_users) ON DELETE CASCADE
);
-- =========================================================================
-- AJOUT DE LA TABLE DES CRÉNEAUX DE DISPONIBILITÉ (POUR L'INTERFACE FIXE)
-- =========================================================================

CREATE TABLE IF NOT EXISTS availabilities (
    id_availabilities INT AUTO_INCREMENT PRIMARY KEY,
    availability_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    is_available BOOLEAN DEFAULT TRUE,
    id_doctors INT NOT NULL,
    FOREIGN KEY (id_doctors) REFERENCES doctors(id_doctors) ON DELETE CASCADE
);

-- =========================================================================
-- INSERTION DES JEUX DE DONNÉES DE TEST (POUR VOS STATISTIQUES À 100%)
-- =========================================================================

-- Ajout de créneaux fictifs pour la Dre Sarah Martin (id_doctors = 1) 
-- afin de remplir instantanément l'interface au premier lancement
INSERT INTO availabilities 
(availability_date, start_time, end_time, is_available, id_doctors)
VALUES
('2026-05-16', '09:00:00', '09:30:00', TRUE, 1),
('2026-05-16', '09:30:00', '10:00:00', FALSE, 1), -- Créneau pris / occupé
('2026-05-16', '10:00:00', '10:30:00', TRUE, 1),
('2026-05-16', '10:30:00', '11:00:00', TRUE, 1),
('2026-05-16', '11:00:00', '11:30:00', TRUE, 1),
('2026-05-16', '14:00:00', '14:30:00', FALSE, 1), -- Créneau pris / occupé
('2026-05-16', '14:30:00', '15:00:00', TRUE, 1);

-- =========================================
-- AJOUT DES COLONNES MANQUANTES
-- =========================================

ALTER TABLE users
    ADD COLUMN first_login BOOLEAN DEFAULT TRUE,
ADD COLUMN must_change_password BOOLEAN DEFAULT FALSE,
ADD COLUMN account_status VARCHAR(50) DEFAULT 'ACTIVE';

ALTER TABLE doctors
    ADD COLUMN address VARCHAR(255),
ADD COLUMN sector VARCHAR(20),
ADD COLUMN accepts_vitale BOOLEAN DEFAULT FALSE,
ADD COLUMN teleconsultation BOOLEAN DEFAULT FALSE,
ADD COLUMN public_profile_completed BOOLEAN DEFAULT FALSE;


-- =========================================
-- INSERTION DES UTILISATEURS MÉDECINS
-- =========================================

INSERT INTO users
(username, email, password, phone, role, city, first_login, must_change_password)
VALUES

    (
        'Sarah Martin',
        'sarah@mediconnect.fr',
        '123456',
        '0600000001',
        'MEDECIN',
        'Paris',
        TRUE,
        TRUE
    ),

    (
        'Marc Dubois',
        'marc@mediconnect.fr',
        '123456',
        '0600000002',
        'MEDECIN',
        'Lyon',
        TRUE,
        TRUE
    ),

    (
        'Julie Bernard',
        'julie@mediconnect.fr',
        '123456',
        '0600000003',
        'MEDECIN',
        'Paris',
        TRUE,
        TRUE
    ),

    (
        'Sophie Durand',
        'sophie@mediconnect.fr',
        '123456',
        '0600000004',
        'MEDECIN',
        'Marseille',
        TRUE,
        TRUE
    ),

    (
        'Mathieu Bernard',
        'mathieu@mediconnect.fr',
        '123456',
        '0600000005',
        'MEDECIN',
        'Toulouse',
        TRUE,
        TRUE
    );


-- =========================================
-- INSERTION DES MÉDECINS
-- =========================================

INSERT INTO doctors
(
    speciality,
    experience,
    consultation_price,
    biography,
    address,
    sector,
    accepts_vitale,
    teleconsultation,
    public_profile_completed,
    id_users
)

VALUES

    (
        'Cardiologue',
        12,
        30,
        'Cardiologue spécialisée dans le suivi cardiaque.',
        '15 Avenue des Champs-Élysées',
        'Secteur 1',
        TRUE,
        TRUE,
        TRUE,
        1
    ),

    (
        'Généraliste',
        8,
        25,
        'Médecin généraliste expérimenté.',
        '23 Rue de la République',
        'Secteur 1',
        TRUE,
        FALSE,
        TRUE,
        2
    ),

    (
        'Dermatologue',
        15,
        50,
        'Dermatologue spécialisée dans les maladies de la peau.',
        '8 Boulevard Saint-Germain',
        'Secteur 2',
        TRUE,
        TRUE,
        TRUE,
        3
    ),

    (
        'Radiologue',
        10,
        45,
        'Spécialiste en radiologie et imagerie médicale.',
        '5 Rue Victor Hugo',
        'Secteur 2',
        FALSE,
        FALSE,
        TRUE,
        4
    ),

    (
        'Généraliste',
        7,
        28,
        'Médecin généraliste orienté téléconsultation.',
        '12 Rue Nationale',
        'Secteur 1',
        TRUE,
        TRUE,
        TRUE,
        5
    );