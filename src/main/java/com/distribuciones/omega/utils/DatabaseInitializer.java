package com.distribuciones.omega.utils;

import com.distribuciones.omega.model.Categoria;
import com.distribuciones.omega.model.Cliente;
import com.distribuciones.omega.model.InsumoOficina;
import com.distribuciones.omega.model.Producto;
import com.distribuciones.omega.model.ProductoMobilario;
import com.distribuciones.omega.model.ProductoTecnologico;
import com.distribuciones.omega.repository.ClienteRepository;
import com.distribuciones.omega.repository.ProductoRepository;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Inicializador de base de datos que verifica y crea las tablas necesarias
 * si no existen y las llena con datos iniciales.
 */
public class DatabaseInitializer {
    
    private static final Logger LOGGER = Logger.getLogger(DatabaseInitializer.class.getName());
    
    /**
     * Inicializa la base de datos verificando y creando tablas según sea necesario.
     */
    public static void initialize() {
        try {
            // Verificar y crear tabla de clientes
            if (!tableExists("clientes")) {
                createClientesTable();
                insertSampleClientes();
                LOGGER.info("Tabla 'clientes' creada e inicializada con datos de muestra");
            }
            
            // Verificar y crear tablas de productos
            boolean productosExists = tableExists("productos");
            boolean structureCorrect = productosExists && isProductosTableCorrect();
            
            if (!productosExists) {
                createProductosTables();
                insertSampleProductos();
                LOGGER.info("Tablas de productos creadas e inicializadas con datos de muestra");
            } else if (!structureCorrect) {
                // La tabla existe pero su estructura es incorrecta
                LOGGER.warning("La estructura de la tabla productos es incorrecta. Recreando tablas...");
                recreateProductosTables();
            }
            
            // Aquí puedes añadir más verificaciones para otras tablas
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al inicializar la base de datos", e);
        }
    }
    
    /**
     * Verifica si una tabla específica existe en la base de datos
     * @param tableName Nombre de la tabla a verificar
     * @return true si la tabla existe, false en caso contrario
     */
    private static boolean tableExists(String tableName) {
        try (Connection conn = DBUtil.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet tables = metaData.getTables(null, null, tableName, null);
            return tables.next();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al verificar la existencia de la tabla " + tableName, e);
            return false;
        }
    }
    
    /**
     * Verifica la estructura de la tabla productos
     * @return true si la estructura es correcta, false en caso contrario
     */
    private static boolean isProductosTableCorrect() {
        try (Connection conn = DBUtil.getConnection()) {
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT tipo_producto FROM productos LIMIT 1")) {
                // Si ejecuta correctamente, la columna existe
                return true;
            } catch (Exception e) {
                // Si hay error, la columna no existe
                LOGGER.log(Level.WARNING, "La tabla productos existe pero su estructura es incorrecta", e);
                return false;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al verificar la estructura de la tabla productos", e);
            return false;
        }
    }
    
