-- Database untuk aplikasi Manajemen Stok Beras
-- Jalankan di phpMyAdmin (tab SQL) atau: mysql -u root < stokberas.sql

CREATE DATABASE IF NOT EXISTS stokberas
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE stokberas;

CREATE TABLE IF NOT EXISTS pengguna (
    id        INT AUTO_INCREMENT PRIMARY KEY,
    username  VARCHAR(50)  NOT NULL UNIQUE,
    password  VARCHAR(64)  NOT NULL            -- disimpan sebagai SHA-256
);

CREATE TABLE IF NOT EXISTS beras (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    kode          VARCHAR(20)   NOT NULL UNIQUE,
    nama          VARCHAR(100)  NOT NULL,
    jenis         VARCHAR(50)   NOT NULL DEFAULT 'Medium',
    stok_kg       DECIMAL(12,2) NOT NULL DEFAULT 0,
    stok_minimum  DECIMAL(12,2) NOT NULL DEFAULT 0,
    harga_per_kg  DECIMAL(12,2) NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS transaksi_stok (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    beras_id    INT           NOT NULL,
    tipe        ENUM('MASUK','KELUAR') NOT NULL,
    jumlah_kg   DECIMAL(12,2) NOT NULL,
    keterangan  VARCHAR(255),
    username    VARCHAR(50),
    waktu       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (beras_id) REFERENCES beras(id) ON DELETE CASCADE
);

-- Akun awal: admin / admin123 (segera ganti setelah login pertama)
INSERT IGNORE INTO pengguna (username, password)
VALUES ('admin', SHA2('admin123', 256));

-- Data contoh (boleh dihapus)
INSERT IGNORE INTO beras (kode, nama, jenis, stok_kg, stok_minimum, harga_per_kg) VALUES
 ('BRS-001', 'Pandan Wangi',   'Premium', 250.00, 50.00, 16000),
 ('BRS-002', 'Rojolele',       'Premium', 180.00, 50.00, 15500),
 ('BRS-003', 'IR64',           'Medium',  500.00, 100.00, 13000),
 ('BRS-004', 'Ciherang',       'Medium',   40.00, 100.00, 12800),
 ('BRS-005', 'Beras Merah',    'Organik',  75.00, 30.00, 22000),
 ('BRS-006', 'Ketan Putih',    'Ketan',    60.00, 25.00, 17000);
