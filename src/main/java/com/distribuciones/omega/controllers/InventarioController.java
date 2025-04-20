package com.distribuciones.omega.controllers;

import com.distribuciones.omega.model.ProductoInventario;
import com.distribuciones.omega.service.InventarioService;
import com.distribuciones.omega.utils.AlertUtils;
import com.distribuciones.omega.service.AlertaStockService;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Controlador para la vista de inventario
 * Esta vista es de solo lectura y muestra el estado actual del inventario
 */
public class InventarioController {
    
    @FXML private TableView<ProductoInventario> tableInventario;
    @FXML private TableColumn<ProductoInventario, Long> colId;
    @FXML private TableColumn<ProductoInventario, String> colCodigo;
    @FXML private TableColumn<ProductoInventario, String> colDescripcion;
    @FXML private TableColumn<ProductoInventario, String> colPrecio;
    @FXML private TableColumn<ProductoInventario, Integer> colStock;
    @FXML private TableColumn<ProductoInventario, String> colNumeroSerie;
    @FXML private TableColumn<ProductoInventario, String> colCategoria;
    @FXML private TableColumn<ProductoInventario, String> colProveedor;
    
    @FXML private TextField txtBuscar;
    @FXML private Label lblTotalProductos;
    @FXML private Label lblValorInventario;
    @FXML private Label lblFechaActualizacion;
    
    private InventarioService inventarioService;
    private AlertaStockService alertaStockService = new AlertaStockService();
    private ObservableList<ProductoInventario> productosData;
    private FilteredList<ProductoInventario> productosFiltrados;
    private NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("es", "MX"));
    
    /**
     * Inicializa el controlador
     */
    @FXML
    private void initialize() {
        try {
            System.out.println("\n==== Inicializando InventarioController ====");
            
            // Inicializar datos - IMPORTANTE: esto primero
            productosData = FXCollections.observableArrayList();
            productosFiltrados = new FilteredList<>(productosData);
            
            // Inicializar servicio
            inventarioService = new InventarioService();
            
            // Configurar columnas de la tabla
            configurarColumnas();
            
            // Asegurarnos que la tabla tenga sus items asignados
            tableInventario.setItems(productosFiltrados);
            
            // Cargar datos
            System.out.println("Cargando datos de inventario...");
            loadInventario();
            
            // Configurar búsqueda
            configurarBusqueda();
            
            System.out.println("==== Inicialización de InventarioController completada ====\n");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error en la inicialización del controlador: " + e.getMessage());
        }
    }
    
    /**
     * Configura las columnas de la tabla
     */
    private void configurarColumnas() {
        colId.setCellValueFactory(new PropertyValueFactory<>("idProducto"));
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colNumeroSerie.setCellValueFactory(new PropertyValueFactory<>("numeroSerie"));
        colProveedor.setCellValueFactory(new PropertyValueFactory<>("proveedor"));
        
        // Formatear categoría
        colCategoria.setCellValueFactory(cellData -> {
            String categoria = cellData.getValue().getCategoria();
            return new SimpleStringProperty(formatCategoria(categoria));
        });
        
        // Formatear precio como moneda
        colPrecio.setCellValueFactory(cellData -> {
            double precio = cellData.getValue().getPrecio();
            return new SimpleStringProperty(currencyFormatter.format(precio));
        });
    }
    
    /**
     * Carga los datos del inventario
     */
    private void loadInventario() {
        try {
            System.out.println("Método loadInventario() iniciado");
            
            // Obtener datos con manejo de excepciones
            List<ProductoInventario> productos = new ArrayList<>();
            try {
                productos = inventarioService.obtenerProductosDisponibles();
                System.out.println("Obtenidos " + productos.size() + " productos del servicio");
            } catch (Exception e) {
                System.err.println("Error obteniendo productos: " + e.getMessage());
                e.printStackTrace();
            }
            
            // Limpiar y cargar datos
            if (productosData != null) {
                productosData.clear();
                if (productos != null && !productos.isEmpty()) {
                    productosData.addAll(productos);
                    System.out.println("Productos cargados en la lista observable");
                } else {
                    System.out.println("ADVERTENCIA: No se encontraron productos en el inventario");
                }
                
                // Verificar tabla
                System.out.println("Elementos en tableView: " + 
                    (tableInventario.getItems() != null ? tableInventario.getItems().size() : "null"));
            } else {
                System.err.println("ERROR: productosData es null");
            }
            
            // Actualizar estadísticas (protegido contra errores)
            try {
                actualizarEstadisticas();
            } catch (Exception e) {
                System.err.println("Error actualizando estadísticas: " + e.getMessage());
            }
            
            System.out.println("Carga de inventario finalizada");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error al cargar inventario: " + e.getMessage());
        }
    }
    
    /**
     * Configura el filtro de búsqueda
     */
    private void configurarBusqueda() {
        txtBuscar.textProperty().addListener((observable, oldValue, newValue) -> {
            productosFiltrados.setPredicate(producto -> {
                // Si el texto está vacío, mostrar todos los productos
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                
                String lowerCaseFilter = newValue.toLowerCase();
                
                // Comparar con diferentes campos
                if (producto.getDescripcion().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (producto.getCodigo().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (producto.getCategoria() != null && 
                           producto.getCategoria().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (producto.getProveedor() != null && 
                           producto.getProveedor().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                
                return false;
            });
            
            // Actualizar estadísticas con base en los elementos filtrados
            actualizarEstadisticas();
        });
    }
    
    /**
     * Actualiza las estadísticas del inventario
     */
    private void actualizarEstadisticas() {
        int totalProductos = productosFiltrados.size();
        double valorTotal = productosFiltrados.stream()
                .mapToDouble(p -> p.getPrecio() * p.getStock())
                .sum();
        
        lblTotalProductos.setText("Total productos: " + totalProductos);
        lblValorInventario.setText("Valor total inventario: " + currencyFormatter.format(valorTotal));
    }
    
    /**
     * Actualiza la marca de tiempo de la última actualización
     */
    private void updateTimestamp() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        String timestamp = LocalDateTime.now().format(formatter);
        lblFechaActualizacion.setText("Última actualización: " + timestamp);
    }
    
    /**
     * Formatea el texto de la categoría para mejor visualización
     */
    private String formatCategoria(String categoria) {
        if (categoria == null || categoria.isEmpty()) {
            return "";
        }
        
        // Reemplazar guiones bajos por espacios y formatear
        String formatted = categoria.replace("_", " ");
        
        // Convertir a título (primera letra de cada palabra en mayúscula)
        String[] words = formatted.split(" ");
        StringBuilder result = new StringBuilder();
        
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(word.substring(0, 1).toUpperCase())
                      .append(word.substring(1).toLowerCase())
                      .append(" ");
            }
        }
        
        return result.toString().trim();
    }
    
    // Modificación del método que actualiza el inventario
    private void actualizarInventario(ProductoInventario producto) {
        // Código existente para actualizar
        if (inventarioService.actualizarProducto(producto)) {
            AlertUtils.mostrarInformacion("Éxito", "Producto actualizado correctamente");
            loadInventario(); // Reemplazar cargarDatos() con loadInventario()
            
            // Verificar stock después de actualizar
            alertaStockService.verificarProducto(producto);
        } else {
            AlertUtils.mostrarError("Error", "No se pudo actualizar el producto");
        }
    }
    
    @FXML
    private void handleRecargarDatos() {
        System.out.println("\n==== Recargando datos manualmente ====");
        loadInventario();
    }
}