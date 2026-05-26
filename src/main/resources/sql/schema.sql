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
    file_path VARCHAR(255),
    date DATETIME DEFAULT CURRENT_TIMESTAMP,
    id_doctors INT,
    id_users INT,
    FOREIGN KEY (id_doctors) REFERENCES doctors(id_doctors) ON DELETE CASCADE,
    FOREIGN KEY (id_users) REFERENCES users(id_users) ON DELETE CASCADE
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
