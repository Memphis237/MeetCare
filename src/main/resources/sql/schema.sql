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
    file_size BIGINT,
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


CREATE TABLE doctor_documents (
    id_doctor_documents INT AUTO_INCREMENT PRIMARY KEY,
    document_type ENUM('DIPLOMA','LICENSE','ID_CARD'),
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