package com.distribuciones.omega.repository;

import com.distribuciones.omega.model.Cliente;
import com.distribuciones.omega.model.ItemOrden;
import com.distribuciones.omega.model.Orden;
import com.distribuciones.omega.model.ProductoInventario;
import com.distribuciones.omega.model.Usuario;
import com.distribuciones.omega.utils.DBUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Repositorio para operaciones CRUD de Órdenes
 */
public class OrdenRepository {
    
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final InventarioRepository inventarioRepository;
    
    public OrdenRepository() {
        this.clienteRepository = new ClienteRepository();
        this.usuarioRepository = new UsuarioRepository();
        this.inventarioRepository = new InventarioRepository();
    }

    /**
     * Guarda una nueva orden en la base de datos
     * @param orden Orden a guardar
     * @return Orden con ID asignado
     */
    public Orden save(Orden orden) {
        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);
            
            // 1. Insertar la orden
            String sqlOrden = "INSERT INTO ordenes (numero_orden, cliente_id, vendedor_id, fecha, " +
                            "cotizacion_id, subtotal, descuento, iva, total, facturada, factura_id) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            try (PreparedStatement stmt = conn.prepareStatement(sqlOrden, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, generarNumeroOrden());
                stmt.setLong(2, orden.getCliente().getIdCliente());
                stmt.setLong(3, orden.getVendedor().getIdUsuario());
                stmt.setTimestamp(4, Timestamp.valueOf(orden.getFecha()));
                stmt.setObject(5, orden.getCotizacionId());
                stmt.setDouble(6, orden.getSubtotal());
                stmt.setDouble(7, orden.getDescuento());
                stmt.setDouble(8, orden.getIva());
                stmt.setDouble(9, orden.getTotal());
                stmt.setBoolean(10, orden.isFacturada());
                stmt.setObject(11, orden.getFacturaId());
                
