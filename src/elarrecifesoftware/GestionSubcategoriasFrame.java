package elarrecifesoftware;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class GestionSubcategoriasFrame extends JDialog {

    private JTable tablaSubcategorias;
    private DefaultTableModel modeloTablaSubcategorias;
    private JTextField txtSubcategoriaId;
    private JTextField txtNombreSubcategoria;
    private JComboBox<String> comboCategorias;
    private JButton btnAgregarSubcategoria, btnActualizarSubcategoria, btnEliminarSubcategoria;

    public GestionSubcategoriasFrame() {
        setTitle("Gestión de Subcategorías");
        setSize(600, 450);
        setLocationRelativeTo(null);
        setModal(true);
        setLayout(new BorderLayout(10, 10));

        // Formulario
        JPanel panelForm = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        panelForm.add(new JLabel("ID:"), gbc);
        gbc.gridx = 1;
        txtSubcategoriaId = new JTextField(5); txtSubcategoriaId.setEditable(false);
        panelForm.add(txtSubcategoriaId, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panelForm.add(new JLabel("Nombre Subcategoría:"), gbc);
        gbc.gridx = 1;
        txtNombreSubcategoria = new JTextField(20);
        panelForm.add(txtNombreSubcategoria, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panelForm.add(new JLabel("Categoría Padre:"), gbc);
        gbc.gridx = 1;
        comboCategorias = new JComboBox<>();
        panelForm.add(comboCategorias, gbc);

        // Botones
        JPanel panelBotones = new JPanel();
        btnAgregarSubcategoria = new JButton("➕ Agregar");
        btnActualizarSubcategoria = new JButton("💾 Actualizar");
        btnEliminarSubcategoria = new JButton("🗑️ Eliminar");
        panelBotones.add(btnAgregarSubcategoria);
        panelBotones.add(btnActualizarSubcategoria);
        panelBotones.add(btnEliminarSubcategoria);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        panelForm.add(panelBotones, gbc);
        add(panelForm, BorderLayout.NORTH);

        // Tabla
        String[] columnas = {"ID", "Nombre Subcategoría", "Categoría"};
        modeloTablaSubcategorias = new DefaultTableModel(columnas, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tablaSubcategorias = new JTable(modeloTablaSubcategorias);
        tablaSubcategorias.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaSubcategorias.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) cargarSubcategoriaSeleccionada();
        });
        add(new JScrollPane(tablaSubcategorias), BorderLayout.CENTER);

        // Listeners
        btnAgregarSubcategoria.addActionListener(e -> agregarSubcategoria());
        btnActualizarSubcategoria.addActionListener(e -> actualizarSubcategoria());
        btnEliminarSubcategoria.addActionListener(e -> eliminarSubcategoria());

        cargarCategoriasCombo();
        cargarSubcategorias();
        limpiarCampos();
    }

    private void cargarCategoriasCombo() {
        comboCategorias.removeAllItems();
        comboCategorias.addItem("Selecciona Categoría...");
        try (Connection con = ConexionDB.conectar();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT nombre FROM categorias ORDER BY nombre")) {
            while (rs.next()) comboCategorias.addItem(rs.getString("nombre"));
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar categorías: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private int getCategoriaId(String nombreCategoria) {
        if (nombreCategoria == null || nombreCategoria.equals("Selecciona Categoría...")) return -1;
        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement("SELECT id FROM categorias WHERE nombre = ?")) {
            ps.setString(1, nombreCategoria);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt("id") : -1;
        } catch (SQLException e) {
            return -1;
        }
    }

    private void cargarSubcategorias() {
        modeloTablaSubcategorias.setRowCount(0);
        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT s.id, s.nombre, c.nombre AS categoria " +
                     "FROM subcategorias s JOIN categorias c ON s.categoria_id = c.id ORDER BY s.nombre");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                modeloTablaSubcategorias.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        rs.getString("categoria")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar subcategorías: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cargarSubcategoriaSeleccionada() {
        int fila = tablaSubcategorias.getSelectedRow();
        if (fila != -1) {
            txtSubcategoriaId.setText(modeloTablaSubcategorias.getValueAt(fila, 0).toString());
            txtNombreSubcategoria.setText(modeloTablaSubcategorias.getValueAt(fila, 1).toString());
            comboCategorias.setSelectedItem(modeloTablaSubcategorias.getValueAt(fila, 2).toString());
        }
    }

    private void limpiarCampos() {
        txtSubcategoriaId.setText("");
        txtNombreSubcategoria.setText("");
        comboCategorias.setSelectedIndex(0);
        tablaSubcategorias.clearSelection();
    }

    private void agregarSubcategoria() {
        String nombre = txtNombreSubcategoria.getText().trim();
        String categoria = (String) comboCategorias.getSelectedItem();

        if (nombre.isEmpty() || categoria.equals("Selecciona Categoría...")) {
            JOptionPane.showMessageDialog(this, "Faltan datos.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int categoriaId = getCategoriaId(categoria);
        if (categoriaId == -1) {
            JOptionPane.showMessageDialog(this, "Categoría no válida.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Connection con = ConexionDB.conectar()) {
            PreparedStatement check = con.prepareStatement("SELECT 1 FROM subcategorias WHERE nombre = ? AND categoria_id = ?");
            check.setString(1, nombre);
            check.setInt(2, categoriaId);
            if (check.executeQuery().next()) {
                JOptionPane.showMessageDialog(this, "Ya existe esa subcategoría en esta categoría.", "Duplicado", JOptionPane.ERROR_MESSAGE);
                return;
            }

            PreparedStatement ps = con.prepareStatement("INSERT INTO subcategorias (nombre, categoria_id) VALUES (?, ?)");
            ps.setString(1, nombre);
            ps.setInt(2, categoriaId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Subcategoría agregada.");
            cargarSubcategorias();
            limpiarCampos();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error SQL: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void actualizarSubcategoria() {
        String idStr = txtSubcategoriaId.getText();
        String nombre = txtNombreSubcategoria.getText().trim();
        String categoria = (String) comboCategorias.getSelectedItem();

        if (idStr.isEmpty() || nombre.isEmpty() || categoria.equals("Selecciona Categoría...")) {
            JOptionPane.showMessageDialog(this, "Faltan datos.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int categoriaId = getCategoriaId(categoria);
        if (categoriaId == -1) {
            JOptionPane.showMessageDialog(this, "Categoría no válida.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Connection con = ConexionDB.conectar()) {
            PreparedStatement ps = con.prepareStatement("UPDATE subcategorias SET nombre = ?, categoria_id = ? WHERE id = ?");
            ps.setString(1, nombre);
            ps.setInt(2, categoriaId);
            ps.setInt(3, Integer.parseInt(idStr));
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Subcategoría actualizada.");
            cargarSubcategorias();
            limpiarCampos();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error SQL: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void eliminarSubcategoria() {
        String idStr = txtSubcategoriaId.getText();
        if (idStr.isEmpty()) return;

        int confirm = JOptionPane.showConfirmDialog(this, "¿Eliminar subcategoría?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection con = ConexionDB.conectar()) {
            int id = Integer.parseInt(idStr);

            PreparedStatement psCheck = con.prepareStatement("SELECT 1 FROM productos WHERE subcategoria_id = ? LIMIT 1");
            psCheck.setInt(1, id);
            if (psCheck.executeQuery().next()) {
                JOptionPane.showMessageDialog(this, "Tiene productos relacionados. No se puede eliminar.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            PreparedStatement ps = con.prepareStatement("DELETE FROM subcategorias WHERE id = ?");
            ps.setInt(1, id);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Subcategoría eliminada.");
            cargarSubcategorias();
            limpiarCampos();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al eliminar: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
