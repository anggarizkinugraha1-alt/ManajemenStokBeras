package com.beras.manajemenstokberas;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Pusat pengaturan koneksi database. Ubah nilai di bawah ini
 * jika MySQL Anda memakai user/password/port yang berbeda.
 */
public final class Koneksi {

    private static final String URL = "jdbc:mysql://localhost:3306/stokberas";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private Koneksi() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
