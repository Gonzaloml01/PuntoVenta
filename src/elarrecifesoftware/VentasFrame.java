package elarrecifesoftware;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class VentasFrame extends JFrame {

    private JButton btnComedor;
    private JButton btnVentaRapida;
    private JButton btnAbrirTurno;
    private JButton btnCerrarTurno;
    private JButton btnGestionCaja;
    private JButton btnGestionProductos; // NUEVO: Botón para gestión de productos
    private JLabel labelInfoTurno;

    // Para el manejo del turno
    private static int turnoActualId = -1;
    private static int usuarioLogueadoId = -1;
    private static String nombreUsuarioLogueado = "Desconocido";
    private static String rolUsuarioLogueado = "empleado"; // NUEVO: Almacenar el rol del usuario
    private static LocalDateTime fechaAperturaTurno = null;
    private static double fondoInicialTurno = 0.0;


    public VentasFrame(int userId, String username, String userRol) { // Recibe ID, nombre y ROL del usuario logueado
        this.usuarioLogueadoId = userId;
        this.nombreUsuarioLogueado = username;
        this.rolUsuarioLogueado = userRol; // Guardar el rol

        setTitle("El Arrecife - Sistema Principal - Usuario: " + nombreUsuarioLogueado + " (Rol: " + rolUsuarioLogueado + ")");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel panelBotonesPrincipales = new JPanel(new GridLayout(6, 1, 30, 30)); // 6 filas para los 6 botones
        panelBotonesPrincipales.setBorder(BorderFactory.createEmptyBorder(50, 100, 50, 100));

        Font buttonFont = new Font("Arial", Font.BOLD, 24);
        Dimension buttonSize = new Dimension(250, 100);

        btnComedor = new JButton("🍽️ Comedor");
        btnComedor.setFont(buttonFont);
        btnComedor.setPreferredSize(buttonSize);
        btnComedor.addActionListener(e -> abrirComedor());

        btnVentaRapida = new JButton("⚡ Venta Rápida");
        btnVentaRapida.setFont(buttonFont);
        btnVentaRapida.setPreferredSize(buttonSize);
        btnVentaRapida.addActionListener(e -> abrirVentaRapida());

        btnAbrirTurno = new JButton("🔓 Abrir Turno");
        btnAbrirTurno.setFont(buttonFont);
        btnAbrirTurno.setPreferredSize(buttonSize);
        btnAbrirTurno.addActionListener(e -> abrirTurno());

        btnCerrarTurno = new JButton("🔒 Cerrar Turno");
        btnCerrarTurno.setFont(buttonFont);
        btnCerrarTurno.setPreferredSize(buttonSize);
        btnCerrarTurno.addActionListener(e -> cerrarTurno());

        btnGestionCaja = new JButton("💰 Gestión de Caja");
        btnGestionCaja.setFont(buttonFont);
        btnGestionCaja.setPreferredSize(buttonSize);
        btnGestionCaja.addActionListener(e -> abrirGestionCaja());

        // NUEVO BOTÓN
        btnGestionProductos = new JButton("🛒 Gestión de Productos");
        btnGestionProductos.setFont(buttonFont);
        btnGestionProductos.setPreferredSize(buttonSize);
        btnGestionProductos.addActionListener(e -> abrirGestionProductos());

        panelBotonesPrincipales.add(btnComedor);
        panelBotonesPrincipales.add(btnVentaRapida);
        panelBotonesPrincipales.add(btnGestionCaja);
        panelBotonesPrincipales.add(btnGestionProductos); // Añadir al panel
        panelBotonesPrincipales.add(btnAbrirTurno);
        panelBotonesPrincipales.add(btnCerrarTurno);

        add(panelBotonesPrincipales, BorderLayout.CENTER);

        labelInfoTurno = new JLabel("Turno: No hay turno abierto", SwingConstants.CENTER);
        labelInfoTurno.setFont(new Font("Arial", Font.ITALIC, 16));
        labelInfoTurno.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(labelInfoTurno, BorderLayout.SOUTH);

        cargarEstadoTurno();
        actualizarVisibilidadBotonesPorRol(); // NUEVO: Ajustar visibilidad según el rol
    }

    private void cargarEstadoTurno() {
        Connection con = ConexionDB.conectar();
        if (con == null) {
            JOptionPane.showMessageDialog(this, "No se pudo conectar a la base de datos para verificar el turno.", "Error de Conexión", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            PreparedStatement ps = con.prepareStatement(
                "SELECT id, fecha_apertura, fondo_inicial FROM turnos WHERE usuario_apertura_id = ? AND fecha_cierre IS NULL ORDER BY fecha_apertura DESC LIMIT 1"
            );
            ps.setInt(1, usuarioLogueadoId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                turnoActualId = rs.getInt("id");
                fechaAperturaTurno = rs.getTimestamp("fecha_apertura").toLocalDateTime();
                fondoInicialTurno = rs.getDouble("fondo_inicial");
                labelInfoTurno.setText("Turno Abierto: ID " + turnoActualId + " por " + nombreUsuarioLogueado + " desde " + fechaAperturaTurno.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + " (Fondo: $" + String.format("%.2f", fondoInicialTurno) + ")");
                btnAbrirTurno.setEnabled(false);
                btnCerrarTurno.setEnabled(true);
                btnComedor.setEnabled(true);
                btnVentaRapida.setEnabled(true);
                btnGestionCaja.setEnabled(true);
            } else {
                turnoActualId = -1;
                fechaAperturaTurno = null;
                fondoInicialTurno = 0.0;
                labelInfoTurno.setText("Turno: No hay turno abierto");
                btnAbrirTurno.setEnabled(true);
                btnCerrarTurno.setEnabled(false);
                btnComedor.setEnabled(false);
                btnVentaRapida.setEnabled(false);
                btnGestionCaja.setEnabled(false);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar estado del turno: " + e.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } finally {
            try { if (con != null) con.close(); } catch (SQLException ex) { /* ignorar */ }
        }
    }

    // NUEVO MÉTODO
    private void actualizarVisibilidadBotonesPorRol() {
        if ("admin".equalsIgnoreCase(rolUsuarioLogueado)) {
            btnGestionProductos.setVisible(true); // Los administradores pueden gestionar productos
        } else {
            btnGestionProductos.setVisible(false); // Los empleados no
        }
        // Puedes agregar más lógica aquí para otros botones si es necesario
    }


    private void abrirTurno() {
        if (turnoActualId != -1) {
            JOptionPane.showMessageDialog(this, "Ya hay un turno abierto.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String fondoInicialStr = JOptionPane.showInputDialog(this, "Ingresa el fondo inicial de caja para este turno:", "Fondo Inicial", JOptionPane.QUESTION_MESSAGE);
        if (fondoInicialStr == null || fondoInicialStr.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Apertura de turno cancelada.", "Cancelado", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            double fondoInicial = Double.parseDouble(fondoInicialStr.trim());
            if (fondoInicial < 0) {
                JOptionPane.showMessageDialog(this, "El fondo inicial no puede ser negativo.", "Error de Entrada", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Connection con = ConexionDB.conectar();
            if (con == null) {
                JOptionPane.showMessageDialog(this, "No se pudo conectar a la base de datos para abrir turno.", "Error de Conexión", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO turnos (usuario_apertura_id, fondo_inicial) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS
                );
                ps.setInt(1, usuarioLogueadoId);
                ps.setDouble(2, fondoInicial);
                ps.executeUpdate();

                ResultSet rsKeys = ps.getGeneratedKeys();
                if (rsKeys.next()) {
                    turnoActualId = rsKeys.getInt(1);
                    fechaAperturaTurno = LocalDateTime.now();
                    fondoInicialTurno = fondoInicial;
                    labelInfoTurno.setText("Turno Abierto: ID " + turnoActualId + " por " + nombreUsuarioLogueado + " desde " + fechaAperturaTurno.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + " (Fondo: $" + String.format("%.2f", fondoInicialTurno) + ")");
                    btnAbrirTurno.setEnabled(false);
                    btnCerrarTurno.setEnabled(true);
                    btnComedor.setEnabled(true);
                    btnVentaRapida.setEnabled(true);
                    btnGestionCaja.setEnabled(true);
                    JOptionPane.showMessageDialog(this, "Turno abierto correctamente con fondo de $" + String.format("%.2f", fondoInicial) + ".", "Turno Abierto", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error al abrir turno: " + e.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            } finally {
                try { if (con != null) con.close(); } catch (SQLException ex) { /* ignorar */ }
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Por favor, ingresa un valor numérico válido para el fondo inicial.", "Error de Entrada", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cerrarTurno() {
        if (turnoActualId == -1) {
            JOptionPane.showMessageDialog(this, "No hay un turno abierto para cerrar.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "¿Estás seguro de que quieres cerrar el turno actual?", "Confirmar Cierre de Turno", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.NO_OPTION) {
            return;
        }

        Connection con = ConexionDB.conectar();
        if (con == null) {
            JOptionPane.showMessageDialog(this, "No se pudo conectar a la base de datos para cerrar turno.", "Error de Conexión", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            con.setAutoCommit(false);

            PreparedStatement psVentas = con.prepareStatement(
                "SELECT SUM(total) AS total_ventas, " +
                "       SUM(CASE WHEN metodo_pago = 'Efectivo' THEN total ELSE 0 END) AS ventas_efectivo, " +
                "       SUM(CASE WHEN metodo_pago = 'Tarjeta' THEN total ELSE 0 END) AS ventas_tarjeta, " +
                "       SUM(CASE WHEN metodo_pago = 'Transferencia' THEN total ELSE 0 END) AS ventas_transferencia " +
                "FROM ventas WHERE fecha >= ? AND fecha <= NOW()"
            );
            psVentas.setTimestamp(1, Timestamp.valueOf(fechaAperturaTurno));
            ResultSet rsVentas = psVentas.executeQuery();

            double totalVentasTurno = 0.0;
            double ventasEfectivo = 0.0;
            double ventasTarjeta = 0.0;
            double ventasTransferencia = 0.0;

            if (rsVentas.next()) {
                totalVentasTurno = rsVentas.getDouble("total_ventas");
                ventasEfectivo = rsVentas.getDouble("ventas_efectivo");
                ventasTarjeta = rsVentas.getDouble("ventas_tarjeta");
                ventasTransferencia = rsVentas.getDouble("ventas_transferencia");
            }

            PreparedStatement psMovimientos = con.prepareStatement(
                "SELECT SUM(CASE WHEN tipo = 'Retiro' THEN monto ELSE 0 END) AS total_retiros, " +
                "       SUM(CASE WHEN tipo = 'Deposito' THEN monto ELSE 0 END) AS total_depositos " +
                "FROM movimientos_caja WHERE turno_id = ?"
            );
            psMovimientos.setInt(1, turnoActualId);
            ResultSet rsMovimientos = psMovimientos.executeQuery();

            double retirosEfectivo = 0.0;
            double depositosEfectivo = 0.0;
            if (rsMovimientos.next()) {
                retirosEfectivo = rsMovimientos.getDouble("total_retiros");
                depositosEfectivo = rsMovimientos.getDouble("total_depositos");
            }

            double totalCajaEsperado = fondoInicialTurno + ventasEfectivo + depositosEfectivo - retirosEfectivo;

            String cajaFinalStr = JOptionPane.showInputDialog(this,
                    "Total de efectivo esperado en caja: $" + String.format("%.2f", totalCajaEsperado) + "\n" +
                    "Ingresa el total de efectivo contado en caja:",
                    "Cierre de Turno - Conteo de Caja", JOptionPane.QUESTION_MESSAGE);

            if (cajaFinalStr == null || cajaFinalStr.trim().isEmpty()) {
                con.rollback();
                JOptionPane.showMessageDialog(this, "Cierre de turno cancelado.", "Cancelado", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            double totalCajaFinal = 0.0;
            try {
                totalCajaFinal = Double.parseDouble(cajaFinalStr.trim());
            } catch (NumberFormatException e) {
                con.rollback();
                JOptionPane.showMessageDialog(this, "Valor no numérico ingresado para el efectivo final. Cierre de turno cancelado.", "Error de Entrada", JOptionPane.ERROR_MESSAGE);
                return;
            }

            double diferenciaCaja = totalCajaFinal - totalCajaEsperado;

            PreparedStatement psUpdateTurno = con.prepareStatement(
                "UPDATE turnos SET fecha_cierre = NOW(), usuario_cierre_id = ?, " +
                "ventas_efectivo = ?, ventas_tarjeta = ?, ventas_transferencia = ?, total_ventas = ?, " +
                "retiros_efectivo = ?, depositos_efectivo = ?, " +
                "total_caja_esperado = ?, total_caja_final = ?, diferencia_caja = ? " +
                "WHERE id = ?"
            );
            psUpdateTurno.setInt(1, usuarioLogueadoId);
            psUpdateTurno.setDouble(2, ventasEfectivo);
            psUpdateTurno.setDouble(3, ventasTarjeta);
            psUpdateTurno.setDouble(4, ventasTransferencia);
            psUpdateTurno.setDouble(5, totalVentasTurno);
            psUpdateTurno.setDouble(6, retirosEfectivo);
            psUpdateTurno.setDouble(7, depositosEfectivo);
            psUpdateTurno.setDouble(8, totalCajaEsperado);
            psUpdateTurno.setDouble(9, totalCajaFinal);
            psUpdateTurno.setDouble(10, diferenciaCaja);
            psUpdateTurno.setInt(11, turnoActualId);
            psUpdateTurno.executeUpdate();

            con.commit();

            String mensajeCierre = String.format(
                "Turno cerrado correctamente.\n" +
                "-----------------------------------\n" +
                "Fondo Inicial: $%.2f\n" +
                "Ventas Efectivo: $%.2f\n" +
                "Depósitos Efectivo: $%.2f\n" +
                "Retiros Efectivo: $%.2f\n" +
                "-----------------------------------\n" +
                "TOTAL ESPERADO EN CAJA: $%.2f\n" +
                "TOTAL CONTADO EN CAJA: $%.2f\n" +
                "DIFERENCIA: $%.2f\n" +
                "-----------------------------------\n" +
                "Total Ventas (Todo método): $%.2f\n" +
                "Ventas Tarjeta: $%.2f\n" +
                "Ventas Transferencia: $%.2f",
                fondoInicialTurno, ventasEfectivo, depositosEfectivo, retirosEfectivo,
                totalCajaEsperado, totalCajaFinal, diferenciaCaja,
                totalVentasTurno, ventasTarjeta, ventasTransferencia
            );
            JOptionPane.showMessageDialog(this, mensajeCierre, "Turno Cerrado - Reporte", JOptionPane.INFORMATION_MESSAGE);

            turnoActualId = -1;
            fechaAperturaTurno = null;
            fondoInicialTurno = 0.0;
            cargarEstadoTurno();
        } catch (SQLException e) {
            try { if (con != null) con.rollback(); } catch (SQLException ex) { /* ignorar */ }
            JOptionPane.showMessageDialog(this, "Error al cerrar turno: " + e.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } finally {
            try { if (con != null) con.close(); } catch (SQLException ex) { /* ignorar */ }
        }
    }


    private void abrirComedor() {
        if (turnoActualId == -1) {
            JOptionPane.showMessageDialog(this, "Debes abrir un turno para acceder al comedor.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }
        SwingUtilities.invokeLater(() -> new ComedorFrame(usuarioLogueadoId, turnoActualId).setVisible(true));
    }

    private void abrirVentaRapida() {
        if (turnoActualId == -1) {
            JOptionPane.showMessageDialog(this, "Debes abrir un turno para realizar ventas rápidas.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }
        SwingUtilities.invokeLater(() -> new VentaRapidaFrame(usuarioLogueadoId, turnoActualId).setVisible(true));
    }

    private void abrirGestionCaja() {
        if (turnoActualId == -1) {
            JOptionPane.showMessageDialog(this, "Debes abrir un turno para gestionar la caja.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }
        SwingUtilities.invokeLater(() -> new GestionCajaFrame(usuarioLogueadoId, turnoActualId).setVisible(true));
    }

    // NUEVO MÉTODO
    private void abrirGestionProductos() {
        // No se necesita un turno abierto para gestionar productos
        // Asegúrate de que el usuario tenga el rol de 'admin'
        if ("admin".equalsIgnoreCase(rolUsuarioLogueado)) {
            SwingUtilities.invokeLater(() -> new GestionProductosFrame().setVisible(true));
        } else {
            JOptionPane.showMessageDialog(this, "Solo los administradores pueden gestionar productos.", "Acceso Denegado", JOptionPane.WARNING_MESSAGE);
        }
    }


    // El método main ahora solo inicia el LoginFrame
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}