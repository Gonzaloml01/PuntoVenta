package elarrecifesoftware;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.util.Vector;

public class GestionProductosFrame extends JFrame {

    private JTable tablaProductos;
    private DefaultTableModel modeloTablaProductos;

    private JTextField txtProductoId; // Para mostrar el ID del producto seleccionado (no editable)
    private JTextField txtNombre;
    private JTextField txtPrecio;
    private JTextField txtStock;
    private JComboBox<String> comboCategorias;
    private JComboBox<String> comboSubcategorias;

    private JButton btnNuevo;
    private JButton btnGuardar;
    private JButton btnEliminar;
    private JButton btnLimpiarCampos;

    // Para gestionar categorías y subcategorías
    private JButton btnGestionarCategorias;
    private JButton btnGestionarSubcategorias;

    public GestionProductosFrame() {
        setTitle("El Arrecife - Gestión de Productos y Menú");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE); // Cierra solo esta ventana
        setLayout(new BorderLayout(10, 10));

        // --- Panel de Formulario de Edición/Creación ---
        JPanel panelFormulario = new JPanel(new GridBagLayout());
        panelFormulario.setBorder(BorderFactory.createTitledBorder("Detalles del Producto"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Fila 0: ID del Producto (solo lectura)
        gbc.gridx = 0; gbc.gridy = 0; panelFormulario.add(new JLabel("ID Producto:"), gbc);
        gbc.gridx = 1; txtProductoId = new JTextField(15); txtProductoId.setEditable(false); panelFormulario.add(txtProductoId, gbc);

        // Fila 1: Nombre
        gbc.gridx = 0; gbc.gridy = 1; panelFormulario.add(new JLabel("Nombre:"), gbc);
        gbc.gridx = 1; txtNombre = new JTextField(20); panelFormulario.add(txtNombre, gbc);

        // Fila 2: Precio
        gbc.gridx = 0; gbc.gridy = 2; panelFormulario.add(new JLabel("Precio:"), gbc);
        gbc.gridx = 1; txtPrecio = new JTextField(10); panelFormulario.add(txtPrecio, gbc);

        // Fila 3: Stock
        gbc.gridx = 0; gbc.gridy = 3; panelFormulario.add(new JLabel("Stock:"), gbc);
        gbc.gridx = 1; txtStock = new JTextField(10); panelFormulario.add(txtStock, gbc);

        // Fila 4: Categoría
        gbc.gridx = 0; gbc.gridy = 4; panelFormulario.add(new JLabel("Categoría:"), gbc);
        gbc.gridx = 1; comboCategorias = new JComboBox<>(); panelFormulario.add(comboCategorias, gbc);

        // Fila 5: Subcategoría
        gbc.gridx = 0; gbc.gridy = 5; panelFormulario.add(new JLabel("Subcategoría:"), gbc);
        gbc.gridx = 1; comboSubcategorias = new JComboBox<>(); panelFormulario.add(comboSubcategorias, gbc);

        // Botones de acción del formulario
        JPanel panelBotonesFormulario = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btnNuevo = new JButton("➕ Nuevo Producto");
        btnGuardar = new JButton("💾 Guardar Cambios");
        btnEliminar = new JButton("🗑️ Eliminar Producto");
        btnLimpiarCampos = new JButton("🧹 Limpiar Campos");

        panelBotonesFormulario.add(btnNuevo);
        panelBotonesFormulario.add(btnGuardar);
        panelBotonesFormulario.add(btnEliminar);
        panelBotonesFormulario.add(btnLimpiarCampos);

        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2; // Ocupa 2 columnas
        panelFormulario.add(panelBotonesFormulario, gbc);

        add(panelFormulario, BorderLayout.NORTH);

        // --- Panel de Tabla de Productos ---
        JPanel panelTabla = new JPanel(new BorderLayout());
        panelTabla.setBorder(BorderFactory.createTitledBorder("Inventario de Productos"));

        String[] columnas = {"ID", "Nombre", "Precio", "Stock", "Categoría", "Subcategoría"};
        modeloTablaProductos = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Hacer que las celdas no sean editables
            }
        };
        tablaProductos = new JTable(modeloTablaProductos);
        tablaProductos.setFillsViewportHeight(true);
        tablaProductos.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                cargarProductoSeleccionado();
            }
        });
        panelTabla.add(new JScrollPane(tablaProductos), BorderLayout.CENTER);

        add(panelTabla, BorderLayout.CENTER);

        // --- Panel Inferior: Gestión de Categorías/Subcategorías ---
        JPanel panelGestionExtra = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnGestionarCategorias = new JButton("📂 Gestionar Categorías");
        btnGestionarSubcategorias = new JButton("📁 Gestionar Subcategorías");
        panelGestionExtra.add(btnGestionarCategorias);
        panelGestionExtra.add(btnGestionarSubcategorias);
        add(panelGestionExtra, BorderLayout.SOUTH);

        // --- Listeners ---
        btnNuevo.addActionListener(e -> nuevoProducto());
        btnGuardar.addActionListener(e -> guardarProducto());
        btnEliminar.addActionListener(e -> eliminarProducto());
        btnLimpiarCampos.addActionListener(e -> limpiarCampos());

        btnGestionarCategorias.addActionListener(e -> new GestionCategoriasFrame().setVisible(true));
        btnGestionarSubcategorias.addActionListener(e -> new GestionSubcategoriasFrame().setVisible(true));

        comboCategorias.addActionListener(e -> cargarSubcategorias()); // Cuando cambia la categoría, actualiza subcategorías

        // Cargar datos iniciales
        cargarProductos();
        cargarCategorias();
        limpiarCampos(); // Iniciar con campos limpios
    }

    private void cargarProductos() {
        modeloTablaProductos.setRowCount(0); // Limpiar tabla
        Connection con = ConexionDB.conectar();
        if (con == null) return;

        try {
            PreparedStatement ps = con.prepareStatement(
                "SELECT p.id, p.nombre, p.precio, p.stock, c.nombre AS categoria_nombre, s.nombre AS subcategoria_nombre " +
                "FROM productos p " +
                "LEFT JOIN categorias c ON p.categoria_id = c.id " +
                "LEFT JOIN subcategorias s ON p.subcategoria_id = s.id " +
                "ORDER BY p.nombre"
            );
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                modeloTablaProductos.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("nombre"),
                    rs.getDouble("precio"),
                    rs.getInt("stock"),
                    rs.getString("categoria_nombre"),
                    rs.getString("subcategoria_nombre")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar productos: " + e.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } finally {
            try { if (con != null) con.close(); } catch (SQLException ex) { /* ignorar */ }
        }
    }

    private void cargarCategorias() {
        comboCategorias.removeAllItems();
        comboCategorias.addItem("Selecciona Categoría..."); // Opción por defecto
        Connection con = ConexionDB.conectar();
        if (con == null) return;

        try {
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT id, nombre FROM categorias ORDER BY nombre");
            while (rs.next()) {
                comboCategorias.addItem(rs.getString("nombre"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar categorías: " + e.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } finally {
            try { if (con != null) con.close(); } catch (SQLException ex) { /* ignorar */ }
        }
    }

    private void cargarSubcategorias() {
        comboSubcategorias.removeAllItems();
        comboSubcategorias.addItem("Selecciona Subcategoría..."); // Opción por defecto

        String categoriaSeleccionada = (String) comboCategorias.getSelectedItem();
        if (categoriaSeleccionada == null || categoriaSeleccionada.equals("Selecciona Categoría...")) {
            return;
        }

        Connection con = ConexionDB.conectar();
        if (con == null) return;

        try {
            PreparedStatement ps = con.prepareStatement(
                "SELECT s.id, s.nombre FROM subcategorias s JOIN categorias c ON s.categoria_id = c.id WHERE c.nombre = ? ORDER BY s.nombre"
            );
            ps.setString(1, categoriaSeleccionada);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                comboSubcategorias.addItem(rs.getString("nombre"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar subcategorías: " + e.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } finally {
            try { if (con != null) con.close(); } catch (SQLException ex) { /* ignorar */ }
        }
    }

    private int getCategoriaId(String nombreCategoria) {
        if (nombreCategoria == null || nombreCategoria.equals("Selecciona Categoría...")) return -1;
        Connection con = ConexionDB.conectar();
        if (con == null) return -1;
        try {
            PreparedStatement ps = con.prepareStatement("SELECT id FROM categorias WHERE nombre = ?");
            ps.setString(1, nombreCategoria);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (SQLException e) { e.printStackTrace(); } finally { try { con.close(); } catch (SQLException ex) { /* ignorar */ } }
        return -1;
    }

    private int getSubcategoriaId(String nombreSubcategoria) {
        if (nombreSubcategoria == null || nombreSubcategoria.equals("Selecciona Subcategoría...")) return -1;
        Connection con = ConexionDB.conectar();
        if (con == null) return -1;
        try {
            PreparedStatement ps = con.prepareStatement("SELECT id FROM subcategorias WHERE nombre = ?");
            ps.setString(1, nombreSubcategoria);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (SQLException e) { e.printStackTrace(); } finally { try { con.close(); } catch (SQLException ex) { /* ignorar */ } }
        return -1;
    }

    private void limpiarCampos() {
        txtProductoId.setText("");
        txtNombre.setText("");
        txtPrecio.setText("");
        txtStock.setText("");
        comboCategorias.setSelectedIndex(0); // Seleccionar "Selecciona Categoría..."
        comboSubcategorias.removeAllItems(); // Limpiar subcategorías
        comboSubcategorias.addItem("Selecciona Subcategoría..."); // Añadir opción por defecto
        btnGuardar.setText("💾 Guardar Nuevo Producto"); // Cambiar texto del botón para nuevo
        tablaProductos.clearSelection(); // Deseleccionar cualquier fila
    }

    private void cargarProductoSeleccionado() {
        int selectedRow = tablaProductos.getSelectedRow();
        if (selectedRow == -1) {
            limpiarCampos();
            return;
        }

        txtProductoId.setText(modeloTablaProductos.getValueAt(selectedRow, 0).toString());
        txtNombre.setText(modeloTablaProductos.getValueAt(selectedRow, 1).toString());
        txtPrecio.setText(modeloTablaProductos.getValueAt(selectedRow, 2).toString());
        txtStock.setText(modeloTablaProductos.getValueAt(selectedRow, 3).toString());

        String categoriaNombre = (String) modeloTablaProductos.getValueAt(selectedRow, 4);
        if (categoriaNombre != null) {
            comboCategorias.setSelectedItem(categoriaNombre);
            // La llamada a cargarSubcategorias() ya se dispara por el listener del comboCategorias
            // Damos tiempo a que se carguen antes de seleccionar la subcategoría
            SwingUtilities.invokeLater(() -> {
                String subcategoriaNombre = (String) modeloTablaProductos.getValueAt(selectedRow, 5);
                if (subcategoriaNombre != null) {
                    comboSubcategorias.setSelectedItem(subcategoriaNombre);
                } else {
                    comboSubcategorias.setSelectedIndex(0);
                }
            });
        } else {
            comboCategorias.setSelectedIndex(0);
        }
        btnGuardar.setText("💾 Actualizar Producto"); // Cambiar texto del botón para actualizar
    }

    private void nuevoProducto() {
        limpiarCampos();
    }

    private void guardarProducto() {
        String idStr = txtProductoId.getText();
        String nombre = txtNombre.getText().trim();
        String precioStr = txtPrecio.getText().trim();
        String stockStr = txtStock.getText().trim();
        String categoriaNombre = (String) comboCategorias.getSelectedItem();
        String subcategoriaNombre = (String) comboSubcategorias.getSelectedItem();

        if (nombre.isEmpty() || precioStr.isEmpty() || stockStr.isEmpty() || categoriaNombre.equals("Selecciona Categoría...")) {
            JOptionPane.showMessageDialog(this, "Por favor, completa todos los campos obligatorios (Nombre, Precio, Stock, Categoría).", "Campos Incompletos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        double precio;
        int stock;
        try {
            precio = Double.parseDouble(precioStr);
            stock = Integer.parseInt(stockStr);
            if (precio <= 0 || stock < 0) {
                JOptionPane.showMessageDialog(this, "Precio debe ser mayor a 0 y Stock no puede ser negativo.", "Valores Inválidos", JOptionPane.WARNING_MESSAGE);
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Por favor, ingresa valores numéricos válidos para Precio y Stock.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int categoriaId = getCategoriaId(categoriaNombre);
        int subcategoriaId = getSubcategoriaId(subcategoriaNombre); // Puede ser -1 si no hay subcategoría seleccionada o si es la opción por defecto

        Connection con = ConexionDB.conectar();
        if (con == null) return;

        try {
            if (idStr.isEmpty()) { // Nuevo producto
                PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO productos (nombre, precio, stock, categoria_id, subcategoria_id) VALUES (?, ?, ?, ?, ?)"
                );
                ps.setString(1, nombre);
                ps.setDouble(2, precio);
                ps.setInt(3, stock);
                if (categoriaId != -1) ps.setInt(4, categoriaId); else ps.setNull(4, java.sql.Types.INTEGER);
                if (subcategoriaId != -1) ps.setInt(5, subcategoriaId); else ps.setNull(5, java.sql.Types.INTEGER);
                ps.executeUpdate();
                JOptionPane.showMessageDialog(this, "Producto agregado correctamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } else { // Actualizar producto existente
                int productId = Integer.parseInt(idStr);
                PreparedStatement ps = con.prepareStatement(
                    "UPDATE productos SET nombre = ?, precio = ?, stock = ?, categoria_id = ?, subcategoria_id = ? WHERE id = ?"
                );
                ps.setString(1, nombre);
                ps.setDouble(2, precio);
                ps.setInt(3, stock);
                if (categoriaId != -1) ps.setInt(4, categoriaId); else ps.setNull(4, java.sql.Types.INTEGER);
                if (subcategoriaId != -1) ps.setInt(5, subcategoriaId); else ps.setNull(5, java.sql.Types.INTEGER);
                ps.setInt(6, productId);
                ps.executeUpdate();
                JOptionPane.showMessageDialog(this, "Producto actualizado correctamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            }
            cargarProductos(); // Recargar la tabla
            limpiarCampos(); // Limpiar campos después de guardar
        } catch (SQLException e) {
            if (e.getMessage().contains("Duplicate entry") && e.getMessage().contains("for key 'productos.nombre'")) {
                JOptionPane.showMessageDialog(this, "Ya existe un producto con este nombre.", "Error de Duplicado", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Error al guardar producto: " + e.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
            }
            e.printStackTrace();
        } finally {
            try { if (con != null) con.close(); } catch (SQLException ex) { /* ignorar */ }
        }
    }

    private void eliminarProducto() {
        String idStr = txtProductoId.getText();
        if (idStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Selecciona un producto para eliminar.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "¿Estás seguro de que quieres eliminar el producto '" + txtNombre.getText() + "'?",
                "Confirmar Eliminación", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            int productId = Integer.parseInt(idStr);
            Connection con = ConexionDB.conectar();
            if (con == null) return;

            try {
                // Antes de eliminar el producto, verifica si está referenciado en detalle_venta o pedidos
                PreparedStatement psCheckVentas = con.prepareStatement("SELECT 1 FROM detalle_venta WHERE producto_id = ? LIMIT 1");
                psCheckVentas.setInt(1, productId);
                ResultSet rsVentas = psCheckVentas.executeQuery();
                if (rsVentas.next()) {
                    JOptionPane.showMessageDialog(this, "No se puede eliminar el producto porque está referenciado en ventas existentes.", "Error de Dependencia", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                PreparedStatement psCheckPedidos = con.prepareStatement("SELECT 1 FROM pedidos WHERE producto_id = ? LIMIT 1");
                psCheckPedidos.setInt(1, productId);
                ResultSet rsPedidos = psCheckPedidos.executeQuery();
                if (rsPedidos.next()) {
                    JOptionPane.showMessageDialog(this, "No se puede eliminar el producto porque está actualmente en pedidos de mesas abiertas.", "Error de Dependencia", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Si no hay dependencias, procede con la eliminación
                PreparedStatement psDelete = con.prepareStatement("DELETE FROM productos WHERE id = ?");
                psDelete.setInt(1, productId);
                int rowsAffected = psDelete.executeUpdate();

                if (rowsAffected > 0) {
                    JOptionPane.showMessageDialog(this, "Producto eliminado correctamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                    cargarProductos();
                    limpiarCampos();
                } else {
                    JOptionPane.showMessageDialog(this, "No se pudo eliminar el producto.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error al eliminar producto: " + e.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            } finally {
                try { if (con != null) con.close(); } catch (SQLException ex) { /* ignorar */ }
            }
        }
    }

    // El main solo para pruebas individuales de esta ventana
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GestionProductosFrame().setVisible(true));
    }
}