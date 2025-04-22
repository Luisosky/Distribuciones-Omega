package com.distribuciones.omega.repository;

import com.distribuciones.omega.model.ProductoInventario;
import com.distribuciones.omega.utils.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repositorio para operaciones CRUD de Productos en Inventario
 */
public class InventarioRepository {


    /**
     * Crea la tabla de inventario si no existe
     */
    public void createTableIfNotExists() {
        try (Connection conn = DBUtil.getConnection()) {
            // Verificar si la tabla ya existe
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet rs = meta.getTables(null, null, "inventario", null);
            
            if (!rs.next()) {
                // La tabla no existe, crearla
                System.out.println("Creando tabla 'inventario'...");
                
                try (Statement stmt = conn.createStatement()) {
                    // Desactivar temporalmente la verificación de claves foráneas
                    stmt.execute("SET FOREIGN_KEY_CHECKS=0");
                    
                    String sql = "CREATE TABLE inventario (" +
                                 "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                                 "producto_id VARCHAR(20), " +
                                 "ubicacion VARCHAR(50) DEFAULT 'Almacén General', " +
                                 "stock_minimo INT DEFAULT 5, " +
                                 "stock_maximo INT DEFAULT 100, " +
                                 "ultima_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                                 "INDEX idx_producto_id (producto_id), " +  // Añadir índice mejora rendimiento
                                 "FOREIGN KEY (producto_id) REFERENCES productos(id) ON DELETE CASCADE" +
                                 ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
                    
                    stmt.executeUpdate(sql);
                    System.out.println("Tabla 'inventario' creada exitosamente");
                    
                    // Insertar datos iniciales desde la tabla productos
                    String insertSql = "INSERT INTO inventario (producto_id, ubicacion) " +
                                      "SELECT id, 'Almacén General' FROM productos";
                    
                    int filas = stmt.executeUpdate(insertSql);
                    System.out.println("Se insertaron " + filas + " registros en la tabla inventario");
                    
                    // Reactivar la verificación de claves foráneas
                    stmt.execute("SET FOREIGN_KEY_CHECKS=1");
                }
            } else {
                System.out.println("La tabla 'inventario' ya existe");
            }
        } catch (SQLException e) {
            System.err.println("Error al crear tabla 'inventario': " + e.getMessage());
            e.printStackTrace();
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
        
        String sql = "SELECT * FROM productos WHERE codigo = ? OR id = ?";
        
        try (Connection conn = DBUtil.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, codigo);
            stmt.setString(2, codigo);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    ProductoInventario producto = new ProductoInventario();
                    producto.setCodigo(rs.getString("id")); // Usar id como código
                    
                    try {
                        producto.setDescripcion(rs.getString("nombre"));
                    } catch (SQLException e) {
                        try {
                            producto.setDescripcion(rs.getString("descripcion"));
                        } catch (SQLException ex) {
                            producto.setDescripcion("Producto " + codigo);
                        }
                    }
                    
                    try {
                        producto.setPrecio(rs.getDouble("precio"));
                    } catch (SQLException e) {
                        producto.setPrecio(0.0);
                    }
                    
                    try {
                        producto.setCantidad(rs.getInt("cantidad"));
                    } catch (SQLException e) {
                        try {
                            producto.setCantidad(rs.getInt("stock"));
                        } catch (SQLException ex) {
                            producto.setCantidad(0);
                        }
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
     * Obtiene todos los productos del inventario
     * @return Lista de productos en inventario
     */
    public List<ProductoInventario> findAll() {
        List<ProductoInventario> productos = new ArrayList<>();
        
        System.out.println("DEBUG: InventarioRepository.findAll() - Buscando todos los productos");
        
        // Verificar primero si la tabla inventario existe
        boolean inventarioExists = false;
        try (Connection conn = DBUtil.getConnection();
             ResultSet tables = conn.getMetaData().getTables(null, null, "inventario", null)) {
            inventarioExists = tables.next();
            System.out.println("¿Tabla inventario existe? " + (inventarioExists ? "SÍ" : "NO"));
        } catch (SQLException e) {
            System.err.println("Error al verificar tabla inventario: " + e.getMessage());
        }
        
        // SQL adaptativo dependiendo de si existe la tabla inventario
        String sql;
        if (inventarioExists) {
            sql = "SELECT p.*, " +
                  "CASE " +
                  "  WHEN i.producto_id IS NOT NULL THEN i.ubicacion " +
                  "  ELSE 'Almacén General' " +
                  "END AS ubicacion " +
                  "FROM productos p " +
                  "LEFT JOIN inventario i ON p.id = i.producto_id";
        } else {
            // Consulta simplificada sin JOIN que funcionará aunque no exista la tabla inventario
            sql = "SELECT p.*, 'Almacén General' AS ubicacion FROM productos p";
        }
        
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            System.out.println("Ejecutando consulta para inventario...");
            
            while (rs.next()) {
                ProductoInventario producto = mapResultSetToProducto(rs);
                productos.add(producto);
                System.out.println("Producto encontrado: " + producto.getDescripcion() + 
                                   " (ID: " + producto.getCodigo() + 
                                   ", Stock: " + producto.getStock() + ")");
            }
            
            System.out.println("Total de productos procesados: " + productos.size());
            
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error al obtener productos: " + e.getMessage());
        }
        
        return productos;
    }
    
    /**
     * Busca un producto por su código
     * @param codigo Código del producto
     * @return ProductoInventario encontrado o null
     */
    public ProductoInventario findByCodigo(String codigo) {
        // Primero intentamos buscar por la columna id_producto
        String sql = "SELECT * FROM productos WHERE id_producto = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, codigo);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToProducto(rs);
            }
            
            // Si no encontramos por id_producto, intentamos con id
            sql = "SELECT * FROM productos WHERE id = ?";
            try (PreparedStatement stmt2 = conn.prepareStatement(sql)) {
                stmt2.setString(1, codigo);
                ResultSet rs2 = stmt2.executeQuery();
                
                if (rs2.next()) {
                    return mapResultSetToProducto(rs2);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error buscando producto por código: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Busca un producto por su ID
     * @param id ID del producto
     * @return Producto encontrado o null si no existe
     */
    public ProductoInventario findById(Long id) {
        String sql = "SELECT * FROM productos WHERE id_producto = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToProducto(rs);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    
    /**
     * Guarda un nuevo producto en el inventario
     * @param producto Producto a guardar
     * @return Producto con ID asignado
     */
    public ProductoInventario save(ProductoInventario producto) {
        String sql = "INSERT INTO productos (codigo, descripcion, precio, stock, numero_serie, categoria, proveedor, activo) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, producto.getCodigo());
            stmt.setString(2, producto.getDescripcion());
            stmt.setDouble(3, producto.getPrecio());
            stmt.setInt(4, producto.getStock());
            stmt.setString(5, producto.getNumeroSerie());
            stmt.setString(6, producto.getCategoria());
            stmt.setString(7, producto.getProveedor());
            stmt.setBoolean(8, producto.isActivo());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("La creación del producto falló, no se insertaron filas.");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    producto.setIdProducto(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("La creación del producto falló, no se obtuvo el ID.");
                }
            }
            
            return producto;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void imprimirEstructuraTablaProductos() {
        try (Connection conn = DBUtil.getConnection()) {
            System.out.println("===== ESTRUCTTURA DE LA TABLA PRODUCTOS =====");
            
            // Obtener metadatos de la tabla
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("DESCRIBE productos")) {
                
                System.out.println("Columnas en la tabla productos:");
                while (rs.next()) {
                    String columnName = rs.getString("Field");
                    String columnType = rs.getString("Type");
                    String isNullable = rs.getString("Null");
                    String key = rs.getString("Key");
                    String defaultValue = rs.getString("Default");
                    
                    System.out.println(columnName + " | " + columnType + " | " + isNullable + " | " + key + " | " + defaultValue);
                }
            } catch (SQLException e) {
                System.out.println("No se pudo ejecutar DESCRIBE. Intentando con metadatos de JDBC...");
                
                DatabaseMetaData metaData = conn.getMetaData();
                ResultSet columns = metaData.getColumns(null, null, "productos", null);
                
                System.out.println("Columnas en la tabla productos (via JDBC):");
                while (columns.next()) {
                    String columnName = columns.getString("COLUMN_NAME");
                    String columnType = columns.getString("TYPE_NAME");
                    int nullable = columns.getInt("NULLABLE");
                    
                    System.out.println(columnName + " | " + columnType + " | " + (nullable == 1 ? "YES" : "NO"));
                }
            }
            
            // Imprimir algunos registros de ejemplo
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM productos LIMIT 3")) {
                
                System.out.println("\nEjemplos de registros:");
                ResultSetMetaData rsmd = rs.getMetaData();
                int columnCount = rsmd.getColumnCount();
                
                // Nombres de columnas
                for (int i = 1; i <= columnCount; i++) {
                    System.out.print(rsmd.getColumnName(i) + "\t");
                }
                System.out.println();
                
                // Datos
                while (rs.next()) {
                    for (int i = 1; i <= columnCount; i++) {
                        System.out.print(rs.getObject(i) + "\t");
                    }
                    System.out.println();
                }
            }
            
            System.out.println("================================================");
        } catch (SQLException e) {
            System.err.println("Error al imprimir estructura de la tabla productos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Actualiza un producto en el inventario
     * @param producto Producto a actualizar
     * @return true si la actualización fue exitosa
     */
    public boolean update(ProductoInventario producto) {
        try (Connection conn = DBUtil.getConnection()) {
            // Basado en la estructura de tabla que se mostró en el diagnóstico
            // Usamos 'id' como clave primaria (no 'codigo')
            String sql = "UPDATE productos SET cantidad = ? WHERE id = ?";
            System.out.println("Ejecutando consulta: " + sql);
            System.out.println("Actualizando producto: ID=" + producto.getCodigo() + ", Nuevo stock=" + producto.getStock());
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, producto.getStock());
                stmt.setString(2, producto.getCodigo()); // Asumiendo que 'codigo' en tu objeto corresponde a 'id' en la BD
                
                int filasAfectadas = stmt.executeUpdate();
                System.out.println("Filas afectadas: " + filasAfectadas);
                
                if (filasAfectadas == 0) {
                    System.err.println("No se encontró el producto con ID=" + producto.getCodigo());
                    return false;
                }
                
                return filasAfectadas > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error al actualizar producto en base de datos: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Actualiza solo el stock de un producto
     * @param codigo Código del producto
     * @param nuevoStock Nuevo valor del stock
     * @return true si la actualización fue exitosa
     */
    public boolean updateStock(String codigo, int nuevoStock) {
        String sql = "UPDATE productos SET cantidad = ? WHERE codigo = ?"; // Cambié 'stock' por 'cantidad'
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, nuevoStock);
            stmt.setString(2, codigo);
            
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Busca productos con stock menor a un umbral
     * @param umbral Nivel mínimo de stock
     * @return Lista de productos con stock bajo
     */
    public List<ProductoInventario> findByStockLessThanActivo(int umbral) {
        List<ProductoInventario> productos = new ArrayList<>();
        String sql = "SELECT * FROM productos WHERE stock < ? AND activo = true";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, umbral);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                productos.add(mapResultSetToProducto(rs));
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return productos;
    }
    
    /**
     * Busca productos por categoría
     * @param categoria Categoría a buscar
     * @return Lista de productos de la categoría
     */
    public List<ProductoInventario> findByCategoria(String categoria) {
        List<ProductoInventario> productos = new ArrayList<>();
        String sql = "SELECT * FROM productos WHERE categoria = ? AND activo = true";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, categoria);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                productos.add(mapResultSetToProducto(rs));
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return productos;
    }
    
    /**
     * Convierte un ResultSet en un objeto ProductoInventario
     */
    private ProductoInventario mapResultSetToProducto(ResultSet rs) throws SQLException {
        ProductoInventario producto = new ProductoInventario();
        
        // Intentar obtener los campos según los nombres de columna que pueden existir
        try {
            producto.setIdProducto(rs.getLong("id_producto"));
        } catch (SQLException e) {
            // Si la columna no existe, intentar con "id"
            try {
                producto.setIdProducto(rs.getLong("id"));
            } catch (SQLException ex) {
                // Si ninguna columna existe, usar un valor predeterminado
                producto.setIdProducto(0L);
            }
        }
        
        // Obtener el resto de campos
        try {
            producto.setCodigo(rs.getString("codigo"));
        } catch (SQLException e) {
            // Si no hay columna 'codigo', intentar usar 'id' o 'id_producto' como código
            try {
                producto.setCodigo(rs.getString("id"));
            } catch (SQLException ex) {
                try {
                    producto.setCodigo(rs.getString("id_producto"));
                } catch (SQLException e2) {
                    producto.setCodigo("");
                }
            }
        }
        
        try {
            producto.setDescripcion(rs.getString("descripcion"));
        } catch (SQLException e) {
            // Intentar con "nombre"
            try {
                producto.setDescripcion(rs.getString("nombre"));
            } catch (SQLException ex) {
                producto.setDescripcion("");
            }
        }
        
        try {
            producto.setPrecio(rs.getDouble("precio"));
        } catch (SQLException e) {
            producto.setPrecio(0.0);
        }
        
        try {
            producto.setStock(rs.getInt("stock"));
        } catch (SQLException e) {
            // Intentar con "cantidad"
            try {
                producto.setStock(rs.getInt("cantidad"));
            } catch (SQLException ex) {
                producto.setStock(0);
            }
        }
        
        // Intentar obtener otros campos opcionales
        try { producto.setCategoria(rs.getString("categoria")); } catch (SQLException e) { }
        try { producto.setProveedor(rs.getString("proveedor")); } catch (SQLException e) { }
        try { producto.setActivo(rs.getBoolean("activo")); } catch (SQLException e) { }
        
        return producto;
    }

    /**
     * Verifica si un ResultSet tiene una columna específica
     */
    private boolean hasColumn(ResultSet rs, String columnName) {
        try {
            rs.findColumn(columnName);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Busca productos con stock menor que el umbral especificado
     * @param umbral Nivel mínimo de stock
     * @return Lista de productos con stock bajo
     */
    public List<ProductoInventario> findByStockLessThan(int umbral) {
        List<ProductoInventario> productosBajoStock = new ArrayList<>();
        
        // Consulta simplificada que no depende de la relación con categorías
        String sql = "SELECT * FROM productos WHERE cantidad < ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, umbral);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                productosBajoStock.add(mapResultSetToProductoSinCategoria(rs));
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return productosBajoStock;
    }

    /**
     * Mapea un ResultSet a un objeto ProductoInventario sin cargar datos de categoría
     */
    private ProductoInventario mapResultSetToProductoSinCategoria(ResultSet rs) throws SQLException {
        ProductoInventario producto = new ProductoInventario();
        
        // Modificar estas líneas para leer id como String
        String id = rs.getString("id");
        producto.setCodigo(id);
        
        // Opcionalmente, si realmente necesitas un ID numérico:
        try {
            producto.setIdProducto(Long.parseLong(id.replaceAll("[^0-9]", "")));
        } catch (NumberFormatException e) {
            // En caso de que no se pueda convertir, asignar un valor por defecto o usar otro campo
            producto.setIdProducto(0L);
        }
        
        producto.setDescripcion(rs.getString("nombre"));
        producto.setPrecio(rs.getDouble("precio"));
        
        // Mapear correctamente la cantidad al stock
        producto.setStock(rs.getInt("cantidad"));
        
        // Leer la categoría como String
        producto.setCategoria(rs.getString("categoria"));
        
        // Comprobar y mapear otros campos disponibles
        if (hasColumn(rs, "tipo_producto")) {
            producto.setProveedor(rs.getString("tipo_producto"));
        }
        
        if (hasColumn(rs, "activo")) {
            producto.setActivo(rs.getBoolean("activo"));
        } else {
            producto.setActivo(true);
        }
        
        return producto;
    }
}