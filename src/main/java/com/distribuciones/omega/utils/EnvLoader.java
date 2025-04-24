package com.distribuciones.omega.utils;

import io.github.cdimascio.dotenv.Dotenv;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilidad para cargar variables de entorno desde archivo .env
 */
public class EnvLoader {
    private static final Logger LOGGER = Logger.getLogger(EnvLoader.class.getName());
    private static Dotenv dotenv;
    
    static {
        try {
            // Cargar .env desde la raíz del proyecto o desde el directorio de ejecución
            dotenv = Dotenv.configure()
                    .ignoreIfMissing() // No falla si no encuentra el archivo
                    .load();
            LOGGER.info("Archivo .env cargado correctamente");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al cargar archivo .env: " + e.getMessage(), e);
        }
    }
    
    /**
     * Obtiene el valor de una variable de entorno
     * @param key Nombre de la variable
     * @param defaultValue Valor por defecto si no existe
     * @return Valor de la variable o el valor por defecto
     */
    public static String get(String key, String defaultValue) {
        try {
            if (dotenv == null) {
                String value = System.getenv(key);
                return (value != null) ? value.trim() : defaultValue;
            }
            String value = dotenv.get(key);
            return (value != null && !value.isEmpty()) ? value.trim() : defaultValue;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al leer variable " + key + ": " + e.getMessage(), e);
            return defaultValue;
        }
    }
    
    /**
     * Obtiene el valor de una variable de entorno
     * @param key Nombre de la variable
     * @return Valor de la variable o null si no existe
     */
    public static String get(String key) {
        return get(key, null);
    }
    
    // Métodos específicos para la base de datos
    public static String getDbUrl() {
        return get("DB_URL", "jdbc:mysql://localhost:3306/omega?allowPublicKeyRetrieval=true&useSSL=false");
    }
    
    public static String getDbUser() {
        return get("DB_USER", "root");
    }
    
    public static String getDbPassword() {
        return get("DB_PASS", "root123");
    }
}