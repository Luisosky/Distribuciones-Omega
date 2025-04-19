package com.distribuciones.omega.repository;

import com.distribuciones.omega.model.Cliente;
import com.distribuciones.omega.model.Cotizacion;
import com.distribuciones.omega.model.ItemCotizacion;
import com.distribuciones.omega.model.ProductoInventario;
import com.distribuciones.omega.model.Usuario;
import com.distribuciones.omega.utils.DBUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Repositorio para operaciones CRUD de Cotizaciones
 */
public class CotizacionRepository {
    
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final InventarioRepository inventarioRepository;
    
    public CotizacionRepository() {
        this.clienteRepository = new ClienteRepository();
        this.usuarioRepository = new UsuarioRepository();
        this.inventarioRepository = new InventarioRepository();
    }

    /**
     * Guarda una nueva cotización en la base de datos
     * @param cotizacion Cotización a guardar
     * @return Cotización con ID asignado
     */
    public Cotizacion save(Cotizacion cotizacion) {
        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);
            
            // 1. Insertar la cotización
            String sqlCotizacion = "INSERT INTO cotizaciones (numero_cotizacion, cliente_id, vendedor_id, fecha, " +
                                 "subtotal, descuento, iva, total, convertida_a_orden) " +
                                 "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            try (PreparedStatement stmt = conn.prepareStatement(sqlCotizacion, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, generarNumeroCotizacion());
                stmt.setLong(2, cotizacion.getCliente().getIdCliente());
                stmt.setLong(3, cotizacion.getVendedor().getIdUsuario());
                stmt.setTimestamp(4, Timestamp.valueOf(cotizacion.getFecha()));
                stmt.setDouble(5, cotizacion.getSubtotal());
                stmt.setDouble(6, cotizacion.getDescuento());
                stmt.setDouble(7, cotizacion.getIva());
                stmt.setDouble(8, cotizacion.getTotal());
                stmt.setBoolean(9, cotizacion.isConvertidaAOrden());
                
