package com.distribuciones.omega.controllers;

import com.distribuciones.omega.model.ProductoInventario;
import com.distribuciones.omega.service.InventarioService;

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
    private ObservableList<ProductoInventario> productosData;
    private FilteredList<ProductoInventario> productosFiltrados;
    private NumberFormat currencyFormatter;
    
    /**
     * Inicializa el controlador
     */
    @FXML
    public void initialize() {
        inventarioService = new InventarioService();
        currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("es", "EC"));
        
        // Configurar columnas
        configureTable();
        
        // Cargar datos
        loadInventario();
        
        // Configurar filtro de búsqueda
        setupSearch();
        
        // Mostrar fecha de actualización
        updateTimestamp();
    }
    
    /**
     * Configura las columnas de la tabla
     */
    private void configureTable() {
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
            List<ProductoInventario> productos = inventarioService.obtenerProductosDisponibles();
            productosData = FXCollections.observableArrayList(productos);
            productosFiltrados = new FilteredList<>(productosData);
            tableInventario.setItems(productosFiltrados);
            
            // Actualizar estadísticas
            updateStats();
        } catch (Exception e) {
            e.printStackTrace();
            // En una aplicación real, aquí mostrarías un diálogo de error
        }
    }
    
    /**
     * Configura el filtro de búsqueda
     */
    private void setupSearch() {
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
            updateStats();
        });
    }
    
    /**
     * Actualiza las estadísticas del inventario
     */
    private void updateStats() {
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
}