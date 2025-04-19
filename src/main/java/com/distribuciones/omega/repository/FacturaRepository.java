package com.distribuciones.omega.repository;

import com.distribuciones.omega.model.*;
import com.distribuciones.omega.utils.DBUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Repositorio para operaciones CRUD de Facturas
 */
public class FacturaRepository {

    private final InventarioRepository inventarioRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    
    public FacturaRepository() {
        this.inventarioRepository = new InventarioRepository();
        this.clienteRepository = new ClienteRepository();
        this.usuarioRepository = new UsuarioRepository();
    }
    
    /**
     * Guarda una nueva factura en la base de datos
     * @param factura Factura a guardar
     * @return Factura con ID asignado
     */
    public Factura save(Factura factura) {
        Connection conn = null;
        PreparedStatement stmtFactura = null;
        PreparedStatement stmtItems = null;
        
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);
            
            // Insertar la factura
            String sqlFactura = "INSERT INTO facturas (numero_factura, cliente_id, vendedor_id, fecha, orden_id, " +
                             "subtotal, descuento, iva, total, anulada, motivo_anulacion, fecha_anulacion, forma_pago) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            stmtFactura = conn.prepareStatement(sqlFactura, Statement.RETURN_GENERATED_KEYS);
            stmtFactura.setString(1, factura.getNumeroFactura());
            stmtFactura.setLong(2, factura.getCliente().getIdCliente());
            stmtFactura.setLong(3, factura.getVendedor().getIdUsuario());
            stmtFactura.setTimestamp(4, Timestamp.valueOf(factura.getFecha() != null ? factura.getFecha() : LocalDateTime.now()));
            
            // El orden_id puede ser null
            if (factura.getOrdenId() != null) {
                stmtFactura.setLong(5, factura.getOrdenId());
            } else {
                stmtFactura.setNull(5, Types.BIGINT);
            }
            
            stmtFactura.setDouble(6, factura.getSubtotal());
            stmtFactura.setDouble(7, factura.getDescuento());
            stmtFactura.setDouble(8, factura.getIva());
            stmtFactura.setDouble(9, factura.getTotal());
            stmtFactura.setBoolean(10, factura.isAnulada());
            
            // Campos de anulación pueden ser null
            if (factura.getMotivoAnulacion() != null) {
                stmtFactura.setString(11, factura.getMotivoAnulacion());
            } else {
                stmtFactura.setNull(11, Types.VARCHAR);
            }
            
            if (factura.getFechaAnulacion() != null) {
                stmtFactura.setTimestamp(12, Timestamp.valueOf(factura.getFechaAnulacion()));
            } else {
                stmtFactura.setNull(12, Types.TIMESTAMP);
            }
            
            // Forma de pago puede ser null
            if (factura.getFormaPago() != null) {
                stmtFactura.setString(13, factura.getFormaPago());
            } else {
                stmtFactura.setString(13, "EFECTIVO"); // Por defecto EFECTIVO
            }
            