                stmt.executeUpdate();
                
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        orden.setId(generatedKeys.getLong(1));
                    } else {
                        throw new SQLException("La creación de la orden falló, no se obtuvo el ID.");
                    }
                }
            }
            
            // 2. Insertar los items de la orden
            if (orden.getItems() != null && !orden.getItems().isEmpty()) {
                String sqlItems = "INSERT INTO items_orden (orden_id, producto_id, cantidad, precio_unitario, subtotal) " +
                                "VALUES (?, ?, ?, ?, ?)";
                
                try (PreparedStatement stmt = conn.prepareStatement(sqlItems, Statement.RETURN_GENERATED_KEYS)) {
                    for (ItemOrden item : orden.getItems()) {
                        stmt.setLong(1, orden.getId());
                        stmt.setLong(2, item.getProducto().getIdProducto());
                        stmt.setInt(3, item.getCantidad());
                        stmt.setDouble(4, item.getPrecioUnitario());
                        stmt.setDouble(5, item.getSubtotal());
                        stmt.addBatch();
                    }
                    stmt.executeBatch();
                }
            }
            
            // 3. Si hay un ID de cotización, marcarla como convertida
            if (orden.getCotizacionId() != null) {
                String sqlUpdate = "UPDATE cotizaciones SET convertida_a_orden = true WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sqlUpdate)) {
                    stmt.setLong(1, orden.getCotizacionId());
                    stmt.executeUpdate();
                }
            }
            
            conn.commit();
            return orden;
            
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
     * Actualiza una orden existente
     * @param orden Orden con datos actualizados
     * @return true si la actualización fue exitosa
     */
    public boolean update(Orden orden) {
        String sql = "UPDATE ordenes SET cliente_id = ?, vendedor_id = ?, facturada = ?, " +
                   "factura_id = ? WHERE id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, orden.getCliente().getIdCliente());
            stmt.setLong(2, orden.getVendedor().getIdUsuario());
            stmt.setBoolean(3, orden.isFacturada());
            stmt.setObject(4, orden.getFacturaId());
            stmt.setLong(5, orden.getId());
            
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Busca una orden por su ID
     * @param id ID de la orden
     * @return Orden encontrada o null si no existe
     */
    public Orden findById(Long id) {
        String sql = "SELECT * FROM ordenes WHERE id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                Orden orden = mapResultSetToOrden(rs);
                orden.setItems(findItemsByOrdenId(orden.getId()));
                return orden;
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Obtiene todas las órdenes pendientes de facturación
     * @return Lista de órdenes pendientes
     */
    public List<Orden> findPendientes() {
        List<Orden> ordenes = new ArrayList<>();
        String sql = "SELECT * FROM ordenes WHERE facturada = false ORDER BY fecha DESC";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Orden orden = mapResultSetToOrden(rs);
                orden.setItems(findItemsByOrdenId(orden.getId()));
                ordenes.add(orden);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return ordenes;
    }
    
    /**
     * Obtiene la última orden creada
     * @return Última orden o null si no hay órdenes
     */
    public Orden findLast() {
        String sql = "SELECT * FROM ordenes ORDER BY id DESC LIMIT 1";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                Orden orden = mapResultSetToOrden(rs);
                orden.setItems(findItemsByOrdenId(orden.getId()));
                return orden;
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Obtiene los items de una orden
     * @param ordenId ID de la orden
     * @return Lista de items de la orden
     */
    private List<ItemOrden> findItemsByOrdenId(Long ordenId) {
        List<ItemOrden> items = new ArrayList<>();
        String sql = "SELECT * FROM items_orden WHERE orden_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, ordenId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                ItemOrden item = new ItemOrden();
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
     * Genera un número de orden
     * @return Número de orden generado
     */
    private String generarNumeroOrden() {
        // Formato: ORD-YYYYMMDD-XXXX donde XXXX es un número secuencial
        LocalDateTime now = LocalDateTime.now();
        String fecha = String.format("%04d%02d%02d", now.getYear(), now.getMonthValue(), now.getDayOfMonth());
        
        // Obtener el último número de orden del día
        String sql = "SELECT numero_orden FROM ordenes " +
                     "WHERE numero_orden LIKE 'ORD-" + fecha + "-%' " +
                     "ORDER BY id DESC LIMIT 1";
        
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            int secuencia = 1;
            
            if (rs.next()) {
                String ultimaOrden = rs.getString("numero_orden");
                String[] partes = ultimaOrden.split("-");
                if (partes.length == 3) {
                    try {
                        secuencia = Integer.parseInt(partes[2]) + 1;
                    } catch (NumberFormatException e) {
                        // Si hay error de formato, usar 1
                        secuencia = 1;
                    }
                }
            }
            
            return String.format("ORD-%s-%04d", fecha, secuencia);
            
        } catch (SQLException e) {
            e.printStackTrace();
            // En caso de error, generar un número aleatorio
            return String.format("ORD-%s-%04d", fecha, (int)(Math.random() * 9000) + 1000);
        }
    }
    
    /**
     * Convierte un ResultSet en un objeto Orden
     */
    private Orden mapResultSetToOrden(ResultSet rs) throws SQLException {
        Orden orden = new Orden();
        orden.setId(rs.getLong("id"));
        orden.setNumeroOrden(rs.getString("numero_orden"));
        
        Long clienteId = rs.getLong("cliente_id");
        Cliente cliente = clienteRepository.findById(clienteId);
        orden.setCliente(cliente);
        
        Long vendedorId = rs.getLong("vendedor_id");
        Usuario vendedor = usuarioRepository.findById(vendedorId);
        orden.setVendedor(vendedor);
        
        orden.setFecha(rs.getTimestamp("fecha").toLocalDateTime());
        
        // Obtener cotizacion_id si no es null
        Object cotizacionId = rs.getObject("cotizacion_id");
        if (cotizacionId != null) {
            orden.setCotizacionId(rs.getLong("cotizacion_id"));
        }
        
        orden.setSubtotal(rs.getDouble("subtotal"));
        orden.setDescuento(rs.getDouble("descuento"));
        orden.setIva(rs.getDouble("iva"));
        orden.setTotal(rs.getDouble("total"));
        orden.setFacturada(rs.getBoolean("facturada"));
        
        // Obtener factura_id si no es null
        Object facturaId = rs.getObject("factura_id");
        if (facturaId != null) {
            orden.setFacturaId(rs.getLong("factura_id"));
        }
        
        return orden;
    }
}