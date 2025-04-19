package com.distribuciones.omega.repository;

import com.distribuciones.omega.model.*;
import com.distribuciones.omega.utils.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repositorio para operaciones CRUD de Productos
 */
public class ProductoRepository {

    /**
     * Inicializa la tabla si no existe
     */
    public void createTableIfNotExists() {
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Tabla base de productos
            String sqlProductos = "CREATE TABLE IF NOT EXISTS productos (" +
                             "id VARCHAR(20) PRIMARY KEY, " +
                             "nombre VARCHAR(100) NOT NULL, " +
                             "precio DOUBLE NOT NULL, " +
                             "cantidad INT NOT NULL, " +
                             "categoria VARCHAR(50) NOT NULL, " +
                             "tipo_producto VARCHAR(50) NOT NULL" +
                             ")";
            
            // Tabla para InsumoOficina
            String sqlInsumos = "CREATE TABLE IF NOT EXISTS insumos_oficina (" +
                             "producto_id VARCHAR(20) PRIMARY KEY, " +
                             "presentacion VARCHAR(50), " +
                             "tipo_papel VARCHAR(50), " +
                             "cantidad_por_paquete INT, " +
                             "FOREIGN KEY (producto_id) REFERENCES productos(id) ON DELETE CASCADE" +
                             ")";
            
            // Tabla para ProductoMobiliario
            String sqlMobiliarios = "CREATE TABLE IF NOT EXISTS productos_mobiliarios (" +
                             "producto_id VARCHAR(20) PRIMARY KEY, " +
                             "tipo_mobiliario VARCHAR(50), " +
                             "material VARCHAR(50), " +
                             "color VARCHAR(50), " +
                             "dimensiones VARCHAR(50), " +
                             "FOREIGN KEY (producto_id) REFERENCES productos(id) ON DELETE CASCADE" +
                             ")";
            
            // Tabla para ProductoTecnologico
            String sqlTecnologicos = "CREATE TABLE IF NOT EXISTS productos_tecnologicos (" +
                             "producto_id VARCHAR(20) PRIMARY KEY, " +
                             "marca VARCHAR(50), " +
                             "modelo VARCHAR(50), " +
                             "numero_serie VARCHAR(50), " +
                             "garantia_meses INT, " +
                             "especificaciones_tecnicas TEXT, " +
                             "FOREIGN KEY (producto_id) REFERENCES productos(id) ON DELETE CASCADE" +
                             ")";
            
            // Ejecutar todas las creaciones de tablas
            stmt.executeUpdate(sqlProductos);
            stmt.executeUpdate(sqlInsumos);
            stmt.executeUpdate(sqlMobiliarios);
            stmt.executeUpdate(sqlTecnologicos);
            
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error al crear las tablas de productos", e);
        }
    }
    
    /**
     * Guarda un nuevo producto
     * @param producto Producto a guardar
     * @return Producto guardado
     */
    public Producto save(Producto producto) {
        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            DBUtil.beginTransaction(conn);
            
            // 1. Guardar en tabla productos (datos comunes)
            String sqlProducto = "INSERT INTO productos (id, nombre, precio, cantidad, categoria, tipo_producto) " +
                                 "VALUES (?, ?, ?, ?, ?, ?)";
            
            try (PreparedStatement stmt = conn.prepareStatement(sqlProducto)) {
                stmt.setString(1, producto.getId());
                stmt.setString(2, producto.getNombre());
                stmt.setDouble(3, producto.getPrecio());
                stmt.setInt(4, producto.getCantidad());
                stmt.setString(5, producto.getCategoria().name());
                
                // 2. Determinar el tipo e insertar en la tabla específica
                if (producto instanceof InsumoOficina) {
                    InsumoOficina insumo = (InsumoOficina) producto;
                    stmt.setString(6, "InsumoOficina");
                    stmt.executeUpdate();
                    
                    String sqlInsumo = "INSERT INTO insumos_oficina (producto_id, presentacion, tipo_papel, cantidad_por_paquete) " +
                                      "VALUES (?, ?, ?, ?)";
                    try (PreparedStatement stmtInsumo = conn.prepareStatement(sqlInsumo)) {
                        stmtInsumo.setString(1, insumo.getId());
                        stmtInsumo.setString(2, insumo.getPresentacion());
                        stmtInsumo.setString(3, insumo.getTipoPapel());
                        stmtInsumo.setInt(4, insumo.getCantidadPorPaquete());
                        stmtInsumo.executeUpdate();
                    }
                } else if (producto instanceof ProductoMobilario) {
                    ProductoMobilario mobiliario = (ProductoMobilario) producto;
                    stmt.setString(6, "ProductoMobiliario");
                    stmt.executeUpdate();
                    
                    String sqlMobiliario = "INSERT INTO productos_mobiliarios (producto_id, tipo_mobiliario, material, color, dimensiones) " +
                                           "VALUES (?, ?, ?, ?, ?)";
                    try (PreparedStatement stmtMobiliario = conn.prepareStatement(sqlMobiliario)) {
                        stmtMobiliario.setString(1, mobiliario.getId());
                        stmtMobiliario.setString(2, mobiliario.getTipoMobilario());
                        stmtMobiliario.setString(3, mobiliario.getMaterial());
                        stmtMobiliario.setString(4, mobiliario.getColor());
                        stmtMobiliario.setString(5, mobiliario.getDimensiones());
                        stmtMobiliario.executeUpdate();
                    }
                } else if (producto instanceof ProductoTecnologico) {
                    ProductoTecnologico tecno = (ProductoTecnologico) producto;
                    stmt.setString(6, "ProductoTecnologico");
                    stmt.executeUpdate();
                    
                    String sqlTecnologico = "INSERT INTO productos_tecnologicos (producto_id, marca, modelo, numero_serie, garantia_meses, especificaciones_tecnicas) " +
                                            "VALUES (?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement stmtTecnologico = conn.prepareStatement(sqlTecnologico)) {
                        stmtTecnologico.setString(1, tecno.getId());
                        stmtTecnologico.setString(2, tecno.getMarca());
                        stmtTecnologico.setString(3, tecno.getModelo());
                        stmtTecnologico.setString(4, tecno.getNumeroSerie());
                        stmtTecnologico.setInt(5, tecno.getGarantiaMeses());
                        stmtTecnologico.setString(6, tecno.getEspecificacionesTecnicas());
                        stmtTecnologico.executeUpdate();
                    }
                } else {
                    stmt.setString(6, "Producto");
                    stmt.executeUpdate();
                }
            }
            
            DBUtil.commitTransaction(conn);
            return producto;
            
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    DBUtil.rollbackTransaction(conn);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw new RuntimeException("Error al guardar el producto", e);
        } finally {
            DBUtil.closeQuietly(conn);
        }
    }
    
    /**
     * Actualiza un producto existente
     * @param producto Producto con datos actualizados
     * @return true si la actualización fue exitosa
     */
    public boolean update(Producto producto) {
        try (Connection conn = DBUtil.getConnection()) {
            // Verificar que existe
            if (findById(producto.getId()) == null) {
                return false;
            }
            
            // Preparar SQL según el tipo de producto
            StringBuilder sql = new StringBuilder(
                "UPDATE productos SET nombre = ?, precio = ?, cantidad = ?, categoria = ?");
            
            // Añadir columnas específicas según el tipo
            if (producto instanceof InsumoOficina) {
                sql.append(", presentacion = ?, tipo_papel = ?, cantidad_por_paquete = ?");
            } else if (producto instanceof ProductoMobilario) {
                sql.append(", tipo_mobiliario = ?, material = ?, color = ?, dimensiones = ?");
            } else if (producto instanceof ProductoTecnologico) {
                sql.append(", marca = ?, modelo = ?, numero_serie = ?, garantia_meses = ?, especificaciones_tecnicas = ?");
            }
            
            sql.append(" WHERE id = ?");
            
            try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
                // Parámetros comunes
                stmt.setString(1, producto.getNombre());
                stmt.setDouble(2, producto.getPrecio());
                stmt.setInt(3, producto.getCantidad());
                stmt.setString(4, producto.getCategoria().name());
                
                int index = 5;
                
                // Parámetros específicos según el tipo
                if (producto instanceof InsumoOficina) {
                    InsumoOficina insumo = (InsumoOficina) producto;
                    stmt.setString(index++, insumo.getPresentacion());
                    stmt.setString(index++, insumo.getTipoPapel());
                    stmt.setInt(index++, insumo.getCantidadPorPaquete());
                } else if (producto instanceof ProductoMobilario) {
                    ProductoMobilario mobiliario = (ProductoMobilario) producto;
                    stmt.setString(index++, mobiliario.getTipoMobilario());
                    stmt.setString(index++, mobiliario.getMaterial());
                    stmt.setString(index++, mobiliario.getColor());
                    stmt.setString(index++, mobiliario.getDimensiones());
                } else if (producto instanceof ProductoTecnologico) {
                    ProductoTecnologico tecno = (ProductoTecnologico) producto;
                    stmt.setString(index++, tecno.getMarca());
                    stmt.setString(index++, tecno.getModelo());
                    stmt.setString(index++, tecno.getNumeroSerie());
                    stmt.setInt(index++, tecno.getGarantiaMeses());
                    stmt.setString(index++, tecno.getEspecificacionesTecnicas());
                }
                
                // ID al final para la cláusula WHERE
                stmt.setString(index, producto.getId());
                
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error al actualizar el producto", e);
        }
    }
    
    /**
     * Elimina un producto por su ID
     * @param id ID del producto
     * @return true si la eliminación fue exitosa
     */
    public boolean delete(String id) {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM productos WHERE id = ?")) {
            
            stmt.setString(1, id);
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error al eliminar el producto", e);
        }
    }
    
    /**
     * Busca un producto por su ID
     * @param id ID del producto
     * @return Producto encontrado o null si no existe
     */
    public Producto findById(String id) {
        try (Connection conn = DBUtil.getConnection()) {
            // 1. Obtener datos básicos del producto
            String sql = "SELECT * FROM productos WHERE id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, id);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }
                    
                    String tipoProducto = rs.getString("tipo_producto");
                    Producto producto = null;
                    
                    // 2. Según el tipo, obtener datos específicos
                    switch (tipoProducto) {
                        case "InsumoOficina":
                            String sqlInsumo = "SELECT p.*, i.* FROM productos p " +
                                             "JOIN insumos_oficina i ON p.id = i.producto_id " +
                                             "WHERE p.id = ?";
                            try (PreparedStatement stmtInsumo = conn.prepareStatement(sqlInsumo)) {
                                stmtInsumo.setString(1, id);
                                try (ResultSet rsInsumo = stmtInsumo.executeQuery()) {
                                    if (rsInsumo.next()) {
                                        InsumoOficina insumo = new InsumoOficina(
                                            rsInsumo.getString("nombre"),
                                            rsInsumo.getString("id"),
                                            rsInsumo.getDouble("precio"),
                                            rsInsumo.getInt("cantidad"),
                                            rsInsumo.getString("presentacion"),
                                            rsInsumo.getString("tipo_papel"),
                                            rsInsumo.getInt("cantidad_por_paquete")
                                        );
                                        insumo.setCategoria(Categoria.valueOf(rsInsumo.getString("categoria")));
                                        producto = insumo;
                                    }
                                }
                            }
                            break;
                        
                        case "ProductoMobiliario":
                            String sqlMobiliario = "SELECT p.*, m.* FROM productos p " +
                                                   "JOIN productos_mobiliarios m ON p.id = m.producto_id " +
                                                   "WHERE p.id = ?";
                            try (PreparedStatement stmtMobiliario = conn.prepareStatement(sqlMobiliario)) {
                                stmtMobiliario.setString(1, id);
                                try (ResultSet rsMobiliario = stmtMobiliario.executeQuery()) {
                                    if (rsMobiliario.next()) {
                                        ProductoMobilario mobiliario = new ProductoMobilario(
                                            rsMobiliario.getString("nombre"),
                                            rsMobiliario.getString("id"),
                                            rsMobiliario.getDouble("precio"),
                                            rsMobiliario.getInt("cantidad"),
                                            rsMobiliario.getString("tipo_mobiliario"),
                                            rsMobiliario.getString("material"),
                                            rsMobiliario.getString("color"),
                                            rsMobiliario.getString("dimensiones")
                                        );
                                        mobiliario.setCategoria(Categoria.valueOf(rsMobiliario.getString("categoria")));
                                        producto = mobiliario;
                                    }
                                }
                            }                       
                            break;
                            
                        case "ProductoTecnologico":
                            String sqlTecnologico = "SELECT p.*, t.* FROM productos p " +
                                                    "JOIN productos_tecnologicos t ON p.id = t.producto_id " +
                                                    "WHERE p.id = ?";
                            try (PreparedStatement stmtTecnologico = conn.prepareStatement(sqlTecnologico)) {
                                stmtTecnologico.setString(1, id);
                                try (ResultSet rsTecnologico = stmtTecnologico.executeQuery()) {
                                    if (rsTecnologico.next()) {
                                        ProductoTecnologico tecno = new ProductoTecnologico(
                                            rsTecnologico.getString("nombre"),
                                            rsTecnologico.getString("id"),
                                            rsTecnologico.getDouble("precio"),
                                            rsTecnologico.getInt("cantidad"),
                                            rsTecnologico.getString("marca"),
                                            rsTecnologico.getString("modelo"),
                                            rsTecnologico.getString("numero_serie"),
                                            rsTecnologico.getInt("garantia_meses"),
                                            rsTecnologico.getString("especificaciones_tecnicas")
                                        );
                                        tecno.setCategoria(Categoria.valueOf(rsTecnologico.getString("categoria")));
                                        producto = tecno;
                                    }
                                }
                            }
                            break;
                            
                        default:
                            // Producto básico
                            producto = new Producto(
                                rs.getString("nombre"),
                                rs.getString("id"),
                                rs.getDouble("precio"),
                                rs.getInt("cantidad"),
                                Categoria.valueOf(rs.getString("categoria"))
                            );
                    }
                    
                    return producto;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error al buscar el producto por ID", e);
        }
    }
    
    /**
     * Obtiene todos los productos
     * @return Lista de todos los productos
     */
    public List<Producto> findAll() {
        List<Producto> productos = new ArrayList<>();
        
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM productos")) {
            
            while (rs.next()) {
                productos.add(mapResultSetToProducto(rs));
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error al obtener todos los productos", e);
        }
        
        return productos;
    }
    
    /**
     * Genera un nuevo ID para un producto según su categoría
     * @param categoria Categoría del producto
     * @return Nuevo ID generado
     */
    public String generateNewId(Categoria categoria) {
        // Prefijo según categoría
        String prefix;
        switch (categoria) {
            case INSUMO_OFICINA:
                prefix = "INS";
                break;
            case PRODUCTO_MOBILIARIO:
                prefix = "MOB";
                break;
            case PRODUCTO_TECNOLOGICO:
                prefix = "TEC";
                break;
            default:
                prefix = "PRD";
        }
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT MAX(CAST(SUBSTRING(id, LENGTH(?) + 1) AS UNSIGNED)) AS max_id " +
                 "FROM productos WHERE id LIKE CONCAT(?, '%')")) {
            
            stmt.setString(1, prefix);
            stmt.setString(2, prefix);
            
            try (ResultSet rs = stmt.executeQuery()) {
                int maxId = 0;
                if (rs.next() && rs.getString("max_id") != null) {
                    maxId = rs.getInt("max_id");
                }
                
                // Generar nuevo ID
                return String.format("%s%06d", prefix, maxId + 1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error al generar nuevo ID", e);
        }
    }
    
    /**
     * Busca productos por nombre (búsqueda parcial)
     * @param nombre Fragmento del nombre a buscar
     * @return Lista de productos que coinciden
     */
    public List<Producto> findByNombreContaining(String nombre) {
        List<Producto> productos = new ArrayList<>();
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT * FROM productos WHERE nombre LIKE ?")) {
            
            stmt.setString(1, "%" + nombre + "%");
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    productos.add(mapResultSetToProducto(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error al buscar productos por nombre", e);
        }
        
        return productos;
    }
    
    /**
     * Busca productos por categoría
     * @param categoria Categoría a buscar
     * @return Lista de productos de la categoría
     */
    public List<Producto> findByCategoria(Categoria categoria) {
        List<Producto> productos = new ArrayList<>();
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT * FROM productos WHERE categoria = ?")) {
            
            stmt.setString(1, categoria.name());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    productos.add(mapResultSetToProducto(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error al buscar productos por categoría", e);
        }
        
        return productos;
    }
    
    /**
     * Convierte un ResultSet en un objeto Producto del tipo correcto
     */
    private Producto mapResultSetToProducto(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String nombre = rs.getString("nombre");
        double precio = rs.getDouble("precio");
        int cantidad = rs.getInt("cantidad");
        Categoria categoria = Categoria.valueOf(rs.getString("categoria"));
        String tipoProducto = rs.getString("tipo_producto");
        
        // Crear el objeto según el tipo
        switch (tipoProducto) {
            case "InsumoOficina":
                String presentacion = rs.getString("presentacion");
                String tipoPapel = rs.getString("tipo_papel");
                int cantidadPorPaquete = rs.getInt("cantidad_por_paquete");
                return new InsumoOficina(nombre, id, precio, cantidad, presentacion, tipoPapel, cantidadPorPaquete);
                
            case "ProductoMobiliario":
                String tipoMobiliario = rs.getString("tipo_mobiliario");
                String material = rs.getString("material");
                String color = rs.getString("color");
                String dimensiones = rs.getString("dimensiones");
                return new ProductoMobilario(nombre, id, precio, cantidad, tipoMobiliario, material, color, dimensiones);
                
            case "ProductoTecnologico":
                String marca = rs.getString("marca");
                String modelo = rs.getString("modelo");
                String numeroSerie = rs.getString("numero_serie");
                int garantiaMeses = rs.getInt("garantia_meses");
                String especificaciones = rs.getString("especificaciones_tecnicas");
                return new ProductoTecnologico(nombre, id, precio, cantidad, marca, modelo, numeroSerie, garantiaMeses, especificaciones);
                
            default:
                return new Producto(nombre, id, precio, cantidad, categoria);
        }
    }
}