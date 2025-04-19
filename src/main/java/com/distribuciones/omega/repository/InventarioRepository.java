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
        String sql = "CREATE TABLE IF NOT EXISTS inventario (" +
                    "id_inventario BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "producto_id VARCHAR(20) NOT NULL, " +
                    "stock INT NOT NULL DEFAULT 0, " +
                    "ubicacion VARCHAR(100), " +
                    "ultimo_reabastecimiento DATETIME, " +
                    "stock_minimo INT DEFAULT 5, " +
                    "stock_maximo INT DEFAULT 100, " +
                    "FOREIGN KEY (producto_id) REFERENCES productos(id) ON DELETE CASCADE" +
                    ")";
        
        try (Connection conn = DBUtil.getConnection();
            Statement stmt = conn.createStatement()) {
            
            stmt.executeUpdate(sql);
            System.out.println("Tabla de inventario creada o verificada correctamente");
            
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error al crear la tabla de inventario: " + e.getMessage());
        }
    }

    /**
     * Obtiene todos los productos del inventario
     * @return Lista de productos en inventario
     */
    public List<ProductoInventario> findAll() {
        List<ProductoInventario> productos = new ArrayList<>();
        String sql = "SELECT * FROM productos WHERE activo = true";
        
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                productos.add(mapResultSetToProducto(rs));
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return productos;
    }
    
    /**
     * Busca un producto por su código
     * @param codigo Código único del producto
     * @return Producto encontrado o null si no existe
     */
    public ProductoInventario findByCodigo(String codigo) {
        String sql = "SELECT * FROM productos WHERE codigo = ? AND activo = true";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, codigo);
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
    
    /**
     * Actualiza un producto existente
     * @param producto Producto con datos actualizados
     * @return true si la actualización fue exitosa
     */
    public boolean update(ProductoInventario producto) {
        String sql = "UPDATE productos SET descripcion = ?, precio = ?, cantidad = ?, " + // Cambié 'stock' por 'cantidad'
                     "numero_serie = ?, categoria = ?, proveedor = ?, activo = ? " +
                     "WHERE codigo = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, producto.getDescripcion());
            stmt.setDouble(2, producto.getPrecio());
            stmt.setInt(3, producto.getStock()); // Aquí stock se guarda en cantidad
            stmt.setString(4, producto.getNumeroSerie());
            stmt.setString(5, producto.getCategoria());
            stmt.setString(6, producto.getProveedor());
            stmt.setBoolean(7, producto.isActivo());
            stmt.setString(8, producto.getCodigo());
            
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
            
        } catch (SQLException e) {
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
        
        // Mapear desde la tabla productos
        if (hasColumn(rs, "id")) {
            String id = rs.getString("id");
            producto.setCodigo(id);
            // Usar el ID del producto como clave primaria para búsquedas futuras
            try {
                producto.setIdProducto(Long.parseLong(id));
            } catch (NumberFormatException e) {
                producto.setIdProducto(0L);
            }
        }
        
        if (hasColumn(rs, "nombre")) {
            producto.setDescripcion(rs.getString("nombre"));
        }
        
        if (hasColumn(rs, "precio")) {
            producto.setPrecio(rs.getDouble("precio"));
        }
        
        if (hasColumn(rs, "cantidad")) {
            producto.setStock(rs.getInt("cantidad"));
        } else if (hasColumn(rs, "stock")) {
            producto.setStock(rs.getInt("stock"));
        }
        
        if (hasColumn(rs, "categoria")) {
            producto.setCategoria(rs.getString("categoria"));
        }
        
        if (hasColumn(rs, "tipo_producto")) {
            producto.setProveedor(rs.getString("tipo_producto"));
        }
        
        if (hasColumn(rs, "activo")) {
            producto.setActivo(rs.getBoolean("activo"));
        } else {
            producto.setActivo(true);
        }
        
        // Datos específicos de inventario (si están presentes)
        if (hasColumn(rs, "ubicacion")) {
            producto.setUbicacion(rs.getString("ubicacion"));
        }
        
        if (hasColumn(rs, "stock_minimo")) {
            producto.setStockMinimo(rs.getInt("stock_minimo"));
        } else {
            producto.setStockMinimo(5); // Valor predeterminado
        }
        
        if (hasColumn(rs, "stock_maximo")) {
            producto.setStockMaximo(rs.getInt("stock_maximo"));
        } else {
            producto.setStockMaximo(100); // Valor predeterminado
        }
        
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