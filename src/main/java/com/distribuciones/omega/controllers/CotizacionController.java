package com.distribuciones.omega.controllers;

import com.distribuciones.omega.model.*;
import com.distribuciones.omega.service.*;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ResourceBundle;

public class CotizacionController implements Initializable {

    @FXML private ComboBox<Cliente> cmbCliente;
    @FXML private TableView<ProductoInventario> tblInventario;
    @FXML private TableView<ItemCotizacion> tblCotizacion;
    @FXML private TableColumn<ProductoInventario, String> colCodigo;
    @FXML private TableColumn<ProductoInventario, String> colDescripcion;
    @FXML private TableColumn<ProductoInventario, Double> colPrecio;
    @FXML private TableColumn<ProductoInventario, Integer> colStock;
    @FXML private TableColumn<ProductoInventario, String> colSerie;
    
    @FXML private TableColumn<ItemCotizacion, String> colItemCodigo;
    @FXML private TableColumn<ItemCotizacion, String> colItemDescripcion;
    @FXML private TableColumn<ItemCotizacion, Integer> colItemCantidad;
    @FXML private TableColumn<ItemCotizacion, Double> colItemPrecio;
    @FXML private TableColumn<ItemCotizacion, Double> colItemSubtotal;
    
    @FXML private Label lblSubtotal;
    @FXML private Label lblDescuento;
    @FXML private Label lblIVA;
    @FXML private Label lblTotal;
    @FXML private TextField txtDescuentoAdicional;
    @FXML private Button btnAgregar;
    @FXML private Button btnQuitar;
    @FXML private Button btnCrearCotizacion;
    @FXML private Button btnGenerarOrden;
    @FXML private Button btnGenerarFactura;
    @FXML private Label lblVendedor;
    
    private InventarioService inventarioService;
    private ClienteService clienteService;
    private CotizacionService cotizacionService;
    private FacturaService facturaService;
    private UsuarioService usuarioService;
    private PromocionService promocionService;
    
    private ObservableList<ProductoInventario> productosInventario;
    private ObservableList<ItemCotizacion> itemsCotizacion;
    private Usuario vendedorActual;
    private double subtotal = 0;
    private double descuento = 0;
    private double iva = 0;
    private double total = 0;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Inicializar servicios
        inventarioService = new InventarioService();
        clienteService = new ClienteService();
        cotizacionService = new CotizacionService();
        facturaService = new FacturaService();
        usuarioService = new UsuarioService();
        promocionService = new PromocionService();
        
        // Obtener vendedor actual (usuario logueado)
        vendedorActual = usuarioService.getUsuarioActual();
        lblVendedor.setText("Vendedor: " + vendedorActual.getNombre());
        
        // Inicializar listas
        productosInventario = FXCollections.observableArrayList();
        itemsCotizacion = FXCollections.observableArrayList();
        
        // Configurar tablas
        configurarTablaInventario();
        configurarTablaCotizacion();
        
        // Cargar datos iniciales
        cargarInventario();
        cargarClientes();
        
        // Configurar eventos
        btnAgregar.setOnAction(this::agregarProducto);
        btnQuitar.setOnAction(this::quitarProducto);
        btnCrearCotizacion.setOnAction(this::crearCotizacion);
        btnGenerarOrden.setOnAction(this::generarOrden);
        btnGenerarFactura.setOnAction(this::generarFactura);
        
        // Escuchar cambios en cliente seleccionado para aplicar descuentos
        cmbCliente.setOnAction(e -> actualizarTotales());
        
