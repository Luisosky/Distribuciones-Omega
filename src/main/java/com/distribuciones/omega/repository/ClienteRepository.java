package com.distribuciones.omega.repository;

import com.distribuciones.omega.model.Cliente;
import com.distribuciones.omega.utils.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repositorio para operaciones CRUD de Clientes
 */
public class ClienteRepository {

    /**
     * Guarda un nuevo cliente en la base de datos
     * @param cliente Cliente a guardar
     * @return Cliente con ID asignado
     */
    public Cliente save(Cliente cliente) {
        String sql = "INSERT INTO clientes (nombre, id, email, telefono, direccion, activo, mayorista, limite_credito) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, cliente.getNombre());
            stmt.setString(2, cliente.getId());
            stmt.setString(3, cliente.getEmail());
            stmt.setString(4, cliente.getTelefono());
            stmt.setString(5, cliente.getDireccion());
            stmt.setBoolean(6, cliente.isActive());
            stmt.setBoolean(7, cliente.isMayorista());
            stmt.setDouble(8, cliente.getLimiteCredito());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("La creación del cliente falló, no se insertaron filas.");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    cliente.setIdCliente(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("La creación del cliente falló, no se obtuvo el ID.");
                }
            }
            
            return cliente;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Actualiza un cliente existente
     * @param cliente Cliente con datos actualizados
     * @return true si la actualización fue exitosa
     */
    public boolean update(Cliente cliente) {
        String sql = "UPDATE clientes SET nombre = ?, id = ?, email = ?, telefono = ?, " +
                     "direccion = ?, activo = ?, mayorista = ?, limite_credito = ? " +
                     "WHERE id_cliente = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, cliente.getNombre());
            stmt.setString(2, cliente.getId());
            stmt.setString(3, cliente.getEmail());
            stmt.setString(4, cliente.getTelefono());
            stmt.setString(5, cliente.getDireccion());
            stmt.setBoolean(6, cliente.isActive());
            stmt.setBoolean(7, cliente.isMayorista());
            stmt.setDouble(8, cliente.getLimiteCredito());
            stmt.setLong(9, cliente.getIdCliente());
            
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Busca un cliente por su ID
     * @param id ID del cliente
     * @return Cliente encontrado o null si no existe
     */
    public Cliente findById(Long id) {
        String sql = "SELECT * FROM clientes WHERE id_cliente = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToCliente(rs);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Busca un cliente por su número de identificación (RUC o CI)
     * @param numeroIdentificacion Número de identificación
     * @return Cliente encontrado o null si no existe
     */
    public Cliente findByNumeroIdentificacion(String numeroIdentificacion) {
        String sql = "SELECT * FROM clientes WHERE id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, numeroIdentificacion);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToCliente(rs);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Obtiene todos los clientes
     * @return Lista de todos los clientes
     */
    public List<Cliente> findAll() {
        List<Cliente> clientes = new ArrayList<>();
        // Modificamos la consulta para ser más tolerante si la columna 'activo' no existe
        String sql = "SELECT * FROM clientes";
        
        try (Connection conn = DBUtil.getConnection();
            Statement stmt = conn.createStatement()) {
            
            try {
                // Primero intentamos con el filtro de activo
                ResultSet rs = stmt.executeQuery(sql + " WHERE activo = true");
                while (rs.next()) {
                    clientes.add(mapResultSetToCliente(rs));
                }
            } catch (SQLException e) {
                // Si hay error (ej: columna no existe), intentamos sin el filtro
                if (e.getMessage().contains("Unknown column 'activo'")) {
                    ResultSet rs = stmt.executeQuery(sql);
                    while (rs.next()) {
                        clientes.add(mapResultSetToCliente(rs));
                    }
                } else {
                    // Si es otro tipo de error, lo propagamos
                    throw e;
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return clientes;
    }
    
    /**
     * Busca clientes por su estado de mayorista
     * @param esMayorista true para buscar mayoristas, false para minoristas
     * @return Lista de clientes que cumplen la condición
     */
    public List<Cliente> findByMayorista(boolean esMayorista) {
        List<Cliente> clientes = new ArrayList<>();
        String sql = "SELECT * FROM clientes WHERE mayorista = ? AND activo = true";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setBoolean(1, esMayorista);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                clientes.add(mapResultSetToCliente(rs));
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return clientes;
    }
    
    /**
     * Busca clientes por nombre (búsqueda parcial)
     * @param nombre Fragmento del nombre a buscar
     * @return Lista de clientes que coinciden
     */
    public List<Cliente> findByNombreContaining(String nombre) {
        List<Cliente> clientes = new ArrayList<>();
        String sql = "SELECT * FROM clientes WHERE nombre LIKE ? AND activo = true";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, "%" + nombre + "%");
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                clientes.add(mapResultSetToCliente(rs));
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return clientes;
    }
    
    /**
     * Marca un cliente como inactivo (eliminación lógica)
     * @param id ID del cliente
     * @return true si la operación fue exitosa
     */
    public boolean softDelete(Long id) {
        String sql = "UPDATE clientes SET activo = false WHERE id_cliente = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Convierte un ResultSet en un objeto Cliente
     */
    private Cliente mapResultSetToCliente(ResultSet rs) throws SQLException {
        Cliente cliente = new Cliente();
        cliente.setIdCliente(rs.getLong("id_cliente"));
        cliente.setNombre(rs.getString("nombre"));
        cliente.setId(rs.getString("id"));
        
        // Manejar campos que podrían ser nulos o no existir
        try {
            cliente.setEmail(rs.getString("email"));
        } catch (SQLException e) {
            cliente.setEmail("");
        }
        
        try {
            cliente.setTelefono(rs.getString("telefono"));
        } catch (SQLException e) {
            cliente.setTelefono("");
        }
        
        try {
            cliente.setDireccion(rs.getString("direccion"));
        } catch (SQLException e) {
            cliente.setDireccion("");
        }
        
        try {
            cliente.setActive(rs.getBoolean("activo"));
        } catch (SQLException e) {
            cliente.setActive(true); // Por defecto activo
        }
        
        try {
            cliente.setMayorista(rs.getBoolean("mayorista"));
        } catch (SQLException e) {
            cliente.setMayorista(false); // Por defecto no mayorista
        }
        
        try {
            cliente.setLimiteCredito(rs.getDouble("limite_credito"));
        } catch (SQLException e) {
            cliente.setLimiteCredito(0.0); // Sin límite por defecto
        }
        
        return cliente;
    }
}