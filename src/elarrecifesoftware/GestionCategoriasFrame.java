package elarrecifesoftware;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class GestionCategoriasFrame extends JDialog {

    private JTable tablaCategorias;
    private DefaultTableModel modeloTablaCategorias;
    private JTextField txtCategoriaId;
    private JTextField txtNombreCategoria;
    private JButton btnAgregarCategoria;
    private JButton btnActualizarCategoria;
    private JButton btnEliminarCategoria;

    public GestionCategoriasFrame() {
        setTitle("Gestión de Categorías");
        setSize(500, 400);
        setLocationRelativeTo(null);
        setModal(true);
        setLayout(new BorderLayout(10, 10));

        // Panel Formulario
        JPanel panelForm = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        panelForm.add(new JLabel("ID:"), gbc);
        gbc.gridx = 1;
        txtCategoriaId = new JTextField(5);
        txtCategoriaId.setEditable(false);
        panelForm.add(txtCategoriaId, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panelForm.add(new JLabel("Nombre Categoría:"), gbc);
        gbc.gridx = 1;
        txtNombreCategoria = new JTextField(20);
        panelForm.add(txtNombreCategoria, gbc);

        JPanel panelBotones = new JPanel(new FlowLayout());
        btnAgregarCategoria = new JButton("➕ Agregar");
        btnActualizarCategoria = new JButton("💾 Actualizar");
        btnEliminarCategoria = new JButton("🗑️ Eliminar");
        panelBotones.add(btnAgregarCategoria);
        panelBotones.add(btnActualizarCategoria);
        panelBotones.add(btnEliminarCategoria);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        panelForm.add(panelBotones, gbc);
        add(panelForm, BorderLayout.NORTH);

        // Tabla
        String[] columnas = {"ID", "Nombre"};
        modeloTablaCategorias = new DefaultTableModel(columnas, 0) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tablaCategorias = new JTable(modeloTablaCategorias);
        tablaCategorias.setFillsViewportHeight(true);
        tablaCategorias.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaCategorias.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) cargarCategoriaSeleccionada();
        });
        add(new JScrollPane(tablaCategorias), BorderLayout.CENTER);

        // Listeners
        btnAgregarCategoria.addActionListener(e -> agregarCategoria());
        btnActualizarCategoria.addActionListener(e -> actualizarCategoria());
        btnEliminarCategoria.addActionListener(e -> eliminarCategoria());

        cargarCategorias();
        limpiarCampos();
    }

    private void cargarCategorias() {
        modeloTablaCategorias.setRowCount(0);
        try (Connection con = ConexionDB.conectar();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, nombre FROM categorias ORDER BY nombre")) {
            while (rs.next()) {
                modeloTablaCategorias.addRow(new Object[]{rs.getInt("id"), rs.getString("nombre")});
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar categorías: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void limpiarCampos() {
        txtCategoriaId.setText("");
        txtNombreCategoria.setText("");
        tablaCategorias.clearSelection();
    }

    private void cargarCategoriaSeleccionada() {
        int fila = tablaCategorias.getSelectedRow();
        if (fila != -1) {
            txtCategoriaId.setText(modeloTablaCategorias.getValueAt(fila, 0).toString());
            txtNombreCategoria.setText(modeloTablaCategorias.getValueAt(fila, 1).toString());
        }
    }

    private void agregarCategoria() {
        String nombre = txtNombreCategoria.getText().trim();
        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nombre vacío.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try (Connection con = ConexionDB.conectar()) {
            PreparedStatement check = con.prepareStatement("SELECT 1 FROM categorias WHERE nombre = ?");
            check.setString(1, nombre);
            if (check.executeQuery().next()) {
                JOptionPane.showMessageDialog(this, "Categoría ya existe.", "Duplicado", JOptionPane.ERROR_MESSAGE);
                return;
            }

            PreparedStatement ps = con.prepareStatement("INSERT INTO categorias (nombre) VALUES (?)");
            ps.setString(1, nombre);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Agregada correctamente.");
            cargarCategorias();
            limpiarCampos();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error SQL: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void actualizarCategoria() {
        String idStr = txtCategoriaId.getText();
        String nombre = txtNombreCategoria.getText().trim();
        if (idStr.isEmpty() || nombre.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Selecciona y escribe un nombre.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try (Connection con = ConexionDB.conectar()) {
            PreparedStatement ps = con.prepareStatement("UPDATE categorias SET nombre = ? WHERE id = ?");
            ps.setString(1, nombre);
            ps.setInt(2, Integer.parseInt(idStr));
            if (ps.executeUpdate() > 0) {
                JOptionPane.showMessageDialog(this, "Actualizada correctamente.");
                cargarCategorias();
                limpiarCampos();
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error SQL: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void eliminarCategoria() {
        String idStr = txtCategoriaId.getText();
        if (idStr.isEmpty()) return;

        int confirm = JOptionPane.showConfirmDialog(this, "¿Eliminar esta categoría?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection con = ConexionDB.conectar()) {
            int id = Integer.parseInt(idStr);

            PreparedStatement psCheck = con.prepareStatement("SELECT 1 FROM productos WHERE categoria_id = ? LIMIT 1");
            psCheck.setInt(1, id);
            if (psCheck.executeQuery().next()) {
                JOptionPane.showMessageDialog(this, "Tiene productos asociados. No se puede eliminar.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            psCheck = con.prepareStatement("SELECT 1 FROM subcategorias WHERE categoria_id = ? LIMIT 1");
            psCheck.setInt(1, id);
            if (psCheck.executeQuery().next()) {
                JOptionPane.showMessageDialog(this, "Tiene subcategorías asociadas. No se puede eliminar.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            PreparedStatement ps = con.prepareStatement("DELETE FROM categorias WHERE id = ?");
            ps.setInt(1, id);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Categoría eliminada.");
            cargarCategorias();
            limpiarCampos();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al eliminar: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}