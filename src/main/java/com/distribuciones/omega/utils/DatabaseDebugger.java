package com.distribuciones.omega.utils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Clase de utilidad para depurar la estructura de la base de datos
 */
public class DatabaseDebugger {
    
    /**
     * Imprime la estructura de una tabla
     * @param tableName Nombre de la tabla
     */
    public static void printTableStructure(String tableName) {
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("DESCRIBE " + tableName)) {
            
            System.out.println("===== ESTRUCTURA DE LA TABLA " + tableName.toUpperCase() + " =====");
            while (rs.next()) {
                String columnName = rs.getString("Field");
                String columnType = rs.getString("Type");
                String isNullable = rs.getString("Null");
                String key = rs.getString("Key");
                String defaultValue = rs.getString("Default");
                
                System.out.println(columnName + " | " + columnType + " | " + isNullable + " | " + key + " | " + defaultValue);
            }
            System.out.println("================================================");
        } catch (SQLException e) {
            System.err.println("Error al imprimir estructura de la tabla " + tableName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}