                stmt.executeUpdate();
                
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        cotizacion.setId(generatedKeys.getLong(1));
                    } else {
                        throw new SQLException("La creación de la cotización falló, no se obtuvo el ID.");
                    }
                }
            }
            
            // 2. Insertar los items de la cotización
            if (cotizacion.getItems() != null && !cotizacion.getItems().isEmpty()) {
                String sqlItems = "INSERT INTO items_cotizacion (cotizacion_id, producto_id, cantidad, precio_unitario, subtotal) " +
                                "VALUES (?, ?, ?, ?, ?)";
                
                try (PreparedStatement stmt = conn.prepareStatement(sqlItems, Statement.RETURN_GENERATED_KEYS)) {
                    for (ItemCotizacion item : cotizacion.getItems()) {
                        stmt.setLong(1, cotizacion.getId());
                        stmt.setLong(2, item.getProducto().getIdProducto());
                        stmt.setInt(3, item.getCantidad());
                        stmt.setDouble(4, item.getPrecioUnitario());
                        stmt.setDouble(5, item.getSubtotal());
                        stmt.addBatch();
                    }
                    stmt.executeBatch();
                }
            }
            
            conn.commit();
            return cotizacion;
            
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
     * Busca una cotización por su ID
     * @param id ID de la cotización
     * @return Cotización encontrada o null si no existe
     */
    public Cotizacion findById(Long id) {
        String sql = "SELECT * FROM cotizaciones WHERE id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                Cotizacion cotizacion = mapResultSetToCotizacion(rs);
                cotizacion.setItems(findItemsByCotizacionId(cotizacion.getId()));
                return cotizacion;
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Obtiene todas las cotizaciones de un cliente
     * @param clienteId ID del cliente
     * @return Lista de cotizaciones del cliente
     */
    public List<Cotizacion> findByClienteId(Long clienteId) {
        List<Cotizacion> cotizaciones = new ArrayList<>();
        String sql = "SELECT * FROM cotizaciones WHERE cliente_id = ? ORDER BY fecha DESC";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, clienteId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Cotizacion cotizacion = mapResultSetToCotizacion(rs);
                cotizacion.setItems(findItemsByCotizacionId(cotizacion.getId()));
                cotizaciones.add(cotizacion);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return cotizaciones;
    }
    
    /**
     * Obtiene todas las cotizaciones de un vendedor
     * @param vendedorId ID del vendedor
     * @return Lista de cotizaciones del vendedor
     */
    public List<Cotizacion> findByVendedorId(Long vendedorId) {
        List<Cotizacion> cotizaciones = new ArrayList<>();
        String sql = "SELECT * FROM cotizaciones WHERE vendedor_id = ? ORDER BY fecha DESC";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, vendedorId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Cotizacion cotizacion = mapResultSetToCotizacion(rs);
                cotizacion.setItems(findItemsByCotizacionId(cotizacion.getId()));
                cotizaciones.add(cotizacion);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return cotizaciones;
    }
    
    /**
     * Obtiene la última cotización creada
     * @return Última cotización o null si no hay cotizaciones
     */
    public Cotizacion findLast() {
        String sql = "SELECT * FROM cotizaciones ORDER BY id DESC LIMIT 1";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                Cotizacion cotizacion = mapResultSetToCotizacion(rs);
                cotizacion.setItems(findItemsByCotizacionId(cotizacion.getId()));
                return cotizacion;
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Marca una cotización como convertida a orden
     * @param id ID de la cotización
     * @return true si la actualización fue exitosa
     */
    public boolean marcarComoConvertida(Long id) {
        String sql = "UPDATE cotizaciones SET convertida_a_orden = true WHERE id = ?";
        
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
     * Obtiene los items de una cotización
     * @param cotizacionId ID de la cotización
     * @return Lista de items de la cotización
     */
    private List<ItemCotizacion> findItemsByCotizacionId(Long cotizacionId) {
        List<ItemCotizacion> items = new ArrayList<>();
        String sql = "SELECT * FROM items_cotizacion WHERE cotizacion_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, cotizacionId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                ItemCotizacion item = new ItemCotizacion();
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
     * Genera un número de cotización
     * @return Número de cotización generado
     */
    private String generarNumeroCotizacion() {
        // Formato: COT-YYYYMMDD-XXXX donde XXXX es un número secuencial
        LocalDateTime now = LocalDateTime.now();
        String fecha = String.format("%04d%02d%02d", now.getYear(), now.getMonthValue(), now.getDayOfMonth());
        
        // Obtener el último número de cotización del día
        String sql = "SELECT numero_cotizacion FROM cotizaciones " +
                     "WHERE numero_cotizacion LIKE 'COT-" + fecha + "-%' " +
                     "ORDER BY id DESC LIMIT 1";
        
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            int secuencia = 1;
            
            if (rs.next()) {
                String ultimaCotizacion = rs.getString("numero_cotizacion");
                String[] partes = ultimaCotizacion.split("-");
                if (partes.length == 3) {
                    try {
                        secuencia = Integer.parseInt(partes[2]) + 1;
                    } catch (NumberFormatException e) {
                        // Si hay error de formato, usar 1
                        secuencia = 1;
                    }
                }
            }
            
            return String.format("COT-%s-%04d", fecha, secuencia);
            
        } catch (SQLException e) {
            e.printStackTrace();
            // En caso de error, generar un número aleatorio
            return String.format("COT-%s-%04d", fecha, (int)(Math.random() * 9000) + 1000);
        }
    }
    
    /**
     * Convierte un ResultSet en un objeto Cotizacion
     */
    private Cotizacion mapResultSetToCotizacion(ResultSet rs) throws SQLException {
        Cotizacion cotizacion = new Cotizacion();
        cotizacion.setId(rs.getLong("id"));
        cotizacion.setNumeroCotizacion(rs.getString("numero_cotizacion"));
        
        Long clienteId = rs.getLong("cliente_id");
        Cliente cliente = clienteRepository.findById(clienteId);
        cotizacion.setCliente(cliente);
        
        Long vendedorId = rs.getLong("vendedor_id");
        Usuario vendedor = usuarioRepository.findById(vendedorId);
        cotizacion.setVendedor(vendedor);
        
        cotizacion.setFecha(rs.getTimestamp("fecha").toLocalDateTime());
        cotizacion.setSubtotal(rs.getDouble("subtotal"));
        cotizacion.setDescuento(rs.getDouble("descuento"));
        cotizacion.setIva(rs.getDouble("iva"));
        cotizacion.setTotal(rs.getDouble("total"));
        cotizacion.setConvertidaAOrden(rs.getBoolean("convertida_a_orden"));
        
        return cotizacion;
    }
}