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
    account_status ENUM('PENDING','VERIFIED','REJECTED','SUSPENDED') DEFAULT 'PENDING',
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
    address VARCHAR(255),
    experience INT,
    consultation_price DECIMAL(10,2),
    biography TEXT,
    sector VARCHAR(20),
    accepts_vitale BOOLEAN DEFAULT FALSE,
    teleconsultation BOOLEAN DEFAULT FALSE,
    public_profile_completed BOOLEAN DEFAULT FALSE,
    id_users INT,
    FOREIGN KEY (id_users) REFERENCES users(id_users) ON DELETE CASCADE
);

-- DOCTOR AVAILABILITY BLOCKS
CREATE TABLE IF NOT EXISTS doctor_availability_blocks (
    id_blocks INT AUTO_INCREMENT PRIMARY KEY,
    id_users INT NOT NULL,
    block_date DATE NOT NULL,
    start_time TIME NULL,
    end_time TIME NULL,
    whole_day BOOLEAN DEFAULT FALSE,
    reason VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
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

CREATE TABLE conversation_participants (
    id_conversation_participants INT AUTO_INCREMENT PRIMARY KEY,
    id_conversations INT NOT NULL,
    id_users INT NOT NULL,
    joined_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_conversation_user (id_conversations, id_users),
    FOREIGN KEY (id_conversations) REFERENCES conversations(id_conversations) ON DELETE CASCADE,
    FOREIGN KEY (id_users) REFERENCES users(id_users) ON DELETE CASCADE
);

CREATE TABLE conversation_typing (
    id_conversation_typing INT AUTO_INCREMENT PRIMARY KEY,
    id_conversations INT NOT NULL,
    id_users INT NOT NULL,
    is_typing BOOLEAN DEFAULT FALSE,
    typing_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_typing_conversation_user (id_conversations, id_users),
    FOREIGN KEY (id_conversations) REFERENCES conversations(id_conversations) ON DELETE CASCADE,
    FOREIGN KEY (id_users) REFERENCES users(id_users) ON DELETE CASCADE
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
    date DATETIME DEFAULT CURRENT_TIMESTAMP,
    id_doctors INT,
    id_users INT,
    FOREIGN KEY (id_doctors) REFERENCES doctors(id_doctors) ON DELETE CASCADE,
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
    file_size BIGINT,
    date DATETIME DEFAULT CURRENT_TIMESTAMP,
    id_messages INT,
    FOREIGN KEY (id_messages) REFERENCES messages(id_messages) ON DELETE CASCADE
);

-- MEDICAL RECORDS
CREATE TABLE medical_records (
    id_medical_records INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255),
    description TEXT,
    document_type VARCHAR(50) NOT NULL DEFAULT 'OTHER',
    file_path VARCHAR(255),
    date DATETIME DEFAULT CURRENT_TIMESTAMP,
    id_doctors INT,
    id_users INT,
    share_consent BOOLEAN DEFAULT FALSE,
    id_shared_doctors INT,
    shared_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    prescription_diagnosis TEXT,
    prescription_treatment TEXT,
    prescription_instructions TEXT,
    prescription_validity VARCHAR(50),
    FOREIGN KEY (id_doctors) REFERENCES doctors(id_doctors) ON DELETE CASCADE,
    FOREIGN KEY (id_users) REFERENCES users(id_users) ON DELETE CASCADE,
    FOREIGN KEY (id_shared_doctors) REFERENCES doctors(id_doctors) ON DELETE SET NULL
);


CREATE TABLE doctor_documents (
    id_doctor_documents INT AUTO_INCREMENT PRIMARY KEY,
    document_type ENUM('DIPLOMA','LICENSE','ID_CARD','RIB','CARTE_VITALE','TITRE_SEJOUR','PASSEPORT','VISA','JUSTIFICATIF_DOMICILE','ASSURANCE_HABITATION','PHOTO_PRO'),
    file_name VARCHAR(255),
    file_path VARCHAR(255),
    verification_status ENUM('PENDING','APPROVED','REJECTED') DEFAULT 'PENDING',
    uploaded_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    id_doctors INT,
    FOREIGN KEY (id_doctors) REFERENCES doctors(id_doctors) ON DELETE CASCADE
);

CREATE TABLE doctor_verifications (
    id_verification INT AUTO_INCREMENT PRIMARY KEY,
    decision ENUM('APPROVED','REJECTED'),
    comment TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    id_doctors INT,
    id_admin INT,
    FOREIGN KEY (id_doctors) REFERENCES doctors(id_doctors) ON DELETE CASCADE,
    FOREIGN KEY (id_admin) REFERENCES users(id_users) ON DELETE CASCADE
);

