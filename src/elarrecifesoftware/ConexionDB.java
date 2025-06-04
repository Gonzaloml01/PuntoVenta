package elarrecifesoftware;

import java.sql.Connection;
import java.sql.DriverManager;

public class ConexionDB {
    private static final String URL = "jdbc:mysql://localhost:3306/softarrecife?" +
                                     "useSSL=false&" +
                                     "allowPublicKeyRetrieval=true&" +
                                     "serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "Admin123";

    public static Connection conectar() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (Exception e) {
            System.err.println("Error de conexión: " + e.getMessage());
            return null;
        }
    }
}