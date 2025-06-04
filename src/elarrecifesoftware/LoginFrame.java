package elarrecifesoftware;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class LoginFrame extends JFrame {

    private JTextField txtUsuario;
    private JPasswordField txtContrasena;
    private JButton btnLogin;

    public LoginFrame() {
        setTitle("El Arrecife - Iniciar Sesión");
        setSize(400, 250);
        setLocationRelativeTo(null); // Centrar en la pantalla
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new GridBagLayout()); // Usar GridBagLayout para un mejor control del diseño

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10); // Espaciado entre componentes
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Título o Logo
        JLabel lblTitulo = new JLabel("Bienvenido a El Arrecife", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 24));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2; // Ocupa dos columnas
        add(lblTitulo, gbc);

        // Usuario
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1; // Vuelve a una columna
        add(new JLabel("Usuario:"), gbc);
        gbc.gridx = 1;
        txtUsuario = new JTextField(15);
        add(txtUsuario, gbc);

        // Contraseña
        gbc.gridx = 0;
        gbc.gridy = 2;
        add(new JLabel("Contraseña:"), gbc);
        gbc.gridx = 1;
        txtContrasena = new JPasswordField(15);
        add(txtContrasena, gbc);

        // Botón Login
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        btnLogin = new JButton("Iniciar Sesión");
        btnLogin.setFont(new Font("Arial", Font.BOLD, 16));
        add(btnLogin, gbc);

        btnLogin.addActionListener(e -> validarLogin());
    }

    private void validarLogin() {
        String username = txtUsuario.getText();
        String password = new String(txtContrasena.getPassword());

        Connection con = ConexionDB.conectar();
        if (con == null) {
            JOptionPane.showMessageDialog(this, "No se pudo conectar a la base de datos.", "Error de Conexión", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // Nota: En un sistema real, la contraseña no se almacena en texto plano.
            // Se debería usar un hashing (ej. BCrypt) y comparar el hash.
            PreparedStatement ps = con.prepareStatement("SELECT id, username, rol FROM usuarios WHERE username = ? AND password_hash = ?");
            ps.setString(1, username);
            ps.setString(2, password); // Comparación de contraseña en texto plano, cambiar en producción.
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int userId = rs.getInt("id");
                String userRol = rs.getString("rol"); // Obtener el rol del usuario
                JOptionPane.showMessageDialog(this, "¡Bienvenido, " + username + "!", "Login Exitoso", JOptionPane.INFORMATION_MESSAGE);
                this.dispose(); // Cierra la ventana de login
                // Pasar el userId, username y userRol a VentasFrame
                new VentasFrame(userId, username, userRol).setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this, "Usuario o contraseña incorrectos.", "Error de Login", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al iniciar sesión: " + e.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } finally {
            try { if (con != null) con.close(); } catch (SQLException ex) { /* ignorar */ }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}