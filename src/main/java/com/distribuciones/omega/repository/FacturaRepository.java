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
     * Guarda un item de factura en la base de datos
     * @param item Item a guardar
     * @return true si el guardado fue exitoso
     */
    public boolean guardarItemFactura(ItemFactura item) {
        String sql = "INSERT INTO items_factura (factura_id, producto_id, cantidad, precio_unitario, descuento, subtotal) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, item.getFactura().getId());
            stmt.setLong(2, item.getProducto().getIdProducto());
            stmt.setInt(3, item.getCantidad());
            stmt.setDouble(4, item.getPrecioUnitario());
            stmt.setDouble(5, item.getDescuento()); // Añadido
            stmt.setDouble(6, item.getSubtotal());
            
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
            
        } catch (SQLException e) {
            System.err.println("Error al guardar item de factura: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Actualiza una factura existente
     * @param factura Factura con datos actualizados
     * @return true si la actualización fue exitosa
     */
    public boolean update(Factura factura) {
        String sql = "UPDATE facturas SET anulada = ?, motivo_anulacion = ?, fecha_anulacion = ?, " +
                    "forma_pago = ?, pagada = ?, fecha_pago = ? WHERE id_factura = ?";  // Cambiar 'id' por 'id_factura'
        
        try (Connection conn = DBUtil.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setBoolean(1, factura.isAnulada());
            
            // Campos que pueden ser null
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
            stmt.setBoolean(5, factura.isPagada());
            
            // Fecha de pago (puede ser null)
            if (factura.getFechaPago() != null) {
                stmt.setTimestamp(6, Timestamp.valueOf(factura.getFechaPago()));
            } else {
                stmt.setNull(6, Types.TIMESTAMP);
            }
            
            stmt.setLong(7, factura.getId());
            
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
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "SELECT * FROM facturas WHERE id_factura = ?")) {
                
            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Factura factura = mapResultSetToFactura(rs);
                    // Cargar los items de la factura
                    cargarItemsFactura(factura);
                    return factura;
                }
            }
            
            return null;
        } catch (SQLException e) {
            System.err.println("Error al buscar factura por ID: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
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
     * Busca los items de una factura por su ID
     * @param facturaId ID de la factura
     * @return Lista de items encontrados
     */
    public List<ItemFactura> findItemsByFacturaId(Long facturaId) {
        List<ItemFactura> items = new ArrayList<>();
        
        String sql = "SELECT i.*, p.id_producto, p.nombre " +
                    "FROM items_factura i " +
                    "JOIN productos p ON i.producto_id = p.id_producto " +
                    "WHERE i.factura_id = ?";
                    
        try (Connection conn = DBUtil.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
            pstmt.setLong(1, facturaId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ItemFactura item = new ItemFactura();
                    item.setId(rs.getLong("id"));
                    item.setFacturaId(facturaId);
                    
                    // Crear producto básico con información mínima
                    ProductoInventario producto = new ProductoInventario();
                    producto.setIdProducto(rs.getLong("id_producto"));
                    producto.setDescripcion(rs.getString("nombre"));  // Usando nombre en lugar de descripcion
                    
                    item.setProducto(producto);
                    item.setCantidad(rs.getInt("cantidad"));
                    item.setPrecioUnitario(rs.getDouble("precio_unitario"));
                    item.setSubtotal(rs.getDouble("subtotal"));
                    
                    items.add(item);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error al cargar items de factura: " + e.getMessage());
            e.printStackTrace();
        }
        
        return items;
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

    

    public void imprimirEstructuraTablaFacturas() {
        try (Connection conn = DBUtil.getConnection()) {
            System.out.println("===== ESTRUCTURA DE LA TABLA FACTURAS =====");
            
            // Obtener metadatos de la tabla
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("DESCRIBE facturas")) {
                
                System.out.println("Columnas en la tabla facturas:");
                while (rs.next()) {
                    String columnName = rs.getString("Field");
                    String columnType = rs.getString("Type");
                    String isNullable = rs.getString("Null");
                    String key = rs.getString("Key");
                    String defaultValue = rs.getString("Default");
                    
                    System.out.println(columnName + " | " + columnType + " | " + isNullable + " | " + key + " | " + defaultValue);
                }
            } catch (SQLException e) {
                // Código para manejar la excepción...
            }
            
            System.out.println("================================================");
        } catch (SQLException e) {
            System.err.println("Error al imprimir estructura de la tabla facturas: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Carga los items asociados a una factura
     * @param factura La factura a la que se cargarán los items
     */
    private void cargarItemsFactura(Factura factura) {
        if (factura == null) return;
        
        // Consulta modificada con los campos correctos de la tabla
        String sql = "SELECT i.*, p.id_producto, p.nombre " +
                     "FROM items_factura i " +
                     "JOIN productos p ON i.producto_id = p.id_producto " +
                     "WHERE i.factura_id = ?";
                         
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
            pstmt.setLong(1, factura.getId());
            List<ItemFactura> items = new ArrayList<>();
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ItemFactura item = new ItemFactura();
                    item.setId(rs.getLong("id"));
                    item.setFacturaId(factura.getId());
                    
                    // Crear producto básico con información mínima
                    ProductoInventario producto = new ProductoInventario();
                    producto.setIdProducto(rs.getLong("id_producto"));
                    // Usamos nombre en vez de descripcion
                    producto.setDescripcion(rs.getString("nombre"));
                    
                    item.setProducto(producto);
                    item.setCantidad(rs.getInt("cantidad"));
                    item.setPrecioUnitario(rs.getDouble("precio_unitario"));
                    item.setSubtotal(rs.getDouble("subtotal"));
                    
                    items.add(item);
                }
            }
            
            factura.setItems(items);
            System.out.println("Cargados " + items.size() + " items para la factura ID: " + factura.getId());
            
        } catch (SQLException e) {
            System.err.println("Error al cargar items de factura: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Convierte un ResultSet en un objeto Factura
     */
    private Factura mapResultSetToFactura(ResultSet rs) throws SQLException {
        Factura factura = new Factura();
        factura.setId(rs.getLong("id_factura"));
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
        factura.setPagada(rs.getBoolean("pagada"));
        
        Timestamp fechaPago = rs.getTimestamp("fecha_pago");
        if (fechaPago != null) {
            factura.setFechaPago(fechaPago.toLocalDateTime());
        }
        
        return factura;
    }

    /**
     * Actualiza el estado de pago de una factura
     * @param facturaId ID de la factura
     * @param pagada Estado de pago (true: pagada, false: pendiente)
     * @return true si la actualización fue exitosa
     */
    public boolean actualizarEstadoPago(Long facturaId, boolean pagada) {
        String sql = "UPDATE facturas SET pagada = ?, fecha_pago = ? WHERE id_factura = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setBoolean(1, pagada);
            
            // Si está pagada, registrar fecha actual; si no, NULL
            if (pagada) {
                pstmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            } else {
                pstmt.setNull(2, Types.TIMESTAMP);
            }
            
            pstmt.setLong(3, facturaId);
            
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
            
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar estado de pago de la factura: " + e.getMessage(), e);
        }
    }

    /**
     * Busca facturas por vendedor y rango de fechas
     * @param vendedorId ID del vendedor
     * @param fechaInicio Fecha de inicio del rango
     * @param fechaFin Fecha de fin del rango
     * @return Lista de facturas que cumplen con los criterios
     */
    public List<Factura> buscarFacturasPorVendedorYRango(Long vendedorId, LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        List<Factura> facturas = new ArrayList<>();
        String sql = "SELECT * FROM facturas WHERE id_vendedor = ? AND fecha_emision BETWEEN ? AND ? ORDER BY fecha_emision DESC";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, vendedorId);
            stmt.setTimestamp(2, Timestamp.valueOf(fechaInicio));
            stmt.setTimestamp(3, Timestamp.valueOf(fechaFin));
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Factura factura = mapearFacturaDesdeResultSet(rs);
                    facturas.add(factura);
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error al buscar facturas por vendedor y rango de fechas: " + e.getMessage());
        }
        
        return facturas;
    }
    
    /**
     * Busca facturas por rango de fechas
     * @param fechaInicio Fecha de inicio del rango
     * @param fechaFin Fecha de fin del rango
     * @return Lista de facturas que cumplen con los criterios
     */
    public List<Factura> buscarFacturasPorRango(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        List<Factura> facturas = new ArrayList<>();
        String sql = "SELECT * FROM facturas WHERE fecha_emision BETWEEN ? AND ? ORDER BY fecha_emision DESC";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setTimestamp(1, Timestamp.valueOf(fechaInicio));
            stmt.setTimestamp(2, Timestamp.valueOf(fechaFin));
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Factura factura = mapearFacturaDesdeResultSet(rs);
                    facturas.add(factura);
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error al buscar facturas por rango de fechas: " + e.getMessage());
        }
        
        return facturas;
    }
    
    /**
     * Mapea una factura desde un ResultSet
     * @param rs ResultSet con datos de la factura
     * @return Objeto Factura mapeado
     * @throws SQLException Si hay error al acceder a los datos
     */
    private Factura mapearFacturaDesdeResultSet(ResultSet rs) throws SQLException {
        Factura factura = new Factura();
        factura.setId(rs.getLong("id_factura"));
        factura.setNumeroFactura(rs.getString("numero_factura"));
        factura.setFecha(rs.getTimestamp("fecha_emision").toLocalDateTime());
        
        // Cargar cliente
        Long clienteId = rs.getLong("id_cliente");
        Cliente cliente = clienteRepository.findById(clienteId);
        factura.setCliente(cliente);
        
        // Cargar vendedor
        Long vendedorId = rs.getLong("id_vendedor");
        Usuario vendedor = usuarioRepository.findById(vendedorId);
        factura.setVendedor(vendedor);
        
        // Cargar otros campos
        factura.setSubtotal(rs.getDouble("subtotal"));
        factura.setDescuento(rs.getDouble("descuento"));
        factura.setIva(rs.getDouble("iva"));
        factura.setTotal(rs.getDouble("total"));
        factura.setAnulada(rs.getBoolean("anulada"));
        
        if (rs.getString("motivo_anulacion") != null) {
            factura.setMotivoAnulacion(rs.getString("motivo_anulacion"));
        }
        
        // Cargar items de la factura
        cargarItemsFactura(factura);
        
        return factura;
    }
    
    /**
     * Crea la tabla de facturas si no existe
     */
    public void createTableIfNotExists() {
        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false); // Iniciar transacción
            
            // Verificar estructura de la tabla productos
            boolean productosExiste = false;
            String idProductoColumna = "id"; // Por defecto
            String idDefinitionExacta = "VARCHAR(20)"; // Por defecto
            
            try (Statement checkStmt = conn.createStatement();
                 ResultSet rsCheck = checkStmt.executeQuery("SHOW COLUMNS FROM productos WHERE Field = 'id'")) {
                
                productosExiste = true;
                if (rsCheck.next()) {
                    idDefinitionExacta = rsCheck.getString("Type");
                    System.out.println("Clave primaria de productos: id (" + idDefinitionExacta + ")");
                }
            } catch (SQLException e) {
                System.out.println("Error al verificar tabla productos: " + e.getMessage());
                productosExiste = false;
            }
            
            // Si la tabla productos no existe, terminamos sin error
            if (!productosExiste) {
                System.out.println("La tabla 'productos' no existe. No se crearán tablas de facturas.");
                return; // Terminamos sin error
            }
            
            try (Statement stmt = conn.createStatement()) {
                // Desactivar verificación de claves foráneas temporalmente
                stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
                
                // Verificar si la tabla facturas existe
                boolean facturasExiste = false;
                try (ResultSet rsTables = stmt.executeQuery("SHOW TABLES LIKE 'facturas'")) {
                    facturasExiste = rsTables.next();
                }
                
                // Crear la tabla facturas si no existe
                if (!facturasExiste) {
                    System.out.println("Creando tabla facturas...");
                    String sqlFacturas = "CREATE TABLE facturas (" +
                        "id_factura INT AUTO_INCREMENT PRIMARY KEY, " +
                        "numero_factura VARCHAR(50) NOT NULL UNIQUE, " +
                        "cliente_id INT NOT NULL, " +
                        "vendedor_id INT NOT NULL, " +
                        "fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                        "orden_id INT, " +
                        "subtotal DECIMAL(10,2) NOT NULL, " +
                        "descuento DECIMAL(10,2) NOT NULL DEFAULT 0, " +
                        "iva DECIMAL(10,2) NOT NULL, " +
                        "total DECIMAL(10,2) NOT NULL, " +
                        "anulada BOOLEAN NOT NULL DEFAULT FALSE, " +
                        "motivo_anulacion VARCHAR(255), " +
                        "fecha_anulacion TIMESTAMP NULL, " +
                        "forma_pago VARCHAR(50) NOT NULL DEFAULT 'EFECTIVO', " +
                        "pagada BOOLEAN NOT NULL DEFAULT FALSE, " +
                        "fecha_pago TIMESTAMP NULL, " +
                        "INDEX (cliente_id), " +
                        "INDEX (vendedor_id)" +
                        ")";
                    
                    stmt.executeUpdate(sqlFacturas);
                } else {
                    System.out.println("La tabla facturas ya existe");
                }
                
                // Verificar si la tabla items_factura existe
                boolean itemsExiste = false;
                try (ResultSet rsTables = stmt.executeQuery("SHOW TABLES LIKE 'items_factura'")) {
                    itemsExiste = rsTables.next();
                }
                
                // Crear la tabla items_factura si no existe
                if (!itemsExiste) {
                    System.out.println("Creando tabla items_factura...");
                    String sqlItems = "CREATE TABLE items_factura (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY, " +
                        "factura_id INT NOT NULL, " +
                        "producto_id " + idDefinitionExacta + " NOT NULL, " +
                        "cantidad INT NOT NULL, " +
                        "precio_unitario DECIMAL(10,2) NOT NULL, " +
                        "subtotal DECIMAL(10,2) NOT NULL, " +
                        "INDEX (factura_id), " +
                        "INDEX (producto_id), " +
                        "FOREIGN KEY (factura_id) REFERENCES facturas(id_factura) ON DELETE CASCADE" +
                        ")";
                    
                    stmt.executeUpdate(sqlItems);
                    
                    // Intentar añadir la restricción de clave foránea por separado
                    try {
                        String addFKSQL = "ALTER TABLE items_factura " +
                                         "ADD CONSTRAINT fk_items_factura_producto " +
                                         "FOREIGN KEY (producto_id) REFERENCES productos(" + idProductoColumna + ") " +
                                         "ON DELETE RESTRICT";
                        
                        stmt.executeUpdate(addFKSQL);
                        System.out.println("Clave foránea a productos añadida exitosamente");
                    } catch (SQLException e) {
                        System.out.println("No se pudo añadir la clave foránea a productos: " + e.getMessage());
                        System.out.println("La tabla items_factura se ha creado pero sin la restricción de clave foránea");
                        // No lanzamos la excepción para permitir que la aplicación continúe
                    }
                } else {
                    System.out.println("La tabla items_factura ya existe");
                }
                
                // Reactivar verificación de claves foráneas
                stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
                
                conn.commit();
                System.out.println("Tablas facturas e items_factura verificadas correctamente");
                
            } catch (SQLException e) {
                if (conn != null) {
                    try {
                        conn.rollback();
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }
                throw e;
            } finally {
                if (conn != null) {
                    try {
                        // Asegurarse de que FOREIGN_KEY_CHECKS vuelva a 1
                        try (Statement stmt = conn.createStatement()) {
                            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
                        }
                        conn.setAutoCommit(true);
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al crear la tabla facturas: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error al crear la tabla facturas", e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

        /**
     * Obtiene el último número de factura que coincida con un patrón de fecha
     * @param fechaPattern Patrón de fecha en formato YYYYMMDD
     * @return Último número de factura o null si no existe ninguno
     */
    public String obtenerUltimoNumeroFactura(String fechaPattern) {
        String sql = "SELECT numero_factura FROM facturas WHERE numero_factura LIKE ? " +
                    "ORDER BY id_factura DESC LIMIT 1";
        
        try (Connection conn = DBUtil.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // Patrón para buscar facturas del día específico
            stmt.setString(1, "FACT-" + fechaPattern + "-%");
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("numero_factura");
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error al obtener último número de factura: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
}
