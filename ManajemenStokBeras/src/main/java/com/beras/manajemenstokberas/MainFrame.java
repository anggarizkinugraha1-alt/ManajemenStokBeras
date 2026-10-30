package com.beras.manajemenstokberas;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * Layar utama: daftar stok beras, tambah/ubah/hapus, stok masuk/keluar,
 * dan riwayat transaksi.
 */
public class MainFrame extends JFrame {

    private static final Locale ID = Locale.forLanguageTag("id-ID");
    private static final NumberFormat FMT_KG = NumberFormat.getNumberInstance(ID);
    private static final NumberFormat FMT_RP = NumberFormat.getNumberInstance(ID);

    static {
        FMT_KG.setMaximumFractionDigits(2);
        FMT_RP.setMaximumFractionDigits(0);
    }

    // Indeks kolom di tabel stok
    private static final int COL_ID = 0, COL_KODE = 1, COL_NAMA = 2, COL_JENIS = 3,
            COL_STOK = 4, COL_MIN = 5, COL_HARGA = 6;

    private final String username;

    private final DefaultTableModel modelStok = new DefaultTableModel(
            new String[]{"ID", "Kode", "Nama Beras", "Jenis", "Stok (kg)", "Stok Min (kg)", "Harga/kg (Rp)"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        @Override
        public Class<?> getColumnClass(int c) {
            if (c == COL_ID) {
                return Integer.class;
            }
            if (c >= COL_STOK) {
                return BigDecimal.class;
            }
            return String.class;
        }
    };

    private final DefaultTableModel modelRiwayat = new DefaultTableModel(
            new String[]{"Waktu", "Nama Beras", "Tipe", "Jumlah (kg)", "Keterangan", "Oleh"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable tblStok = new JTable(modelStok);
    private final JTable tblRiwayat = new JTable(modelRiwayat);
    private final JTextField txtCari = new JTextField(20);
    private final JLabel lblRingkasan = new JLabel(" ");

    public MainFrame(String username) {
        this.username = username;
        setTitle("Manajemen Stok Beras");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(960, 560);
        setLocationRelativeTo(null);

        tblStok.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblStok.setAutoCreateRowSorter(true);
        tblStok.setRowHeight(24);
        tblStok.removeColumn(tblStok.getColumnModel().getColumn(COL_ID)); // ID disembunyikan
        StokRenderer renderer = new StokRenderer();
        tblStok.setDefaultRenderer(Object.class, renderer);
        tblStok.setDefaultRenderer(BigDecimal.class, renderer);

        tblRiwayat.setRowHeight(24);
        tblRiwayat.setAutoCreateRowSorter(true);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Stok Beras", buildTabStok());
        tabs.addTab("Riwayat Transaksi", new JScrollPane(tblRiwayat));
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedIndex() == 1) {
                muatRiwayat();
            }
        });

        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        header.add(new JLabel("Manajemen Stok Beras"), BorderLayout.WEST);
        JButton btnKeluar = new JButton("Logout (" + username + ")");
        btnKeluar.addActionListener(e -> logout());
        header.add(btnKeluar, BorderLayout.EAST);

        getContentPane().add(header, BorderLayout.NORTH);
        getContentPane().add(tabs, BorderLayout.CENTER);

        muatStok();
    }

    // ------------------------------------------------------------------ UI

    private JPanel buildTabStok() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel atas = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnCari = new JButton("Cari");
        JButton btnReset = new JButton("Reset");
        btnCari.addActionListener(e -> muatStok());
        txtCari.addActionListener(e -> muatStok());
        btnReset.addActionListener(e -> {
            txtCari.setText("");
            muatStok();
        });
        atas.add(new JLabel("Cari (kode / nama / jenis):"));
        atas.add(txtCari);
        atas.add(btnCari);
        atas.add(btnReset);

        JPanel bawah = new JPanel(new BorderLayout());
        JPanel tombol = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnTambah = new JButton("Tambah");
        JButton btnUbah = new JButton("Ubah");
        JButton btnHapus = new JButton("Hapus");
        JButton btnMasuk = new JButton("Stok Masuk");
        JButton btnKeluar = new JButton("Stok Keluar");
        btnTambah.addActionListener(e -> tambahBeras());
        btnUbah.addActionListener(e -> ubahBeras());
        btnHapus.addActionListener(e -> hapusBeras());
        btnMasuk.addActionListener(e -> ubahStok(true));
        btnKeluar.addActionListener(e -> ubahStok(false));
        tombol.add(btnTambah);
        tombol.add(btnUbah);
        tombol.add(btnHapus);
        tombol.add(btnMasuk);
        tombol.add(btnKeluar);
        bawah.add(tombol, BorderLayout.WEST);
        bawah.add(lblRingkasan, BorderLayout.EAST);

