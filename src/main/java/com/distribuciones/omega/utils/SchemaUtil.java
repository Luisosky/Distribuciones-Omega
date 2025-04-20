package com.distribuciones.omega.utils;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilidad para gestionar el esquema de la base de datos
 */
public class SchemaUtil {
    
    private static final Logger LOGGER = Logger.getLogger(SchemaUtil.class.getName());
    
    /**
     * Obtiene información sobre la clave primaria de una tabla
     * @param conn Conexión a la base de datos
     * @param tableName Nombre de la tabla
     * @return Un arreglo con [nombreColumna, tipoColumna]
     * @throws SQLException si hay error al acceder a la base de datos
     */
    public static String[] getPrimaryKeyInfo(Connection conn, String tableName) throws SQLException {
        String pkColumn = null;
        String pkType = null;
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("DESCRIBE " + tableName)) {
            
            while (rs.next()) {
                String columnName = rs.getString("Field");
                String columnType = rs.getString("Type");
                String keyType = rs.getString("Key");
                
                if ("PRI".equals(keyType)) {
                    pkColumn = columnName;
                    pkType = columnType;
                    LOGGER.info("Clave primaria de " + tableName + ": " + pkColumn + " (" + pkType + ")");
                    break;
                }
            }
        }
        
        if (pkColumn == null) {
            throw new SQLException("No se encontró clave primaria en la tabla " + tableName);
        }
        
        return new String[] { pkColumn, pkType };
    }
    
    /**
     * Verifica si una tabla existe en la base de datos
     * @param conn Conexión a la base de datos
     * @param tableName Nombre de la tabla a verificar
     * @return true si la tabla existe, false en caso contrario
     * @throws SQLException si hay error al acceder a la base de datos
     */
    public static boolean tableExists(Connection conn, String tableName) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        ResultSet rs = meta.getTables(null, null, tableName, null);
        boolean exists = rs.next();
        rs.close();
        return exists;
    }
    
    /**
     * Elimina una tabla si existe
     * @param conn Conexión a la base de datos
     * @param tableName Nombre de la tabla a eliminar
     * @throws SQLException si hay error al eliminar la tabla
     */
    public static void dropTableIfExists(Connection conn, String tableName) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DROP TABLE IF EXISTS " + tableName);
            LOGGER.info("Tabla " + tableName + " eliminada (si existía)");
        }
    }
    
    /**
     * Verifica si una tabla existente funciona correctamente
     * @param conn Conexión a la base de datos
     * @param tableName Nombre de la tabla a verificar
     * @return true si la tabla funciona correctamente, false si tiene problemas
     */
    public static boolean isTableWorking(Connection conn, String tableName) {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeQuery("SELECT * FROM " + tableName + " LIMIT 1");
            return true;
        } catch (SQLException e) {
            LOGGER.warning("Tabla " + tableName + " existe pero tiene problemas: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Recrea una tabla si tiene problemas
     * @param conn Conexión a la base de datos
     * @param tableName Nombre de la tabla
     * @param createTableSQL SQL para crear la tabla
     * @throws SQLException si hay error al recrear la tabla
     */
    public static void recreateTableIfNeeded(Connection conn, String tableName, String createTableSQL) throws SQLException {
        if (tableExists(conn, tableName)) {
            if (!isTableWorking(conn, tableName)) {
                dropTableIfExists(conn, tableName);
                executeUpdate(conn, createTableSQL);
                LOGGER.info("Tabla " + tableName + " recreada debido a problemas");
            } else {
                LOGGER.info("Tabla " + tableName + " ya existe y funciona correctamente");
            }
        } else {
            executeUpdate(conn, createTableSQL);
            LOGGER.info("Tabla " + tableName + " creada");
        }
    }
    
    /**
     * Ejecuta una sentencia SQL de actualización
     * @param conn Conexión a la base de datos
     * @param sql Sentencia SQL a ejecutar
     * @throws SQLException si hay error al ejecutar la sentencia
     */
    public static void executeUpdate(Connection conn, String sql) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

        /**
     * Obtiene la definición exacta de una columna
     * @param conn Conexión a la base de datos
     * @param tableName Nombre de la tabla
     * @param columnName Nombre de la columna
     * @return La definición completa de la columna (tipo, atributos, etc.)
     * @throws SQLException si hay error al acceder a la base de datos
     */
    public static String getColumnDefinition(Connection conn, String tableName, String columnName) throws SQLException {
        String definition = null;
        
        try (Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SHOW COLUMNS FROM " + tableName + " WHERE Field = '" + columnName + "'")) {
            
            if (rs.next()) {
                // Field, Type, Null, Key, Default, Extra
                String type = rs.getString("Type");
                String isNull = rs.getString("Null");
                String defaultVal = rs.getString("Default");
                String extra = rs.getString("Extra");
                
                StringBuilder sb = new StringBuilder(type);
                if ("NO".equals(isNull)) {
                    sb.append(" NOT NULL");
                }
                if (defaultVal != null) {
                    sb.append(" DEFAULT ").append(defaultVal);
                }
                if (extra != null && !extra.isEmpty()) {
                    sb.append(" ").append(extra);
                }
                
                definition = sb.toString();
                LOGGER.info("Definición de columna " + tableName + "." + columnName + ": " + definition);
            }
        }
        
        if (definition == null) {
            throw new SQLException("Columna " + columnName + " no encontrada en tabla " + tableName);
        }
        
        return definition;
    }
}