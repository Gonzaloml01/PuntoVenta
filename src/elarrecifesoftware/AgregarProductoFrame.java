package elarrecifesoftware;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class AgregarProductoFrame extends JFrame {
    private JTextField txtNombre, txtPrecio; // txtStock eliminado
    private JButton btnAgregar;
    private JTextArea areaLog;

    private JComboBox<String> comboCategorias;
    private JComboBox<String> comboSubcategorias;

    public AgregarProductoFrame() {
        setTitle("Agregar Producto - El Arrecife");
        setSize(500, 350); // Ajustar tamaño ya que hay menos campos
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new GridLayout(7, 2, 10, 10)); // Ajustar número de filas

        txtNombre = new JTextField();
        txtPrecio = new JTextField();
        // txtStock eliminado
        btnAgregar = new JButton("Agregar Producto");
        areaLog = new JTextArea();
        areaLog.setEditable(false);
        comboCategorias = new JComboBox<>();
        comboSubcategorias = new JComboBox<>();

        add(new JLabel("Nombre del producto:"));
        add(txtNombre);
        add(new JLabel("Precio:"));
        add(txtPrecio);
        // add(new JLabel("Stock inicial:")); // Eliminado
        // add(txtStock); // Eliminado
        add(new JLabel("Categoría:"));
        add(comboCategorias);
        add(new JLabel("Subcategoría (opcional):"));
        add(comboSubcategorias);
        add(btnAgregar);
        add(new JLabel()); // espacio vacío
        add(new JScrollPane(areaLog));

        btnAgregar.addActionListener(e -> agregarProducto());

        cargarCategorias();
        comboCategorias.addActionListener(e -> cargarSubcategorias());
    }

    private void cargarCategorias() {
        comboCategorias.removeAllItems();
        comboCategorias.addItem("Seleccione Categoría");
        Connection con = ConexionDB.conectar();
        if (con == null) {
            areaLog.append("❌ Error al conectar para cargar categorías.\n");
            return;
        }
        try (Statement stmt = con.createStatement(); ResultSet rs = stmt.executeQuery("SELECT nombre FROM categorias")) {
            while (rs.next()) {
                comboCategorias.addItem(rs.getString("nombre"));
            }
        } catch (SQLException e) {
            areaLog.append("❌ Error al cargar categorías: " + e.getMessage() + "\n");
        } finally {
            try { if (con != null) con.close(); } catch (SQLException ex) { /* ignorar */ }
        }
    }

    private void cargarSubcategorias() {
        comboSubcategorias.removeAllItems();
        comboSubcategorias.addItem("Ninguna / Seleccione Subcategoría");
        String categoriaSeleccionada = (String) comboCategorias.getSelectedItem();
        if (categoriaSeleccionada == null || categoriaSeleccionada.equals("Seleccione Categoría")) {
            return;
        }

        Connection con = ConexionDB.conectar();
        if (con == null) {
            areaLog.append("❌ Error al conectar para cargar subcategorías.\n");
            return;
        }
        try {
            PreparedStatement ps = con.prepareStatement("SELECT s.nombre FROM subcategorias s JOIN categorias c ON s.categoria_id = c.id WHERE c.nombre = ?");
            ps.setString(1, categoriaSeleccionada);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                comboSubcategorias.addItem(rs.getString("nombre"));
            }
        } catch (SQLException e) {
            areaLog.append("❌ Error al cargar subcategorías: " + e.getMessage() + "\n");
        } finally {
            try { if (con != null) con.close(); } catch (SQLException ex) { /* ignorar */ }
        }
    }

    private void agregarProducto() {
        String nombre = txtNombre.getText();
        String precioStr = txtPrecio.getText();
        // String stockStr = txtStock.getText(); // Eliminado
        String categoriaNombre = (String) comboCategorias.getSelectedItem();
        String subcategoriaNombre = (String) comboSubcategorias.getSelectedItem();

        if (nombre.isEmpty() || precioStr.isEmpty() || categoriaNombre.equals("Seleccione Categoría")) { // stockStr eliminado de la validación
            areaLog.append("❗ Todos los campos obligatorios deben ser llenados.\n");
            return;
        }

        double precio;
        // int stock; // Eliminado
        Integer categoriaId = null;
        Integer subcategoriaId = null;

        try {
            precio = Double.parseDouble(precioStr);
            // stock = Integer.parseInt(stockStr); // Eliminado
        } catch (NumberFormatException ex) {
            areaLog.append("Error: precio inválido (debe ser un número).\n"); // Mensaje de error ajustado
            return;
        }

        Connection con = ConexionDB.conectar();
        if (con == null) {
            areaLog.append("❌ No se pudo conectar a la base de datos.\n");
            return;
        }

        try {
            con.setAutoCommit(false);

            // Obtener ID de la categoría
            PreparedStatement psCat = con.prepareStatement("SELECT id FROM categorias WHERE nombre = ?");
            psCat.setString(1, categoriaNombre);
            ResultSet rsCat = psCat.executeQuery();
            if (rsCat.next()) {
                categoriaId = rsCat.getInt("id");
            } else {
                areaLog.append("❗ Categoría no encontrada: " + categoriaNombre + "\n");
                con.rollback();
                return;
            }

            // Obtener ID de la subcategoría si está seleccionada
            if (subcategoriaNombre != null && !subcategoriaNombre.equals("Ninguna / Seleccione Subcategoría")) {
                PreparedStatement psSubcat = con.prepareStatement("SELECT id FROM subcategorias WHERE nombre = ? AND categoria_id = ?");
                psSubcat.setString(1, subcategoriaNombre);
                psSubcat.setInt(2, categoriaId);
                ResultSet rsSubcat = psSubcat.executeQuery();
                if (rsSubcat.next()) {
                    subcategoriaId = rsSubcat.getInt("id");
                } else {
                    areaLog.append("❗ Subcategoría no encontrada o no pertenece a la categoría seleccionada: " + subcategoriaNombre + "\n");
                    con.rollback();
                    return;
                }
            }

            // Consulta SQL ajustada: sin 'stock'
            String sql = "INSERT INTO productos (nombre, precio, categoria_id, subcategoria_id) VALUES (?, ?, ?, ?)";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, nombre);
            ps.setDouble(2, precio);
            // ps.setInt(3, stock); // Eliminado
            ps.setObject(3, categoriaId, java.sql.Types.INTEGER);
            ps.setObject(4, subcategoriaId, java.sql.Types.INTEGER);
            ps.executeUpdate();

            con.commit();
            areaLog.append("Producto agregado correctamente: " + nombre + "\n");
            // Limpiar campos
            txtNombre.setText("");
            txtPrecio.setText("");
            // txtStock.setText(""); // Eliminado
            comboCategorias.setSelectedIndex(0);
            comboSubcategorias.removeAllItems();
            comboSubcategorias.addItem("Ninguna / Seleccione Subcategoría");

        } catch (SQLException ex) {
            try { if (con != null) con.rollback(); } catch (SQLException e) { /* ignore */ }
            areaLog.append("Error al agregar producto: " + ex.getMessage() + "\n");
        } finally {
            try { if (con != null) con.close(); } catch (SQLException ex) { /* ignorar */ }
        }
    }
    // main method for testing, can be removed if not needed
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AgregarProductoFrame().setVisible(true));
    }
}