CREATE DATABASE meetcare;
USE meetcare;

-- =========================
-- TABLE USERS
-- =========================
CREATE TABLE users (
   id_users INT PRIMARY KEY AUTO_INCREMENT,
   username VARCHAR(255) NOT NULL,
   email VARCHAR(150) UNIQUE NOT NULL,
   password VARCHAR(255) NOT NULL,
   phone VARCHAR(20),
   avatar VARCHAR(255),
   role ENUM('PATIENT','DOCTOR','ADMIN') DEFAULT 'PATIENT',
   is_online BOOLEAN DEFAULT FALSE,
   last_seen DATETIME DEFAULT CURRENT_TIMESTAMP,
   date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =========================
-- TABLE DOCTORS
-- =========================
CREATE TABLE doctors (
     id_doctors INT PRIMARY KEY AUTO_INCREMENT,
     users_id INT UNIQUE NOT NULL,
     speciality VARCHAR(100),
     experience INT,
     consultation_price DECIMAL(10,2),
     biography TEXT,
     FOREIGN KEY (users_id) REFERENCES users(id_users) ON DELETE CASCADE
);

-- =========================
-- TABLE APPOINTMENTS
-- =========================
CREATE TABLE appointments (
      id_appointments INT PRIMARY KEY AUTO_INCREMENT,
      patients_id_users INT NOT NULL,
      doctors_id INT NOT NULL,
      appointment_date DATE NOT NULL,
      appointment_time TIME NOT NULL,
      status ENUM('PENDING','CONFIRMED','CANCELLED','DONE') DEFAULT 'PENDING',
      notes TEXT,
      date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY (patients_id_users) REFERENCES users(id_users),
      FOREIGN KEY (doctors_id) REFERENCES doctors(id_doctors)
);

-- =========================
-- TABLE CONVERSATIONS
-- =========================
CREATE TABLE conversations (
   id_conversations INT PRIMARY KEY AUTO_INCREMENT,
   date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =========================
-- TABLE MESSAGES
-- =========================
CREATE TABLE messages (
      id_messages INT PRIMARY KEY AUTO_INCREMENT,
      conversation_id INT NOT NULL,
      sender_id_users INT NOT NULL,
      content TEXT,
      message_type ENUM('TEXT','IMAGE','FILE','AUDIO') DEFAULT 'TEXT',
      is_read BOOLEAN DEFAULT FALSE,
      sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY (conversation_id) REFERENCES conversations(id_conversations) ON DELETE CASCADE,
      FOREIGN KEY (sender_id_users) REFERENCES users(id_users)
);

-- =========================
-- TABLE ATTACHMENTS
-- =========================
CREATE TABLE attachments (
     id_attachments INT PRIMARY KEY AUTO_INCREMENT,
     messages_id INT NOT NULL,
     file_name VARCHAR(255) NOT NULL,
     file_path VARCHAR(255) NOT NULL,
     file_type VARCHAR(100),
     file_size BIGINT,
     date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
     FOREIGN KEY (messages_id) REFERENCES messages(id_messages) ON DELETE CASCADE
);

-- =========================
-- TABLE INVOICES
-- =========================
CREATE TABLE invoices (
      id_invoices INT PRIMARY KEY AUTO_INCREMENT,
      patients_id_users INT NOT NULL,
      doctors_id INT NOT NULL,
      appointment_id INT,
      amount DECIMAL(10,2) NOT NULL,
      description TEXT,
      status ENUM('UNPAID','PAID') DEFAULT 'UNPAID',
      date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY (patients_id_users) REFERENCES users(id_users),
      FOREIGN KEY (doctors_id) REFERENCES doctors(id_doctors),
      FOREIGN KEY (appointment_id) REFERENCES appointments(id_appointments)
);

-- =========================
-- TABLE CHATBOT IA
-- =========================
CREATE TABLE chatbot_messages (
      id_chatbot_messages INT PRIMARY KEY AUTO_INCREMENT,
      users_id INT NOT NULL,
      question TEXT NOT NULL,
      response TEXT,
      model VARCHAR(100),
      date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY (users_id) REFERENCES users(id_users) ON DELETE CASCADE
);

-- =========================
-- TABLE NOTIFICATIONS
-- =========================
CREATE TABLE notifications (
       id_notifications INT PRIMARY KEY AUTO_INCREMENT,
       users_id INT NOT NULL,
       title VARCHAR(255) NOT NULL,
       message TEXT NOT NULL,
       type ENUM('APPOINTMENT','MESSAGE','PAYMENT','SYSTEM') DEFAULT 'SYSTEM',
       is_read BOOLEAN DEFAULT FALSE,
       date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
       FOREIGN KEY (users_id) REFERENCES users(id_users) ON DELETE CASCADE
);