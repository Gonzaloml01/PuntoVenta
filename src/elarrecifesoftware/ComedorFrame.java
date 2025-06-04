package elarrecifesoftware;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class ComedorFrame extends JFrame {
    private JList<String> listaMesas;
    private DefaultListModel<String> mesasModel;
    private JTable tablaPedidos;
    private DefaultTableModel modeloTablaPedidos;
     private JButton btnAbrirMesa, btnRenombrarMesa, btnCerrarMesa, btnCobrarMesa,
            btnAgregarProducto, btnAgregarBebida, btnEliminarProducto;
    private int usuarioLogueadoId, turnoActualId, mesaSeleccionadaId = -1;
    private java.util.List<Integer> mesaIds = new java.util.ArrayList<>();

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
         JPanel panelBotonesMesas = new JPanel(new GridLayout(1, 4));
        btnAbrirMesa = new JButton("Abrir Mesa");
        btnRenombrarMesa = new JButton("Renombrar Mesa");
        btnCerrarMesa = new JButton("Cerrar/Eliminar Mesa");
        btnCobrarMesa = new JButton("Cobrar Mesa");
        btnAbrirMesa.addActionListener(e -> abrirNuevaMesa());
        btnRenombrarMesa.addActionListener(e -> renombrarMesa());
        btnCerrarMesa.addActionListener(e -> cerrarMesa());
        btnCobrarMesa.addActionListener(e -> cobrarMesa());
        panelBotonesMesas.add(btnAbrirMesa);
        panelBotonesMesas.add(btnRenombrarMesa);
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
        btnAgregarBebida = new JButton("Agregar Bebida");
        btnEliminarProducto = new JButton("Eliminar Producto");
        btnAgregarProducto.addActionListener(e -> agregarProductoAMesa(false));
        btnAgregarBebida.addActionListener(e -> agregarProductoAMesa(true));
        btnEliminarProducto.addActionListener(e -> eliminarProductoDePedido());
        panelBotonesPedidos.add(btnAgregarProducto);
        panelBotonesPedidos.add(btnAgregarBebida);
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
            int index = listaMesas.getSelectedIndex();
            if (index != -1 && index < mesaIds.size()) {
                mesaSeleccionadaId = mesaIds.get(index);
                cargarPedidosMesa(mesaSeleccionadaId);
            } else {
                mesaSeleccionadaId = -1;
            }
            actualizarEstadoBotones();
        }
    }

    private void actualizarEstadoBotones() {
              boolean mesaSeleccionada = mesaSeleccionadaId != -1;
        btnRenombrarMesa.setEnabled(mesaSeleccionada);
        btnAgregarProducto.setEnabled(mesaSeleccionada);
        btnAgregarBebida.setEnabled(mesaSeleccionada);
        btnEliminarProducto.setEnabled(mesaSeleccionada);
        btnCerrarMesa.setEnabled(mesaSeleccionada);
        btnCobrarMesa.setEnabled(mesaSeleccionada);
    }

    private void cargarMesas() {
          mesasModel.clear();
        mesaIds.clear();
        try (Connection con = ConexionDB.conectar()) {
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT id, numero, estado FROM mesas ORDER BY numero");
            while (rs.next()) {
                mesaIds.add(rs.getInt("id"));
                String info = "Mesa " + rs.getInt("numero") + " - " + rs.getString("estado");
                mesasModel.addElement(info);
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

             if (psInsert.getGeneratedKeys().next()) {
                JOptionPane.showMessageDialog(this, "Mesa creada", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                cargarMesas();
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Ingrese un número válido", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al crear mesa: " + e.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void renombrarMesa() {
        if (mesaSeleccionadaId == -1) return;

        String input = JOptionPane.showInputDialog(this, "Nuevo número de mesa:", "Renombrar Mesa", JOptionPane.QUESTION_MESSAGE);
        if (input == null || input.trim().isEmpty()) return;

        try (Connection con = ConexionDB.conectar()) {
            int numero = Integer.parseInt(input);

            PreparedStatement ps = con.prepareStatement("UPDATE mesas SET numero = ? WHERE id = ?");
            ps.setInt(1, numero);
            ps.setInt(2, mesaSeleccionadaId);
            if (ps.executeUpdate() > 0) {
                cargarMesas();
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Ingrese un número válido", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al renombrar mesa: " + e.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
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
        new SelectorProductoDialog(mesaSeleccionadaId).setVisible(true);
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

    /**
     * Diálogo para seleccionar productos mediante botones de categorías y
     * subcategorías. Permite elegir la cantidad a agregar y utiliza el método
     * existente para registrar el pedido en la mesa.
     */
    /**
     * Selector de productos que muestra categorías de la base de datos.
     * Si {@code soloBebidas} es verdadero filtra aquellas donde
     * la columna "tipo" de la tabla categorías sea "Bebida".
     */
    private class SelectorProductoDialog extends JDialog {
        private final JPanel panelCategorias = new JPanel(new GridLayout(0, 4, 10, 10));
        private final JPanel panelSubcategorias = new JPanel(new GridLayout(0, 4, 10, 10));
        private final JPanel panelProductos = new JPanel(new GridLayout(0, 4, 10, 10));
        private final int mesaId;
        private final boolean soloBebidas;

        SelectorProductoDialog(int mesaId, boolean soloBebidas) {
            super(ComedorFrame.this, "Seleccionar Producto", true);
            this.mesaId = mesaId;
            this.soloBebidas = soloBebidas;

            setLayout(new BorderLayout(5, 5));
            add(new JScrollPane(panelCategorias), BorderLayout.NORTH);
            add(new JScrollPane(panelSubcategorias), BorderLayout.CENTER);
            add(new JScrollPane(panelProductos), BorderLayout.SOUTH);

            cargarCategorias();

            setSize(700, 500);
            setLocationRelativeTo(ComedorFrame.this);
        }

        private JButton crearBoton(String texto) {
            JButton btn = new JButton("<html><center" + ">" + texto + "</center></html>");
            btn.setFont(btn.getFont().deriveFont(Font.BOLD, 16f));
            btn.setPreferredSize(new Dimension(150, 60));
            return btn;
        }

        private void cargarCategorias() {
            panelCategorias.removeAll();
            String sql = "SELECT id, nombre FROM categorias" +
                         (soloBebidas ? " WHERE tipo = 'Bebida'" : "") +
                         " ORDER BY nombre";
            try (Connection con = ConexionDB.conectar();
                 Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String nombre = rs.getString("nombre");
                    JButton btn = crearBoton(nombre);
                    btn.addActionListener(e -> cargarSubcategorias(id));
                    panelCategorias.add(btn);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error al cargar categorías: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
            panelCategorias.revalidate();
            panelCategorias.repaint();
        }

        private void cargarSubcategorias(int categoriaId) {
            panelSubcategorias.removeAll();
            boolean tieneSub = false;
            try (Connection con = ConexionDB.conectar();
                 PreparedStatement ps = con.prepareStatement("SELECT id, nombre FROM subcategorias WHERE categoria_id = ? ORDER BY nombre")) {
                ps.setInt(1, categoriaId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    tieneSub = true;
                    int id = rs.getInt("id");
                    String nombre = rs.getString("nombre");
                    JButton btn = crearBoton(nombre);
                    btn.addActionListener(e -> cargarProductos(categoriaId, id));
                    panelSubcategorias.add(btn);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error al cargar subcategorías: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
            panelSubcategorias.revalidate();
            panelSubcategorias.repaint();
            if (!tieneSub) {
                cargarProductos(categoriaId, -1);
            } else {
                panelProductos.removeAll();
                panelProductos.revalidate();
                panelProductos.repaint();
            }
        }

        private void cargarProductos(int categoriaId, int subcategoriaId) {
            panelProductos.removeAll();
            String sql = "SELECT id, nombre FROM productos WHERE categoria_id = ?" +
                         (subcategoriaId == -1 ? "" : " AND subcategoria_id = ?") +
                         " ORDER BY nombre";
            try (Connection con = ConexionDB.conectar();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, categoriaId);
                if (subcategoriaId != -1) ps.setInt(2, subcategoriaId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String nombre = rs.getString("nombre");
                    JButton btn = crearBoton(nombre);
                    btn.addActionListener(e -> seleccionarProducto(id));
                    panelProductos.add(btn);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error al cargar productos: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
            panelProductos.revalidate();
            panelProductos.repaint();
        }

        private void seleccionarProducto(int productoId) {
            String cantStr = JOptionPane.showInputDialog(this, "Cantidad:", "1");
            if (cantStr == null) return;
            try {
                int cantidad = Integer.parseInt(cantStr);
                if (cantidad > 0) {
                    agregarProductoAPedidoMesaDB(mesaId, productoId, cantidad);
                    dispose();
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Cantidad inválida", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}