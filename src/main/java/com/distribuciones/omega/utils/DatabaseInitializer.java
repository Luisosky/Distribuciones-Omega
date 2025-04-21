package com.distribuciones.omega.utils;

import com.distribuciones.omega.model.Categoria;
import com.distribuciones.omega.model.Cliente;
import com.distribuciones.omega.model.InsumoOficina;
import com.distribuciones.omega.model.Producto;
import com.distribuciones.omega.model.ProductoMobilario;
import com.distribuciones.omega.model.ProductoTecnologico;
import com.distribuciones.omega.repository.ClienteRepository;
import com.distribuciones.omega.repository.ProductoRepository;
import com.distribuciones.omega.repository.InventarioRepository;
import com.distribuciones.omega.repository.FacturaRepository;
import com.distribuciones.omega.repository.PagoRepository;
import com.distribuciones.omega.repository.CotizacionRepository;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Inicializador de base de datos que verifica y crea las tablas necesarias
 * si no existen y las llena con datos iniciales.
 */
public class DatabaseInitializer {
    
    private static final Logger LOGGER = Logger.getLogger(DatabaseInitializer.class.getName());
    private static boolean initialized = false;
    
    /**
     * Inicializa la base de datos verificando y creando tablas según sea necesario.
     * Las tablas se crean en orden de dependencia para respetar las relaciones.
     */
    public static void initialize() {
        // Check if already initialized in this session
        if (initialized) {
            System.out.println("Database already initialized in this session. Skipping.");
            return;
        }
        ensureDatabaseExists();
        
        try {
            Connection conn = DBUtil.getConnection();
            
            // Iniciamos una transacción para garantizar la integridad
            conn.setAutoCommit(false);
            
            try {
                // 1. TABLAS SIN DEPENDENCIAS (NIVEL 1)
                System.out.println("Inicializando tablas de nivel 1 (sin dependencias)...");
                
                // 1.0 Tabla de usuarios (nueva)
                if (!tableExists("usuarios")) {
                    createUsuariosTable();
                    insertSampleUsuarios();
                    System.out.println("Tabla usuarios creada y datos iniciales insertados.");
                } else {
                    System.out.println("Tabla usuarios ya existe.");
                }
                
                // 1.1 Tabla de clientes
                if (!tableExists("clientes")) {
                    createClientesTable();
                    insertSampleClientes();
                    System.out.println("Tabla clientes creada y datos iniciales insertados.");
                } else {
                    System.out.println("Tabla clientes ya existe.");
                }
                
                // 1.2 Tablas relacionadas con productos
                boolean productosExist = tableExists("productos") && 
                                        tableExists("insumos_oficina") && 
                                        tableExists("productos_mobiliarios") && 
                                        tableExists("productos_tecnologicos");
                
                if (!productosExist || !isProductosTableCorrect()) {
                    if (productosExist) {
                        System.out.println("Recreando tablas de productos debido a estructura incorrecta...");
                        recreateProductosTables();
                    } else {
                        System.out.println("Creando tablas de productos...");
                        createProductosTables();
                    }
                    
                    // Verificar si hay datos de productos
                    if (isProductosTableEmpty()) {
                        System.out.println("Insertando datos de ejemplo para productos...");
                        insertSampleProductos();
                    }
                } else {
                    System.out.println("Tablas de productos ya existen y tienen estructura correcta.");
                }
                
                // 2. TABLAS CON DEPENDENCIAS NIVEL 1 (NIVEL 2)
                System.out.println("Inicializando tablas de nivel 2 (con dependencias simples)...");
                
                // 2.1 Tabla de inventario (depende de productos)
                if (!tableExists("inventario")) {
                    System.out.println("Creando tabla de inventario...");
                    createInventarioTable();
                } else {
                    System.out.println("Tabla de inventario ya existe.");
                }
                
                // 2.2 Tabla de cotizaciones (depende de clientes y productos)
                if (!tableExists("cotizaciones")) {
                    System.out.println("Creando tabla de cotizaciones...");
                    createCotizacionesTable();
                } else {
                    System.out.println("Tabla de cotizaciones ya existe.");
                }
                
                // 2.3 Tabla de facturas (depende de clientes)
                if (!tableExists("facturas")) {
                    System.out.println("Creando tabla de facturas...");
                    createFacturasTable();
                } else {
                    System.out.println("Tabla de facturas ya existe.");
                }
                
                // 3. TABLAS CON DEPENDENCIAS NIVEL 2 (NIVEL 3)
                System.out.println("Inicializando tablas de nivel 3 (con dependencias complejas)...");
                
                // 3.1 Tabla de pagos (depende de facturas)
                if (!tableExists("pagos")) {
                    System.out.println("Creando tabla de pagos...");
                    createPagosTable();
                } else {
                    System.out.println("Tabla de pagos ya existe.");
                }
                
                // 3.2 Tabla de detalle_cotizacion (depende de cotizaciones y productos)
                if (!tableExists("detalle_cotizacion")) {
                    System.out.println("Creando tabla de detalle_cotizacion...");
                    createDetalleCotizacionTable();
                } else {
                    System.out.println("Tabla de detalle_cotizacion ya existe.");
                }
                
                // 3.3 Tabla de detalle_factura (depende de facturas y productos)
                if (!tableExists("detalle_factura")) {
                    System.out.println("Creando tabla de detalle_factura...");
                    createDetalleFacturaTable();
                } else {
                    System.out.println("Tabla de detalle_factura ya existe.");
                }
                
                // Si llegamos aquí, todo se creó correctamente
                conn.commit();
                initialized = true;
                System.out.println("Inicialización de base de datos completada con éxito.");
                
            } catch (Exception e) {
                // Si hay error, hacer rollback
                conn.rollback();
                throw e;
            } finally {
                // Restaurar autocommit
                conn.setAutoCommit(true);
                conn.close();
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error durante la inicialización de la base de datos", e);
            e.printStackTrace();
        }
    }

        /**
     * Crea la base de datos si no existe
     */
    public static void ensureDatabaseExists() {
        Connection conn = null;
        String dbName = null;
        String url = null;
        String user = null;
        String password = null;
        
        try {
            // Obtener las credenciales y nombres desde el archivo .env o variables de entorno
            dbName = System.getenv("DB_NAME");
            if (dbName == null) dbName = "omega"; // Valor por defecto
            
            // Construir URL sin el nombre de la base de datos
            String baseUrl = System.getenv("DB_URL");
            if (baseUrl != null && baseUrl.contains("/")) {
                url = baseUrl.substring(0, baseUrl.lastIndexOf("/"));
            } else {
                url = "jdbc:mysql://localhost:3306";
            }
            
            user = System.getenv("DB_USER");
            if (user == null) user = "root"; // Valor por defecto
            
            password = System.getenv("DB_PASS");
            if (password == null) password = "root"; // Valor por defecto
            
            System.out.println("Intentando conectar a MySQL para verificar/crear la base de datos: " + dbName);
            
            // Conectar al servidor MySQL sin especificar una base de datos
            conn = DriverManager.getConnection(url, user, password);
            
            // Crear la base de datos si no existe
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + dbName + 
                            " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            
            System.out.println("Base de datos " + dbName + " verificada/creada correctamente.");
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al crear la base de datos: " + e.getMessage(), e);
            System.err.println("ERROR: No se pudo crear la base de datos. " + e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Error al cerrar la conexión", e);
                }
            }
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
     * Verifica si la tabla productos está vacía
     * @return true si la tabla está vacía, false si contiene datos
     */
    private static boolean isProductosTableEmpty() {
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM productos")) {
            
            return rs.next() && rs.getInt("count") == 0;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al verificar si la tabla productos está vacía", e);
            return true; // En caso de error, asumimos que está vacía para intentar insertar datos
        }
    }
    
    /**
     * Verifica si la estructura de la tabla productos es correcta
     * @return true si la tabla tiene la estructura correcta, false en caso contrario
     */
    private static boolean isProductosTableCorrect() {
        try (Connection conn = DBUtil.getConnection()) {
            // Verificar columnas esenciales que deben existir
            boolean hasRequiredColumns = 
                columnExists(conn, "productos", "id_producto") &&
                columnExists(conn, "productos", "nombre") &&
                columnExists(conn, "productos", "codigo") &&
                columnExists(conn, "productos", "precio") &&
                columnExists(conn, "productos", "cantidad") &&
                columnExists(conn, "productos", "categoria") &&
                columnExists(conn, "productos", "activo");
            
            if (!hasRequiredColumns) {
                LOGGER.warning("La tabla productos no tiene todas las columnas requeridas");
                return false;
            }
            
            // Verificar tablas de especialización
            boolean hasSpecializationTables = 
                tableExists("insumos_oficina") &&
                tableExists("productos_mobiliarios") &&
                tableExists("productos_tecnologicos");
                
            if (!hasSpecializationTables) {
                LOGGER.warning("Faltan una o más tablas de especialización de productos");
                return false;
            }
            
            // Verificar estructura de las tablas de especialización
            boolean insumosCorrect = 
                columnExists(conn, "insumos_oficina", "id_producto") &&
                columnExists(conn, "insumos_oficina", "unidad_medida") &&
                columnExists(conn, "insumos_oficina", "tipo_papel") &&
                columnExists(conn, "insumos_oficina", "cantidad_hojas");
                
            boolean mobiliarioCorrect = 
                columnExists(conn, "productos_mobiliarios", "id_producto") &&
                columnExists(conn, "productos_mobiliarios", "tipo") &&
                columnExists(conn, "productos_mobiliarios", "material") &&
                columnExists(conn, "productos_mobiliarios", "color") &&
                columnExists(conn, "productos_mobiliarios", "dimensiones");
                
            boolean tecnologicoCorrect = 
                columnExists(conn, "productos_tecnologicos", "id_producto") &&
                columnExists(conn, "productos_tecnologicos", "marca") &&
                columnExists(conn, "productos_tecnologicos", "modelo") &&
                columnExists(conn, "productos_tecnologicos", "numero_serie") &&
                columnExists(conn, "productos_tecnologicos", "garantia_meses") &&
                columnExists(conn, "productos_tecnologicos", "especificaciones");
            
            return insumosCorrect && mobiliarioCorrect && tecnologicoCorrect;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al verificar la estructura de tablas de productos", e);
            return false;
        }
    }

    /**
     * Recrea las tablas de productos eliminando las existentes y creándolas de nuevo
     */
    public static void recreateProductosTables() {
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement()) {
            
            LOGGER.info("Recreando tablas que faltan o están incorrectas");
            System.out.println("Recreando tablas que faltan o están incorrectas");
            
            // Desactivar verificación de claves foráneas
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
            
            try {
                // Primero eliminar tablas dependientes que tienen FK hacia productos
                stmt.executeUpdate("DROP TABLE IF EXISTS detalle_factura");
                stmt.executeUpdate("DROP TABLE IF EXISTS items_factura");
                stmt.executeUpdate("DROP TABLE IF EXISTS detalle_cotizacion");
                stmt.executeUpdate("DROP TABLE IF EXISTS items_cotizacion");
                stmt.executeUpdate("DROP TABLE IF EXISTS inventario");
                stmt.executeUpdate("DROP TABLE IF EXISTS productos_tecnologicos");
                stmt.executeUpdate("DROP TABLE IF EXISTS productos_mobiliarios");
                stmt.executeUpdate("DROP TABLE IF EXISTS insumos_oficina");
                
                // Ahora eliminar la tabla principal
                stmt.executeUpdate("DROP TABLE IF EXISTS productos");
                
                // Crear tablas nuevamente
                createProductosTables();
                
            } finally {
                // Reactivar verificación de claves foráneas
                stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
            }
            
        } catch (SQLException e) {
            LOGGER.severe("Error al recrear tablas de productos: " + e.getMessage());
            e.printStackTrace();
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

    /**
     * Crea la tabla de inventario
     */
    private static void createInventarioTable() {
        try {
            InventarioRepository repo = new InventarioRepository();
            repo.createTableIfNotExists();
            LOGGER.info("Tabla de inventario creada exitosamente");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al crear la tabla de inventario", e);
        }
    }
    
    /**
     * Crea la tabla de facturas
     */
    private static void createFacturasTable() {
        try {
            FacturaRepository repository = new FacturaRepository();
            repository.createTableIfNotExists();
            LOGGER.info("Tabla de facturas creada exitosamente");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al crear la tabla de facturas", e);
            throw new RuntimeException("Error al crear la tabla de facturas", e);
        }
    }
    
    /**
     * Crea la tabla de pagos
     */
    private static void createPagosTable() {
        try {
            PagoRepository repository = new PagoRepository();
            repository.createTableIfNotExists();
            LOGGER.info("Tabla de pagos creada exitosamente");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al crear la tabla de pagos", e);
            throw new RuntimeException("Error al crear la tabla de pagos", e);
        }
    }
    
    /**
     * Crea la tabla de cotizaciones
     */
    private static void createCotizacionesTable() {
        try {
            CotizacionRepository repository = new CotizacionRepository();
            repository.createTableIfNotExists();
            LOGGER.info("Tabla de cotizaciones creada exitosamente");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al crear la tabla de cotizaciones", e);
            throw new RuntimeException("Error al crear la tabla de cotizaciones", e);
        }
    }
    
    /**
     * Crea la tabla de detalle_cotizacion
     */
    private static void createDetalleCotizacionTable() {
        try (Connection conn = DBUtil.getConnection()) {
            // Verificar si la tabla productos existe
            if (!SchemaUtil.tableExists(conn, "productos")) {
                throw new SQLException("La tabla 'productos' no existe. Debe crearla primero.");
            }
            
            // Obtener información detallada de la clave primaria de productos
            String[] pkInfo = SchemaUtil.getPrimaryKeyInfo(conn, "productos");
            String idProductoColumna = pkInfo[0]; // Este será "id" según el log
            
            // Obtener la definición exacta de la columna
            String idDefinitionExacta = SchemaUtil.getColumnDefinition(conn, "productos", idProductoColumna);
            
            // Verificar la estructura exacta de la tabla productos para diagnóstico
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SHOW CREATE TABLE productos")) {
                if (rs.next()) {
                    String createTable = rs.getString(2); // La segunda columna contiene el CREATE TABLE
                    LOGGER.info("Estructura completa de tabla productos: " + createTable);
                }
            }
            
            // Eliminar la tabla si existe para recrearla
            if (SchemaUtil.tableExists(conn, "detalle_cotizacion")) {
                SchemaUtil.dropTableIfExists(conn, "detalle_cotizacion");
            }
            
            // Crear una tabla temporal primero para probar la compatibilidad
            String sqlTest = "CREATE TEMPORARY TABLE test_detalle_cotizacion (" +
                             "id_detalle INT AUTO_INCREMENT PRIMARY KEY, " +
                             "id_cotizacion INT NOT NULL, " +
                             // Columna con el mismo nombre que en productos
                             idProductoColumna + " " + idDefinitionExacta + ", " +
                             "cantidad INT NOT NULL, " +
                             "precio_unitario DECIMAL(10,2) NOT NULL, " +
                             "subtotal DECIMAL(10,2) NOT NULL" +
                             ")";
            
            try {
                SchemaUtil.executeUpdate(conn, sqlTest);
                SchemaUtil.executeUpdate(conn, "DROP TEMPORARY TABLE test_detalle_cotizacion");
                LOGGER.info("Prueba de creación de tabla temporal exitosa");
            } catch (SQLException e) {
                LOGGER.severe("Error al crear tabla temporal: " + e.getMessage());
                // Intentar con una definición más simple
                idDefinitionExacta = "VARCHAR(20)";
                LOGGER.info("Usando definición simplificada: " + idDefinitionExacta);
            }
            
            // Ahora crear la tabla real
            String sql = "CREATE TABLE detalle_cotizacion (" +
                         "id_detalle INT AUTO_INCREMENT PRIMARY KEY, " +
                         "id_cotizacion INT NOT NULL, " +
                         idProductoColumna + " " + idDefinitionExacta + " NOT NULL, " +
                         "cantidad INT NOT NULL, " +
                         "precio_unitario DECIMAL(10,2) NOT NULL, " +
                         "subtotal DECIMAL(10,2) NOT NULL, " +
                         "FOREIGN KEY (id_cotizacion) REFERENCES cotizaciones(id_cotizacion) ON DELETE CASCADE" +
                         ")";
            
            SchemaUtil.executeUpdate(conn, sql);
            LOGGER.info("Tabla detalle_cotizacion creada exitosamente");
            
            // Ahora añadir la clave foránea por separado para poder controlar mejor los errores
            try {
                String addFKSQL = "ALTER TABLE detalle_cotizacion " +
                                 "ADD CONSTRAINT fk_detalle_producto " +
                                 "FOREIGN KEY (" + idProductoColumna + ") " + 
                                 "REFERENCES productos(" + idProductoColumna + ") " +
                                 "ON DELETE RESTRICT";
                
                SchemaUtil.executeUpdate(conn, addFKSQL);
                LOGGER.info("Clave foránea a productos añadida exitosamente");
            } catch (SQLException e) {
                LOGGER.warning("No se pudo añadir la clave foránea a productos: " + e.getMessage());
                LOGGER.warning("La tabla detalle_cotizacion se ha creado pero sin la restricción de clave foránea");
                // No lanzamos la excepción para permitir que la aplicación continúe funcionando
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al crear la tabla de detalle_cotizacion", e);
            throw new RuntimeException("Error al crear la tabla de detalle_cotizacion", e);
        }
    }
    
    /**
     * Crea la tabla de detalle_factura
     */
    private static void createDetalleFacturaTable() {
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Verificar si la tabla existe y eliminarla para recrearla
            stmt.executeUpdate("DROP TABLE IF EXISTS detalle_factura");
            
            // Crear la tabla con la estructura correcta, usando 'id' para hacer referencia a productos
            String sql = "CREATE TABLE detalle_factura (" +
                    "id_detalle INT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                    "id_factura INT NOT NULL, " +
                    "id VARCHAR(20) NOT NULL, " +  // Debe coincidir con productos.id (VARCHAR(20))
                    "cantidad INT NOT NULL, " +
                    "precio_unitario DECIMAL(10,2) NOT NULL, " +
                    "subtotal DECIMAL(10,2) NOT NULL, " +
                    "INDEX (id_factura), " +
                    "INDEX (id), " +
                    "FOREIGN KEY (id_factura) REFERENCES facturas(id_factura) ON DELETE CASCADE" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
            
            stmt.executeUpdate(sql);
            LOGGER.info("Tabla detalle_factura creada exitosamente");
            
            // Ahora añadir la clave foránea a productos por separado para mejor manejo de errores
            try {
                String addForeignKeySQL = "ALTER TABLE detalle_factura " +
                                         "ADD CONSTRAINT fk_detalle_factura_producto " +
                                         "FOREIGN KEY (id) REFERENCES productos(id)";
                
                stmt.executeUpdate(addForeignKeySQL);
                LOGGER.info("Clave foránea a productos añadida a detalle_factura");
            } catch (SQLException e) {
                // Si la restricción ya existe o hay un problema, loguear el error pero continuar
                LOGGER.warning("No se pudo añadir la restricción de clave foránea a productos: " + e.getMessage());
                // La tabla ya está creada, así que continuamos sin la FK si hay problemas
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al crear tabla detalle_factura: " + e.getMessage(), e);
            throw new RuntimeException("Error al crear la tabla de detalle_factura", e);
        }
    }
    
    /**
     * Crea la tabla de usuarios
     */
    private static void createUsuariosTable() {
        String createTableSQL = 
            "CREATE TABLE usuarios (" +
            "id_usuario INT AUTO_INCREMENT PRIMARY KEY, " +
            "username VARCHAR(50) NOT NULL UNIQUE, " +
            "password VARCHAR(100) NOT NULL, " +
            "nombre VARCHAR(100) NOT NULL, " +
            "edad INT, " +
            "id VARCHAR(20), " +  // Cédula o identificación
            "direccion VARCHAR(200), " +
            "telefono VARCHAR(20), " +
            "email VARCHAR(100), " +
            "salario DECIMAL(10,2), " +
            "tipo_contrato VARCHAR(50), " +
            "rol VARCHAR(20) NOT NULL, " +
            "activo BOOLEAN DEFAULT TRUE" +
            ")";
        
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.execute(createTableSQL);
            LOGGER.info("Tabla 'usuarios' creada exitosamente");
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al crear la tabla 'usuarios'", e);
        }
    }
    
    /**
     * Inserta usuarios de muestra en la tabla
     */
    private static void insertSampleUsuarios() {
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement()) {
            
            String insertSQL = 
                "INSERT INTO usuarios (username, password, nombre, edad, id, direccion, telefono, email, salario, tipo_contrato, rol, activo) VALUES " +
                "('admin', 'encrypted:admin123', 'Administrador del Sistema', 35, '9999999999', 'Oficina Central', '099-999-9999', 'admin@omega.com', 1500.00, 'INDEFINIDO', 'ADMIN', true), " +
                "('vendedor1', 'encrypted:secret', 'Juan Pérez', 28, '1234567890', 'Av. Principal 123', '098-765-4321', 'juan@omega.com', 850.00, 'FIJO', 'VENDEDOR', true), " +
                "('almacen', 'encrypted:almacen123', 'María López', 32, '0987654321', 'Calle Secundaria 456', '097-123-4567', 'maria@omega.com', 900.00, 'FIJO', 'ALMACEN', true), " +
                "('vendedor2', 'encrypted:secret2', 'Carlos Rodríguez', 25, '1122334455', 'Urb. Las Palmas', '099-333-2211', 'carlos@omega.com', 850.00, 'FIJO', 'VENDEDOR', true)";
            
            stmt.executeUpdate(insertSQL);
            LOGGER.info("Usuarios de muestra insertados exitosamente");
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al insertar usuarios de muestra", e);
        }
    }
}