    /**
     * Elimina y recrea las tablas de productos
     */
    private static void recreateProductosTables() {
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Eliminar tablas en orden inverso (por las claves foráneas)
            stmt.executeUpdate("DROP TABLE IF EXISTS productos_tecnologicos");
            stmt.executeUpdate("DROP TABLE IF EXISTS productos_mobiliarios");
            stmt.executeUpdate("DROP TABLE IF EXISTS insumos_oficina");
            stmt.executeUpdate("DROP TABLE IF EXISTS productos");
            
            LOGGER.info("Tablas de productos eliminadas para recreación");
            
            // Crear las tablas nuevamente
            createProductosTables();
            insertSampleProductos();
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al recrear las tablas de productos", e);
        }
    }
    
    /**
     * Crea la tabla de clientes
     */
    private static void createClientesTable() {
        String createTableSQL = 
            "CREATE TABLE clientes (" +
            "id_cliente INT AUTO_INCREMENT PRIMARY KEY, " +
            "nombre VARCHAR(100) NOT NULL, " +
            "id VARCHAR(20) NOT NULL UNIQUE, " +  // Número de identificación (RUC/CI)
            "email VARCHAR(100), " +
            "telefono VARCHAR(20), " +
            "direccion VARCHAR(200), " +
            "activo BOOLEAN DEFAULT TRUE, " +
            "mayorista BOOLEAN DEFAULT FALSE, " +
            "limite_credito DECIMAL(10,2) DEFAULT 0.00" +
            ")";
        
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.execute(createTableSQL);
            LOGGER.info("Tabla 'clientes' creada exitosamente");
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al crear la tabla 'clientes'", e);
        }
    }
    
    /**
     * Inserta clientes de muestra en la tabla
     */
    private static void insertSampleClientes() {
        ClienteRepository repository = new ClienteRepository();
        
        // Crear algunos clientes de ejemplo
        Cliente[] clientes = {
            new Cliente("Juan Pérez", "1234567890", "juan@example.com", "099-123-4567", "Av. Principal 123"),
            new Cliente("María López", "0987654321", "maria@example.com", "098-765-4321", "Calle Secundaria 456"),
            new Cliente("Empresa ABC", "1793456789001", "contacto@abc.com", "02-123-4567", "Zona Industrial 789")
        };
        
        // Configurar el cliente empresarial como mayorista
        clientes[2].setMayorista(true);
        clientes[2].setLimiteCredito(5000.00);
        
        // Guardar clientes
        for (Cliente c : clientes) {
            try {
                repository.save(c);
                LOGGER.info("Cliente de muestra creado: " + c.getNombre());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error al crear cliente de muestra: " + c.getNombre(), e);
            }
        }
    }
    
    /**
     * Crea las tablas relacionadas con productos utilizando el repositorio
     */
    private static void createProductosTables() {
        try {
            ProductoRepository repository = new ProductoRepository();
            repository.createTableIfNotExists();
            verifyProductosTable();
            LOGGER.info("Tablas de productos creadas exitosamente");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al crear las tablas de productos", e);
        }
    }
    
    /**
     * Inserta productos de muestra en las tablas correspondientes
     */
    private static void insertSampleProductos() {
        ProductoRepository repository = new ProductoRepository();
        
        try {
            // 1. Crear productos básicos
            Producto[] productosBasicos = {
                new Producto("Grapadora Industrial", "PRD000001", 25.99, 50, Categoria.INSUMO_OFICINA),
                new Producto("Pizarra Acrílica", "PRD000002", 45.50, 20, Categoria.PRODUCTO_MOBILIARIO),
                new Producto("Mouse inalámbrico", "PRD000003", 18.75, 100, Categoria.PRODUCTO_TECNOLOGICO)
            };
            
            // 2. Crear insumos de oficina
            InsumoOficina[] insumosOficina = {
                new InsumoOficina("Resma Papel A4", "INS000001", 3.99, 200, "Paquete", "Bond 75g", 500),
                new InsumoOficina("Cuaderno Universitario", "INS000002", 2.50, 150, "Unidad", "Bond rayado", 100),
                new InsumoOficina("Post-it Notas Adhesivas", "INS000003", 1.99, 300, "Pack", "Bond color", 100)
            };
            
            // 3. Crear productos mobiliarios
            ProductoMobilario[] mobiliarios = {
                new ProductoMobilario("Escritorio Ejecutivo", "MOB000001", 189.99, 10, "Escritorio", "Madera MDF", "Café", "120x60x75cm"),
                new ProductoMobilario("Silla Ergonómica", "MOB000002", 120.50, 15, "Silla", "Metal y Malla", "Negro", "60x60x110cm"),
                new ProductoMobilario("Archivador Metálico", "MOB000003", 85.75, 8, "Archivador", "Metal", "Gris", "47x62x132cm")
            };
            
            // 4. Crear productos tecnológicos
            ProductoTecnologico[] tecnologicos = {
                new ProductoTecnologico("Laptop Core i5", "TEC000001", 799.99, 5, "HP", "Pavilion 15", "SN12345678", 12, "Intel Core i5, 8GB RAM, 512GB SSD"),
                new ProductoTecnologico("Impresora Multifuncional", "TEC000002", 299.50, 8, "Epson", "L3150", "IMF98765432", 12, "Impresora, escáner y copiadora, sistema de tinta continua"),
                new ProductoTecnologico("Proyector HD", "TEC000003", 450.75, 3, "Epson", "PowerLite", "PJ56781234", 6, "3600 lúmenes, HDMI, resolución nativa 1280x800")
            };
            
            // Guardar todos los productos de muestra
            for (Producto p : productosBasicos) {
                repository.save(p);
                LOGGER.info("Producto básico creado: " + p.getNombre());
            }
            
            for (InsumoOficina i : insumosOficina) {
                repository.save(i);
                LOGGER.info("Insumo de oficina creado: " + i.getNombre());
            }
            
            for (ProductoMobilario m : mobiliarios) {
                repository.save(m);
                LOGGER.info("Producto mobiliario creado: " + m.getNombre());
            }
            
            for (ProductoTecnologico t : tecnologicos) {
                repository.save(t);
                LOGGER.info("Producto tecnológico creado: " + t.getNombre());
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al insertar productos de muestra", e);
            e.printStackTrace();
        }
    }

    /**
     * Verifica y actualiza la estructura de la tabla productos
     */
    private static void verifyProductosTable() {
        try (Connection conn = DBUtil.getConnection()) {
            // Verificar si existe la columna 'activo'
            boolean activoExists = columnExists(conn, "productos", "activo");
            
            if (!activoExists) {
                LOGGER.info("Añadiendo columna 'activo' a la tabla productos");
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("ALTER TABLE productos ADD COLUMN activo BOOLEAN DEFAULT TRUE");
                }
            }
            
            // Verificar otras columnas si es necesario...
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al verificar la estructura de la tabla productos", e);
        }
    }

    /**
     * Verifica si una columna existe en una tabla
     */
    private static boolean columnExists(Connection conn, String tableName, String columnName) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        ResultSet rs = meta.getColumns(null, null, tableName, columnName);
        return rs.next();
    }

}