package com.distribuciones.omega.dao;

import java.sql.*;
import java.util.*;
import com.distribuciones.omega.model.Cliente;

public class ClienteDAO {
    private final Connection conn;

    public ClienteDAO(Connection conn) {
        this.conn = conn;
    }


    public void createTableIfNotExists() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS clientes (" +
                     "id VARCHAR(20) PRIMARY KEY," +
                     "nombre VARCHAR(100) NOT NULL," +
                     "email VARCHAR(100)," +
                     "telefono VARCHAR(20)," +
                     "direccion VARCHAR(200)," +
                     "active BOOLEAN DEFAULT TRUE" +
                     ")";
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            
            // Verificar si hay datos existentes
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM clientes");
            rs.next();
            int count = rs.getInt(1);
            
            // Si no hay datos, insertar algunos ejemplos
            if (count == 0) {
                String insertSql = "INSERT INTO clientes (id, nombre, email, telefono, direccion, active) " +
                                  "VALUES " +
                                  "('CLI001', 'Juan Pérez', 'juan@example.com', '555-1234', 'Calle Principal 123', 1)," +
                                  "('CLI002', 'María López', 'maria@example.com', '555-5678', 'Avenida Central 456', 1)," +
                                  "('CLI003', 'Carlos Rodríguez', 'carlos@example.com', '555-9012', 'Plaza Mayor 789', 1)";
                stmt.execute(insertSql);
            }
        }
    }



    public List<Cliente> getActiveClientes() throws SQLException {
        String sql = "SELECT id,nombre,email,telefono,direccion FROM clientes WHERE active=1";
        List<Cliente> list = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Cliente(
                    rs.getString("nombre"),
                    rs.getString("id"),
                    rs.getString("email"),
                    rs.getString("telefono"),
                    rs.getString("direccion")
                ));
            }
        }
        return list;
    }

    public void addCliente(Cliente c) throws SQLException {
        String sql = "INSERT INTO clientes(id,nombre,email,telefono,direccion,active) VALUES(?,?,?,?,?,1)";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, c.getId());
            pst.setString(2, c.getNombre());
            pst.setString(3, c.getEmail()); 
            pst.setString(4, c.getTelefono());
            pst.setString(5, c.getDireccion());
            pst.executeUpdate();
        }
    }

    public void updateCliente(Cliente c) throws SQLException {
        String sql = "UPDATE clientes SET nombre=?,email=?,telefono=?,direccion=? WHERE id=?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, c.getNombre());
            pst.setString(2, c.getEmail()); 
            pst.setString(3, c.getTelefono());
            pst.setString(4, c.getDireccion());
            pst.setString(5, c.getId());
            pst.executeUpdate();
        }
    }

    public void deactivateCliente(String id) throws SQLException {
        String sql = "UPDATE clientes SET active=0 WHERE id=?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, id);
            pst.executeUpdate();
        }
    }

    public List<Cliente> buscarClientes(String criterio) throws SQLException {
        String sql = "SELECT id, nombre, email, telefono, direccion FROM clientes " +
                     "WHERE active=1 AND (id LIKE ? OR nombre LIKE ?)";
        List<Cliente> list = new ArrayList<>();
        
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            String paramBusqueda = "%" + criterio + "%";
            pst.setString(1, paramBusqueda);
            pst.setString(2, paramBusqueda);
            
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    list.add(new Cliente(
                        rs.getString("nombre"),
                        rs.getString("id"),
                        rs.getString("email"),
                        rs.getString("telefono"),
                        rs.getString("direccion")
                    ));
                }
            }
        }
        
        return list;
    }
}