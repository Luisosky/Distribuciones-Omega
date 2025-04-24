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
    
    // Método para limpiar los valores de espacios
    private static String cleanValue(String value) {
        return value != null ? value.trim() : null;
    }
    
    // Variables para la conexión a la BD - Con limpieza de espacios
    public static final String URL = cleanValue(dotenv.get("DB_URL"));
    public static final String USER = cleanValue(dotenv.get("DB_USER"));
    public static final String PASSWORD = cleanValue(dotenv.get("DB_PASS"));
    
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
    
    // Métodos adicionales para el DatabaseInitializer
    public static String getDBUrl() {
        return cleanValue(URL);
    }
    
    public static String getDBUser() {
        return cleanValue(USER);
    }
    
    public static String getDBPassword() {
        return cleanValue(PASSWORD);
    }
    
    /**
     * Obtiene una conexión a la base de datos
     * @return Conexión a la BD
     * @throws SQLException Si ocurre un error al conectar
     */
    public static Connection getConnection() throws SQLException {
        // Limpiar explícitamente los valores para asegurar que no haya espacios
        String cleanUrl = cleanValue(URL);
        String cleanUser = cleanValue(USER);
        String cleanPassword = cleanValue(PASSWORD);
        
        // Imprimir depuración con corchetes para ver espacios
        System.out.println("DEBUG - Conexión a base de datos:");
        System.out.println("URL: [" + cleanUrl + "]");
        System.out.println("Usuario: [" + cleanUser + "]");
        
        // Añadir parámetro allowPublicKeyRetrieval=true si no está incluido
        if (cleanUrl != null && !cleanUrl.contains("allowPublicKeyRetrieval=")) {
            cleanUrl = cleanUrl.contains("?") 
                ? cleanUrl + "&allowPublicKeyRetrieval=true" 
                : cleanUrl + "?allowPublicKeyRetrieval=true";
        }
        
        return DriverManager.getConnection(cleanUrl, cleanUser, cleanPassword);
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