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
            
            // Verificar la existencia de la tabla items_cotizacion o detalle_cotizacion
            boolean tablaExiste = false;
            String nombreTablaItems = "";
            
            try (Statement stmt = conn.createStatement()) {
                // Verificar si existe items_cotizacion
                try (ResultSet rs = stmt.executeQuery("SHOW TABLES LIKE 'items_cotizacion'")) {
                    if (rs.next()) {
                        tablaExiste = true;
                        nombreTablaItems = "items_cotizacion";
                        System.out.println("Usando tabla: items_cotizacion");
                    }
                }
                
                // Si no existe, verificar si existe detalle_cotizacion
                if (!tablaExiste) {
                    try (ResultSet rs = stmt.executeQuery("SHOW TABLES LIKE 'detalle_cotizacion'")) {
                        if (rs.next()) {
                            tablaExiste = true;
                            nombreTablaItems = "detalle_cotizacion";
                            System.out.println("Usando tabla: detalle_cotizacion");
                        }
                    }
                }
                
                // Si ninguna tabla existe, crearla
                if (!tablaExiste) {
                    System.out.println("Creando tabla items_cotizacion...");
                    
                    // Obtener el tipo y nombre de la clave primaria de productos
                    String idTipo = "VARCHAR(20)"; // Por defecto
                    String idColumna = "id"; // Por defecto
                    
                    try (ResultSet rsColumns = stmt.executeQuery("DESCRIBE productos")) {
                        while (rsColumns.next()) {
                            if ("PRI".equals(rsColumns.getString("Key"))) {
                                idColumna = rsColumns.getString("Field");
                                idTipo = rsColumns.getString("Type");
                                break;
                            }
                        }
                    }
                    
                    String sql = "CREATE TABLE items_cotizacion (" +
                                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                                "cotizacion_id INT NOT NULL, " +
                                "producto_id " + idTipo + " NOT NULL, " +
                                "cantidad INT NOT NULL, " +
                                "precio_unitario DECIMAL(10,2) NOT NULL, " +
                                "subtotal DECIMAL(10,2) NOT NULL, " +
                                "INDEX (cotizacion_id), " +
                                "INDEX (producto_id), " +
                                "FOREIGN KEY (cotizacion_id) REFERENCES cotizaciones(id_cotizacion), " +
                                "FOREIGN KEY (producto_id) REFERENCES productos(" + idColumna + ")" +
                                ")";
                    
                    stmt.executeUpdate(sql);
                    nombreTablaItems = "items_cotizacion";
                    System.out.println("Tabla items_cotizacion creada exitosamente");
                }
            } catch (SQLException e) {
                System.err.println("Error al verificar o crear tabla de items: " + e.getMessage());
                throw e;
            }
            
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
                // Adaptar el SQL según la tabla que exista
                String sqlItems;
                if (nombreTablaItems.equals("items_cotizacion")) {
                    sqlItems = "INSERT INTO items_cotizacion (cotizacion_id, producto_id, cantidad, precio_unitario, subtotal) " +
                             "VALUES (?, ?, ?, ?, ?)";
                } else {
                    sqlItems = "INSERT INTO detalle_cotizacion (id_cotizacion, id, cantidad, precio_unitario, subtotal) " +
                             "VALUES (?, ?, ?, ?, ?)";
                }
                
                try (PreparedStatement stmt = conn.prepareStatement(sqlItems, Statement.RETURN_GENERATED_KEYS)) {
                    for (ItemCotizacion item : cotizacion.getItems()) {
                        stmt.setLong(1, cotizacion.getId());
                        stmt.setString(2, item.getProducto().getIdProducto().toString());  // Convertir a String si es necesario
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
     * Crea la tabla de cotizaciones si no existe
     */
    public void createTableIfNotExists() {
        try (Connection conn = DBUtil.getConnection()) {
            // Primero verificamos la estructura de la tabla productos
            boolean productosExiste = false;
            String idProductoTipo = "VARCHAR(20)"; // Tipo por defecto
            String idProductoColumna = "id"; // Columna por defecto - CAMBIADO a "id" para que coincida con la PK real
            
            try (Statement checkStmt = conn.createStatement();
                 ResultSet rsCheck = checkStmt.executeQuery("DESCRIBE productos")) {
                
                productosExiste = true; // Si llegamos aquí, la tabla existe
                
                // Verificar qué columna es la clave primaria y su tipo
                while (rsCheck.next()) {
                    String columnName = rsCheck.getString("Field");
                    String columnType = rsCheck.getString("Type");
                    String keyType = rsCheck.getString("Key");
                    
                    if ("PRI".equals(keyType)) {
                        // Esta es la columna de clave primaria
                        idProductoColumna = columnName;
                        idProductoTipo = columnType;
                        System.out.println("Clave primaria de productos: " + idProductoColumna + " (" + idProductoTipo + ")");
                        break;
                    }
                }
            } catch (SQLException e) {
                // La tabla productos no existe, será creada más tarde
                System.out.println("La tabla productos no existe todavía: " + e.getMessage());
                productosExiste = false;
            }
            
            // Imprimimos la estructura completa de productos para diagnóstico
            try (Statement stmtTemp = conn.createStatement();
                 ResultSet rsTemp = stmtTemp.executeQuery("DESCRIBE productos")) {
                
                System.out.println("Estructura de la tabla productos:");
                while (rsTemp.next()) {
                    String field = rsTemp.getString("Field");
                    String type = rsTemp.getString("Type");
                    String key = rsTemp.getString("Key");
                    System.out.println(field + " - " + type + " - " + key);
                }
            } catch (SQLException e) {
                System.out.println("Error al obtener estructura de productos: " + e.getMessage());
            }
            
            // Si la tabla productos no existe, no podemos continuar
            if (!productosExiste) {
                throw new SQLException("La tabla 'productos' no existe. Debe crear primero la tabla productos.");
            }
            
            // Verificar si las tablas de cotización ya existen
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rsCotizaciones = metaData.getTables(null, null, "cotizaciones", null);
            ResultSet rsItems = metaData.getTables(null, null, "items_cotizacion", null);
            
            // Eliminar tablas existentes para recrearlas si hay problemas
            boolean eliminarExistentes = false;
            
            if (rsCotizaciones.next() || rsItems.next()) {
                // Al menos una de las tablas existe, verificamos si hay problemas
                try (Statement testStmt = conn.createStatement()) {
                    // Intenta hacer una consulta simple para verificar si las tablas están bien
                    if (rsItems.next()) {
                        testStmt.executeQuery("SELECT * FROM items_cotizacion LIMIT 1");
                    }
                } catch (SQLException e) {
                    System.out.println("Error al verificar tablas de cotización existentes: " + e.getMessage());
                    eliminarExistentes = true;
                }
            }
            
            // Si hay que eliminar tablas existentes con problemas
            if (eliminarExistentes) {
                try (Statement dropStmt = conn.createStatement()) {
                    System.out.println("Eliminando tablas de cotización existentes con problemas...");
                    dropStmt.executeUpdate("DROP TABLE IF EXISTS items_cotizacion");
                    dropStmt.executeUpdate("DROP TABLE IF EXISTS cotizaciones");
                }
            }
            
            // Ahora creamos las tablas según corresponda
            try (Statement stmt = conn.createStatement()) {
                // Verificar si cotizaciones existe
                rsCotizaciones = metaData.getTables(null, null, "cotizaciones", null);
                if (!rsCotizaciones.next()) {
                    System.out.println("Creando tabla cotizaciones...");
                    String sqlCotizaciones = "CREATE TABLE cotizaciones (" +
                            "id_cotizacion INT AUTO_INCREMENT PRIMARY KEY, " +
                            "numero_cotizacion VARCHAR(50) NOT NULL UNIQUE, " +
                            "cliente_id INT NOT NULL, " +
                            "vendedor_id INT NOT NULL, " +
                            "fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                            "subtotal DECIMAL(10,2) NOT NULL, " +
                            "descuento DECIMAL(10,2) NOT NULL DEFAULT 0, " +
                            "iva DECIMAL(10,2) NOT NULL, " +
                            "total DECIMAL(10,2) NOT NULL, " +
                            "convertida_a_orden BOOLEAN NOT NULL DEFAULT FALSE, " +
                            "vigencia_dias INT DEFAULT 30, " +
                            "observaciones TEXT, " +
                            "FOREIGN KEY (cliente_id) REFERENCES clientes(id_cliente) ON DELETE RESTRICT" +
                            ")";
                    
                    stmt.executeUpdate(sqlCotizaciones);
                    System.out.println("Tabla cotizaciones creada con éxito");
                } else {
                    System.out.println("La tabla cotizaciones ya existe");
                }
                
                // Verificar si items_cotizacion existe
                rsItems = metaData.getTables(null, null, "items_cotizacion", null);
                if (!rsItems.next()) {
                    System.out.println("Creando tabla items_cotizacion...");
                    // Usar el tipo correcto según la tabla productos
                    String sqlItems = "CREATE TABLE items_cotizacion (" +
                            "id_item INT AUTO_INCREMENT PRIMARY KEY, " +
                            "cotizacion_id INT NOT NULL, " +
                            "producto_id " + idProductoTipo + " NOT NULL, " + // CORREGIDO: Usar el tipo exacto detectado
                            "cantidad INT NOT NULL, " +
                            "precio_unitario DECIMAL(10,2) NOT NULL, " +
                            "subtotal DECIMAL(10,2) NOT NULL, " +
                            "FOREIGN KEY (cotizacion_id) REFERENCES cotizaciones(id_cotizacion), " +
                            "FOREIGN KEY (producto_id) REFERENCES productos(" + idProductoColumna + ")" + // CORREGIDO: Usar columna PK correcta
                            ")";
                    
                    stmt.executeUpdate(sqlItems);
                    System.out.println("Tabla items_cotizacion creada con éxito");
                } else {
                    System.out.println("La tabla items_cotizacion ya existe");
                }
            }
            
            System.out.println("Tablas cotizaciones e items_cotizacion verificadas correctamente");
            
        } catch (SQLException e) {
            System.err.println("Error al crear las tablas de cotizaciones: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error al crear las tablas de cotizaciones", e);
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
                     "ORDER BY id_cotizacion DESC LIMIT 1";  // Cambiado de "id" a "id_cotizacion"
        
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
     * Actualiza una cotización existente
     * @param cotizacion Cotización con datos actualizados
     * @return true si la actualización fue exitosa
     */
    public boolean update(Cotizacion cotizacion) {
        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);
            
            // 1. Actualizar la cotización principal
            String sqlCotizacion = "UPDATE cotizaciones SET " +
                                 "cliente_id = ?, " +
                                 "vendedor_id = ?, " +
                                 "fecha = ?, " +
                                 "subtotal = ?, " +
                                 "descuento = ?, " +
                                 "iva = ?, " +
                                 "total = ?, " +
                                 "convertida_a_orden = ? " +
                                 "WHERE id_cotizacion = ?";  // Cambiado de "WHERE id = ?"
            
            try (PreparedStatement stmt = conn.prepareStatement(sqlCotizacion)) {
                stmt.setLong(1, cotizacion.getCliente().getIdCliente());
                stmt.setLong(2, cotizacion.getVendedor().getIdUsuario());
                stmt.setTimestamp(3, Timestamp.valueOf(cotizacion.getFecha()));
                stmt.setDouble(4, cotizacion.getSubtotal());
                stmt.setDouble(5, cotizacion.getDescuento());
                stmt.setDouble(6, cotizacion.getIva());
                stmt.setDouble(7, cotizacion.getTotal());
                stmt.setBoolean(8, cotizacion.isConvertidaAOrden());
                stmt.setLong(9, cotizacion.getId());
                
                int affectedRows = stmt.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("La actualización de la cotización falló, no se encontró la cotización con ID: " + cotizacion.getId());
                }
            }
            
            // Verificar qué tabla de detalles existe y usar esa
            boolean itemsCotizacionExists = false;
            try (Statement checkStmt = conn.createStatement()) {
                try (ResultSet rs = checkStmt.executeQuery("SHOW TABLES LIKE 'items_cotizacion'")) {
                    itemsCotizacionExists = rs.next();
                }
            }
            
            String tableName = itemsCotizacionExists ? "items_cotizacion" : "detalle_cotizacion";
            String idColumnName = itemsCotizacionExists ? "cotizacion_id" : "id_cotizacion";
            
            // 2. Eliminar los items existentes
            String sqlDeleteItems = "DELETE FROM " + tableName + " WHERE " + idColumnName + " = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sqlDeleteItems)) {
                stmt.setLong(1, cotizacion.getId());
                stmt.executeUpdate();
            }
            
            // 3. Insertar los nuevos items
            if (cotizacion.getItems() != null && !cotizacion.getItems().isEmpty()) {
                String sqlItems;
                if (itemsCotizacionExists) {
                    sqlItems = "INSERT INTO items_cotizacion (cotizacion_id, producto_id, cantidad, precio_unitario, subtotal) " +
                            "VALUES (?, ?, ?, ?, ?)";
                } else {
                    sqlItems = "INSERT INTO detalle_cotizacion (id_cotizacion, id, cantidad, precio_unitario, subtotal) " +
                            "VALUES (?, ?, ?, ?, ?)";
                }
                
                try (PreparedStatement stmt = conn.prepareStatement(sqlItems)) {
                    for (ItemCotizacion item : cotizacion.getItems()) {
                        stmt.setLong(1, cotizacion.getId());
                        stmt.setString(2, item.getProducto().getIdProducto().toString());
                        stmt.setInt(3, item.getCantidad());
                        stmt.setDouble(4, item.getPrecioUnitario());
                        stmt.setDouble(5, item.getSubtotal());
                        stmt.addBatch();
                    }
                    stmt.executeBatch();
                }
            }
            
            conn.commit();
            return true;
            
        } catch (SQLException e) {
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
            return false;
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