-- SAMPLE DATA
INSERT INTO users (username, prenom, email, password, phone, avatar, role, account_status, address, city, code_postal, country)
VALUES
('martin', 'Antoine', 'dr.martin@meetcare.test', 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f', '0600000001', NULL, 'MEDECIN', 'VERIFIED', '12 rue Victor-Hugo', 'Paris', '75001', 'France'),
('dupont', 'Claire', 'dr.dupont@meetcare.test', 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f', '0600000002', NULL, 'MEDECIN', 'VERIFIED', '8 avenue Foch', 'Lyon', '69002', 'France'),
('bernard', 'Luc', 'dr.bernard@meetcare.test', 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f', '0600000003', NULL, 'MEDECIN', 'VERIFIED', '45 boulevard Michelet', 'Marseille', '13008', 'France'),
('laurent', 'Sophie', 'dr.laurent@meetcare.test', 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f', '0600000004', NULL, 'MEDECIN', 'VERIFIED', '19 rue Nationale', 'Lille', '59000', 'France'),
('petit', 'Nicolas', 'dr.petit@meetcare.test', 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f', '0600000005', NULL, 'MEDECIN', 'VERIFIED', '72 place Bellecour', 'Lyon', '69002', 'France'),
('robert', 'Emma', 'dr.robert@meetcare.test', 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f', '0600000006', NULL, 'MEDECIN', 'VERIFIED', '3 rue de la Gare', 'Toulouse', '31000', 'France'),
('richard', 'Julien', 'dr.richard@meetcare.test', 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f', '0600000007', NULL, 'MEDECIN', 'VERIFIED', '14 cours de la Liberté', 'Bordeaux', '33000', 'France'),
('durand', 'Manon', 'dr.durand@meetcare.test', 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f', '0600000008', NULL, 'MEDECIN', 'VERIFIED', '27 rue de Strasbourg', 'Nantes', '44000', 'France'),
('faure', 'Thomas', 'dr.faure@meetcare.test', 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f', '0600000009', NULL, 'MEDECIN', 'VERIFIED', '6 avenue du Parc', 'Nice', '06000', 'France'),
('mercier', 'Laura', 'dr.mercier@meetcare.test', 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f', '0600000010', NULL, 'MEDECIN', 'VERIFIED', '101 rue des Lilas', 'Strasbourg', '67000', 'France');

INSERT INTO doctors (speciality, address, experience, consultation_price, biography, sector, accepts_vitale, teleconsultation, public_profile_completed, id_users)
VALUES
('Cardiologie', '12 rue Victor-Hugo', 15, 70.00, 'Spécialiste du cœur et du suivi cardiovasculaire.', 'Secteur 2', TRUE, TRUE, TRUE, (SELECT id_users FROM users WHERE email = 'dr.martin@meetcare.test')),
('Dermatologie', '8 avenue Foch', 11, 60.00, 'Consultations peau, cheveux et ongles.', 'Secteur 1', TRUE, TRUE, TRUE, (SELECT id_users FROM users WHERE email = 'dr.dupont@meetcare.test')),
('Généraliste', '45 boulevard Michelet', 9, 35.00, 'Médecine générale pour toute la famille.', 'Secteur 1', TRUE, FALSE, TRUE, (SELECT id_users FROM users WHERE email = 'dr.bernard@meetcare.test')),
('Pédiatrie', '19 rue Nationale', 13, 55.00, 'Suivi médical des enfants et adolescents.', 'Secteur 2', TRUE, TRUE, TRUE, (SELECT id_users FROM users WHERE email = 'dr.laurent@meetcare.test')),
('Ophtalmologie', '72 place Bellecour', 17, 80.00, 'Bilan visuel et pathologies oculaires.', 'Secteur 2', TRUE, FALSE, TRUE, (SELECT id_users FROM users WHERE email = 'dr.petit@meetcare.test')),
('Gynécologie', '3 rue de la Gare', 14, 75.00, 'Suivi gynécologique et prévention.', 'Secteur 2', TRUE, TRUE, TRUE, (SELECT id_users FROM users WHERE email = 'dr.robert@meetcare.test')),
('ORL', '14 cours de la Liberté', 10, 65.00, 'Oreille, nez, gorge et chirurgie ORL légère.', 'Secteur 1', TRUE, TRUE, TRUE, (SELECT id_users FROM users WHERE email = 'dr.richard@meetcare.test')),
('Psychiatrie', '27 rue de Strasbourg', 12, 90.00, 'Accompagnement psychologique et psychiatrique.', 'Secteur 2', FALSE, TRUE, TRUE, (SELECT id_users FROM users WHERE email = 'dr.durand@meetcare.test')),
('Radiologie', '6 avenue du Parc', 16, 85.00, 'Imagerie médicale et diagnostics par radio.', 'Secteur 2', TRUE, FALSE, TRUE, (SELECT id_users FROM users WHERE email = 'dr.faure@meetcare.test')),
('Endocrinologie', '101 rue des Lilas', 8, 68.00, 'Diabète, thyroïde et métabolisme.', 'Secteur 1', TRUE, TRUE, TRUE, (SELECT id_users FROM users WHERE email = 'dr.mercier@meetcare.test'));
