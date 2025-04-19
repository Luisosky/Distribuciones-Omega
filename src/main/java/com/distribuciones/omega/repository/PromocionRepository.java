package com.distribuciones.omega.repository;

import com.distribuciones.omega.model.Promocion;
import com.distribuciones.omega.utils.DBUtil;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Repositorio para operaciones CRUD de Promociones en la base de datos
 * Adaptado para trabajar con la estructura actual de la clase Promocion
 */
public class PromocionRepository {

    /**
     * Guarda una nueva promoción en la base de datos
     * @param promocion Promoción a guardar
     * @return Promoción con ID asignado
     */
    public Promocion save(Promocion promocion) {
        String sql = "INSERT INTO promociones (descripcion, tipo, valor, codigo_producto, fecha_inicio, fecha_fin, activa) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, promocion.getDescripcion());
            // Usar isPorcentaje para determinar el tipo
            stmt.setString(2, promocion.isPorcentaje() ? "PORCENTAJE" : "VALOR_FIJO");
            stmt.setDouble(3, promocion.getValor());
            // Usar categoriasAplicables como código_producto temporal
            stmt.setString(4, getFirstCategory(promocion.getCategoriasAplicables()));
            stmt.setDate(5, promocion.getFechaInicio() != null ? 
                          Date.valueOf(promocion.getFechaInicio()) : 
                          Date.valueOf(LocalDate.now()));
            stmt.setDate(6, promocion.getFechaFin() != null ? 
                          Date.valueOf(promocion.getFechaFin()) : 
                          Date.valueOf(LocalDate.now().plusMonths(1)));
            stmt.setBoolean(7, promocion.isActiva());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("La creación de la promoción falló, no se insertaron filas.");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    // Convertir Long a int para que coincida con el tipo de la clase Promocion
                    promocion.setId((int)generatedKeys.getLong(1));
                } else {
                    throw new SQLException("La creación de la promoción falló, no se obtuvo el ID.");
                }
            }
            
            return promocion;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Actualiza una promoción existente
     * @param promocion Promoción con datos actualizados
     * @return true si la actualización fue exitosa
     */
    public boolean update(Promocion promocion) {
        String sql = "UPDATE promociones SET descripcion = ?, tipo = ?, valor = ?, codigo_producto = ?, " +
                     "fecha_inicio = ?, fecha_fin = ?, activa = ? WHERE id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, promocion.getDescripcion());
            // Usar isPorcentaje para determinar el tipo
            stmt.setString(2, promocion.isPorcentaje() ? "PORCENTAJE" : "VALOR_FIJO");
            stmt.setDouble(3, promocion.getValor());
            // Usar categoriasAplicables como código_producto temporal
            stmt.setString(4, getFirstCategory(promocion.getCategoriasAplicables()));
            stmt.setDate(5, promocion.getFechaInicio() != null ? 
                          Date.valueOf(promocion.getFechaInicio()) : 
                          Date.valueOf(LocalDate.now()));
            stmt.setDate(6, promocion.getFechaFin() != null ? 
                          Date.valueOf(promocion.getFechaFin()) : 
                          Date.valueOf(LocalDate.now().plusMonths(1)));
            stmt.setBoolean(7, promocion.isActiva());
            stmt.setInt(8, promocion.getId());  // Cambiado de setLong a setInt
            
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Elimina una promoción por su ID
     * @param id ID de la promoción a eliminar
     * @return true si la eliminación fue exitosa
     */
    public boolean delete(int id) {  // Cambiado de Long a int
        String sql = "DELETE FROM promociones WHERE id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);  // Cambiado de setLong a setInt
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Busca una promoción por su ID
     * @param id ID de la promoción
     * @return Promoción encontrada o null si no existe
     */
    public Promocion findById(int id) {  // Cambiado de Long a int
        String sql = "SELECT * FROM promociones WHERE id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);  // Cambiado de setLong a setInt
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToPromocion(rs);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Obtiene todas las promociones
     * @return Lista de todas las promociones
     */
    public List<Promocion> findAll() {
        List<Promocion> promociones = new ArrayList<>();
        String sql = "SELECT * FROM promociones";
        
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                promociones.add(mapResultSetToPromocion(rs));
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return promociones;
    }
    
    /**
     * Busca promociones por producto y fechas (para comprobar vigencia)
     * @param codigoProducto Código del producto
     * @param fechaInicio Fecha de inicio para validar vigencia
     * @param fechaFin Fecha de fin para validar vigencia
     * @return Lista de promociones vigentes para el producto
     */
    public List<Promocion> findByProductoAndFechas(String codigoProducto, LocalDate fechaInicio, LocalDate fechaFin) {
        List<Promocion> promociones = new ArrayList<>();
        String sql = "SELECT * FROM promociones WHERE codigo_producto = ? AND activa = true " +
                     "AND fecha_inicio <= ? AND fecha_fin >= ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, codigoProducto);
            stmt.setDate(2, Date.valueOf(fechaFin));
            stmt.setDate(3, Date.valueOf(fechaInicio));
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                promociones.add(mapResultSetToPromocion(rs));
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return promociones;
    }
    
    /**
     * Busca promociones vigentes en un rango de fechas
     * @param fechaInicio Fecha de inicio
     * @param fechaFin Fecha de fin
     * @return Lista de promociones vigentes en el rango de fechas
     */
    public List<Promocion> findByFechas(LocalDate fechaInicio, LocalDate fechaFin) {
        List<Promocion> promociones = new ArrayList<>();
        String sql = "SELECT * FROM promociones WHERE activa = true " +
                     "AND fecha_inicio <= ? AND fecha_fin >= ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setDate(1, Date.valueOf(fechaFin));
            stmt.setDate(2, Date.valueOf(fechaInicio));
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                promociones.add(mapResultSetToPromocion(rs));
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return promociones;
    }
    
    /**
     * Convierte un ResultSet en un objeto Promocion
     */
    private Promocion mapResultSetToPromocion(ResultSet rs) throws SQLException {
        Promocion promocion = new Promocion();
        // Convertir Long a int para que coincida con el tipo de la clase Promocion
        promocion.setId((int)rs.getLong("id"));
        promocion.setDescripcion(rs.getString("descripcion"));
        
        // Mapear el "tipo" de la DB al atributo "porcentaje" del modelo
        String tipo = rs.getString("tipo");
        promocion.setPorcentaje("PORCENTAJE".equals(tipo));
        
        promocion.setValor(rs.getDouble("valor"));
        
        // Mapear código_producto a categoriasAplicables
        String codigoProducto = rs.getString("codigo_producto");
        if (codigoProducto != null && !codigoProducto.isEmpty()) {
            if (codigoProducto.equals("TODAS")) {
                promocion.setCategoriasAplicables("TODAS");
            } else {
                promocion.setCategoriasAplicables(codigoProducto);
            }
        }
        
        // Mapear fechas
        Date fechaInicio = rs.getDate("fecha_inicio");
        if (fechaInicio != null) {
            promocion.setFechaInicio(fechaInicio.toLocalDate());
        }
        
        Date fechaFin = rs.getDate("fecha_fin");
        if (fechaFin != null) {
            promocion.setFechaFin(fechaFin.toLocalDate());
        }
        
        promocion.setActiva(rs.getBoolean("activa"));
        return promocion;
    }
    
    /**
     * Obtiene la primera categoría de una lista separada por comas
     * o devuelve la cadena completa si no hay comas
     */
    private String getFirstCategory(String categorias) {
        if (categorias == null || categorias.isEmpty()) {
            return "TODAS";
        }
        
        if (categorias.contains(",")) {
            return categorias.split(",")[0];
        }
        
        return categorias;
    }
}