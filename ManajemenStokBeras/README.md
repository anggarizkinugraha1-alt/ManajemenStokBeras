# Manajemen Stok Beras

Aplikasi desktop Java Swing + MySQL untuk mengelola stok beras.

## Cara menjalankan
1. Nyalakan MySQL (XAMPP / Laragon), lalu jalankan `database/stokberas.sql`
   (phpMyAdmin → tab SQL, atau `mysql -u root < database/stokberas.sql`).
2. Buka folder ini di NetBeans (Open Project), lalu Run.
   Main class: `com.beras.manajemenstokberas.Main`.
3. Login awal: **admin / admin123**.

Jika user/password/port MySQL berbeda, ubah di `Koneksi.java`.

## Fitur
- Login (password disimpan SHA-256)
- Daftar stok beras + pencarian + sorting kolom
- Tambah / ubah / hapus jenis beras
- Stok masuk & stok keluar (stok keluar ditolak bila melebihi stok)
- Baris merah bila stok <= stok minimum
- Riwayat transaksi (siapa, kapan, berapa kg)
