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
                       city VARCHAR(100),
                       code_postal VARCHAR(20),
                       is_online BOOLEAN DEFAULT FALSE,
                       last_seen DATETIME DEFAULT CURRENT_TIMESTAMP,
                       date_ TIMESTAMP DEFAULT CURRENT_TIMESTAMP
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
-- TABLE DOCTOR AVAILABILITY BLOCKS
-- =========================
CREATE TABLE doctor_availability_blocks (
                                            id_blocks INT PRIMARY KEY AUTO_INCREMENT,
                                            id_users INT NOT NULL,
                                            block_date DATE NOT NULL,
                                            start_time TIME NULL,
                                            end_time TIME NULL,
                                            whole_day BOOLEAN DEFAULT FALSE,
                                            reason VARCHAR(255),
                                            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                            FOREIGN KEY (id_users) REFERENCES users(id_users) ON DELETE CASCADE
);

-- =========================
-- TABLE APPOINTMENTS (CONSULTATIONS)
-- =========================
CREATE TABLE appointments (
                              id_appointments INT PRIMARY KEY AUTO_INCREMENT,
                              patients_id_users INT NOT NULL,
                              doctors_id INT NOT NULL,
                              appointment_date DATE NOT NULL,
                              appointment_time TIME NOT NULL,
                              status ENUM('PENDING','CONFIRMED','CANCELLED','COMPLETED') DEFAULT 'PENDING',
                              urgency VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
                              notes TEXT,
                              completed_at DATETIME,
                              date_ TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              FOREIGN KEY (patients_id_users) REFERENCES users(id_users),
                              FOREIGN KEY (doctors_id) REFERENCES doctors(id_doctors)
);

-- =========================
-- TABLE INVOICES (FACTURES - SIMPLE)
-- =========================
CREATE TABLE invoices (
                          id_invoices INT PRIMARY KEY AUTO_INCREMENT,
                          patients_id_users INT NOT NULL,
                          doctors_id INT NOT NULL,
                          appointment_id INT NOT NULL,
                          amount DECIMAL(10,2) NOT NULL,
                          description TEXT,
                          status ENUM('UNPAID','PAID') DEFAULT 'UNPAID',
                          payments_method ENUM('CASH','CARD','ONLINE') DEFAULT 'ONLINE',
                          paid_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                          FOREIGN KEY (patients_id_users) REFERENCES users(id_users),
                          FOREIGN KEY (doctors_id) REFERENCES doctors(id_doctors),
                          FOREIGN KEY (appointment_id) REFERENCES appointments(id_appointments)
);

-- =========================
-- TABLE CONVERSATIONS
-- =========================
CREATE TABLE conversations (
                               id_conversations INT PRIMARY KEY AUTO_INCREMENT,
                               date_ TIMESTAMP DEFAULT CURRENT_TIMESTAMP
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
                             file_name VARCHAR(255),
                             file_path VARCHAR(255),
                             file_type VARCHAR(100),
                             file_size BIGINT,
                             date_ TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                             FOREIGN KEY (messages_id) REFERENCES messages(id_messages) ON DELETE CASCADE
);

-- =========================
-- TABLE MEDICAL RECORDS
-- =========================
CREATE TABLE medical_records (
                                 id_medical_records INT PRIMARY KEY AUTO_INCREMENT,
                                 title VARCHAR(255),
                                 description TEXT,
                                 file_path VARCHAR(255),
                                 date_ TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                 doctors_id INT,
                                 users_id INT,
                                 appointment_id INT,
                                 FOREIGN KEY (doctors_id) REFERENCES doctors(id_doctors),
                                 FOREIGN KEY (users_id) REFERENCES users(id_users),
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
                                  date_ TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                  FOREIGN KEY (users_id) REFERENCES users(id_users) ON DELETE CASCADE
);

-- =========================
-- TABLE NOTIFICATIONS
-- =========================
CREATE TABLE notifications (
                               id_notifications INT PRIMARY KEY AUTO_INCREMENT,
                               users_id INT NOT NULL,
                               title VARCHAR(255),
                               message TEXT,
                               type ENUM('APPOINTMENT','MESSAGE','PAYMENT','SYSTEM') DEFAULT 'SYSTEM',
                               is_read BOOLEAN DEFAULT FALSE,
                               date_ TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               FOREIGN KEY (users_id) REFERENCES users(id_users) ON DELETE CASCADE
);
