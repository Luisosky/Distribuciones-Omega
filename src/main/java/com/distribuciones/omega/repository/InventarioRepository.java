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
        String sql = "UPDATE productos SET descripcion = ?, precio = ?, stock = ?, " +
                     "numero_serie = ?, categoria = ?, proveedor = ?, activo = ? " +
                     "WHERE codigo = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, producto.getDescripcion());
            stmt.setDouble(2, producto.getPrecio());
            stmt.setInt(3, producto.getStock());
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
        String sql = "UPDATE productos SET stock = ? WHERE codigo = ?";
        
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
    public List<ProductoInventario> findByStockLessThan(int umbral) {
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
        producto.setIdProducto(rs.getLong("id_producto"));
        producto.setCodigo(rs.getString("codigo"));
        producto.setDescripcion(rs.getString("descripcion"));
        producto.setPrecio(rs.getDouble("precio"));
        producto.setStock(rs.getInt("stock"));
        producto.setNumeroSerie(rs.getString("numero_serie"));
        producto.setCategoria(rs.getString("categoria"));
        producto.setProveedor(rs.getString("proveedor"));
        producto.setActivo(rs.getBoolean("activo"));
        return producto;
    }
}