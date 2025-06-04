package elarrecifesoftware;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.format.DateTimeFormatter;

public class GestionCajaFrame extends JFrame {

    private JRadioButton rbRetiro;
    private JRadioButton rbDeposito;
    private JTextField txtMonto;
    private JTextArea txtDescripcion;
    private JButton btnRegistrar;
    private JTable tablaMovimientos;
    private DefaultTableModel modeloTablaMovimientos;

    private int usuarioLogueadoId;
    private int turnoActualId;

    public GestionCajaFrame(int userId, int turnoId) {
        this.usuarioLogueadoId = userId;
        this.turnoActualId = turnoId;

        setTitle("Gestión de Caja - Turno ID: " + turnoActualId);
        setSize(700, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // Panel Superior - Registro de Movimientos
        JPanel panelRegistro = new JPanel(new GridBagLayout());
        panelRegistro.setBorder(BorderFactory.createTitledBorder("Registrar Movimiento de Caja"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Tipo de Movimiento
        ButtonGroup tipoGrupo = new ButtonGroup();
        rbRetiro = new JRadioButton("Retiro / Salida de Dinero");
        rbDeposito = new JRadioButton("Depósito / Entrada de Dinero");
        tipoGrupo.add(rbRetiro);
        tipoGrupo.add(rbDeposito);
        rbRetiro.setSelected(true); // Por defecto, es un retiro

        JPanel panelTipo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelTipo.add(rbRetiro);
        panelTipo.add(rbDeposito);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panelRegistro.add(panelTipo, gbc);

        // Monto
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        panelRegistro.add(new JLabel("Monto: $"), gbc);
        gbc.gridx = 1;
        txtMonto = new JTextField(15);
        panelRegistro.add(txtMonto, gbc);

        // Descripción
        gbc.gridx = 0;
        gbc.gridy = 2;
        panelRegistro.add(new JLabel("Descripción:"), gbc);
        gbc.gridx = 1;
        txtDescripcion = new JTextArea(3, 15);
        txtDescripcion.setLineWrap(true);
        txtDescripcion.setWrapStyleWord(true);
        JScrollPane scrollDesc = new JScrollPane(txtDescripcion);
        panelRegistro.add(scrollDesc, gbc);

        // Botón Registrar
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        btnRegistrar = new JButton("Registrar Movimiento");
        panelRegistro.add(btnRegistrar, gbc);

        add(panelRegistro, BorderLayout.NORTH);

        // Panel Central - Historial de Movimientos
        JPanel panelHistorial = new JPanel(new BorderLayout());
        panelHistorial.setBorder(BorderFactory.createTitledBorder("Historial de Movimientos del Turno"));

        String[] columnas = {"Tipo", "Monto", "Fecha", "Descripción", "Usuario"};
        modeloTablaMovimientos = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Hacer que las celdas no sean editables
            }
        };
        tablaMovimientos = new JTable(modeloTablaMovimientos);
        tablaMovimientos.setFillsViewportHeight(true);
        panelHistorial.add(new JScrollPane(tablaMovimientos), BorderLayout.CENTER);

        add(panelHistorial, BorderLayout.CENTER);

        // Acciones
        btnRegistrar.addActionListener(e -> registrarMovimiento());
        cargarMovimientos();
    }

    private void registrarMovimiento() {
        String tipo = rbRetiro.isSelected() ? "Retiro" : "Deposito";
        double monto;
        String descripcion = txtDescripcion.getText().trim();

        if (descripcion.isEmpty()) {
            descripcion = tipo + " de efectivo"; // Descripción por defecto
        }

        try {
            monto = Double.parseDouble(txtMonto.getText());
            if (monto <= 0) {
                JOptionPane.showMessageDialog(this, "El monto debe ser positivo.", "Error de Entrada", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Por favor, ingresa un monto numérico válido.", "Error de Entrada", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Connection con = ConexionDB.conectar();
        if (con == null) {
            JOptionPane.showMessageDialog(this, "No se pudo conectar a la base de datos.", "Error de Conexión", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO movimientos_caja (turno_id, tipo, monto, descripcion, usuario_id) VALUES (?, ?, ?, ?, ?)"
            );
            ps.setInt(1, turnoActualId);
            ps.setString(2, tipo);
            ps.setDouble(3, monto);
            ps.setString(4, descripcion);
            ps.setInt(5, usuarioLogueadoId);
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "Movimiento de " + tipo + " por $" + String.format("%.2f", monto) + " registrado.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            txtMonto.setText("");
            txtDescripcion.setText("");
            rbRetiro.setSelected(true); // Resetear a retiro por defecto
            cargarMovimientos(); // Recargar el historial
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al registrar movimiento: " + e.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } finally {
            try { if (con != null) con.close(); } catch (SQLException ex) { /* ignorar */ }
        }
    }

    private void cargarMovimientos() {
        modeloTablaMovimientos.setRowCount(0); // Limpiar tabla
        Connection con = ConexionDB.conectar();
        if (con == null) {
            return;
        }

        try {
            PreparedStatement ps = con.prepareStatement(
                "SELECT mc.tipo, mc.monto, mc.fecha, mc.descripcion, u.username " +
                "FROM movimientos_caja mc JOIN usuarios u ON mc.usuario_id = u.id " +
                "WHERE mc.turno_id = ? ORDER BY mc.fecha DESC"
            );
            ps.setInt(1, turnoActualId);
            ResultSet rs = ps.executeQuery();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

            while (rs.next()) {
                String tipo = rs.getString("tipo");
                double monto = rs.getDouble("monto");
                String fecha = rs.getTimestamp("fecha").toLocalDateTime().format(formatter);
                String descripcion = rs.getString("descripcion");
                String usuario = rs.getString("username");

                modeloTablaMovimientos.addRow(new Object[]{tipo, String.format("$%.2f", monto), fecha, descripcion, usuario});
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar movimientos de caja: " + e.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } finally {
            try { if (con != null) con.close(); } catch (SQLException ex) { /* ignorar */ }
        }
    }
}
