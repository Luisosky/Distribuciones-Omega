package com.distribuciones.omega.utils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DatabaseDiagnostic {
    
    public static void main(String[] args) {
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Primero verificamos si existen las tablas que queremos analizar
            List<String> tablesToCheck = new ArrayList<>();
            tablesToCheck.add("productos");
            tablesToCheck.add("detalle_factura");
            tablesToCheck.add("items_cotizacion");
            tablesToCheck.add("detalle_cotizacion");
            tablesToCheck.add("inventario");
            tablesToCheck.add("usuarios");
            
            // Obtener lista de tablas existentes
            System.out.println("=== TABLAS EXISTENTES EN LA BASE DE DATOS ===");
            ResultSet tablesRS = conn.getMetaData().getTables(null, null, "%", new String[]{"TABLE"});
            List<String> existingTables = new ArrayList<>();
            while (tablesRS.next()) {
                String tableName = tablesRS.getString("TABLE_NAME");
                existingTables.add(tableName);
                System.out.println(tableName);
            }
            System.out.println();
            
            // Analizar cada tabla existente de nuestra lista
            for (String table : tablesToCheck) {
                if (existingTables.contains(table)) {
                    analyzeTable(conn, stmt, table);
                } else {
                    System.out.println("*** La tabla '" + table + "' no existe en la base de datos ***\n");
                }
            }
            
            // Buscar todas las referencias (foreign keys) hacia la tabla productos
            System.out.println("\n=== REFERENCIAS A LA TABLA PRODUCTOS ===");
            ResultSet fkRS = conn.getMetaData().getExportedKeys(null, null, "productos");
            while (fkRS.next()) {
                String pkTableName = fkRS.getString("PKTABLE_NAME");
                String pkColumnName = fkRS.getString("PKCOLUMN_NAME");
                String fkTableName = fkRS.getString("FKTABLE_NAME");
                String fkColumnName = fkRS.getString("FKCOLUMN_NAME");
                String constraintName = fkRS.getString("FK_NAME");
                
                System.out.println("FK: " + constraintName);
                System.out.println("  Tabla PK: " + pkTableName + " (" + pkColumnName + ")");
                System.out.println("  Tabla FK: " + fkTableName + " (" + fkColumnName + ")");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Analiza la estructura de una tabla específica
     */
    private static void analyzeTable(Connection conn, Statement stmt, String tableName) throws SQLException {
        System.out.println("=== ESTRUCTURA DE TABLA " + tableName.toUpperCase() + " ===");
        
        // Mostrar estructura de la tabla
        ResultSet rs = stmt.executeQuery("DESCRIBE " + tableName);
        while (rs.next()) {
            System.out.printf("%-15s %-20s %-10s %-10s%n", 
                rs.getString("Field"),
                rs.getString("Type"),
                rs.getString("Null"),
                rs.getString("Key")
            );
        }
        
        // Mostrar definición completa de la tabla
        System.out.println("\n=== DEFINICIÓN COMPLETA DE TABLA " + tableName.toUpperCase() + " ===");
        rs = stmt.executeQuery("SHOW CREATE TABLE " + tableName);
        if (rs.next()) {
            System.out.println(rs.getString(2));
        }
        
        // Para mayor claridad, agregamos separadores
        System.out.println("\n------------------------------------------------------\n");
    }
    
    /**
     * Método para facilitar el diagnóstico desde otras partes del código
     */
    public static void runDiagnostic() {
        main(new String[]{});
    }
    
    /**
     * Analiza todas las tablas de la base de datos
     */
    public static void analyzeAllTables() {
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement()) {
            
            System.out.println("=== ANALIZANDO TODAS LAS TABLAS DE LA BASE DE DATOS ===");
            
            // Obtener todas las tablas
            ResultSet tablesRS = conn.getMetaData().getTables(null, null, "%", new String[]{"TABLE"});
            while (tablesRS.next()) {
                String tableName = tablesRS.getString("TABLE_NAME");
                // Excluir tablas del sistema si es necesario
                if (!tableName.startsWith("sys_")) {
                    analyzeTable(conn, stmt, tableName);
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}