        // Escuchar cambios en el campo de descuento adicional
        txtDescuentoAdicional.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*(\\.\\d*)?")) {
                txtDescuentoAdicional.setText(oldVal);
            } else {
                actualizarTotales();
            }
        });
    }
    
    private void configurarTablaInventario() {
        colCodigo.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCodigo()));
        colDescripcion.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescripcion()));
        colPrecio.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getPrecio()).asObject());
        colStock.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getStock()).asObject());
        colSerie.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNumeroSerie()));
        
        tblInventario.setItems(productosInventario);
    }
    
    private void configurarTablaCotizacion() {
        colItemCodigo.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProducto().getCodigo()));
        colItemDescripcion.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProducto().getDescripcion()));
        colItemCantidad.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getCantidad()).asObject());
        colItemPrecio.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getPrecioUnitario()).asObject());
        colItemSubtotal.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getSubtotal()).asObject());
        
        tblCotizacion.setItems(itemsCotizacion);
    }
    
    private void cargarInventario() {
        List<ProductoInventario> productos = inventarioService.obtenerProductosDisponibles();
        productosInventario.setAll(productos);
    }
    
    private void cargarClientes() {
        List<Cliente> clientes = clienteService.obtenerTodosClientes();
        cmbCliente.setItems(FXCollections.observableArrayList(clientes));
    }
    
    private void agregarProducto(ActionEvent event) {
        ProductoInventario producto = tblInventario.getSelectionModel().getSelectedItem();
        
        if (producto == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Selección requerida", "Por favor seleccione un producto del inventario");
            return;
        }
        
        // Verificar stock
        if (producto.getStock() <= 0) {
            mostrarAlerta(Alert.AlertType.WARNING, "Sin stock", "No hay unidades disponibles de este producto");
            return;
        }
        
        // Verificar si ya existe en la cotización
        boolean existente = false;
        for (ItemCotizacion item : itemsCotizacion) {
            if (item.getProducto().getCodigo().equals(producto.getCodigo())) {
                // Si el producto tiene número de serie, no permitir agregar más unidades
                if (producto.getNumeroSerie() != null && !producto.getNumeroSerie().isEmpty()) {
                    mostrarAlerta(Alert.AlertType.WARNING, "Producto con serie", 
                            "Este producto tiene número de serie y no puede agregarse más de una unidad");
                    return;
                }
                
                // Incrementar cantidad si hay stock suficiente
                if (item.getCantidad() < producto.getStock()) {
                    item.setCantidad(item.getCantidad() + 1);
                    item.actualizarSubtotal();
                    existente = true;
                    break;
                } else {
                    mostrarAlerta(Alert.AlertType.WARNING, "Stock insuficiente", 
                            "No hay más unidades disponibles de este producto");
                    return;
                }
            }
        }
        
        // Si no existe, agregar nuevo item
        if (!existente) {
            // Aplicar promoción si existe
            Promocion promocion = promocionService.buscarPromocionPorProducto(producto.getCodigo());
            double precioFinal = producto.getPrecio();
            
            if (promocion != null) {
                precioFinal = calcularPrecioConPromocion(producto.getPrecio(), promocion);
            }
            
            ItemCotizacion nuevoItem = new ItemCotizacion(producto, 1, precioFinal);
            itemsCotizacion.add(nuevoItem);
        }
        
        tblCotizacion.refresh();
        actualizarTotales();
    }
    
    private double calcularPrecioConPromocion(double precioOriginal, Promocion promocion) {
        if (promocion.getTipo().equals("PORCENTAJE")) {
            return precioOriginal * (1 - promocion.getValor() / 100);
        } else if (promocion.getTipo().equals("2X1")) {
            // Implementar lógica 2x1
            return precioOriginal / 2;
        }
        return precioOriginal;
    }
    
    private void quitarProducto(ActionEvent event) {
        ItemCotizacion item = tblCotizacion.getSelectionModel().getSelectedItem();
        
        if (item == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Selección requerida", "Por favor seleccione un producto de la cotización");
            return;
        }
        
        if (item.getCantidad() > 1) {
            item.setCantidad(item.getCantidad() - 1);
            item.actualizarSubtotal();
        } else {
            itemsCotizacion.remove(item);
        }
        
        tblCotizacion.refresh();
        actualizarTotales();
    }
    
    private void actualizarTotales() {
        // Calcular subtotal
        subtotal = itemsCotizacion.stream()
                .mapToDouble(ItemCotizacion::getSubtotal)
                .sum();
        
        // Calcular descuento
        descuento = 0;
        
        // Aplicar descuento por cliente mayorista
        Cliente clienteSeleccionado = cmbCliente.getValue();
        if (clienteSeleccionado != null && clienteSeleccionado.isMayorista()) {
            descuento += subtotal * 0.05; // 5% de descuento para mayoristas
        }
        
        // Aplicar descuento adicional (manual)
        String descuentoAdicionalStr = txtDescuentoAdicional.getText();
        if (descuentoAdicionalStr != null && !descuentoAdicionalStr.isEmpty()) {
            try {
                double descuentoAdicional = Double.parseDouble(descuentoAdicionalStr);
                descuento += subtotal * (descuentoAdicional / 100);
            } catch (NumberFormatException e) {
                // Ignorar error de formato
            }
        }
        
        // Calcular IVA (16%)
        double subtotalConDescuento = subtotal - descuento;
        iva = subtotalConDescuento * 0.16;
        
        // Calcular total
        total = subtotalConDescuento + iva;
        
        // Actualizar etiquetas
        lblSubtotal.setText(String.format("$%.2f", subtotal));
        lblDescuento.setText(String.format("$%.2f", descuento));
        lblIVA.setText(String.format("$%.2f", iva));
        lblTotal.setText(String.format("$%.2f", total));
    }
    
    private void crearCotizacion(ActionEvent event) {
        if (!validarFormulario()) {
            return;
        }
        
        Cliente cliente = cmbCliente.getValue();
        
        // Crear objeto cotización
        Cotizacion cotizacion = new Cotizacion();
        cotizacion.setCliente(cliente);
        cotizacion.setVendedor(vendedorActual);
        cotizacion.setFecha(LocalDateTime.now());
        cotizacion.setItems(new java.util.ArrayList<>(itemsCotizacion));
        cotizacion.setSubtotal(subtotal);
        cotizacion.setDescuento(descuento);
        cotizacion.setIva(iva);
        cotizacion.setTotal(total);
        
        // Guardar cotización
        Cotizacion cotizacionGuardada = cotizacionService.guardarCotizacion(cotizacion);
        
        if (cotizacionGuardada != null) {
            mostrarAlerta(Alert.AlertType.INFORMATION, "Cotización creada", 
                    "Cotización N° " + cotizacionGuardada.getId() + " creada exitosamente");
            
            // Habilitar botón para generar orden
            btnGenerarOrden.setDisable(false);
        } else {
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudo crear la cotización");
        }
    }
    
    private void generarOrden(ActionEvent event) {
        if (!validarInventario()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Inventario insuficiente", 
                    "No hay suficiente stock para algunos productos");
            return;
        }
        
        // Crear orden a partir de la cotización actual
        Orden orden = cotizacionService.convertirCotizacionAOrden(
                cotizacionService.obtenerUltimaCotizacion());
        
        if (orden != null) {
            mostrarAlerta(Alert.AlertType.INFORMATION, "Orden generada", 
                    "Orden N° " + orden.getId() + " generada exitosamente");
            
            // Habilitar botón para generar factura
            btnGenerarFactura.setDisable(false);
        } else {
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudo generar la orden");
        }
    }
    
    private void generarFactura(ActionEvent event) {
        // Genera factura automáticamente desde la última orden
        Orden ultimaOrden = cotizacionService.obtenerUltimaOrden();
        
        if (ultimaOrden == null) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "No hay órdenes para facturar");
            return;
        }
        
        // Crear y guardar factura
        Factura factura = facturaService.generarFacturaDesdeOrden(ultimaOrden);
        
        if (factura != null) {
            mostrarAlerta(Alert.AlertType.INFORMATION, "Factura generada", 
                    "Factura N° " + factura.getNumeroFactura() + " generada exitosamente");
            
            // Actualizar el inventario
            actualizarInventarioPostVenta(factura);
            
            // Resetear la interfaz
            limpiarFormulario();
        } else {
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudo generar la factura");
        }
    }
    
    private boolean validarInventario() {
        for (ItemCotizacion item : itemsCotizacion) {
            ProductoInventario productoActual = inventarioService
                    .obtenerProductoPorCodigo(item.getProducto().getCodigo());
            
            if (productoActual.getStock() < item.getCantidad()) {
                return false;
            }
        }
        return true;
    }
    
    private void actualizarInventarioPostVenta(Factura factura) {
        for (ItemFactura item : factura.getItems()) {
            inventarioService.actualizarStockProducto(
                    item.getProducto().getCodigo(), 
                    item.getCantidad());
        }
        // Recargar inventario
        cargarInventario();
    }
    
    private boolean validarFormulario() {
        if (cmbCliente.getValue() == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Cliente requerido", "Debe seleccionar un cliente");
            return false;
        }
        
        if (itemsCotizacion.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Sin productos", "Debe agregar al menos un producto");
            return false;
        }
        
        return true;
    }
    
    private void limpiarFormulario() {
        cmbCliente.setValue(null);
        itemsCotizacion.clear();
        txtDescuentoAdicional.clear();
        actualizarTotales();
        btnGenerarOrden.setDisable(true);
        btnGenerarFactura.setDisable(true);
    }
    
    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}