package com.distribuciones.omega.repository;

import com.distribuciones.omega.model.Factura;
import com.distribuciones.omega.model.MetodoPago;
import com.distribuciones.omega.model.Pago;
import com.distribuciones.omega.utils.DBUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Repositorio para operaciones CRUD de Pagos
 */
public class PagoRepository {

    /**
     * Crea la tabla de pagos si no existe
     */
    public void createTableIfNotExists() {
        try (Connection conn = DBUtil.getConnection()) {
            // Verificar primero si la tabla facturas existe
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet rsFacturas = meta.getTables(null, null, "facturas", null);
            
            if (!rsFacturas.next()) {
                // La tabla facturas no existe, creemos una estructura temporal (o lanzamos excepción)
                System.out.println("ADVERTENCIA: La tabla 'facturas' no existe. Se intentará crear primero.");
                
                try {
                    // Intentar inicializar la tabla facturas primero
                    FacturaRepository facturaRepo = new FacturaRepository();
                    facturaRepo.createTableIfNotExists();
                    System.out.println("Tabla facturas creada exitosamente.");
                } catch (Exception ex) {
                    System.err.println("Error al crear la tabla facturas: " + ex.getMessage());
                    throw new SQLException("No se puede crear la tabla 'pagos' porque la tabla 'facturas' no existe.", ex);
                }
            }
            
            // Ahora verificamos si la tabla pagos existe
            ResultSet rsPagos = meta.getTables(null, null, "pagos", null);
            
            if (!rsPagos.next()) {
                // Tabla no existe, crearla
                System.out.println("Creando tabla pagos...");
                
                try (Statement stmt = conn.createStatement()) {
                    String sql = "CREATE TABLE pagos (" +
                             "id INT AUTO_INCREMENT PRIMARY KEY, " +
                             "factura_id INT NOT NULL, " +
                             "monto DECIMAL(10,2) NOT NULL, " +
                             "metodo_pago VARCHAR(50) NOT NULL, " +
                             "referencia VARCHAR(100), " +
                             "fecha_pago DATETIME NOT NULL, " +
                             "aprobado BOOLEAN DEFAULT FALSE, " +
                             "observaciones TEXT, " +
                             "FOREIGN KEY (factura_id) REFERENCES facturas(id_factura)" +
                             ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
                
                    stmt.executeUpdate(sql);
                    System.out.println("Tabla pagos creada exitosamente");
                }
            } else {
                System.out.println("La tabla pagos ya existe");
            }
        } catch (SQLException e) {
            System.err.println("Error al crear tabla pagos: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error al crear tabla pagos", e);
        }
    }

    /**
     * Guarda un nuevo pago
     * @param pago Pago a guardar
     * @return Pago guardado con ID generado
     */
    public Pago save(Pago pago) {
        String sql = "INSERT INTO pagos (factura_id, monto, metodo_pago, referencia, fecha_pago, aprobado, observaciones) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setLong(1, pago.getFactura().getId());
            pstmt.setDouble(2, pago.getMonto());
            pstmt.setString(3, pago.getMetodoPago().name());
            pstmt.setString(4, pago.getReferencia());
            pstmt.setTimestamp(5, Timestamp.valueOf(pago.getFechaPago()));
            pstmt.setBoolean(6, pago.isAprobado());
            pstmt.setString(7, pago.getObservaciones());
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("No se pudo guardar el pago, no se afectaron filas.");
            }
            
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    pago.setId(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("No se pudo obtener el ID generado para el pago.");
                }
            }
            
            return pago;
            
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar el pago: " + e.getMessage(), e);
        }
    }
    
    /**
     * Obtiene todos los pagos para una factura
     * @param facturaId ID de la factura
     * @return Lista de pagos asociados a la factura
     */
    public List<Pago> findByFacturaId(Long facturaId) {
        List<Pago> pagos = new ArrayList<>();
        String sql = "SELECT p.*, f.numero_factura FROM pagos p " +
                     "JOIN facturas f ON p.factura_id = f.id " +
                     "WHERE p.factura_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, facturaId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    pagos.add(mapResultSetToPago(rs));
                }
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar pagos por factura: " + e.getMessage(), e);
        }
        
        return pagos;
    }
    
    /**
     * Obtiene un pago por su ID
     * @param id ID del pago
     * @return Pago encontrado o null si no existe
     */
    public Pago findById(Long id) {
        String sql = "SELECT p.*, f.numero_factura FROM pagos p " +
                     "JOIN facturas f ON p.factura_id = f.id " +
                     "WHERE p.id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPago(rs);
                }
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar pago por ID: " + e.getMessage(), e);
        }
        
        return null;
    }
    
    /**
     * Actualiza el estado de un pago
     * @param pago Pago con datos actualizados
     * @return true si la actualización fue exitosa
     */
    public boolean updateEstado(Pago pago) {
        String sql = "UPDATE pagos SET aprobado = ?, observaciones = ? WHERE id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setBoolean(1, pago.isAprobado());
            pstmt.setString(2, pago.getObservaciones());
            pstmt.setLong(3, pago.getId());
            
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
            
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar estado del pago: " + e.getMessage(), e);
        }
    }
    
    /**
     * Mapea un ResultSet a un objeto Pago
     */
    private Pago mapResultSetToPago(ResultSet rs) throws SQLException {
        Pago pago = new Pago();
        pago.setId(rs.getLong("id"));
        pago.setMonto(rs.getDouble("monto"));
        pago.setMetodoPago(MetodoPago.valueOf(rs.getString("metodo_pago")));
        pago.setReferencia(rs.getString("referencia"));
        pago.setFechaPago(rs.getTimestamp("fecha_pago").toLocalDateTime());
        pago.setAprobado(rs.getBoolean("aprobado"));
        pago.setObservaciones(rs.getString("observaciones"));
        
        // Crear factura con datos básicos
        Factura factura = new Factura();
        factura.setId(rs.getLong("factura_id"));
        factura.setNumeroFactura(rs.getString("numero_factura"));
        pago.setFactura(factura);
        
        return pago;
    }
}