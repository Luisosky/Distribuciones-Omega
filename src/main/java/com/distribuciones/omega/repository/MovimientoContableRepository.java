package com.distribuciones.omega.repository;

import com.distribuciones.omega.model.MovimientoContable;
import com.distribuciones.omega.utils.DBUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Repositorio para operaciones CRUD de Movimientos Contables
 */
public class MovimientoContableRepository {

    /**
     * Crea la tabla de movimientos contables si no existe
     */
    public void createTableIfNotExists() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS movimientos_contables (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "fecha TIMESTAMP NOT NULL," +
                "tipo_documento VARCHAR(50) NOT NULL," +
                "numero_documento VARCHAR(50) NOT NULL," +
                "descripcion VARCHAR(255) NOT NULL," +
                "monto DECIMAL(10, 2) NOT NULL," +
                "tipo_movimiento VARCHAR(20) NOT NULL," +
                "usuario VARCHAR(100) NOT NULL," +
                "entidad_relacionada VARCHAR(100) NOT NULL," +
                "referencia VARCHAR(100)," +
                "detalle TEXT" +
                ")";
        
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }
    
    /**
     * Guarda un nuevo movimiento contable
     * 
     * @param movimiento El movimiento a guardar
     * @return El movimiento con ID asignado
     * @throws SQLException Si ocurre un error en la base de datos
     */
    public MovimientoContable save(MovimientoContable movimiento) throws SQLException {
        String sql = "INSERT INTO movimientos_contables " +
                "(fecha, tipo_documento, numero_documento, descripcion, monto, tipo_movimiento, " +
                "usuario, entidad_relacionada, referencia, detalle) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setTimestamp(1, Timestamp.valueOf(movimiento.getFecha()));
            pstmt.setString(2, movimiento.getTipoDocumento());
            pstmt.setString(3, movimiento.getNumeroDocumento());
            pstmt.setString(4, movimiento.getDescripcion());
            pstmt.setDouble(5, movimiento.getMonto());
            pstmt.setString(6, movimiento.getTipoMovimiento());
            pstmt.setString(7, movimiento.getUsuario());
            pstmt.setString(8, movimiento.getEntidadRelacionada());
            pstmt.setString(9, movimiento.getReferencia());
            pstmt.setString(10, movimiento.getDetalle());
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("No se pudo guardar el movimiento contable.");
            }
            
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    movimiento.setId(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("No se pudo obtener el ID generado para el movimiento contable.");
                }
            }
        }
        
        return movimiento;
    }
    
    /**
     * Busca movimientos contables por tipo y rango de fechas
     * 
     * @param tipoMovimiento El tipo de movimiento (DEBITO/CREDITO)
     * @param fechaInicio La fecha de inicio del rango
     * @param fechaFin La fecha de fin del rango
     * @return Lista de movimientos contables
     * @throws SQLException Si ocurre un error en la base de datos
     */
    public List<MovimientoContable> buscarPorTipoYFechas(
            String tipoMovimiento, LocalDateTime fechaInicio, LocalDateTime fechaFin) throws SQLException {
        
        String sql = "SELECT * FROM movimientos_contables " +
                "WHERE tipo_movimiento = ? AND fecha BETWEEN ? AND ? " +
                "ORDER BY fecha DESC";
        
        List<MovimientoContable> movimientos = new ArrayList<>();
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, tipoMovimiento);
            pstmt.setTimestamp(2, Timestamp.valueOf(fechaInicio));
            pstmt.setTimestamp(3, Timestamp.valueOf(fechaFin));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    movimientos.add(mapResultSetToMovimiento(rs));
                }
            }
        }
        
        return movimientos;
    }
    
    /**
     * Busca movimientos contables por rango de fechas
     * 
     * @param fechaInicio La fecha de inicio del rango
     * @param fechaFin La fecha de fin del rango
     * @return Lista de movimientos contables
     * @throws SQLException Si ocurre un error en la base de datos
     */
    public List<MovimientoContable> buscarPorFechas(
            LocalDateTime fechaInicio, LocalDateTime fechaFin) throws SQLException {
        
        String sql = "SELECT * FROM movimientos_contables " +
                "WHERE fecha BETWEEN ? AND ? " +
                "ORDER BY fecha DESC";
        
        List<MovimientoContable> movimientos = new ArrayList<>();
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setTimestamp(1, Timestamp.valueOf(fechaInicio));
            pstmt.setTimestamp(2, Timestamp.valueOf(fechaFin));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    movimientos.add(mapResultSetToMovimiento(rs));
                }
            }
        }
        
        return movimientos;
    }
    
    /**
     * Calcula el total de movimientos por tipo
     * 
     * @param tipoMovimiento El tipo de movimiento (DEBITO/CREDITO)
     * @return El total calculado
     * @throws SQLException Si ocurre un error en la base de datos
     */
    public double calcularTotalPorTipo(String tipoMovimiento) throws SQLException {
        String sql = "SELECT SUM(monto) FROM movimientos_contables WHERE tipo_movimiento = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, tipoMovimiento);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        }
        
        return 0.0;
    }
    
    /**
     * Cuenta los movimientos por tipo de documento
     * 
     * @param tipoDocumento El tipo de documento a contar
     * @return El número de documentos
     * @throws SQLException Si ocurre un error en la base de datos
     */
    public int contarPorTipoDocumento(String tipoDocumento) throws SQLException {
        String sql = "SELECT COUNT(*) FROM movimientos_contables WHERE tipo_documento = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, tipoDocumento);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        
        return 0;
    }
    
    /**
     * Convierte un ResultSet en un objeto MovimientoContable
     */
    private MovimientoContable mapResultSetToMovimiento(ResultSet rs) throws SQLException {
        MovimientoContable movimiento = new MovimientoContable();
        
        movimiento.setId(rs.getLong("id"));
        movimiento.setFecha(rs.getTimestamp("fecha").toLocalDateTime());
        movimiento.setTipoDocumento(rs.getString("tipo_documento"));
        movimiento.setNumeroDocumento(rs.getString("numero_documento"));
        movimiento.setDescripcion(rs.getString("descripcion"));
        movimiento.setMonto(rs.getDouble("monto"));
        movimiento.setTipoMovimiento(rs.getString("tipo_movimiento"));
        movimiento.setUsuario(rs.getString("usuario"));
        movimiento.setEntidadRelacionada(rs.getString("entidad_relacionada"));
        movimiento.setReferencia(rs.getString("referencia"));
        movimiento.setDetalle(rs.getString("detalle"));
        
        return movimiento;
    }
}