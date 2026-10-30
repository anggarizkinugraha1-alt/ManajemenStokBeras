package com.beras.manajemenstokberas;

import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Login login = new Login();
            login.setTitle("Manajemen Stok Beras - Login");
            login.setLocationRelativeTo(null);
            login.setVisible(true);
        });
    }
}
