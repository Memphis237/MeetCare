DROP ALL OBJECTS;

CREATE TABLE users (
    id_users INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    prenom VARCHAR(255) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    avatar VARCHAR(255),
    role VARCHAR(50),
    account_status VARCHAR(20) DEFAULT 'PENDING',
    address VARCHAR(255),
    city VARCHAR(100),
    code_postal VARCHAR(20),
    country VARCHAR(100),
    is_online BOOLEAN DEFAULT FALSE,
    last_seen TIMESTAMP NULL,
    `date` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE doctors (
    id_doctors INT AUTO_INCREMENT PRIMARY KEY,
    id_users INT NOT NULL,
    FOREIGN KEY (id_users) REFERENCES users(id_users) ON DELETE CASCADE
);

CREATE TABLE appointments (
    id_appointments INT AUTO_INCREMENT PRIMARY KEY,
    appointment_date DATE NOT NULL,
    appointment_time TIME NOT NULL,
    status VARCHAR(50),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    id_doctors INT,
    id_users INT,
    FOREIGN KEY (id_doctors) REFERENCES doctors(id_doctors) ON DELETE CASCADE,
    FOREIGN KEY (id_users) REFERENCES users(id_users) ON DELETE CASCADE
);

CREATE TABLE conversations (
    id_conversations INT AUTO_INCREMENT PRIMARY KEY,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE chatbot_messages (
    id_chatbot_messages INT AUTO_INCREMENT PRIMARY KEY,
    question TEXT,
    response TEXT,
    model VARCHAR(100),
    `date` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    id_conversations INT,
    id_users INT,
    FOREIGN KEY (id_conversations) REFERENCES conversations(id_conversations) ON DELETE CASCADE,
    FOREIGN KEY (id_users) REFERENCES users(id_users) ON DELETE CASCADE
);

CREATE TABLE notifications (
    id_notifications INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255),
    message TEXT,
    type VARCHAR(50),
    is_read BOOLEAN DEFAULT FALSE,
    `date` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    id_users INT,
    FOREIGN KEY (id_users) REFERENCES users(id_users) ON DELETE CASCADE
);

CREATE TABLE invoices (
    id_invoices INT AUTO_INCREMENT PRIMARY KEY,
    amount DECIMAL(10,2),
    description TEXT,
    status VARCHAR(50),
    payment_method VARCHAR(50),
    paid_at TIMESTAMP NULL,
    `date` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    id_doctors INT,
    id_users INT,
    FOREIGN KEY (id_doctors) REFERENCES doctors(id_doctors) ON DELETE CASCADE,
    FOREIGN KEY (id_users) REFERENCES users(id_users) ON DELETE CASCADE
);

CREATE TABLE doctor_documents (
    id_doctor_documents INT AUTO_INCREMENT PRIMARY KEY,
    document_type VARCHAR(50),
    file_name VARCHAR(255),
    file_path VARCHAR(255),
    verification_status VARCHAR(20) DEFAULT 'PENDING',
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    id_doctors INT,
    FOREIGN KEY (id_doctors) REFERENCES doctors(id_doctors) ON DELETE CASCADE
);

CREATE TABLE doctor_verifications (
    id_verification INT AUTO_INCREMENT PRIMARY KEY,
    decision VARCHAR(20),
    comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    id_doctors INT,
    id_admin INT,
    FOREIGN KEY (id_doctors) REFERENCES doctors(id_doctors) ON DELETE CASCADE,
    FOREIGN KEY (id_admin) REFERENCES users(id_users) ON DELETE CASCADE
);
