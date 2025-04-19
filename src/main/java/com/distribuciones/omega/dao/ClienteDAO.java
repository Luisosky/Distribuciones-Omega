package com.distribuciones.omega.dao;

import java.sql.*;
import java.util.*;
import com.distribuciones.omega.model.Cliente;

public class ClienteDAO {
    private final Connection conn;

    public ClienteDAO(Connection conn) {
        this.conn = conn;
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
            pst.setString(3, c.getEmailString());
            pst.setString(4, c.getTelefono());
            pst.setString(5, c.getDireccion());
            pst.executeUpdate();
        }
    }

    public void updateCliente(Cliente c) throws SQLException {
        String sql = "UPDATE clientes SET nombre=?,email=?,telefono=?,direccion=? WHERE id=?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, c.getNombre());
            pst.setString(2, c.getEmailString());
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
}