            int affectedRows = stmtFactura.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("La creación de la factura falló, no se insertaron filas.");
            }
            
            // Obtener el ID generado
            try (ResultSet generatedKeys = stmtFactura.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    factura.setId(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("La creación de la factura falló, no se obtuvo el ID.");
                }
            }
            
            // Insertar los items de la factura
            String sqlItems = "INSERT INTO items_factura (factura_id, producto_id, cantidad, precio_unitario, subtotal) " +
                            "VALUES (?, ?, ?, ?, ?)";
            
            stmtItems = conn.prepareStatement(sqlItems, Statement.RETURN_GENERATED_KEYS);
            
            for (ItemFactura item : factura.getItems()) {
                stmtItems.setLong(1, factura.getId());
                stmtItems.setLong(2, item.getProducto().getIdProducto());
                stmtItems.setInt(3, item.getCantidad());
                stmtItems.setDouble(4, item.getPrecioUnitario());
                stmtItems.setDouble(5, item.getSubtotal());
                
                stmtItems.addBatch();
            }
            
            stmtItems.executeBatch();
            
            // Confirmar la transacción
            conn.commit();
            
            return factura;
            
        } catch (SQLException e) {
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (stmtItems != null) {
                    stmtItems.close();
                }
                if (stmtFactura != null) {
                    stmtFactura.close();
                }
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Actualiza una factura existente
     * @param factura Factura con datos actualizados
     * @return true si la actualización fue exitosa
     */
    public boolean update(Factura factura) {
        String sql = "UPDATE facturas SET anulada = ?, motivo_anulacion = ?, fecha_anulacion = ?, " +
                    "forma_pago = ? WHERE id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setBoolean(1, factura.isAnulada());
            
            if (factura.getMotivoAnulacion() != null) {
                stmt.setString(2, factura.getMotivoAnulacion());
            } else {
                stmt.setNull(2, Types.VARCHAR);
            }
            
            if (factura.getFechaAnulacion() != null) {
                stmt.setTimestamp(3, Timestamp.valueOf(factura.getFechaAnulacion()));
            } else {
                stmt.setNull(3, Types.TIMESTAMP);
            }
            
            stmt.setString(4, factura.getFormaPago());
            stmt.setLong(5, factura.getId());
            
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Busca una factura por su ID
     * @param id ID de la factura
     * @return Factura encontrada o null si no existe
     */
    public Factura findById(Long id) {
        String sql = "SELECT * FROM facturas WHERE id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                Factura factura = mapResultSetToFactura(rs);
                factura.setItems(findItemsByFacturaId(factura.getId()));
                return factura;
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Busca una factura por su número
     * @param numeroFactura Número de factura
     * @return Factura encontrada o null si no existe
     */
    public Factura findByNumero(String numeroFactura) {
        String sql = "SELECT * FROM facturas WHERE numero_factura = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, numeroFactura);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                Factura factura = mapResultSetToFactura(rs);
                factura.setItems(findItemsByFacturaId(factura.getId()));
                return factura;
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Obtiene todas las facturas de un cliente
     * @param clienteId ID del cliente
     * @return Lista de facturas del cliente
     */
    public List<Factura> findByClienteId(Long clienteId) {
        List<Factura> facturas = new ArrayList<>();
        String sql = "SELECT * FROM facturas WHERE cliente_id = ? ORDER BY fecha DESC";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, clienteId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Factura factura = mapResultSetToFactura(rs);
                factura.setItems(findItemsByFacturaId(factura.getId()));
                facturas.add(factura);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return facturas;
    }
    
    /**
     * Obtiene todas las facturas de un vendedor
     * @param vendedorId ID del vendedor
     * @return Lista de facturas del vendedor
     */
    public List<Factura> findByVendedorId(Long vendedorId) {
        List<Factura> facturas = new ArrayList<>();
        String sql = "SELECT * FROM facturas WHERE vendedor_id = ? ORDER BY fecha DESC";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, vendedorId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Factura factura = mapResultSetToFactura(rs);
                factura.setItems(findItemsByFacturaId(factura.getId()));
                facturas.add(factura);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return facturas;
    }
    
    /**
     * Obtiene las facturas emitidas en un rango de fechas
     * @param fechaInicio Fecha de inicio
     * @param fechaFin Fecha de fin
     * @return Lista de facturas en el rango
     */
    public List<Factura> findByFechaBetween(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        List<Factura> facturas = new ArrayList<>();
        String sql = "SELECT * FROM facturas WHERE fecha BETWEEN ? AND ? ORDER BY fecha DESC";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setTimestamp(1, Timestamp.valueOf(fechaInicio));
            stmt.setTimestamp(2, Timestamp.valueOf(fechaFin));
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Factura factura = mapResultSetToFactura(rs);
                factura.setItems(findItemsByFacturaId(factura.getId()));
                facturas.add(factura);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return facturas;
    }
    
    /**
     * Obtiene los items de una factura
     * @param facturaId ID de la factura
     * @return Lista de items de la factura
     */
    private List<ItemFactura> findItemsByFacturaId(Long facturaId) {
        List<ItemFactura> items = new ArrayList<>();
        String sql = "SELECT * FROM items_factura WHERE factura_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, facturaId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                ItemFactura item = new ItemFactura();
                item.setId(rs.getLong("id"));
                
                Long productoId = rs.getLong("producto_id");
                ProductoInventario producto = inventarioRepository.findById(productoId);
                item.setProducto(producto);
                
                item.setCantidad(rs.getInt("cantidad"));
                item.setPrecioUnitario(rs.getDouble("precio_unitario"));
                item.setSubtotal(rs.getDouble("subtotal"));
                
                items.add(item);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return items;
    }
    
    /**
     * Convierte un ResultSet en un objeto Factura
     */
    private Factura mapResultSetToFactura(ResultSet rs) throws SQLException {
        Factura factura = new Factura();
        factura.setId(rs.getLong("id"));
        factura.setNumeroFactura(rs.getString("numero_factura"));
        
        Long clienteId = rs.getLong("cliente_id");
        Cliente cliente = clienteRepository.findById(clienteId);
        factura.setCliente(cliente);
        
        Long vendedorId = rs.getLong("vendedor_id");
        Usuario vendedor = usuarioRepository.findById(vendedorId);
        factura.setVendedor(vendedor);
        
        factura.setFecha(rs.getTimestamp("fecha").toLocalDateTime());
        
        // Obtener orden_id si no es null
        Object ordenId = rs.getObject("orden_id");
        if (ordenId != null) {
            factura.setOrdenId(rs.getLong("orden_id"));
        }
        
        factura.setSubtotal(rs.getDouble("subtotal"));
        factura.setDescuento(rs.getDouble("descuento"));
        factura.setIva(rs.getDouble("iva"));
        factura.setTotal(rs.getDouble("total"));
        factura.setAnulada(rs.getBoolean("anulada"));
        factura.setMotivoAnulacion(rs.getString("motivo_anulacion"));
        
        Timestamp fechaAnulacion = rs.getTimestamp("fecha_anulacion");
        if (fechaAnulacion != null) {
            factura.setFechaAnulacion(fechaAnulacion.toLocalDateTime());
        }
        
        factura.setFormaPago(rs.getString("forma_pago"));
        
        return factura;
    }
}