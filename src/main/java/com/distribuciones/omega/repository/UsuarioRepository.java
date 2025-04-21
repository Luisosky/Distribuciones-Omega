package com.distribuciones.omega.repository;

import com.distribuciones.omega.model.Usuario;
import com.distribuciones.omega.utils.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repositorio para operaciones CRUD de Usuarios (Empleados)
 */
public class UsuarioRepository {

    /**
     * Guarda un nuevo usuario en la base de datos
     * @param usuario Usuario a guardar
     * @return Usuario con ID asignado
     */
    public Usuario save(Usuario usuario) {
        String sql = "INSERT INTO usuarios (username, password, nombre, edad, id, direccion, " +
                     "telefono, email, salario, tipo_contrato, rol, activo) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, usuario.getUsername());
            stmt.setString(2, usuario.getPassword());
            stmt.setString(3, usuario.getNombre());
            stmt.setInt(4, usuario.getEdad());
            stmt.setString(5, usuario.getId());
            stmt.setString(6, usuario.getDireccion());
            stmt.setString(7, usuario.getTelefono());
            stmt.setString(8, usuario.getEmail());
            stmt.setDouble(9, usuario.getSalario() != null ? usuario.getSalario() : 0);
            stmt.setString(10, usuario.getTipoContrato());
            stmt.setString(11, usuario.getRol());
            stmt.setBoolean(12, usuario.isActivo());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("La creación del usuario falló, no se insertaron filas.");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    usuario.setIdUsuario(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("La creación del usuario falló, no se obtuvo el ID.");
                }
            }
            
            return usuario;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Actualiza un usuario existente
     * @param usuario Usuario con datos actualizados
     * @return true si la actualización fue exitosa
     */
    public boolean update(Usuario usuario) {
        String sql = "UPDATE usuarios SET nombre = ?, edad = ?, id = ?, direccion = ?, " +
                     "telefono = ?, email = ?, salario = ?, tipo_contrato = ?, rol = ?, activo = ? ";
        
        // Si la contraseña no está vacía, actualizarla
        if (usuario.getPassword() != null && !usuario.getPassword().isEmpty()) {
            sql += ", password = ? ";
        }
        
        sql += "WHERE id_usuario = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, usuario.getNombre());
            stmt.setInt(2, usuario.getEdad());
            stmt.setString(3, usuario.getId());
            stmt.setString(4, usuario.getDireccion());
            stmt.setString(5, usuario.getTelefono());
            stmt.setString(6, usuario.getEmail());
            stmt.setDouble(7, usuario.getSalario() != null ? usuario.getSalario() : 0);
            stmt.setString(8, usuario.getTipoContrato());
            stmt.setString(9, usuario.getRol());
            stmt.setBoolean(10, usuario.isActivo());
            
            int paramIndex = 11;
            
            // Si la contraseña no está vacía, establecer su valor
            if (usuario.getPassword() != null && !usuario.getPassword().isEmpty()) {
                stmt.setString(paramIndex++, usuario.getPassword());
            }
            
            stmt.setLong(paramIndex, usuario.getIdUsuario());
            
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Busca un usuario por su ID
     * @param id ID del usuario
     * @return Usuario encontrado o null si no existe
     */
    public Usuario findById(Long id) {
        String sql = "SELECT * FROM usuarios WHERE id_usuario = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToUsuario(rs);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Busca un usuario por su nombre de usuario
     * @param username Nombre de usuario
     * @return Usuario encontrado o null si no existe
     */
    public Usuario findByUsername(String username) {
        String sql = "SELECT * FROM usuarios WHERE username = ? AND activo = true";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToUsuario(rs);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Obtiene todos los usuarios
     * @return Lista de todos los usuarios
     */
    public List<Usuario> findAll() {
        List<Usuario> usuarios = new ArrayList<>();
        String sql = "SELECT * FROM usuarios WHERE activo = true";
        
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                usuarios.add(mapResultSetToUsuario(rs));
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return usuarios;
    }
    
    /**
     * Busca usuarios por su rol
     * @param rol Rol a buscar
     * @return Lista de usuarios con el rol especificado
     */
    public List<Usuario> findByRol(String rol) {
        List<Usuario> usuarios = new ArrayList<>();
        String sql = "SELECT * FROM usuarios WHERE rol = ? AND activo = true";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, rol);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                usuarios.add(mapResultSetToUsuario(rs));
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return usuarios;
    }
    
    /**
     * Marca un usuario como inactivo (eliminación lógica)
     * @param id ID del usuario
     * @return true si la operación fue exitosa
     */
    public boolean softDelete(Long id) {
        String sql = "UPDATE usuarios SET activo = false WHERE id_usuario = ?";
        
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
     * Convierte un ResultSet en un objeto Usuario
     */
    private Usuario mapResultSetToUsuario(ResultSet rs) throws SQLException {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(rs.getLong("id_usuario"));
        usuario.setUsername(rs.getString("username"));
        
        try {
            usuario.setNombre(rs.getString("nombre"));
        } catch (SQLException e) {
            System.out.println("Advertencia: Columna 'nombre' no encontrada en el ResultSet");
            usuario.setNombre("Usuario " + rs.getString("username")); // Valor por defecto
        }
        
        try {
            usuario.setRol(rs.getString("rol"));
        } catch (SQLException e) {
            usuario.setRol("usuario"); // Rol por defecto
        }
        
        try {
            usuario.setActivo(rs.getBoolean("activo"));
        } catch (SQLException e) {
            usuario.setActivo(true); // Activo por defecto
        }
        
        try {
            usuario.setPassword(rs.getString("password"));
            // Agregar esta línea para diagnóstico
            System.out.println("Contraseña recuperada para usuario " + usuario.getUsername() + ": " + 
                              (usuario.getPassword() != null ? "[presente]" : "[null]"));
        } catch (SQLException e) {
            System.out.println("Error al recuperar contraseña: " + e.getMessage());
            usuario.setPassword(null); // Explícitamente establecer null si hay error
        }
        
        return usuario;
    }
}