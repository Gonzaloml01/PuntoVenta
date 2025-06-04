package elarrecifesoftware;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.time.LocalDateTime;

public class VentaRapidaFrame extends JFrame {

    private JComboBox<String> comboProductos;
    private JTextField txtCantidad;
    private JButton btnVender;
    private JComboBox<String> comboPago;
    private JTextArea areaLog; // Para mostrar mensajes de éxito/error de esta venta rápida

    private int usuarioLogueadoId;
    private int turnoActualId;


    public VentaRapidaFrame(int userId, int turnoId) {
        this.usuarioLogueadoId = userId;
        this.turnoActualId = turnoId;

        setTitle("Venta Rápida Directa - El Arrecife");
        setSize(500, 350);
        setLocationRelativeTo(null); // Centrar en la pantalla
        setDefaultCloseOperation(DISPOSE_ON_CLOSE); // Solo cierra esta ventana

        setLayout(new GridBagLayout()); // Usar GridBagLayout para un mejor control
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Fila 0: Producto
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(new JLabel("Producto:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2; // Ocupa 2 columnas
        comboProductos = new JComboBox<>();
        add(comboProductos, gbc);

        // Fila 1: Cantidad
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        add(new JLabel("Cantidad:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        txtCantidad = new JTextField(10);
        add(txtCantidad, gbc);

        // Fila 2: Método de Pago
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        add(new JLabel("Pago:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        comboPago = new JComboBox<>(new String[]{"Efectivo", "Tarjeta", "Transferencia"});
        add(comboPago, gbc);

        // Fila 3: Botón Vender
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 3; // Ocupa las 3 columnas
        gbc.fill = GridBagConstraints.NONE; // No estirar horizontalmente
        btnVender = new JButton("Vender Directo");
        btnVender.setFont(new Font("Arial", Font.BOLD, 16));
        btnVender.setPreferredSize(new Dimension(180, 50));
        add(btnVender, gbc);

        // Fila 4: Log de mensajes (área de texto)
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.BOTH; // Estirar en ambas direcciones
        gbc.weighty = 1.0; // Darle peso para que se estire verticalmente
        areaLog = new JTextArea();
        areaLog.setEditable(false);
        areaLog.setLineWrap(true);
        areaLog.setWrapStyleWord(true);
        JScrollPane scrollLog = new JScrollPane(areaLog);
        scrollLog.setPreferredSize(new Dimension(400, 100)); // Tamaño inicial
        add(scrollLog, gbc);


        cargarProductos(); // Carga los productos al iniciar la ventana

        btnVender.addActionListener(e -> registrarVenta());
    }

    private void cargarProductos() {
        comboProductos.removeAllItems();
        Connection con = ConexionDB.conectar();
        if (con == null) {
            areaLog.append("❌ No se pudo conectar a la base de datos al cargar productos.\n");
            return;
        }

        try (Statement stmt = con.createStatement(); ResultSet rs = stmt.executeQuery("SELECT nombre FROM productos ORDER BY nombre")) {
            while (rs.next()) {
                comboProductos.addItem(rs.getString("nombre"));
            }
        } catch (SQLException e) {
            areaLog.append("❌ Error al cargar productos: " + e.getMessage() + "\n");
        } finally {
            try { if (con != null) con.close(); } catch (SQLException ex) { /* ignorar */ }
        }
    }

    private void registrarVenta() {
        String producto = (String) comboProductos.getSelectedItem();
        int cantidad;

        try {
            cantidad = Integer.parseInt(txtCantidad.getText());
            if (cantidad <= 0) {
                areaLog.append("❗ La cantidad debe ser mayor que cero.\n");
                return;
            }
        } catch (NumberFormatException ex) {
            areaLog.append("❗ Ingresa un número válido en 'Cantidad'.\n");
            return;
        }

        String metodoPago = (String) comboPago.getSelectedItem();

        Connection con = ConexionDB.conectar();
        if (con == null) {
            areaLog.append("❌ No se pudo conectar a la base de datos.\n");
            return;
        }

        try {
            con.setAutoCommit(false); // Iniciar transacción

            PreparedStatement ps = con.prepareStatement("SELECT id, precio FROM productos WHERE nombre = ?");
            ps.setString(1, producto);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int idProd = rs.getInt("id");
                double precio = rs.getDouble("precio");

                double total = cantidad * precio;

                PreparedStatement psVenta = con.prepareStatement(
                        "INSERT INTO ventas (total, metodo_pago) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS);
                psVenta.setDouble(1, total);
                psVenta.setString(2, metodoPago);
                psVenta.executeUpdate();

                ResultSet rsKeys = psVenta.getGeneratedKeys();
                int idVenta = 0;
                if (rsKeys.next()) {
                    idVenta = rsKeys.getInt(1);
                }

                PreparedStatement psDetalle = con.prepareStatement(
                        "INSERT INTO detalle_venta (venta_id, producto_id, cantidad, subtotal) VALUES (?, ?, ?, ?)");
                psDetalle.setInt(1, idVenta);
                psDetalle.setInt(2, idProd);
                psDetalle.setInt(3, cantidad);
                psDetalle.setDouble(4, total);
                psDetalle.executeUpdate();

                // Opcional: Actualizar stock si tienes la columna stock en productos
                // PreparedStatement psUpdateStock = con.prepareStatement("UPDATE productos SET stock = stock - ? WHERE id = ?");
                // psUpdateStock.setInt(1, cantidad);
                // psUpdateStock.setInt(2, idProd);
                // psUpdateStock.executeUpdate();

                con.commit(); // Confirmar transacción
                areaLog.append("✅ Venta directa registrada por $" + String.format("%.2f", total) + " de " + cantidad + " " + producto + ".\n");
                txtCantidad.setText(""); // Limpiar campo de cantidad
            } else {
                areaLog.append("❗ Producto no encontrado.\n");
                con.rollback(); // Deshacer si el producto no se encuentra
            }
        } catch (SQLException e) {
            try { if (con != null) con.rollback(); } catch (SQLException ex) { /* ignorar */ }
            areaLog.append("❌ Error al registrar venta: " + e.getMessage() + "\n");
        } finally {
            try { if (con != null) con.close(); } catch (SQLException ex) { /* ignorar */ }
        }
    }
}