        panel.add(atas, BorderLayout.NORTH);
        panel.add(new JScrollPane(tblStok), BorderLayout.CENTER);
        panel.add(bawah, BorderLayout.SOUTH);
        return panel;
    }

    /** Baris dengan stok <= stok minimum ditulis merah. */
    private class StokRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            int modelRow = table.convertRowIndexToModel(row);
            int modelCol = table.convertColumnIndexToModel(column);

            if (value instanceof BigDecimal) {
                setText((modelCol == COL_HARGA ? FMT_RP : FMT_KG).format(value));
                setHorizontalAlignment(SwingConstants.RIGHT);
            } else {
                setHorizontalAlignment(SwingConstants.LEFT);
            }

            if (!isSelected) {
                BigDecimal stok = (BigDecimal) modelStok.getValueAt(modelRow, COL_STOK);
                BigDecimal min = (BigDecimal) modelStok.getValueAt(modelRow, COL_MIN);
                setForeground(stok.compareTo(min) <= 0 ? new Color(190, 30, 30) : table.getForeground());
            }
            return this;
        }
    }

    // ---------------------------------------------------------- Muat data

    private void muatStok() {
        String kw = "%" + txtCari.getText().trim() + "%";
        String sql = "SELECT id, kode, nama, jenis, stok_kg, stok_minimum, harga_per_kg "
                + "FROM beras WHERE kode LIKE ? OR nama LIKE ? OR jenis LIKE ? ORDER BY nama";
        try (Connection c = Koneksi.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, kw);
            p.setString(2, kw);
            p.setString(3, kw);
            try (ResultSet rs = p.executeQuery()) {
                modelStok.setRowCount(0);
                BigDecimal total = BigDecimal.ZERO;
                int menipis = 0;
                while (rs.next()) {
                    BigDecimal stok = rs.getBigDecimal("stok_kg");
                    BigDecimal min = rs.getBigDecimal("stok_minimum");
                    modelStok.addRow(new Object[]{
                        rs.getInt("id"), rs.getString("kode"), rs.getString("nama"),
                        rs.getString("jenis"), stok, min, rs.getBigDecimal("harga_per_kg")});
                    total = total.add(stok);
                    if (stok.compareTo(min) <= 0) {
                        menipis++;
                    }
                }
                lblRingkasan.setText(modelStok.getRowCount() + " jenis  |  Total stok: "
                        + FMT_KG.format(total) + " kg  |  Stok menipis: " + menipis);
            }
        } catch (SQLException e) {
            tampilkanError(e);
        }
    }

    private void muatRiwayat() {
        String sql = "SELECT t.waktu, b.nama, t.tipe, t.jumlah_kg, t.keterangan, t.username "
                + "FROM transaksi_stok t JOIN beras b ON b.id = t.beras_id "
                + "ORDER BY t.waktu DESC, t.id DESC LIMIT 500";
        try (Connection c = Koneksi.getConnection();
                Statement st = c.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            modelRiwayat.setRowCount(0);
            while (rs.next()) {
                modelRiwayat.addRow(new Object[]{
                    rs.getTimestamp("waktu"), rs.getString("nama"), rs.getString("tipe"),
                    FMT_KG.format(rs.getBigDecimal("jumlah_kg")),
                    rs.getString("keterangan"), rs.getString("username")});
            }
        } catch (SQLException e) {
            tampilkanError(e);
        }
    }

    // ------------------------------------------------------------ Aksi

    private Integer idTerpilih() {
        int row = tblStok.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Pilih satu baris beras terlebih dahulu.");
            return null;
        }
        return (Integer) modelStok.getValueAt(tblStok.convertRowIndexToModel(row), COL_ID);
    }

    private String teksTerpilih(int kolom) {
        int row = tblStok.convertRowIndexToModel(tblStok.getSelectedRow());
        return String.valueOf(modelStok.getValueAt(row, kolom));
    }

    private void tambahBeras() {
        JTextField kode = new JTextField();
        JTextField nama = new JTextField();
        JComboBox<String> jenis = jenisCombo();
        JTextField stok = new JTextField("0");
        JTextField min = new JTextField("0");
        JTextField harga = new JTextField("0");

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("Kode"));
        form.add(kode);
        form.add(new JLabel("Nama beras"));
        form.add(nama);
        form.add(new JLabel("Jenis"));
        form.add(jenis);
        form.add(new JLabel("Stok awal (kg)"));
        form.add(stok);
        form.add(new JLabel("Stok minimum (kg)"));
        form.add(min);
        form.add(new JLabel("Harga per kg (Rp)"));
        form.add(harga);
        form.add(new JLabel("Desimal pakai titik: 12.5"));

        if (JOptionPane.showConfirmDialog(this, form, "Tambah Beras",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            String k = wajibIsi(kode.getText(), "Kode");
            String n = wajibIsi(nama.getText(), "Nama beras");
            String j = String.valueOf(jenis.getSelectedItem()).trim();
            BigDecimal s = angka(stok.getText(), "Stok awal");
            BigDecimal m = angka(min.getText(), "Stok minimum");
            BigDecimal h = angka(harga.getText(), "Harga");

            try (Connection c = Koneksi.getConnection()) {
                c.setAutoCommit(false);
                try {
                    int id;
                    try (PreparedStatement p = c.prepareStatement(
                            "INSERT INTO beras (kode, nama, jenis, stok_kg, stok_minimum, harga_per_kg) "
                            + "VALUES (?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                        p.setString(1, k);
                        p.setString(2, n);
                        p.setString(3, j);
                        p.setBigDecimal(4, s);
                        p.setBigDecimal(5, m);
                        p.setBigDecimal(6, h);
                        p.executeUpdate();
                        try (ResultSet keys = p.getGeneratedKeys()) {
                            keys.next();
                            id = keys.getInt(1);
                        }
                    }
                    if (s.signum() > 0) {
                        catatTransaksi(c, id, "MASUK", s, "Stok awal");
                    }
                    c.commit();
                } catch (SQLException e) {
                    c.rollback();
                    throw e;
                }
            }
            muatStok();
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                JOptionPane.showMessageDialog(this, "Kode beras sudah dipakai. Gunakan kode lain.");
            } else {
                tampilkanError(e);
            }
        }
    }

    private void ubahBeras() {
        Integer id = idTerpilih();
        if (id == null) {
            return;
        }
        JTextField kode = new JTextField(teksTerpilih(COL_KODE));
        JTextField nama = new JTextField(teksTerpilih(COL_NAMA));
        JComboBox<String> jenis = jenisCombo();
        jenis.setSelectedItem(teksTerpilih(COL_JENIS));
        JTextField min = new JTextField(teksTerpilih(COL_MIN));
        JTextField harga = new JTextField(teksTerpilih(COL_HARGA));

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("Kode"));
        form.add(kode);
        form.add(new JLabel("Nama beras"));
        form.add(nama);
        form.add(new JLabel("Jenis"));
        form.add(jenis);
        form.add(new JLabel("Stok minimum (kg)"));
        form.add(min);
        form.add(new JLabel("Harga per kg (Rp)"));
        form.add(harga);
        form.add(new JLabel("Stok diubah lewat tombol"));
        form.add(new JLabel("Stok Masuk / Stok Keluar"));

        if (JOptionPane.showConfirmDialog(this, form, "Ubah Beras",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            String sql = "UPDATE beras SET kode=?, nama=?, jenis=?, stok_minimum=?, harga_per_kg=? WHERE id=?";
            try (Connection c = Koneksi.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
                p.setString(1, wajibIsi(kode.getText(), "Kode"));
                p.setString(2, wajibIsi(nama.getText(), "Nama beras"));
                p.setString(3, String.valueOf(jenis.getSelectedItem()).trim());
                p.setBigDecimal(4, angka(min.getText(), "Stok minimum"));
                p.setBigDecimal(5, angka(harga.getText(), "Harga"));
                p.setInt(6, id);
                p.executeUpdate();
            }
            muatStok();
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                JOptionPane.showMessageDialog(this, "Kode beras sudah dipakai. Gunakan kode lain.");
            } else {
                tampilkanError(e);
            }
        }
    }

    private void hapusBeras() {
        Integer id = idTerpilih();
        if (id == null) {
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this,
                "Hapus \"" + teksTerpilih(COL_NAMA) + "\"?\nRiwayat transaksinya juga ikut terhapus.",
                "Konfirmasi Hapus", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) {
            return;
        }
        try (Connection c = Koneksi.getConnection();
                PreparedStatement p = c.prepareStatement("DELETE FROM beras WHERE id = ?")) {
            p.setInt(1, id);
            p.executeUpdate();
            muatStok();
        } catch (SQLException e) {
            tampilkanError(e);
        }
    }

    /** Stok masuk (tambah) atau keluar (kurangi) untuk beras yang dipilih. */
    private void ubahStok(boolean masuk) {
        Integer id = idTerpilih();
        if (id == null) {
            return;
        }
        JTextField jumlah = new JTextField();
        JTextField ket = new JTextField();
        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("Beras"));
        form.add(new JLabel(teksTerpilih(COL_NAMA)));
        form.add(new JLabel("Jumlah (kg)"));
        form.add(jumlah);
        form.add(new JLabel(masuk ? "Keterangan (mis. supplier)" : "Keterangan (mis. pembeli)"));
        form.add(ket);

        if (JOptionPane.showConfirmDialog(this, form, masuk ? "Stok Masuk" : "Stok Keluar",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            BigDecimal qty = angka(jumlah.getText(), "Jumlah");
            if (qty.signum() <= 0) {
                throw new IllegalArgumentException("Jumlah harus lebih dari 0.");
            }
            try (Connection c = Koneksi.getConnection()) {
                c.setAutoCommit(false);
                try {
                    String sql = masuk
                            ? "UPDATE beras SET stok_kg = stok_kg + ? WHERE id = ?"
                            : "UPDATE beras SET stok_kg = stok_kg - ? WHERE id = ? AND stok_kg >= ?";
                    try (PreparedStatement p = c.prepareStatement(sql)) {
                        p.setBigDecimal(1, qty);
                        p.setInt(2, id);
                        if (!masuk) {
                            p.setBigDecimal(3, qty);
                        }
                        if (p.executeUpdate() == 0) {
                            c.rollback();
                            JOptionPane.showMessageDialog(this, "Stok tidak cukup untuk dikeluarkan.");
                            return;
                        }
                    }
                    catatTransaksi(c, id, masuk ? "MASUK" : "KELUAR", qty, ket.getText().trim());
                    c.commit();
                } catch (SQLException e) {
                    c.rollback();
                    throw e;
                }
            }
            muatStok();
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
        } catch (SQLException e) {
            tampilkanError(e);
        }
    }

    private void catatTransaksi(Connection c, int berasId, String tipe, BigDecimal qty, String ket)
            throws SQLException {
        try (PreparedStatement p = c.prepareStatement(
                "INSERT INTO transaksi_stok (beras_id, tipe, jumlah_kg, keterangan, username) VALUES (?,?,?,?,?)")) {
            p.setInt(1, berasId);
            p.setString(2, tipe);
            p.setBigDecimal(3, qty);
            p.setString(4, ket.isEmpty() ? null : ket);
            p.setString(5, username);
            p.executeUpdate();
        }
    }

    private void logout() {
        dispose();
        Login login = new Login();
        login.setLocationRelativeTo(null);
        login.setVisible(true);
    }

    // --------------------------------------------------------- Helper

    private JComboBox<String> jenisCombo() {
        JComboBox<String> cb = new JComboBox<>(new String[]{"Premium", "Medium", "Organik", "Ketan", "Lainnya"});
        cb.setEditable(true);
        return cb;
    }

    private static String wajibIsi(String s, String label) {
        if (s == null || s.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " wajib diisi.");
        }
        return s.trim();
    }

    private static BigDecimal angka(String s, String label) {
        try {
            BigDecimal v = new BigDecimal(wajibIsi(s, label).replace(',', '.'));
            if (v.signum() < 0) {
                throw new IllegalArgumentException(label + " tidak boleh negatif.");
            }
            return v.setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " harus berupa angka (contoh: 25 atau 12.5).");
        }
    }

    private void tampilkanError(SQLException e) {
        JOptionPane.showMessageDialog(this, "Kesalahan database: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
    }
}
