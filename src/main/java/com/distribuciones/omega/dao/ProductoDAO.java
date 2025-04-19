package com.distribuciones.omega.dao;

import com.distribuciones.omega.model.Producto;
import com.distribuciones.omega.model.Categoria;
import com.distribuciones.omega.model.InsumoOficina;
import com.distribuciones.omega.model.ProductoMobilario;
import com.distribuciones.omega.model.ProductoTecnologico;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductoDAO {
    private final Connection conn;

    public ProductoDAO(Connection conn) {
        this.conn = conn;
    }

    public void createTableIfNotExists() throws SQLException {
        // Tabla base de productos
        String sqlProductos = "CREATE TABLE IF NOT EXISTS productos (" +
                     "id VARCHAR(20) PRIMARY KEY," +
                     "nombre VARCHAR(100) NOT NULL," +
                     "precio DECIMAL(10,2) NOT NULL," +
                     "cantidad INT NOT NULL," +
                     "categoria VARCHAR(50) NOT NULL" +
                     ")";
        
        // Tabla para insumos de oficina
        String sqlInsumos = "CREATE TABLE IF NOT EXISTS insumos_oficina (" +
                     "id VARCHAR(20) PRIMARY KEY," +
                     "presentacion VARCHAR(100)," +
                     "tipo_papel VARCHAR(50)," +
                     "cantidad_por_paquete INT," +
                     "FOREIGN KEY (id) REFERENCES productos(id) ON DELETE CASCADE" +
                     ")";
        
        // Tabla para productos mobiliarios
        String sqlMobiliarios = "CREATE TABLE IF NOT EXISTS productos_mobiliario (" +
                     "id VARCHAR(20) PRIMARY KEY," +
                     "tipo_mobiliario VARCHAR(100)," +
                     "material VARCHAR(100)," +
                     "color VARCHAR(50)," +
                     "dimensiones VARCHAR(100)," +
                     "FOREIGN KEY (id) REFERENCES productos(id) ON DELETE CASCADE" +
                     ")";
        
        // Tabla para productos tecnológicos
        String sqlTecnologicos = "CREATE TABLE IF NOT EXISTS productos_tecnologicos (" +
                     "id VARCHAR(20) PRIMARY KEY," +
                     "marca VARCHAR(100)," +
                     "modelo VARCHAR(100)," +
                     "numero_serie VARCHAR(100)," +
                     "garantia_meses INT," +
                     "especificaciones VARCHAR(200)," +
                     "FOREIGN KEY (id) REFERENCES productos(id) ON DELETE CASCADE" +
                     ")";
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sqlProductos);
            stmt.execute(sqlInsumos);
            stmt.execute(sqlMobiliarios);
            stmt.execute(sqlTecnologicos);
            
            // Verificar si hay datos existentes
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM productos");
            rs.next();
            int count = rs.getInt(1);
            
            // Si no hay datos, insertar algunos ejemplos
            if (count == 0) {
                // Insertar productos base
                String insertSql = "INSERT INTO productos (id, nombre, precio, cantidad, categoria) VALUES " +
                                   "('INS001', 'Resma Papel A4', 75.50, 100, 'INSUMO_OFICINA'), " +
                                   "('MOB001', 'Silla Ergonómica', 1200.00, 15, 'PRODUCTO_MOBILIARIO'), " +
                                   "('TEC001', 'Monitor LCD 24\"', 2800.00, 10, 'PRODUCTO_TECNOLOGICO')";
                stmt.execute(insertSql);
                
                // Insertar detalles para insumo de oficina
                stmt.execute("INSERT INTO insumos_oficina (id, presentacion, tipo_papel, cantidad_por_paquete) " +
                             "VALUES ('INS001', 'Resma 500 hojas', 'Bond 75g', 500)");
                
                // Insertar detalles para mobiliario
                stmt.execute("INSERT INTO productos_mobiliario (id, tipo_mobiliario, material, color, dimensiones) " +
                             "VALUES ('MOB001', 'Silla de Oficina', 'Plástico y Tela', 'Negro', '60x60x120cm')");
                
                // Insertar detalles para producto tecnológico
                stmt.execute("INSERT INTO productos_tecnologicos (id, marca, modelo, numero_serie, garantia_meses, especificaciones) " +
                             "VALUES ('TEC001', 'Samsung', 'S24F350', 'SM24F350-001', 12, 'Full HD, 60Hz, HDMI')");
            }
        }
    }

    public List<Producto> getAllProductos() throws SQLException {
        List<Producto> productos = new ArrayList<>();
        
        // Consulta base para obtener todos los productos
        String sql = "SELECT id, nombre, precio, cantidad, categoria FROM productos";
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                String id = rs.getString("id");
                String nombre = rs.getString("nombre");
                double precio = rs.getDouble("precio");
                int cantidad = rs.getInt("cantidad");
                Categoria categoria = Categoria.valueOf(rs.getString("categoria"));
                
                // Según la categoría, cargamos el tipo específico de producto
                switch (categoria) {
                    case INSUMO_OFICINA:
                        InsumoOficina insumo = cargarInsumoOficina(id, nombre, precio, cantidad);
                        if (insumo != null) {
                            productos.add(insumo);
                        } else {
                            productos.add(new Producto(nombre, id, precio, cantidad, categoria));
                        }
                        break;
                        
                    case PRODUCTO_MOBILIARIO:
                        ProductoMobilario mobiliario = cargarProductoMobiliario(id, nombre, precio, cantidad);
                        if (mobiliario != null) {
                            productos.add(mobiliario);
                        } else {
                            productos.add(new Producto(nombre, id, precio, cantidad, categoria));
                        }
                        break;
                        
                    case PRODUCTO_TECNOLOGICO:
                        ProductoTecnologico tecnologico = cargarProductoTecnologico(id, nombre, precio, cantidad);
                        if (tecnologico != null) {
                            productos.add(tecnologico);
                        } else {
                            productos.add(new Producto(nombre, id, precio, cantidad, categoria));
                        }
                        break;
                        
                    default:
                        productos.add(new Producto(nombre, id, precio, cantidad, categoria));
                }
            }
        }
        
        return productos;
    }
    
    private InsumoOficina cargarInsumoOficina(String id, String nombre, double precio, int cantidad) throws SQLException {
        String sql = "SELECT presentacion, tipo_papel, cantidad_por_paquete FROM insumos_oficina WHERE id = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                String presentacion = rs.getString("presentacion");
                String tipoPapel = rs.getString("tipo_papel");
                int cantidadPorPaquete = rs.getInt("cantidad_por_paquete");
                
                return new InsumoOficina(nombre, id, precio, cantidad, presentacion, tipoPapel, cantidadPorPaquete);
            }
        }
        
        return null;
    }
    
    private ProductoMobilario cargarProductoMobiliario(String id, String nombre, double precio, int cantidad) throws SQLException {
        String sql = "SELECT tipo_mobiliario, material, color, dimensiones FROM productos_mobiliario WHERE id = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                String tipoMobiliario = rs.getString("tipo_mobiliario");
                String material = rs.getString("material");
                String color = rs.getString("color");
                String dimensiones = rs.getString("dimensiones");
                
                return new ProductoMobilario(nombre, id, precio, cantidad, tipoMobiliario, material, color, dimensiones);
            }
        }
        
        return null;
    }
    
    private ProductoTecnologico cargarProductoTecnologico(String id, String nombre, double precio, int cantidad) throws SQLException {
        String sql = "SELECT marca, modelo, numero_serie, garantia_meses, especificaciones FROM productos_tecnologicos WHERE id = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                String marca = rs.getString("marca");
                String modelo = rs.getString("modelo");
                String numeroSerie = rs.getString("numero_serie");
                int garantiaMeses = rs.getInt("garantia_meses");
                String especificaciones = rs.getString("especificaciones");
                
                return new ProductoTecnologico(nombre, id, precio, cantidad, marca, modelo, numeroSerie, garantiaMeses, especificaciones);
            }
        }
        
        return null;
    }

    public void addProducto(Producto producto) throws SQLException {
        // Primero insertar en la tabla base
        String sqlBase = "INSERT INTO productos (id, nombre, precio, cantidad, categoria) VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sqlBase)) {
            pstmt.setString(1, producto.getId());
            pstmt.setString(2, producto.getNombre());
            pstmt.setDouble(3, producto.getPrecio());
            pstmt.setInt(4, producto.getCantidad());
            pstmt.setString(5, producto.getCategoria().name());
            
            pstmt.executeUpdate();
        }
        
        // Luego insertar en la tabla específica según el tipo
        if (producto instanceof InsumoOficina) {
            InsumoOficina insumo = (InsumoOficina) producto;
            String sqlInsumo = "INSERT INTO insumos_oficina (id, presentacion, tipo_papel, cantidad_por_paquete) VALUES (?, ?, ?, ?)";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sqlInsumo)) {
                pstmt.setString(1, insumo.getId());
                pstmt.setString(2, insumo.getPresentacion());
                pstmt.setString(3, insumo.getTipoPapel());
                pstmt.setInt(4, insumo.getCantidadPorPaquete());
                
                pstmt.executeUpdate();
            }
        } 
        else if (producto instanceof ProductoMobilario) {
            ProductoMobilario mobiliario = (ProductoMobilario) producto;
            String sqlMobiliario = "INSERT INTO productos_mobiliario (id, tipo_mobiliario, material, color, dimensiones) VALUES (?, ?, ?, ?, ?)";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sqlMobiliario)) {
                pstmt.setString(1, mobiliario.getId());
                pstmt.setString(2, mobiliario.getTipoMobilario());
                pstmt.setString(3, mobiliario.getMaterial());
                pstmt.setString(4, mobiliario.getColor());
                pstmt.setString(5, mobiliario.getDimensiones());
                
                pstmt.executeUpdate();
            }
        } 
        else if (producto instanceof ProductoTecnologico) {
            ProductoTecnologico tecnologico = (ProductoTecnologico) producto;
            String sqlTecnologico = "INSERT INTO productos_tecnologicos (id, marca, modelo, numero_serie, garantia_meses, especificaciones) VALUES (?, ?, ?, ?, ?, ?)";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sqlTecnologico)) {
                pstmt.setString(1, tecnologico.getId());
                pstmt.setString(2, tecnologico.getMarca());
                pstmt.setString(3, tecnologico.getModelo());
                pstmt.setString(4, tecnologico.getNumeroSerie());
                pstmt.setInt(5, tecnologico.getGarantiaMeses());
                pstmt.setString(6, tecnologico.getEspecificacionesTecnicas());
                
                pstmt.executeUpdate();
            }
        }
    }

    public void updateProducto(Producto producto) throws SQLException {
        // Primero actualizar la tabla base
        String sqlBase = "UPDATE productos SET nombre = ?, precio = ?, cantidad = ?, categoria = ? WHERE id = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sqlBase)) {
            pstmt.setString(1, producto.getNombre());
            pstmt.setDouble(2, producto.getPrecio());
            pstmt.setInt(3, producto.getCantidad());
            pstmt.setString(4, producto.getCategoria().name());
            pstmt.setString(5, producto.getId());
            
            pstmt.executeUpdate();
        }
        
        // Luego actualizar la tabla específica según el tipo
        if (producto instanceof InsumoOficina) {
            InsumoOficina insumo = (InsumoOficina) producto;
            
            // Verificar si ya existe en la tabla de insumos
            if (existeEnTabla("insumos_oficina", insumo.getId())) {
                String sqlInsumo = "UPDATE insumos_oficina SET presentacion = ?, tipo_papel = ?, cantidad_por_paquete = ? WHERE id = ?";
                
                try (PreparedStatement pstmt = conn.prepareStatement(sqlInsumo)) {
                    pstmt.setString(1, insumo.getPresentacion());
                    pstmt.setString(2, insumo.getTipoPapel());
                    pstmt.setInt(3, insumo.getCantidadPorPaquete());
                    pstmt.setString(4, insumo.getId());
                    
                    pstmt.executeUpdate();
                }
            } else {
                // Si no existe, insertar
                String sqlInsumo = "INSERT INTO insumos_oficina (id, presentacion, tipo_papel, cantidad_por_paquete) VALUES (?, ?, ?, ?)";
                
                try (PreparedStatement pstmt = conn.prepareStatement(sqlInsumo)) {
                    pstmt.setString(1, insumo.getId());
                    pstmt.setString(2, insumo.getPresentacion());
                    pstmt.setString(3, insumo.getTipoPapel());
                    pstmt.setInt(4, insumo.getCantidadPorPaquete());
                    
                    pstmt.executeUpdate();
                }
            }
        } 
        else if (producto instanceof ProductoMobilario) {
            ProductoMobilario mobiliario = (ProductoMobilario) producto;
            
            // Verificar si ya existe en la tabla de mobiliarios
            if (existeEnTabla("productos_mobiliario", mobiliario.getId())) {
                String sqlMobiliario = "UPDATE productos_mobiliario SET tipo_mobiliario = ?, material = ?, color = ?, dimensiones = ? WHERE id = ?";
                
                try (PreparedStatement pstmt = conn.prepareStatement(sqlMobiliario)) {
                    pstmt.setString(1, mobiliario.getTipoMobilario());
                    pstmt.setString(2, mobiliario.getMaterial());
                    pstmt.setString(3, mobiliario.getColor());
                    pstmt.setString(4, mobiliario.getDimensiones());
                    pstmt.setString(5, mobiliario.getId());
                    
                    pstmt.executeUpdate();
                }
            } else {
                // Si no existe, insertar
                String sqlMobiliario = "INSERT INTO productos_mobiliario (id, tipo_mobiliario, material, color, dimensiones) VALUES (?, ?, ?, ?, ?)";
                
                try (PreparedStatement pstmt = conn.prepareStatement(sqlMobiliario)) {
                    pstmt.setString(1, mobiliario.getId());
                    pstmt.setString(2, mobiliario.getTipoMobilario());
                    pstmt.setString(3, mobiliario.getMaterial());
                    pstmt.setString(4, mobiliario.getColor());
                    pstmt.setString(5, mobiliario.getDimensiones());
                    
                    pstmt.executeUpdate();
                }
            }
        } 
        else if (producto instanceof ProductoTecnologico) {
            ProductoTecnologico tecnologico = (ProductoTecnologico) producto;
            
            // Verificar si ya existe en la tabla de tecnológicos
            if (existeEnTabla("productos_tecnologicos", tecnologico.getId())) {
                String sqlTecnologico = "UPDATE productos_tecnologicos SET marca = ?, modelo = ?, numero_serie = ?, garantia_meses = ?, especificaciones = ? WHERE id = ?";
                
                try (PreparedStatement pstmt = conn.prepareStatement(sqlTecnologico)) {
                    pstmt.setString(1, tecnologico.getMarca());
                    pstmt.setString(2, tecnologico.getModelo());
                    pstmt.setString(3, tecnologico.getNumeroSerie());
                    pstmt.setInt(4, tecnologico.getGarantiaMeses());
                    pstmt.setString(5, tecnologico.getEspecificacionesTecnicas());
                    pstmt.setString(6, tecnologico.getId());
                    
                    pstmt.executeUpdate();
                }
            } else {
                // Si no existe, insertar
                String sqlTecnologico = "INSERT INTO productos_tecnologicos (id, marca, modelo, numero_serie, garantia_meses, especificaciones) VALUES (?, ?, ?, ?, ?, ?)";
                
                try (PreparedStatement pstmt = conn.prepareStatement(sqlTecnologico)) {
                    pstmt.setString(1, tecnologico.getId());
                    pstmt.setString(2, tecnologico.getMarca());
                    pstmt.setString(3, tecnologico.getModelo());
                    pstmt.setString(4, tecnologico.getNumeroSerie());
                    pstmt.setInt(5, tecnologico.getGarantiaMeses());
                    pstmt.setString(6, tecnologico.getEspecificacionesTecnicas());
                    
                    pstmt.executeUpdate();
                }
            }
        }
    }
    
    private boolean existeEnTabla(String tabla, String id) throws SQLException {
        String sql = "SELECT 1 FROM " + tabla + " WHERE id = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        }
    }

    public void deleteProducto(String id) throws SQLException {
        // Solo necesitamos eliminar de la tabla base
        // Las restricciones de clave foránea (ON DELETE CASCADE) se encargarán de las tablas específicas
        String sql = "DELETE FROM productos WHERE id = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            pstmt.executeUpdate();
        }
    }
    
    public String generateNewId(Categoria categoria) throws SQLException {
        // El mismo método existente
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
        
        String sql = "SELECT id FROM productos WHERE id LIKE ? ORDER BY id DESC LIMIT 1";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, prefix + "%");
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                String lastId = rs.getString("id");
                String numPart = lastId.substring(3);
                try {
                    int num = Integer.parseInt(numPart);
                    return prefix + String.format("%03d", num + 1);
                } catch (NumberFormatException e) {
                    return prefix + "001";
                }
            } else {
                return prefix + "001";
            }
        }
    }
}