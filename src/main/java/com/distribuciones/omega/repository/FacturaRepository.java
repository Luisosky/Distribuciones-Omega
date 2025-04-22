package com.distribuciones.omega.repository;

import com.distribuciones.omega.model.*;
import com.distribuciones.omega.utils.DBUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
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
        PreparedStatement stmtDetalle = null;
        
        // Añadir diagnóstico
        System.out.println("Guardando factura: " + factura.getNumeroFactura());
        System.out.println("Número de ítems a guardar: " + (factura.getItems() != null ? factura.getItems().size() : 0));
        
        if (factura.getItems() == null || factura.getItems().isEmpty()) {
            System.out.println("ADVERTENCIA: La factura no tiene ítems para guardar!");
        } else {
            for (int i = 0; i < factura.getItems().size(); i++) {
                ItemFactura item = factura.getItems().get(i);
                System.out.println("Ítem " + (i+1) + ": " + 
                                "Producto=" + (item.getProducto() != null ? item.getProducto().getDescripcion() : "NULL") + 
                                ", Código=" + (item.getProducto() != null ? item.getProducto().getCodigo() : "NULL") +
                                ", Cantidad=" + item.getCantidad() + 
                                ", Subtotal=" + item.getSubtotal());
            }
        }
        
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
                    System.out.println("Factura creada con ID: " + factura.getId());
                } else {
                    throw new SQLException("La creación de la factura falló, no se obtuvo el ID.");
                }
            }
            
            // Verificar si hay ítems para guardar
            if (factura.getItems() != null && !factura.getItems().isEmpty()) {
                // 1. Insertar en items_factura
                System.out.println("Intentando guardar " + factura.getItems().size() + " ítems en items_factura...");
                
                String sqlItems = "INSERT INTO items_factura (factura_id, producto_id, cantidad, precio_unitario, descuento, subtotal) " +
                            "VALUES (?, ?, ?, ?, ?, ?)";
                
                stmtItems = conn.prepareStatement(sqlItems);
                
                for (ItemFactura item : factura.getItems()) {
                    try {
                        stmtItems.setLong(1, factura.getId());
                        
                        // Verificar que el producto no sea null y tenga un código válido
                        if (item.getProducto() == null) {
                            System.out.println("ADVERTENCIA: Item con producto NULL, saltando...");
                            continue;
                        }
                        
                        // Preparar los campos usando getCodigo() en lugar de getId()
                        stmtItems.setString(2, item.getProducto().getCodigo());
                        stmtItems.setInt(3, item.getCantidad());
                        stmtItems.setDouble(4, item.getPrecioUnitario());
                        stmtItems.setDouble(6, item.getSubtotal());
                        
                        stmtItems.addBatch();
                        System.out.println("Ítem añadido al batch: producto=" + item.getProducto().getCodigo());
                    } catch (Exception e) {
                        System.err.println("Error al preparar ítem para items_factura: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                
                try {
                    int[] batchResults = stmtItems.executeBatch();
                    System.out.println("Resultados del batch items_factura: " + Arrays.toString(batchResults));
                } catch (SQLException e) {
                    System.err.println("Error al ejecutar batch de items_factura: " + e.getMessage());
                    e.printStackTrace();
                    // No lanzamos la excepción para permitir la segunda inserción
                }
                
                // 2. También insertar en detalle_factura para compatibilidad
                System.out.println("Intentando guardar " + factura.getItems().size() + " ítems en detalle_factura...");
                
                try {
                    String sqlDetalle = "INSERT INTO detalle_factura (id_factura, id, cantidad, precio_unitario, subtotal) " +
                                    "VALUES (?, ?, ?, ?, ?)";
                    
                    stmtDetalle = conn.prepareStatement(sqlDetalle);
                    
                    for (ItemFactura item : factura.getItems()) {
                        try {
                            if (item.getProducto() == null) {
                                System.out.println("ADVERTENCIA: Item con producto NULL, saltando...");
                                continue;
                            }
                            
                            stmtDetalle.setLong(1, factura.getId());
                            stmtDetalle.setString(2, item.getProducto().getCodigo());
                            stmtDetalle.setInt(3, item.getCantidad());
                            stmtDetalle.setDouble(4, item.getPrecioUnitario());
                            stmtDetalle.setDouble(5, item.getSubtotal());
                            
                            stmtDetalle.addBatch();
                            System.out.println("Ítem añadido al batch detalle_factura: producto=" + item.getProducto().getCodigo());
                        } catch (Exception e) {
                            System.err.println("Error al preparar ítem para detalle_factura: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                    
                    try {
                        int[] batchResults = stmtDetalle.executeBatch();
                        System.out.println("Resultados del batch detalle_factura: " + Arrays.toString(batchResults));
                    } catch (SQLException e) {
                        System.err.println("Error al ejecutar batch de detalle_factura: " + e.getMessage());
                        e.printStackTrace();
                        // Continuamos aunque falle detalle_factura
                    }
                } catch (SQLException e) {
                    System.err.println("Error al crear prepared statement para detalle_factura: " + e.getMessage());
                    e.printStackTrace();
                    // Continuamos aunque falle detalle_factura
                }
            } else {
                System.out.println("No hay ítems para guardar en la factura " + factura.getNumeroFactura());
            }
            
            // Confirmar la transacción
            conn.commit();
            System.out.println("Transacción completada y confirmada exitosamente");
            
            return factura;
            
        } catch (SQLException e) {
            try {
                if (conn != null) {
                    conn.rollback();
                    System.out.println("Transacción revertida debido a un error");
                }
            } catch (SQLException ex) {
                System.err.println("Error al revertir la transacción: " + ex.getMessage());
                ex.printStackTrace();
            }
            System.err.println("Error al guardar factura: " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (stmtDetalle != null) stmtDetalle.close();
                if (stmtItems != null) stmtItems.close();
                if (stmtFactura != null) stmtFactura.close();
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                System.err.println("Error al cerrar recursos: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

        /**
     * Busca un producto por su código
     * @param codigo El código del producto a buscar
     * @return El producto encontrado o null si no existe
     */
    public ProductoInventario buscarProductoPorCodigo(String codigo) {
        if (codigo == null || codigo.isEmpty()) {
            return null;
        }
        
        String sql = "SELECT p.*, i.ubicacion, i.stock_minimo, i.stock_maximo " +
                    "FROM productos p " +
                    "LEFT JOIN inventario i ON p.id = i.producto_id " +
                    "WHERE p.id = ? OR p.codigo = ?";
        
        try (Connection conn = DBUtil.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, codigo);
            stmt.setString(2, codigo);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    ProductoInventario producto = new ProductoInventario();
                    
                    // Datos básicos del producto
                    producto.setCodigo(rs.getString("id"));
                    
                    try {
                        producto.setDescripcion(rs.getString("nombre"));
                    } catch (SQLException e) {
                        producto.setDescripcion("Producto " + codigo);
                    }
                    
                    try {
                        producto.setPrecio(rs.getDouble("precio"));
                    } catch (SQLException e) {
                        producto.setPrecio(0.0);
                    }
                    
                    // Intentar obtener campos de inventario si existen
                    try {
                        producto.setCantidad(rs.getInt("cantidad"));
                    } catch (SQLException e) {
                        try {
                            producto.setCantidad(rs.getInt("stock"));
                        } catch (SQLException ex) {
                            producto.setCantidad(0);
                        }
                    }
                    
                    try {
                        producto.setUbicacion(rs.getString("ubicacion"));
                    } catch (SQLException e) {
                        producto.setUbicacion("Almacén General");
                    }
                    
                    try {
                        producto.setStockMinimo(rs.getInt("stock_minimo"));
                    } catch (SQLException e) {
                        producto.setStockMinimo(5); // Valor por defecto
                    }
                    
                    try {
                        producto.setStockMaximo(rs.getInt("stock_maximo"));
                    } catch (SQLException e) {
                        producto.setStockMaximo(100); // Valor por defecto
                    }
                    
                    return producto;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al buscar producto por código: " + e.getMessage());
        }
        
        return null;
    }



    /**
     * Verifica la existencia y estructura de las tablas relacionadas con facturas
     */
    public void diagnosticarTablas() {
        try (Connection conn = DBUtil.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            
            // Verificar tabla facturas
            System.out.println("\n=== DIAGNÓSTICO DE TABLAS DE FACTURAS ===");
            
            ResultSet tablas = meta.getTables(null, null, "facturas", null);
            boolean facturasExiste = tablas.next();
            System.out.println("¿Tabla 'facturas' existe? " + (facturasExiste ? "SÍ" : "NO"));
            
            // Verificar items_factura
            tablas = meta.getTables(null, null, "items_factura", null);
            boolean itemsFacturaExiste = tablas.next();
            System.out.println("¿Tabla 'items_factura' existe? " + (itemsFacturaExiste ? "SÍ" : "NO"));
            
            // Verificar detalle_factura
            tablas = meta.getTables(null, null, "detalle_factura", null);
            boolean detalleFacturaExiste = tablas.next();
            System.out.println("¿Tabla 'detalle_factura' existe? " + (detalleFacturaExiste ? "SÍ" : "NO"));
            
            // Obtener estructura de items_factura si existe
            if (itemsFacturaExiste) {
                System.out.println("\nEstructura de 'items_factura':");
                ResultSet columnas = meta.getColumns(null, null, "items_factura", null);
                while (columnas.next()) {
                    String nombreColumna = columnas.getString("COLUMN_NAME");
                    String tipoColumna = columnas.getString("TYPE_NAME");
                    int tamanoColumna = columnas.getInt("COLUMN_SIZE");
                    System.out.println("   " + nombreColumna + " - " + tipoColumna + 
                                    (tamanoColumna > 0 ? "(" + tamanoColumna + ")" : ""));
                }
                
                // Verificar si hay datos
                try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS total FROM items_factura")) {
                    if (rs.next()) {
                        int total = rs.getInt("total");
                        System.out.println("   Total registros: " + total);
                    }
                } catch (SQLException e) {
                    System.out.println("   Error al contar registros: " + e.getMessage());
                }
            }
            
            // Obtener estructura de detalle_factura si existe
            if (detalleFacturaExiste) {
                System.out.println("\nEstructura de 'detalle_factura':");
                ResultSet columnas = meta.getColumns(null, null, "detalle_factura", null);
                while (columnas.next()) {
                    String nombreColumna = columnas.getString("COLUMN_NAME");
                    String tipoColumna = columnas.getString("TYPE_NAME");
                    int tamanoColumna = columnas.getInt("COLUMN_SIZE");
                    System.out.println("   " + nombreColumna + " - " + tipoColumna + 
                                    (tamanoColumna > 0 ? "(" + tamanoColumna + ")" : ""));
                }
                
                // Verificar si hay datos
                try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS total FROM detalle_factura")) {
                    if (rs.next()) {
                        int total = rs.getInt("total");
                        System.out.println("   Total registros: " + total);
                    }
                } catch (SQLException e) {
                    System.out.println("   Error al contar registros: " + e.getMessage());
                }
            }
            
            System.out.println("=======================================\n");
            
        } catch (SQLException e) {
            System.err.println("Error durante el diagnóstico: " + e.getMessage());
            e.printStackTrace();
        }
    }


        /**
     * Verifica y crea la tabla items_factura si no existe
     */
    public void crearTablaItemsFactura() {
        try (Connection conn = DBUtil.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet tablas = meta.getTables(null, null, "items_factura", null);
            
            if (!tablas.next()) {
                System.out.println("Creando tabla 'items_factura'...");
                
                try (Statement stmt = conn.createStatement()) {
                    // Desactivar temporalmente verificación de claves foráneas
                    stmt.execute("SET FOREIGN_KEY_CHECKS=0");
                    
                    String sql = "CREATE TABLE items_factura (" +
                                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                                "factura_id INT NOT NULL, " +
                                "producto_id VARCHAR(20) NOT NULL, " +
                                "cantidad INT NOT NULL, " +
                                "precio_unitario DECIMAL(10,2) NOT NULL, " +
                                "descuento DECIMAL(10,2) DEFAULT 0.00, " +
                                "subtotal DECIMAL(10,2) NOT NULL, " +
                                "INDEX (factura_id), " +
                                "INDEX (producto_id)" +
                                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
                    
                    stmt.executeUpdate(sql);
                    System.out.println("Tabla 'items_factura' creada exitosamente");
                    
                    // Restaurar verificación de claves foráneas
                    stmt.execute("SET FOREIGN_KEY_CHECKS=1");
                    
                    // Intentar añadir claves foráneas
                    try {
                        String alterSQL = "ALTER TABLE items_factura " +
                                        "ADD CONSTRAINT fk_items_factura_factura " +
                                        "FOREIGN KEY (factura_id) REFERENCES facturas(id_factura) ON DELETE CASCADE";
                        stmt.executeUpdate(alterSQL);
                        
                        alterSQL = "ALTER TABLE items_factura " +
                                "ADD CONSTRAINT fk_items_factura_producto " +
                                "FOREIGN KEY (producto_id) REFERENCES productos(id) ON DELETE CASCADE";
                        stmt.executeUpdate(alterSQL);
                        
                        System.out.println("Restricciones de clave foránea añadidas a 'items_factura'");
                    } catch (SQLException e) {
                        System.out.println("Advertencia: No se pudieron añadir claves foráneas: " + e.getMessage());
                    }
                }
            } else {
                System.out.println("La tabla 'items_factura' ya existe");
            }
        } catch (SQLException e) {
            System.err.println("Error al crear tabla 'items_factura': " + e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * Crea datos de ejemplo para una factura
     * @param facturaId ID de la factura a la que se añadirán ítems
     * @return true si se crearon correctamente
     */
    public boolean crearItemsEjemplo(Long facturaId) {
        System.out.println("Creando ítems de ejemplo para factura ID: " + facturaId);
        
        // Primero verificar si la factura existe
        try (Connection conn = DBUtil.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(
                "SELECT * FROM facturas WHERE id_factura = ?")) {
            
            pstmt.setLong(1, facturaId);
            ResultSet rs = pstmt.executeQuery();
            
            if (!rs.next()) {
                System.out.println("Error: La factura con ID " + facturaId + " no existe");
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Error al verificar factura: " + e.getMessage());
            return false;
        }
        
        // Buscar algunos productos para usar
        List<String> productosCodigos = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT id FROM productos LIMIT 3")) {
            
            while (rs.next()) {
                productosCodigos.add(rs.getString("id"));
            }
        } catch (SQLException e) {
            System.err.println("Error al buscar productos: " + e.getMessage());
            return false;
        }
        
        if (productosCodigos.isEmpty()) {
            System.out.println("Error: No se encontraron productos para crear ítems de ejemplo");
            return false;
        }
        
        // Insertar ítems en items_factura
        try (Connection conn = DBUtil.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO items_factura (factura_id, producto_id, cantidad, precio_unitario, descuento, subtotal) " +
                "VALUES (?, ?, ?, ?, ?, ?)")) {
            
            // Limpiar ítems previos si existen
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("DELETE FROM items_factura WHERE factura_id = " + facturaId);
            } catch (SQLException e) {
                System.out.println("Advertencia al limpiar ítems previos: " + e.getMessage());
            }
            
            // Insertar nuevos ítems
            int itemsCreados = 0;
            for (String codigo : productosCodigos) {
                double precio = 150.0 + (Math.random() * 350); // Precio aleatorio entre 150 y 500
                int cantidad = 1 + (int)(Math.random() * 5);   // Cantidad entre 1 y 5
                double descuento = Math.random() < 0.3 ? Math.random() * 50 : 0; // 30% probabilidad de descuento
                double subtotal = (precio * cantidad) - descuento;
                
                pstmt.setLong(1, facturaId);
                pstmt.setString(2, codigo);
                pstmt.setInt(3, cantidad);
                pstmt.setDouble(4, precio);
                pstmt.setDouble(5, descuento);
                pstmt.setDouble(6, subtotal);
                
                pstmt.addBatch();
                itemsCreados++;
            }
            
            int[] resultados = pstmt.executeBatch();
            int exitosos = 0;
            for (int resultado : resultados) {
                if (resultado > 0) exitosos++;
            }
            
            System.out.println("Items creados con éxito: " + exitosos + " de " + itemsCreados);
            
            // También crear en detalle_factura para compatibilidad
            try {
                PreparedStatement pstmtDetalle = conn.prepareStatement(
                    "INSERT INTO detalle_factura (id_factura, id, cantidad, precio_unitario, subtotal) " +
                    "VALUES (?, ?, ?, ?, ?)");
                
                // Limpiar ítems previos
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("DELETE FROM detalle_factura WHERE id_factura = " + facturaId);
                } catch (SQLException e) {
                    System.out.println("Advertencia al limpiar detalles previos: " + e.getMessage());
                }
                
                // Insertar en detalle_factura
                for (String codigo : productosCodigos) {
                    double precio = 150.0 + (Math.random() * 350);
                    int cantidad = 1 + (int)(Math.random() * 5);
                    double subtotal = precio * cantidad;
                    
                    pstmtDetalle.setLong(1, facturaId);
                    pstmtDetalle.setString(2, codigo);
                    pstmtDetalle.setInt(3, cantidad);
                    pstmtDetalle.setDouble(4, precio);
                    pstmtDetalle.setDouble(5, subtotal);
                    
                    pstmtDetalle.addBatch();
                }
                
                pstmtDetalle.executeBatch();
                System.out.println("Ítems también creados en detalle_factura");
                
                return true;
            } catch (SQLException e) {
                System.out.println("Advertencia: No se pudieron crear ítems en detalle_factura: " + e.getMessage());
                // Continuamos aunque falle detalle_factura
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("Error al crear ítems de ejemplo: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

        /**
     * Actualiza una factura existente en la base de datos
     * @param factura Factura con los datos actualizados
     * @return true si la actualización fue exitosa
     */
    public boolean update(Factura factura) {
        String sql = "UPDATE facturas SET " +
                    "numero_factura = ?, " +
                    "cliente_id = ?, " +
                    "vendedor_id = ?, " +
                    "fecha = ?, " +
                    "orden_id = ?, " +
                    "subtotal = ?, " +
                    "descuento = ?, " +
                    "iva = ?, " +
                    "total = ?, " +
                    "anulada = ?, " +
                    "motivo_anulacion = ?, " +
                    "fecha_anulacion = ?, " +
                    "forma_pago = ?, " +
                    "pagada = ?, " +
                    "fecha_pago = ? " +
                    "WHERE id_factura = ?";
        
        try (Connection conn = DBUtil.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // Establecer los valores de los parámetros
            stmt.setString(1, factura.getNumeroFactura());
            stmt.setLong(2, factura.getCliente().getIdCliente());
            stmt.setLong(3, factura.getVendedor().getIdUsuario());
            stmt.setTimestamp(4, Timestamp.valueOf(factura.getFecha() != null ? factura.getFecha() : LocalDateTime.now()));
            
            // El orden_id puede ser null
            if (factura.getOrdenId() != null) {
                stmt.setLong(5, factura.getOrdenId());
            } else {
                stmt.setNull(5, Types.BIGINT);
            }
            
            stmt.setDouble(6, factura.getSubtotal());
            stmt.setDouble(7, factura.getDescuento());
            stmt.setDouble(8, factura.getIva());
            stmt.setDouble(9, factura.getTotal());
            stmt.setBoolean(10, factura.isAnulada());
            
            // Motivo de anulación puede ser null
            if (factura.getMotivoAnulacion() != null) {
                stmt.setString(11, factura.getMotivoAnulacion());
            } else {
                stmt.setNull(11, Types.VARCHAR);
            }
            
            // Fecha de anulación puede ser null
            if (factura.getFechaAnulacion() != null) {
                stmt.setTimestamp(12, Timestamp.valueOf(factura.getFechaAnulacion()));
            } else {
                stmt.setNull(12, Types.TIMESTAMP);
            }
            
            // Forma de pago puede ser null
            if (factura.getFormaPago() != null) {
                stmt.setString(13, factura.getFormaPago());
            } else {
                stmt.setString(13, "EFECTIVO"); // Por defecto
            }
            
            stmt.setBoolean(14, factura.isPagada());
            
            // Fecha de pago puede ser null
            if (factura.getFechaPago() != null) {
                stmt.setTimestamp(15, Timestamp.valueOf(factura.getFechaPago()));
            } else {
                stmt.setNull(15, Types.TIMESTAMP);
            }
            
            // ID de la factura como último parámetro
            stmt.setLong(16, factura.getId());
            
            // Ejecutar la actualización
            int rowsAffected = stmt.executeUpdate();
            System.out.println("Factura actualizada: " + factura.getId() + " - Filas afectadas: " + rowsAffected);
            
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error al actualizar factura: " + e.getMessage());
            e.printStackTrace();
            return false;
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
            
            stmt.setLong(1, item.getFacturaId());
            stmt.setString(2, item.getProducto().getCodigo());
            stmt.setInt(3, item.getCantidad());
            stmt.setDouble(4, item.getPrecioUnitario());
            stmt.setDouble(5, 0.0); // Descuento por defecto
            stmt.setDouble(6, item.getSubtotal());
            
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
            
        } catch (SQLException e) {
            System.err.println("Error al guardar item de factura: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
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
        
        // Intentar primero una consulta simple sin JOIN
        String sqlSimple = "SELECT * FROM items_factura WHERE factura_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sqlSimple)) {
                
            pstmt.setLong(1, facturaId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ItemFactura item = new ItemFactura();
                    item.setId(rs.getLong("id"));
                    item.setFacturaId(facturaId);
                    
                    // Crear producto básico con información mínima
                    ProductoInventario producto = new ProductoInventario();
                    producto.setCodigo(rs.getString("producto_id"));
                    producto.setDescripcion("Producto " + rs.getString("producto_id")); // Descripción genérica
                    producto.setPrecio(rs.getDouble("precio_unitario"));
                    
                    item.setProducto(producto);
                    item.setCantidad(rs.getInt("cantidad"));
                    item.setPrecioUnitario(rs.getDouble("precio_unitario"));
                    
                    // Intentar obtener descuento si existe
                    try {
                        item.setDescuento(rs.getDouble("descuento"));
                    } catch (SQLException e) {
                        item.setDescuento(0.0);
                    }
                    
                    item.setSubtotal(rs.getDouble("subtotal"));
                    
                    items.add(item);
                }
                
                System.out.println("Cargados " + items.size() + " items de factura ID: " + facturaId);
            }
            
        } catch (SQLException e) {
            System.err.println("Error al cargar items de factura: " + e.getMessage());
            e.printStackTrace();
            
            // Si falla, intentar con detalle_factura como fallback
            String sqlDetalle = "SELECT * FROM detalle_factura WHERE id_factura = ?";
            
            try (Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sqlDetalle)) {
                    
                pstmt.setLong(1, facturaId);
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        ItemFactura item = new ItemFactura();
                        item.setId(rs.getLong("id_detalle"));
                        item.setFacturaId(facturaId);
                        
                        // Crear producto básico con información mínima
                        ProductoInventario producto = new ProductoInventario();
                        producto.setCodigo(rs.getString("id"));
                        producto.setDescripcion("Producto " + rs.getString("id")); // Descripción genérica
                        producto.setPrecio(rs.getDouble("precio_unitario"));
                        
                        item.setProducto(producto);
                        item.setCantidad(rs.getInt("cantidad"));
                        item.setPrecioUnitario(rs.getDouble("precio_unitario"));
                        item.setSubtotal(rs.getDouble("subtotal"));
                        
                        items.add(item);
                    }
                    
                    System.out.println("Fallback: Cargados " + items.size() + " items de detalle_factura ID: " + facturaId);
                }
                
            } catch (SQLException ex) {
                System.err.println("Error en fallback a detalle_factura: " + ex.getMessage());
                ex.printStackTrace();
            }
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
        
        // Ejecutar diagnóstico primero
        System.out.println("Diagnóstico antes de cargar ítems para factura ID: " + factura.getId());
        diagnosticarTablas();
        
        List<ItemFactura> items = new ArrayList<>();
        boolean tablaItemsExiste = false;
        boolean tablaDetalleExiste = false;
        
        // Verificar si las tablas existen
        try (Connection conn = DBUtil.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            
            ResultSet tablas = meta.getTables(null, null, "items_factura", null);
            tablaItemsExiste = tablas.next();
            
            tablas = meta.getTables(null, null, "detalle_factura", null);
            tablaDetalleExiste = tablas.next();
            
            if (!tablaItemsExiste && !tablaDetalleExiste) {
                System.out.println("ADVERTENCIA: Ninguna tabla de ítems existe. Creando tabla items_factura...");
                crearTablaItemsFactura();
                tablaItemsExiste = true;
            }
        } catch (SQLException e) {
            System.err.println("Error al verificar tablas: " + e.getMessage());
        }
        
        // 1. Intentar cargar desde items_factura si existe
        if (tablaItemsExiste) {
            String sqlSimple = "SELECT * FROM items_factura WHERE factura_id = ?";
            
            try (Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sqlSimple)) {
                
                pstmt.setLong(1, factura.getId());
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        ItemFactura item = new ItemFactura();
                        item.setId(rs.getLong("id"));
                        item.setFacturaId(factura.getId());
                        
                        String codigoProducto = rs.getString("producto_id");
                        
                        // Buscar datos completos del producto
                        ProductoInventario producto = inventarioRepository.buscarProductoPorCodigo(codigoProducto);
                        
                        if (producto == null) {
                            // Si no se encontró, crear un producto básico
                            producto = new ProductoInventario();
                            producto.setCodigo(codigoProducto);
                            producto.setDescripcion("Producto " + codigoProducto);
                            producto.setPrecio(rs.getDouble("precio_unitario"));
                        }
                        
                        item.setProducto(producto);
                        item.setCantidad(rs.getInt("cantidad"));
                        item.setPrecioUnitario(rs.getDouble("precio_unitario"));
                        
                        // Intentar obtener descuento si existe
                        try {
                            item.setDescuento(rs.getDouble("descuento"));
                        } catch (SQLException e) {
                            item.setDescuento(0.0);
                        }
                        
                        item.setSubtotal(rs.getDouble("subtotal"));
                        
                        items.add(item);
                    }
                    
                    System.out.println("Cargados " + items.size() + " items desde items_factura para la factura ID: " + factura.getId());
                }
                
            } catch (SQLException e) {
                System.err.println("Error al cargar desde items_factura: " + e.getMessage());
            }
        }
        
        // 2. Si no se encontraron items, intentar con detalle_factura
        if (items.isEmpty() && tablaDetalleExiste) {
            String sqlDetalle = "SELECT * FROM detalle_factura WHERE id_factura = ?";
            
            try (Connection conn = DBUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sqlDetalle)) {
                
                pstmt.setLong(1, factura.getId());
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        ItemFactura item = new ItemFactura();
                        
                        // Intentar obtener id con diferentes nombres
                        try {
                            item.setId(rs.getLong("id_detalle"));
                        } catch (SQLException e) {
                            try {
                                item.setId(rs.getLong("id"));
                            } catch (SQLException ex) {
                                item.setId(0L);
                            }
                        }
                        
                        item.setFacturaId(factura.getId());
                        
                        // Intentar obtener el código del producto
                        String codigoProducto = null;
                        try {
                            codigoProducto = rs.getString("id_producto");
                        } catch (SQLException e) {
                            try {
                                codigoProducto = rs.getString("id");
                            } catch (SQLException ex) {
                                try {
                                    codigoProducto = rs.getString("producto_id");
                                } catch (SQLException e2) {
                                    codigoProducto = "DESCONOCIDO-" + rs.getRow();
                                }
                            }
                        }
                        
                        // Buscar datos completos del producto
                        ProductoInventario producto = inventarioRepository.buscarProductoPorCodigo(codigoProducto);
                        
                        if (producto == null) {
                            // Si no se encontró, crear un producto básico
                            producto = new ProductoInventario();
                            producto.setCodigo(codigoProducto);
                            producto.setDescripcion("Producto " + codigoProducto);
                            
                            try {
                                producto.setPrecio(rs.getDouble("precio_unitario"));
                            } catch (SQLException e) {
                                producto.setPrecio(0.0);
                            }
                        }
                        
                        item.setProducto(producto);
                        
                        try {
                            item.setCantidad(rs.getInt("cantidad"));
                        } catch (SQLException e) {
                            item.setCantidad(1);
                        }
                        
                        try {
                            item.setPrecioUnitario(rs.getDouble("precio_unitario"));
                        } catch (SQLException e) {
                            item.setPrecioUnitario(producto.getPrecio());
                        }
                        
                        try {
                            item.setSubtotal(rs.getDouble("subtotal"));
                        } catch (SQLException e) {
                            item.setSubtotal(item.getCantidad() * item.getPrecioUnitario());
                        }
                        
                        items.add(item);
                    }
                    
                    System.out.println("Cargados " + items.size() + " items desde detalle_factura para la factura ID: " + factura.getId());
                }
                
            } catch (SQLException e) {
                System.err.println("Error al cargar desde detalle_factura: " + e.getMessage());
            }
        }
        
        // 3. Si todavía no hay ítems, crear datos de ejemplo para testing
        if (items.isEmpty()) {
            System.out.println("No se encontraron ítems. Creando datos de ejemplo para testing...");
            if (crearItemsEjemplo(factura.getId())) {
                // Recargar los ítems después de crear los de ejemplo
                items = findItemsByFacturaId(factura.getId());
            } else {
                System.out.println("Error al crear ítems de ejemplo. No se cargaron ítems.");
            }
        }
        factura.setItems(items);
        System.out.println("Total: Cargados " + items.size() + " items para la factura ID: " + factura.getId());
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
     * Limpia los ítems existentes de una factura
     * @param facturaId ID de la factura
     * @return true si la operación fue exitosa
     */
    public boolean limpiarItemsFactura(Long facturaId) {
        try (Connection conn = DBUtil.getConnection()) {
            // Limpiar items_factura
            try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM items_factura WHERE factura_id = ?")) {
                stmt.setLong(1, facturaId);
                stmt.executeUpdate();
            } catch (SQLException e) {
                System.out.println("Advertencia al limpiar items_factura: " + e.getMessage());
            }
            
            // Limpiar detalle_factura
            try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM detalle_factura WHERE id_factura = ?")) {
                stmt.setLong(1, facturaId);
                stmt.executeUpdate();
            } catch (SQLException e) {
                System.out.println("Advertencia al limpiar detalle_factura: " + e.getMessage());
            }
            
            return true;
        } catch (SQLException e) {
            System.err.println("Error al limpiar ítems de factura: " + e.getMessage());
            return false;
        }
    }

        /**
     * Guarda una lista de ítems para una factura
     * @param facturaId ID de la factura
     * @param items Lista de ítems a guardar
     * @return true si la operación fue exitosa
     */
    public boolean guardarItemsFactura(Long facturaId, List<ItemFactura> items) {
        if (items == null || items.isEmpty()) {
            return false;
        }
        
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            
            // 1. Insertar en items_factura
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO items_factura (factura_id, producto_id, cantidad, precio_unitario, descuento, subtotal) " +
                    "VALUES (?, ?, ?, ?, ?, ?)")) {
                
                for (ItemFactura item : items) {
                    stmt.setLong(1, facturaId);
                    stmt.setString(2, item.getProducto().getCodigo());
                    stmt.setInt(3, item.getCantidad());
                    stmt.setDouble(4, item.getPrecioUnitario());
                    
                    // Manejar descuento
                    double descuento = 0;
                    try {
                        descuento = item.getDescuento();
                    } catch (NullPointerException e) {
                        // Usar valor por defecto 0
                    }
                    stmt.setDouble(5, descuento);
                    
                    stmt.setDouble(6, item.getSubtotal());
                    
                    stmt.addBatch();
                }
                
                stmt.executeBatch();
            }
            
            // 2. También insertar en detalle_factura para compatibilidad
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO detalle_factura (id_factura, id, cantidad, precio_unitario, subtotal) " +
                    "VALUES (?, ?, ?, ?, ?)")) {
                
                for (ItemFactura item : items) {
                    stmt.setLong(1, facturaId);
                    stmt.setString(2, item.getProducto().getCodigo());
                    stmt.setInt(3, item.getCantidad());
                    stmt.setDouble(4, item.getPrecioUnitario());
                    stmt.setDouble(5, item.getSubtotal());
                    
                    stmt.addBatch();
                }
                
                stmt.executeBatch();
            } catch (SQLException e) {
                System.out.println("Advertencia al insertar en detalle_factura: " + e.getMessage());
                // Continuamos aunque falle la inserción en detalle_factura
            }
            
            conn.commit();
            System.out.println("Ítems guardados correctamente: " + items.size());
            
            return true;
        } catch (SQLException e) {
            System.err.println("Error al guardar ítems de factura: " + e.getMessage());
            e.printStackTrace();
            return false;
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
