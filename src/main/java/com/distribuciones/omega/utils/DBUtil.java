package com.distribuciones.omega.utils;

import io.github.cdimascio.dotenv.Dotenv;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Clase utilitaria para gestionar conexiones a la base de datos
 */
public class DBUtil {
    
    // Cargar variables desde .env
    private static final Dotenv dotenv = Dotenv.configure()
                                           .directory(".")
                                           .ignoreIfMissing()
                                           .load();
    
    // Variables para la conexión a la BD
    private static final String URL = dotenv.get("DB_URL");
    private static final String USER = dotenv.get("DB_USER");
    private static final String PASSWORD = dotenv.get("DB_PASS");
    
    // Driver de MySQL
    private static final String DRIVER = "com.mysql.cj.jdbc.Driver";
    
    static {
        try {
            // Registrar el driver
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.err.println("Error al cargar el driver de MySQL: " + e.getMessage());
        }
    }
    
    /**
     * Obtiene una conexión a la base de datos
     * @return Conexión a la BD
     * @throws SQLException Si ocurre un error al conectar
     */
    public static Connection getConnection() throws SQLException {
        // Añadir parámetro allowPublicKeyRetrieval=true si no está incluido en la URL
        // Nota: Comprobamos si ya contiene el parámetro para no duplicarlo
        String urlWithParams = URL;
        if (!URL.contains("allowPublicKeyRetrieval=")) {
            urlWithParams = URL.contains("?") 
                ? URL + "&allowPublicKeyRetrieval=true" 
                : URL + "?allowPublicKeyRetrieval=true";
        }
        
        return DriverManager.getConnection(urlWithParams, USER, PASSWORD);
    }

    public static void beginTransaction(Connection conn) throws SQLException {
        conn.setAutoCommit(false);
    }
    
    public static void commitTransaction(Connection conn) throws SQLException {
        conn.commit();
        conn.setAutoCommit(true);
    }
    
    public static void rollbackTransaction(Connection conn) throws SQLException {
        conn.rollback();
        conn.setAutoCommit(true);
    }

    public static void closeQuietly(AutoCloseable... resources) {
        for (AutoCloseable resource : resources) {
            if (resource != null) {
                try {
                    resource.close();
                } catch (Exception e) {
                    // Simplemente registra el error pero no lo propaga
                    e.printStackTrace();
                }
            }
        }
    }
}