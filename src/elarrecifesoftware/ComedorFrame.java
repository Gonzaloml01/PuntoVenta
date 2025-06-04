package elarrecifesoftware;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class ComedorFrame extends JFrame {
    private JList<String> listaMesas;
    private DefaultListModel<String> mesasModel;
    private JTable tablaPedidos;
    private DefaultTableModel modeloTablaPedidos;
    private JButton btnAbrirMesa, btnCerrarMesa, btnAgregarProducto, btnEliminarProducto, btnCobrarMesa;
    private int usuarioLogueadoId, turnoActualId, mesaSeleccionadaId = -1;
    private Map<Integer, Integer> mapaMesaIdToListIndex = new HashMap<>();

    public ComedorFrame(int usuarioLogueadoId, int turnoActualId) {
        this.usuarioLogueadoId = usuarioLogueadoId;
        this.turnoActualId = turnoActualId;
        configurarUI();
        cargarMesas();
    }

    private void configurarUI() {
        setTitle("Gestión de Comedor");
        setSize(800, 600);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        
        // Panel Mesas
        JPanel panelMesas = new JPanel(new BorderLayout());
        mesasModel = new DefaultListModel<>();
        listaMesas = new JList<>(mesasModel);
        listaMesas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaMesas.addListSelectionListener(this::manejarSeleccionMesa);
        panelMesas.add(new JScrollPane(listaMesas), BorderLayout.CENTER);

        // Panel Botones Mesas
        JPanel panelBotonesMesas = new JPanel(new GridLayout(1, 3));
        btnAbrirMesa = new JButton("Abrir Mesa");
        btnCerrarMesa = new JButton("Cerrar/Eliminar Mesa");
        btnCobrarMesa = new JButton("Cobrar Mesa");
        btnAbrirMesa.addActionListener(e -> abrirNuevaMesa());
        btnCerrarMesa.addActionListener(e -> cerrarMesa());
        btnCobrarMesa.addActionListener(e -> cobrarMesa());
        panelBotonesMesas.add(btnAbrirMesa);
        panelBotonesMesas.add(btnCerrarMesa);
        panelBotonesMesas.add(btnCobrarMesa);
        panelMesas.add(panelBotonesMesas, BorderLayout.SOUTH);

        // Panel Pedidos
        JPanel panelPedidos = new JPanel(new BorderLayout());
        modeloTablaPedidos = new DefaultTableModel(new Object[]{"ID", "Producto", "Cantidad", "Precio", "Subtotal"}, 0);
        tablaPedidos = new JTable(modeloTablaPedidos);
        panelPedidos.add(new JScrollPane(tablaPedidos), BorderLayout.CENTER);

        // Panel Botones Pedidos
        JPanel panelBotonesPedidos = new JPanel();
        btnAgregarProducto = new JButton("Agregar Producto");
        btnEliminarProducto = new JButton("Eliminar Producto");
        btnAgregarProducto.addActionListener(e -> agregarProductoAMesa());
        btnEliminarProducto.addActionListener(e -> eliminarProductoDePedido());
        panelBotonesPedidos.add(btnAgregarProducto);
        panelBotonesPedidos.add(btnEliminarProducto);
        panelPedidos.add(panelBotonesPedidos, BorderLayout.SOUTH);

        // Layout Principal
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelMesas, panelPedidos);
        splitPane.setDividerLocation(300);
        add(splitPane);

        actualizarEstadoBotones();
    }

    private void manejarSeleccionMesa(ListSelectionEvent e) {
           if (!e.getValueIsAdjusting()) {
            String selected = listaMesas.getSelectedValue();
            if (selected != null) {
                try {
                    int start = selected.indexOf("ID:") + 3;
                    int end = selected.indexOf(')', start);
                    mesaSeleccionadaId = Integer.parseInt(selected.substring(start, end));
                    cargarPedidosMesa(mesaSeleccionadaId);
                } catch (Exception ex) {
                    mesaSeleccionadaId = -1;
                }
            }
            actualizarEstadoBotones();
        }
    }

    private void actualizarEstadoBotones() {
        boolean mesaSeleccionada = mesaSeleccionadaId != -1;
        btnAgregarProducto.setEnabled(mesaSeleccionada);
        btnEliminarProducto.setEnabled(mesaSeleccionada);
        btnCerrarMesa.setEnabled(mesaSeleccionada);
        btnCobrarMesa.setEnabled(mesaSeleccionada);
    }

    private void cargarMesas() {
        mesasModel.clear();
        mapaMesaIdToListIndex.clear();
        try (Connection con = ConexionDB.conectar()) {
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT id, numero, estado FROM mesas ORDER BY numero");
            int index = 0;
            while (rs.next()) {
                String info = "Mesa " + rs.getInt("numero") + " (ID:" + rs.getInt("id") + ") - " + rs.getString("estado");
                mesasModel.addElement(info);
                mapaMesaIdToListIndex.put(rs.getInt("id"), index++);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar mesas: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void abrirNuevaMesa() {
        String input = JOptionPane.showInputDialog(this, "Ingrese número de mesa:", "Nueva Mesa", JOptionPane.QUESTION_MESSAGE);
        if (input == null || input.trim().isEmpty()) return;

        try (Connection con = ConexionDB.conectar()) {
            int numeroMesa = Integer.parseInt(input);
            
            // Verificar si ya existe
            PreparedStatement psCheck = con.prepareStatement("SELECT 1 FROM mesas WHERE numero = ?");
            psCheck.setInt(1, numeroMesa);
            if (psCheck.executeQuery().next()) {
                JOptionPane.showMessageDialog(this, "Ya existe una mesa con ese número", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Crear nueva mesa
            PreparedStatement psInsert = con.prepareStatement(
                "INSERT INTO mesas (numero, estado) VALUES (?, 'abierta')", 
                Statement.RETURN_GENERATED_KEYS
            );
            psInsert.setInt(1, numeroMesa);
            psInsert.executeUpdate();

            ResultSet rs = psInsert.getGeneratedKeys();
            if (rs.next()) {
                JOptionPane.showMessageDialog(this, "Mesa creada con ID: " + rs.getInt(1), "Éxito", JOptionPane.INFORMATION_MESSAGE);
                cargarMesas();
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Ingrese un número válido", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al crear mesa: " + e.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cerrarMesa() {
        if (mesaSeleccionadaId == -1) return;

        int confirm = JOptionPane.showConfirmDialog(
            this, 
            "¿Eliminar esta mesa y todos sus pedidos?", 
            "Confirmar", 
            JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection con = ConexionDB.conectar()) {
                con.setAutoCommit(false);
                
                // Eliminar pedidos primero
                PreparedStatement psDeletePedidos = con.prepareStatement("DELETE FROM pedidos WHERE mesa_id = ?");
                psDeletePedidos.setInt(1, mesaSeleccionadaId);
                psDeletePedidos.executeUpdate();
                
                // Luego eliminar mesa
                PreparedStatement psDeleteMesa = con.prepareStatement("DELETE FROM mesas WHERE id = ?");
                psDeleteMesa.setInt(1, mesaSeleccionadaId);
                int affected = psDeleteMesa.executeUpdate();
                
                if (affected > 0) {
                    con.commit();
                    mesaSeleccionadaId = -1;
                    cargarMesas();
                    limpiarPedidos();
                    JOptionPane.showMessageDialog(this, "Mesa eliminada correctamente", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    con.rollback();
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error al eliminar mesa: " + e.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void cargarPedidosMesa(int mesaId) {
        modeloTablaPedidos.setRowCount(0);
        try (Connection con = ConexionDB.conectar()) {
            PreparedStatement ps = con.prepareStatement(
                "SELECT p.id, pr.nombre, p.cantidad, pr.precio " +
                "FROM pedidos p JOIN productos pr ON p.producto_id = pr.id " +
                "WHERE p.mesa_id = ?"
            );
            ps.setInt(1, mesaId);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                double precio = rs.getDouble("precio");
                int cantidad = rs.getInt("cantidad");
                modeloTablaPedidos.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("nombre"),
                    cantidad,
                    precio,
                    precio * cantidad
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar pedidos: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void agregarProductoAMesa() {
        if (mesaSeleccionadaId == -1) return;

        JDialog dialog = new JDialog(this, "Agregar Producto", true);
        dialog.setLayout(new GridLayout(3, 2));

        JComboBox<String> comboProductos = new JComboBox<>();
        JTextField txtCantidad = new JTextField("1");
        JButton btnConfirmar = new JButton("Agregar");

        // Cargar productos
        try (Connection con = ConexionDB.conectar()) {
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT id, nombre FROM productos");
            while (rs.next()) {
                comboProductos.addItem(rs.getString("nombre"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(dialog, "Error al cargar productos", "Error", JOptionPane.ERROR_MESSAGE);
        }

        btnConfirmar.addActionListener(e -> {
            try {
                int cantidad = Integer.parseInt(txtCantidad.getText());
                if (cantidad <= 0) throw new NumberFormatException();
                
                String producto = (String) comboProductos.getSelectedItem();
                int productoId = obtenerIdProducto(producto);
                
                if (productoId != -1) {
                    agregarProductoAPedidoMesaDB(mesaSeleccionadaId, productoId, cantidad);
                    dialog.dispose();
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Ingrese cantidad válida", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.add(new JLabel("Producto:"));
        dialog.add(comboProductos);
        dialog.add(new JLabel("Cantidad:"));
        dialog.add(txtCantidad);
        dialog.add(new JLabel());
        dialog.add(btnConfirmar);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private int obtenerIdProducto(String nombre) {
        try (Connection con = ConexionDB.conectar()) {
            PreparedStatement ps = con.prepareStatement("SELECT id FROM productos WHERE nombre = ?");
            ps.setString(1, nombre);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt("id") : -1;
        } catch (SQLException e) {
            return -1;
        }
    }

    private void agregarProductoAPedidoMesaDB(int mesaId, int productoId, int cantidad) {
        try (Connection con = ConexionDB.conectar()) {
            con.setAutoCommit(false);
            
            // Verificar si ya existe el producto en la mesa
            PreparedStatement psCheck = con.prepareStatement(
                "SELECT id, cantidad FROM pedidos WHERE mesa_id = ? AND producto_id = ?"
            );
            psCheck.setInt(1, mesaId);
            psCheck.setInt(2, productoId);
            ResultSet rs = psCheck.executeQuery();
            
            if (rs.next()) {
                // Actualizar cantidad existente
                PreparedStatement psUpdate = con.prepareStatement(
                    "UPDATE pedidos SET cantidad = cantidad + ? WHERE id = ?"
                );
                psUpdate.setInt(1, cantidad);
                psUpdate.setInt(2, rs.getInt("id"));
                psUpdate.executeUpdate();
            } else {
                // Insertar nuevo pedido
                PreparedStatement psInsert = con.prepareStatement(
                    "INSERT INTO pedidos (mesa_id, producto_id, cantidad) VALUES (?, ?, ?)"
                );
                psInsert.setInt(1, mesaId);
                psInsert.setInt(2, productoId);
                psInsert.setInt(3, cantidad);
                psInsert.executeUpdate();
            }
            
            // Actualizar estado de la mesa
            PreparedStatement psUpdateMesa = con.prepareStatement(
                "UPDATE mesas SET estado = 'ocupada' WHERE id = ? AND estado = 'abierta'"
            );
            psUpdateMesa.setInt(1, mesaId);
            psUpdateMesa.executeUpdate();
            
            con.commit();
            cargarPedidosMesa(mesaId);
            JOptionPane.showMessageDialog(this, "Producto agregado correctamente", "Éxito", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al agregar producto: " + e.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void eliminarProductoDePedido() {
        int fila = tablaPedidos.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione un producto", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int pedidoId = (int) modeloTablaPedidos.getValueAt(fila, 0);
        int confirm = JOptionPane.showConfirmDialog(
            this, 
            "¿Eliminar este producto del pedido?", 
            "Confirmar", 
            JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection con = ConexionDB.conectar()) {
                PreparedStatement ps = con.prepareStatement("DELETE FROM pedidos WHERE id = ?");
                ps.setInt(1, pedidoId);
                if (ps.executeUpdate() > 0) {
                    cargarPedidosMesa(mesaSeleccionadaId);
                    verificarEstadoMesa();
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error al eliminar producto: " + e.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void verificarEstadoMesa() {
        try (Connection con = ConexionDB.conectar()) {
            PreparedStatement ps = con.prepareStatement(
                "SELECT COUNT(*) AS total FROM pedidos WHERE mesa_id = ?"
            );
            ps.setInt(1, mesaSeleccionadaId);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next() && rs.getInt("total") == 0) {
                PreparedStatement psUpdate = con.prepareStatement(
                    "UPDATE mesas SET estado = 'abierta' WHERE id = ?"
                );
                psUpdate.setInt(1, mesaSeleccionadaId);
                psUpdate.executeUpdate();
            }
        } catch (SQLException e) {
            // No es crítico, podemos ignorar este error
        }
    }

    private void cobrarMesa() {
        if (mesaSeleccionadaId == -1) return;

        double total = calcularTotalMesa();
        if (total <= 0) {
            JOptionPane.showMessageDialog(this, "La mesa no tiene pedidos", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JPanel panel = new JPanel(new GridLayout(4, 2));
        JTextField txtMonto = new JTextField();
        JComboBox<String> comboMetodo = new JComboBox<>(new String[]{"Efectivo", "Tarjeta", "Transferencia"});
        JLabel lblCambio = new JLabel("$0.00");

        panel.add(new JLabel("Total:"));
        panel.add(new JLabel(String.format("$%.2f", total)));
        panel.add(new JLabel("Método de pago:"));
        panel.add(comboMetodo);
        panel.add(new JLabel("Monto recibido:"));
        panel.add(txtMonto);
        panel.add(new JLabel("Cambio:"));
        panel.add(lblCambio);

        txtMonto.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { calcularCambio(); }
            public void insertUpdate(DocumentEvent e) { calcularCambio(); }
            public void removeUpdate(DocumentEvent e) { calcularCambio(); }
            
            private void calcularCambio() {
                try {
                    double monto = Double.parseDouble(txtMonto.getText());
                    lblCambio.setText(String.format("$%.2f", monto - total));
                } catch (NumberFormatException ex) {
                    lblCambio.setText("$0.00");
                }
            }
        });

        int option = JOptionPane.showConfirmDialog(
            this, panel, "Cobrar Mesa", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
        );

        if (option == JOptionPane.OK_OPTION) {
            try {
                double montoRecibido = Double.parseDouble(txtMonto.getText());
                String metodo = (String) comboMetodo.getSelectedItem();
                
                if (metodo.equals("Efectivo") && montoRecibido < total) {
                    JOptionPane.showMessageDialog(this, "Monto insuficiente", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                registrarVenta(mesaSeleccionadaId, total, metodo);
                cerrarMesa(); // Elimina la mesa después de cobrar
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Monto inválido", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private double calcularTotalMesa() {
        double total = 0;
        for (int i = 0; i < modeloTablaPedidos.getRowCount(); i++) {
            total += (double) modeloTablaPedidos.getValueAt(i, 4);
        }
        return total;
    }

    private void registrarVenta(int mesaId, double total, String metodoPago) {
        try (Connection con = ConexionDB.conectar()) {
            con.setAutoCommit(false);
            
            // Registrar venta
            PreparedStatement psVenta = con.prepareStatement(
                "INSERT INTO ventas (fecha, total, metodo_pago, usuario_id, turno_id) " +
                "VALUES (NOW(), ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS
            );
            psVenta.setDouble(1, total);
            psVenta.setString(2, metodoPago);
            psVenta.setInt(3, usuarioLogueadoId);
            psVenta.setInt(4, turnoActualId);
            psVenta.executeUpdate();
            
            ResultSet rs = psVenta.getGeneratedKeys();
            int ventaId = rs.next() ? rs.getInt(1) : -1;
            
            if (ventaId != -1) {
                // Registrar detalles de venta
                for (int i = 0; i < modeloTablaPedidos.getRowCount(); i++) {
                    int productoId = obtenerIdProducto((String) modeloTablaPedidos.getValueAt(i, 1));
                    int cantidad = (int) modeloTablaPedidos.getValueAt(i, 2);
                    double subtotal = (double) modeloTablaPedidos.getValueAt(i, 4);
                    
                    PreparedStatement psDetalle = con.prepareStatement(
                        "INSERT INTO detalle_venta (venta_id, producto_id, cantidad, subtotal) " +
                        "VALUES (?, ?, ?, ?)"
                    );
                    psDetalle.setInt(1, ventaId);
                    psDetalle.setInt(2, productoId);
                    psDetalle.setInt(3, cantidad);
                    psDetalle.setDouble(4, subtotal);
                    psDetalle.executeUpdate();
                }
                
                con.commit();
                JOptionPane.showMessageDialog(this, "Venta registrada correctamente", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al registrar venta: " + e.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void limpiarPedidos() {
        modeloTablaPedidos.setRowCount(0);
    }
}