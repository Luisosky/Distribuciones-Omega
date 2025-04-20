package com.distribuciones.omega.controllers;

import com.distribuciones.omega.model.*;
import com.distribuciones.omega.service.*;
import com.distribuciones.omega.utils.AlertUtils;
import com.distribuciones.omega.utils.SessionManager;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Controlador para la generación de cotizaciones
 */
public class CotizacionController {

    // Componentes de la interfaz
    @FXML private ComboBox<Cliente> cmbCliente;
    @FXML private ComboBox<String> cmbTipoVenta;
    @FXML private DatePicker dpFecha;
    @FXML private TextField txtVendedor;
    
    @FXML private TextField txtBuscarProducto;
    @FXML private CheckBox chkMostrarSoloDisponibles;
    @FXML private TableView<ProductoInventario> tblProductos;
    @FXML private TableColumn<ProductoInventario, String> colCodigo;
    @FXML private TableColumn<ProductoInventario, String> colNombre;
    @FXML private TableColumn<ProductoInventario, String> colCategoria;
    @FXML private TableColumn<ProductoInventario, Double> colPrecioUnitario;
    @FXML private TableColumn<ProductoInventario, Integer> colStock;
    @FXML private TableColumn<ProductoInventario, Button> colAcciones;
    
    @FXML private TableView<ItemCotizacion> tblDetalleCotizacion;
    @FXML private TableColumn<ItemCotizacion, String> colItemCodigo;
    @FXML private TableColumn<ItemCotizacion, String> colItemNombre;
    @FXML private TableColumn<ItemCotizacion, Integer> colItemCantidad;
    @FXML private TableColumn<ItemCotizacion, Double> colItemPrecioUnitario;
    @FXML private TableColumn<ItemCotizacion, Double> colItemDescuento;
    @FXML private TableColumn<ItemCotizacion, Double> colItemSubtotal;
    @FXML private TableColumn<ItemCotizacion, Button> colItemAcciones;
    
    @FXML private Button btnAplicarPromocion;
    @FXML private Button btnAplicarDescuentoMayorista;
    @FXML private Button btnDescuentoManual;
    
    @FXML private Label lblSubtotal;
    @FXML private Label lblDescuentoTotal;
    @FXML private Label lblIva;
    @FXML private Label lblTotal;
    @FXML private TextArea txtObservaciones;
    @FXML private Button btnGuardar;
    @FXML private Button btnGuardarYFacturar;
    
    // Servicios
    private final ClienteService clienteService = new ClienteService();
    private final InventarioService inventarioService = new InventarioService();
    private final CotizacionService cotizacionService = new CotizacionService();
    private final PromocionService promocionService = new PromocionService();
    private final ContabilidadService contabilidadService = new ContabilidadService();
    private final FacturaService facturaService = new FacturaService();
    
    // Datos
    private ObservableList<ProductoInventario> productosData;
    private FilteredList<ProductoInventario> productosFiltrados;
    private ObservableList<ItemCotizacion> itemsCotizacion;
    private List<Promocion> promocionesDisponibles;
    private NumberFormat currencyFormat;
    private Usuario usuarioActual;
    private double subtotal = 0.0;
    private double descuentoTotal = 0.0;
    private double iva = 0.0;
    private double total = 0.0;
    private static final double IVA_RATE = 0.12;
    
    /**
     * Inicializa el controlador
     */
    @FXML
    public void initialize() {
        // Inicializar formato de moneda
        currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "EC"));
        
        // Obtener usuario actual desde la sesión
        usuarioActual = SessionManager.getInstance().getUsuarioActual();
        
        // Configurar fecha actual y vendedor
        dpFecha.setValue(LocalDate.now());
        txtVendedor.setText(usuarioActual != null ? usuarioActual.getNombre() : "Usuario no identificado");
        
        // Inicializar listas
        productosData = FXCollections.observableArrayList();
        itemsCotizacion = FXCollections.observableArrayList();
        
        // Configurar tipos de venta
        cmbTipoVenta.setItems(FXCollections.observableArrayList("Venta al Detalle", "Venta Mayorista"));
        cmbTipoVenta.getSelectionModel().selectFirst();
        
        // Cargar clientes
        cargarClientes();
        
        // Configurar tabla de productos
        configurarTablaProductos();
        
        // Configurar tabla de items de cotización
        configurarTablaItems();
        
        // Configurar búsqueda de productos
        configurarBusquedaProductos();
        
        // Cargar productos iniciales
        cargarProductos();
        
        // Cargar promociones disponibles
        cargarPromociones();
        
        // Configurar eventos para actualizar totales
        tblDetalleCotizacion.getItems().addListener((javafx.collections.ListChangeListener.Change<? extends ItemCotizacion> c) -> {
            calcularTotales();
        });
        
        // Habilitar/Deshabilitar botones según selección
        tblDetalleCotizacion.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            boolean haySeleccion = newSelection != null;
            btnDescuentoManual.setDisable(!haySeleccion);
        });
        
        // Configurar botón de descuento mayorista
        btnAplicarDescuentoMayorista.setDisable(true);
        cmbCliente.getSelectionModel().selectedItemProperty().addListener((obs, oldCliente, newCliente) -> {
            verificarClienteMayorista(newCliente);
        });
        
        cmbTipoVenta.getSelectionModel().selectedItemProperty().addListener((obs, oldTipo, newTipo) -> {
            if ("Venta Mayorista".equals(newTipo)) {
                verificarClienteMayorista(cmbCliente.getSelectionModel().getSelectedItem());
            } else {
                btnAplicarDescuentoMayorista.setDisable(true);
            }
        });
    }
    
    /**
     * Carga la lista de clientes en el ComboBox
     */
    private void cargarClientes() {
        try {
            List<Cliente> clientes = clienteService.obtenerTodosClientes();
            cmbCliente.setItems(FXCollections.observableArrayList(clientes));
            
            // Configurar cómo se muestran los clientes en el ComboBox
            cmbCliente.setConverter(new StringConverter<Cliente>() {
                @Override
                public String toString(Cliente cliente) {
                    return cliente == null ? "" : cliente.getNombre();
                }
                
                @Override
                public Cliente fromString(String string) {
                    return null; // No es necesario para ComboBox
                }
            });
        } catch (Exception e) {
            AlertUtils.mostrarError("Error al cargar clientes", 
                    "No se pudieron cargar los clientes. " + e.getMessage());
        }
    }
    
    /**
     * Configura la tabla de productos disponibles
     */
    private void configurarTablaProductos() {
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colCategoria.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategoria()));
        colPrecioUnitario.setCellValueFactory(new PropertyValueFactory<>("precio"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        
        // Formatear precio como moneda
        colPrecioUnitario.setCellFactory(tc -> new TableCell<ProductoInventario, Double>() {
            @Override
            protected void updateItem(Double precio, boolean empty) {
                super.updateItem(precio, empty);
                if (empty || precio == null) {
                    setText(null);
                } else {
                    setText(currencyFormat.format(precio));
                }
            }
        });
        
        // Configurar columna de acciones
        colAcciones.setCellFactory(tc -> new TableCell<ProductoInventario, Button>() {
            private final Button btnAgregar = new Button("Agregar");
            
            {
                btnAgregar.setOnAction(event -> {
                    ProductoInventario producto = getTableView().getItems().get(getIndex());
                    mostrarDialogoCantidad(producto);
                });
            }
            
            @Override
            protected void updateItem(Button item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btnAgregar);
                }
            }
        });
    }
    
    /**
     * Configura la tabla de items de la cotización
     */
    private void configurarTablaItems() {
        colItemCodigo.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getProducto().getCodigo()));
        colItemNombre.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getProducto().getDescripcion()));
        colItemCantidad.setCellValueFactory(data -> new SimpleIntegerProperty(
                data.getValue().getCantidad()).asObject());
        colItemPrecioUnitario.setCellValueFactory(data -> new SimpleDoubleProperty(
                data.getValue().getPrecioUnitario()).asObject());
        colItemDescuento.setCellValueFactory(data -> new SimpleDoubleProperty(
                data.getValue().getDescuento()).asObject());
        colItemSubtotal.setCellValueFactory(data -> new SimpleDoubleProperty(
                data.getValue().getSubtotal()).asObject());
        
        // Formatear valores monetarios
        colItemPrecioUnitario.setCellFactory(tc -> new TableCell<ItemCotizacion, Double>() {
            @Override
            protected void updateItem(Double precio, boolean empty) {
                super.updateItem(precio, empty);
                if (empty || precio == null) {
                    setText(null);
                } else {
                    setText(currencyFormat.format(precio));
                }
            }
        });
        
        colItemDescuento.setCellFactory(tc -> new TableCell<ItemCotizacion, Double>() {
            @Override
            protected void updateItem(Double descuento, boolean empty) {
                super.updateItem(descuento, empty);
                if (empty || descuento == null) {
                    setText(null);
                } else {
                    setText(currencyFormat.format(descuento));
                }
            }
        });
        
        colItemSubtotal.setCellFactory(tc -> new TableCell<ItemCotizacion, Double>() {
            @Override
            protected void updateItem(Double subtotal, boolean empty) {
                super.updateItem(subtotal, empty);
                if (empty || subtotal == null) {
                    setText(null);
                } else {
                    setText(currencyFormat.format(subtotal));
                }
            }
        });
        
        // Configurar columna de acciones
        colItemAcciones.setCellFactory(tc -> new TableCell<ItemCotizacion, Button>() {
            private final Button btnEliminar = new Button("Eliminar");
            private final Button btnEditar = new Button("Editar");
            private final HBox box = new HBox(5, btnEditar, btnEliminar);
            
            {
                btnEliminar.setOnAction(event -> {
                    ItemCotizacion item = getTableView().getItems().get(getIndex());
                    eliminarItem(item);
                });
                
                btnEditar.setOnAction(event -> {
                    ItemCotizacion item = getTableView().getItems().get(getIndex());
                    editarItem(item);
                });
            }
            
            @Override
            protected void updateItem(Button item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(box);
                }
            }
        });
        
        tblDetalleCotizacion.setItems(itemsCotizacion);
    }
    
    /**
     * Configura el filtro de búsqueda de productos
     */
    private void configurarBusquedaProductos() {
        productosFiltrados = new FilteredList<>(productosData, p -> true);
        
        txtBuscarProducto.textProperty().addListener((observable, oldValue, newValue) -> {
            productosFiltrados.setPredicate(producto -> {
                if (newValue == null || newValue.isEmpty()) {
                    return mostrarProductoSegunStock(producto);
                }
                
                String lowerCaseFilter = newValue.toLowerCase();
                
                if (producto.getCodigo().toLowerCase().contains(lowerCaseFilter)) {
                    return mostrarProductoSegunStock(producto);
                } else if (producto.getDescripcion().toLowerCase().contains(lowerCaseFilter)) {
                    return mostrarProductoSegunStock(producto);
                }
                return false;
            });
        });
        
        chkMostrarSoloDisponibles.selectedProperty().addListener((observable, oldValue, newValue) -> {
            productosFiltrados.setPredicate(producto -> {
                if (txtBuscarProducto.getText() == null || txtBuscarProducto.getText().isEmpty()) {
                    return mostrarProductoSegunStock(producto);
                }
                
                String lowerCaseFilter = txtBuscarProducto.getText().toLowerCase();
                
                if (producto.getCodigo().toLowerCase().contains(lowerCaseFilter)) {
                    return mostrarProductoSegunStock(producto);
                } else if (producto.getDescripcion().toLowerCase().contains(lowerCaseFilter)) {
                    return mostrarProductoSegunStock(producto);
                }
                return false;
            });
        });
        
        tblProductos.setItems(productosFiltrados);
    }
    
    /**
     * Determina si un producto debe mostrarse según su disponibilidad de stock
     */
    private boolean mostrarProductoSegunStock(ProductoInventario producto) {
        if (chkMostrarSoloDisponibles.isSelected()) {
            return producto.getStock() > 0;
        }
        return true;
    }
    
    /**
     * Carga los productos del inventario
     */
    private void cargarProductos() {
        try {
            List<ProductoInventario> productos = inventarioService.obtenerProductosDisponibles();
            productosData.clear();
            productosData.addAll(productos);
        } catch (Exception e) {
            AlertUtils.mostrarError("Error al cargar productos", 
                    "No se pudieron cargar los productos. " + e.getMessage());
        }
    }
    
    /**
     * Carga las promociones disponibles
     */
    private void cargarPromociones() {
        // Simplificar - No cargar de la base de datos
        promocionesDisponibles = new ArrayList<>();
        
        // Crear promociones predefinidas
        Promocion promoGeneral = new Promocion();
        promoGeneral.setId(1);
        promoGeneral.setDescripcion("Descuento General 5%");
        promoGeneral.setValor(5.0);
        promoGeneral.setPorcentaje(true);
        promoGeneral.setCategoriasAplicables("TODAS");
        promoGeneral.setActiva(true);
        
        Promocion promoElectronicos = new Promocion();
        promoElectronicos.setId(2);
        promoElectronicos.setDescripcion("Descuento Electrónicos 10%");
        promoElectronicos.setValor(10.0);
        promoElectronicos.setPorcentaje(true);
        promoElectronicos.setCategoriasAplicables("ELECTRONICOS,COMPUTACION");
        promoElectronicos.setActiva(true);
        
        promocionesDisponibles.add(promoGeneral);
        promocionesDisponibles.add(promoElectronicos);
        
        // Habilitar/deshabilitar botón de promoción
        btnAplicarPromocion.setDisable(promocionesDisponibles.isEmpty());
    }
    
    /**
     * Verifica si el cliente seleccionado es mayorista
     */
    private void verificarClienteMayorista(Cliente cliente) {
        boolean esMayorista = cliente != null && cliente.isMayorista();
        boolean esVentaMayorista = "Venta Mayorista".equals(cmbTipoVenta.getValue());
        btnAplicarDescuentoMayorista.setDisable(!(esMayorista && esVentaMayorista));
    }
    
    /**
     * Muestra un diálogo para ingresar la cantidad de un producto
     */
    private void mostrarDialogoCantidad(ProductoInventario producto) {
        try {
            // Crear diálogo
            Dialog<Integer> dialog = new Dialog<>();
            dialog.setTitle("Agregar Producto");
            dialog.setHeaderText("Ingrese la cantidad para: " + producto.getDescripcion());
            
            // Configurar botones
            ButtonType btnAceptar = new ButtonType("Aceptar", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(btnAceptar, ButtonType.CANCEL);
            
            // Crear campo para cantidad
            Spinner<Integer> spnCantidad = new Spinner<>(1, producto.getStock(), 1);
            spnCantidad.setEditable(true);
            
            // Crear layout
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));
            
            grid.add(new Label("Producto:"), 0, 0);
            grid.add(new Label(producto.getDescripcion()), 1, 0);
            grid.add(new Label("Stock disponible:"), 0, 1);
            grid.add(new Label(String.valueOf(producto.getStock())), 1, 1);
            grid.add(new Label("Precio unitario:"), 0, 2);
            grid.add(new Label(currencyFormat.format(producto.getPrecio())), 1, 2);
            grid.add(new Label("Cantidad:"), 0, 3);
            grid.add(spnCantidad, 1, 3);
            
            dialog.getDialogPane().setContent(grid);
            
            // Convertir resultado
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == btnAceptar) {
                    return spnCantidad.getValue();
                }
                return null;
            });
            
            // Mostrar y procesar resultado
            Optional<Integer> result = dialog.showAndWait();
            result.ifPresent(cantidad -> {
                if (cantidad > 0 && cantidad <= producto.getStock()) {
                    agregarProductoACotizacion(producto, cantidad);
                } else {
                    AlertUtils.mostrarError("Cantidad inválida", 
                            "La cantidad debe ser mayor a 0 y no puede exceder el stock disponible.");
                }
            });
            
        } catch (Exception e) {
            AlertUtils.mostrarError("Error", "Ocurrió un error al agregar el producto: " + e.getMessage());
        }
    }
    
    /**
     * Agrega un producto a la cotización
     */
    private void agregarProductoACotizacion(ProductoInventario producto, int cantidad) {
        // Verificar si el producto ya está en la lista
        for (ItemCotizacion item : itemsCotizacion) {
            if (item.getProducto().getCodigo().equals(producto.getCodigo())) {
                // Si ya existe, solo actualizar la cantidad
                int nuevaCantidad = item.getCantidad() + cantidad;
                if (nuevaCantidad <= producto.getStock()) {
                    item.setCantidad(nuevaCantidad);
                    item.calcularSubtotal();
                    tblDetalleCotizacion.refresh();
                    calcularTotales();
                    return;
                } else {
                    AlertUtils.mostrarError("Stock insuficiente", 
                            "No hay suficiente stock para agregar más unidades de este producto.");
                    return;
                }
            }
        }
        
        // Si no existe, crear un nuevo item
        ItemCotizacion nuevoItem = new ItemCotizacion();
        nuevoItem.setProducto(producto);
        nuevoItem.setCantidad(cantidad);
        nuevoItem.setPrecioUnitario(producto.getPrecio());
        nuevoItem.setDescuento(0.0); // Sin descuento inicial
        nuevoItem.calcularSubtotal();
        
        itemsCotizacion.add(nuevoItem);
        calcularTotales();
    }
    
    /**
     * Elimina un item de la cotización
     */
    private void eliminarItem(ItemCotizacion item) {
        itemsCotizacion.remove(item);
        calcularTotales();
    }
    
    /**
     * Edita un item de la cotización (cantidad y/o descuento)
     */
    private void editarItem(ItemCotizacion item) {
        try {
            // Crear diálogo
            Dialog<ItemCotizacion> dialog = new Dialog<>();
            dialog.setTitle("Editar Item");
            dialog.setHeaderText("Modificar: " + item.getProducto().getDescripcion());
            
            // Configurar botones
            ButtonType btnAceptar = new ButtonType("Aceptar", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(btnAceptar, ButtonType.CANCEL);
            
            // Crear campos
            Spinner<Integer> spnCantidad = new Spinner<>(1, item.getProducto().getStock(), item.getCantidad());
            spnCantidad.setEditable(true);
            
            TextField txtDescuento = new TextField(String.valueOf(item.getDescuento()));
            txtDescuento.setPromptText("Valor del descuento");
            
            // Crear layout
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));
            
            grid.add(new Label("Producto:"), 0, 0);
            grid.add(new Label(item.getProducto().getDescripcion()), 1, 0);
            grid.add(new Label("Stock disponible:"), 0, 1);
            grid.add(new Label(String.valueOf(item.getProducto().getStock())), 1, 1);
            grid.add(new Label("Precio unitario:"), 0, 2);
            grid.add(new Label(currencyFormat.format(item.getPrecioUnitario())), 1, 2);
            grid.add(new Label("Cantidad:"), 0, 3);
            grid.add(spnCantidad, 1, 3);
            grid.add(new Label("Descuento:"), 0, 4);
            grid.add(txtDescuento, 1, 4);
            
            dialog.getDialogPane().setContent(grid);
            
            // Convertir resultado
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == btnAceptar) {
                    try {
                        int cantidad = spnCantidad.getValue();
                        double descuento = Double.parseDouble(txtDescuento.getText());
                        
                        if (cantidad <= 0 || cantidad > item.getProducto().getStock()) {
                            AlertUtils.mostrarError("Cantidad inválida", 
                                    "La cantidad debe ser mayor a 0 y no puede exceder el stock disponible.");
                            return null;
                        }
                        
                        if (descuento < 0) {
                            AlertUtils.mostrarError("Descuento inválido", 
                                    "El descuento no puede ser negativo.");
                            return null;
                        }
                        
                        item.setCantidad(cantidad);
                        item.setDescuento(descuento);
                        return item;
                    } catch (NumberFormatException e) {
                        AlertUtils.mostrarError("Valor inválido", 
                                "Por favor ingrese valores numéricos válidos.");
                        return null;
                    }
                }
                return null;
            });
            
            // Mostrar y procesar resultado
            Optional<ItemCotizacion> result = dialog.showAndWait();
            result.ifPresent(itemActualizado -> {
                itemActualizado.calcularSubtotal();
                tblDetalleCotizacion.refresh();
                calcularTotales();
            });
            
        } catch (Exception e) {
            AlertUtils.mostrarError("Error", "Ocurrió un error al editar el item: " + e.getMessage());
        }
    }
    
    /**
     * Calcula los totales de la cotización
     */
    private void calcularTotales() {
        subtotal = 0.0;
        descuentoTotal = 0.0;
        
        for (ItemCotizacion item : itemsCotizacion) {
            subtotal += item.getPrecioUnitario() * item.getCantidad();
            descuentoTotal += item.getDescuento();
        }
        
        iva = (subtotal - descuentoTotal) * IVA_RATE;
        total = subtotal - descuentoTotal + iva;
        
        // Actualizar etiquetas
        lblSubtotal.setText(currencyFormat.format(subtotal));
        lblDescuentoTotal.setText(currencyFormat.format(descuentoTotal));
        lblIva.setText(currencyFormat.format(iva));
        lblTotal.setText(currencyFormat.format(total));
    }
    
    /**
     * Muestra el diálogo para crear un nuevo cliente
     */
    @FXML
    private void mostrarNuevoCliente() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/cliente-form.fxml"));
            Parent root = loader.load();
            
            Stage stage = new Stage();
            stage.setTitle("Nuevo Cliente");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
            
            // Recargar lista de clientes
            cargarClientes();
            
        } catch (IOException e) {
            AlertUtils.mostrarError("Error", "No se pudo abrir el formulario de clientes: " + e.getMessage());
        }
    }
    
    /**
     * Ejecuta la búsqueda de productos
     */
    @FXML
    private void buscarProductos() {
        String termino = txtBuscarProducto.getText().trim();
        if (termino.isEmpty()) {
            productosFiltrados.setPredicate(producto -> mostrarProductoSegunStock(producto));
        } else {
            productosFiltrados.setPredicate(producto -> {
                if (producto.getCodigo().toLowerCase().contains(termino.toLowerCase()) ||
                    producto.getDescripcion().toLowerCase().contains(termino.toLowerCase())) {
                    return mostrarProductoSegunStock(producto);
                }
                return false;
            });
        }
    }
    
    /**
     * Aplica una promoción a los items seleccionados
     */
    @FXML
    private void aplicarPromocion() {
        if (promocionesDisponibles.isEmpty()) {
            AlertUtils.mostrarAdvertencia("Sin promociones", 
                    "No hay promociones disponibles en este momento.");
            return;
        }
        
        // Crear diálogo para seleccionar promoción
        ChoiceDialog<Promocion> dialog = new ChoiceDialog<>(
                promocionesDisponibles.get(0), promocionesDisponibles);
        dialog.setTitle("Aplicar Promoción");
        dialog.setHeaderText("Seleccione una promoción para aplicar");
        dialog.setContentText("Promoción:");
        
        // Configurar cómo se muestran las promociones
        dialog.getItems().setAll(promocionesDisponibles);
        
        StringConverter<Promocion> converter = new StringConverter<Promocion>() {
            @Override
            public String toString(Promocion promocion) {
                return promocion.getDescripcion() + " - " + 
                        (promocion.isPorcentaje() ? promocion.getValor() + "%" : 
                            currencyFormat.format(promocion.getValor()));
            }
            
            @Override
            public Promocion fromString(String string) {
                return null;
            }
        };
        
        ((ComboBox<Promocion>) dialog.getDialogPane().lookup(".combo-box")).setConverter(converter);
        
        // Mostrar y procesar
        Optional<Promocion> result = dialog.showAndWait();
        result.ifPresent(this::aplicarPromocionSeleccionada);
    }
    
    /**
     * Aplica la promoción seleccionada a los items
     */
    private void aplicarPromocionSeleccionada(Promocion promocion) {
        if (itemsCotizacion.isEmpty()) {
            AlertUtils.mostrarAdvertencia("Sin items", 
                    "No hay items en la cotización para aplicar la promoción.");
            return;
        }
        
        // Verificar categorías aplicables
        List<String> categoriasAplicables = Arrays.asList(
                promocion.getCategoriasAplicables().split(","));
        
        for (ItemCotizacion item : itemsCotizacion) {
            String categoriaItem = item.getProducto().getCategoria();
            
            // Si la promoción aplica a todas las categorías o a la categoría del item
            if (categoriasAplicables.contains("TODAS") || 
                    categoriasAplicables.contains(categoriaItem)) {
                
                double descuentoActual = item.getDescuento();
                double nuevoDescuento;
                
                if (promocion.isPorcentaje()) {
                    // Descuento porcentual
                    double porcentaje = promocion.getValor() / 100.0;
                    nuevoDescuento = item.getPrecioUnitario() * item.getCantidad() * porcentaje;
                } else {
                    // Descuento de valor fijo
                    nuevoDescuento = promocion.getValor();
                }
                
                // Aplicar el descuento mayor
                if (nuevoDescuento > descuentoActual) {
                    item.setDescuento(nuevoDescuento);
                    item.calcularSubtotal();
                }
            }
        }
        
        tblDetalleCotizacion.refresh();
        calcularTotales();
        
        AlertUtils.mostrarInformacion("Promoción Aplicada", 
                "La promoción " + promocion.getDescripcion() + " ha sido aplicada correctamente.");
    }
    
    /**
     * Aplica descuento para cliente mayorista
     */
    @FXML
    private void aplicarDescuentoMayorista() {
        Cliente cliente = cmbCliente.getSelectionModel().getSelectedItem();
        if (cliente == null || !cliente.isMayorista()) {
            AlertUtils.mostrarAdvertencia("Cliente no mayorista", 
                    "El cliente seleccionado no es mayorista o no hay cliente seleccionado.");
            return;
        }
        
        if (itemsCotizacion.isEmpty()) {
            AlertUtils.mostrarAdvertencia("Sin items", 
                    "No hay items en la cotización para aplicar descuento mayorista.");
            return;
        }
        
        // Aplicar descuento a todos los items (por ejemplo, 10%)
        double porcentajeDescuento = 0.10; // 10% de descuento para mayoristas
        
        for (ItemCotizacion item : itemsCotizacion) {
            double descuentoMayorista = item.getPrecioUnitario() * item.getCantidad() * porcentajeDescuento;
            item.setDescuento(descuentoMayorista);
            item.calcularSubtotal();
        }
        
        tblDetalleCotizacion.refresh();
        calcularTotales();
        
        AlertUtils.mostrarInformacion("Descuento Aplicado", 
                "Se ha aplicado el descuento mayorista del 10% a todos los items.");
    }
    
    /**
     * Aplica un descuento manual al item seleccionado
     */
    @FXML
    private void aplicarDescuentoManual() {
        ItemCotizacion item = tblDetalleCotizacion.getSelectionModel().getSelectedItem();
        if (item == null) {
            AlertUtils.mostrarAdvertencia("Selección requerida", 
                    "Por favor, seleccione un item para aplicar el descuento.");
            return;
        }
        
        // Crear diálogo para ingresar descuento
        TextInputDialog dialog = new TextInputDialog(String.valueOf(item.getDescuento()));
        dialog.setTitle("Descuento Manual");
        dialog.setHeaderText("Aplicar descuento manual para: " + item.getProducto().getDescripcion());
        dialog.setContentText("Valor del descuento:");
        
        // Mostrar y procesar
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(valor -> {
            try {
                double descuento = Double.parseDouble(valor);
                if (descuento < 0) {
                    AlertUtils.mostrarError("Valor inválido", 
                            "El descuento no puede ser negativo.");
                    return;
                }
                
                double subtotalItem = item.getPrecioUnitario() * item.getCantidad();
                if (descuento > subtotalItem) {
                    AlertUtils.mostrarError("Valor inválido", 
                            "El descuento no puede ser mayor que el subtotal del item.");
                    return;
                }
                
                item.setDescuento(descuento);
                item.calcularSubtotal();
                tblDetalleCotizacion.refresh();
                calcularTotales();
                
            } catch (NumberFormatException e) {
                AlertUtils.mostrarError("Valor inválido", 
                        "Por favor ingrese un valor numérico válido.");
            }
        });
    }
    
    /**
     * Cancela la cotización actual
     */
    @FXML
    private void cancelarCotizacion() {
        if (itemsCotizacion.isEmpty()) {
            cerrarVentana();
            return;
        }
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Cancelar Cotización");
        alert.setHeaderText("¿Está seguro de cancelar esta cotización?");
        alert.setContentText("Todos los cambios se perderán.");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            cerrarVentana();
        }
    }
    
    /**
     * Guarda la cotización
     */
    @FXML
    private void guardarCotizacion() {
        if (!validarCotizacion()) {
            return;
        }
        
        try {
            Cotizacion cotizacion = crearCotizacion();
            cotizacion = cotizacionService.guardarCotizacion(cotizacion);
            
            // Registrar movimiento contable
            registrarMovimientoContable(cotizacion, "COTIZACION");
            
            AlertUtils.mostrarInformacion("Cotización Guardada", 
                    "La cotización se ha guardado exitosamente con el número: " + cotizacion.getId());
            
            cerrarVentana();
            
        } catch (Exception e) {
            AlertUtils.mostrarError("Error al guardar", 
                    "No se pudo guardar la cotización: " + e.getMessage());
        }
    }
 
    /**
     * Guarda la cotización y genera una factura
     */
    @FXML
    private void guardarYFacturar() {
        if (!validarCotizacion()) {
            return;
        }
        
        try {
            // 1. Crear y guardar la cotización
            Cotizacion cotizacion = crearCotizacion();
            cotizacion = cotizacionService.guardarCotizacion(cotizacion);
            
            // 2. Actualizar la cotización como convertida a orden
            cotizacion.setConvertidaAOrden(true);
            cotizacionService.actualizarCotizacion(cotizacion);
            
            // 3. Verificar disponibilidad de stock para todos los productos
            boolean stockSuficiente = true;
            StringBuilder mensajeError = new StringBuilder("No hay suficiente stock para los productos:\n");
            
            for (ItemCotizacion item : cotizacion.getItems()) {
                ProductoInventario producto = item.getProducto();
                
                // Debug mejorado para claridad
                System.out.println("Verificando producto: " + producto.getDescripcion() + 
                                " (ID: " + producto.getIdProducto() + 
                                ", Código: " + producto.getCodigo() + 
                                ", Stock actual: " + producto.getStock() + 
                                ", Solicitado: " + item.getCantidad() + ")");
                
                // Validar disponibilidad usando ambos identificadores para mayor seguridad
                if (!inventarioService.verificarDisponibilidad(producto, item.getCantidad())) {
                    stockSuficiente = false;
                    mensajeError.append("- ").append(producto.getDescripcion())
                            .append(" (Solicitado: ").append(item.getCantidad())
                            .append(", Disponible: ").append(producto.getStock()).append(")\n");
                }
            }
            
            if (!stockSuficiente) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Stock Insuficiente");
                alert.setHeaderText("No hay suficiente stock");
                alert.setContentText(mensajeError.toString());
                alert.showAndWait();
                return;
            }
            
            // 4. Generar la factura a partir de la cotización
            Factura factura = facturaService.generarFacturaDesdeContizacion(cotizacion);
            
            if (factura == null || factura.getId() == 0) {
                throw new Exception("No se pudo generar la factura. Verifique los datos e intente nuevamente.");
            }
            
            // 5. Actualizar stock - Con corrección para usar el objeto producto completo
            boolean todosActualizados = true;
            for (ItemCotizacion item : cotizacion.getItems()) {
                ProductoInventario producto = item.getProducto();
                
                // Actualizar usando el objeto producto completo
                if (!inventarioService.actualizarStockProducto(producto, item.getCantidad())) {
                    System.err.println("Error al actualizar stock para: " + producto.getDescripcion());
                    todosActualizados = false;
                } else {
                    System.out.println("Stock actualizado correctamente para: " + producto.getDescripcion() + 
                                    " (Nuevo stock: " + (producto.getStock() - item.getCantidad()) + ")");
                }
            }
            
            // 6. Mostrar mensaje de factura generada
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Factura Generada");
            alert.setHeaderText("Operación Exitosa");
            alert.setContentText("La factura se ha generado exitosamente con el número: " + factura.getNumeroFactura());
            
            ButtonType btnProcederPago = new ButtonType("Proceder al Pago");
            ButtonType btnCerrar = new ButtonType("Cerrar", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(btnProcederPago, btnCerrar);
            
            Optional<ButtonType> result = alert.showAndWait();
            
            if (result.isPresent() && result.get() == btnProcederPago) {
                // 7. Navegar a la pantalla de pago - IMPLEMENTACIÓN CORREGIDA
                try {
                    // Cargar el FXML de pago con manejo explícito de errores
                    FXMLLoader loader = new FXMLLoader();
                    loader.setLocation(getClass().getResource("/fxml/pago.fxml"));
                    Parent root = loader.load();
                    
                    // Aplicar CSS programáticamente solo si existe
                    Scene scene = new Scene(root);
                    try {
                        String cssPath = "/css/pago.css";
                        if (getClass().getResource(cssPath) != null) {
                            scene.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());
                        }
                    } catch (Exception cssEx) {
                        System.err.println("Advertencia: No se pudo cargar el CSS: " + cssEx.getMessage());
                        // Continuar sin el CSS
                    }
                    
                    // Obtener el controlador y pasarle la factura
                    PagoController pagoController = loader.getController();
                    pagoController.inicializarDatos(factura);
                    
                    // Crear un nuevo Stage para mostrar la pantalla de pago
                    Stage pagoStage = new Stage();
                    pagoStage.setTitle("Pago de Factura");
                    pagoStage.setScene(scene);
                    pagoStage.initModality(Modality.APPLICATION_MODAL);
                    pagoStage.initOwner(txtVendedor.getScene().getWindow());
                    
                    // Mostrar y esperar
                    pagoStage.showAndWait();
                    
                    // Al cerrar la ventana de pago, limpiar formulario si el pago fue exitoso
                    if (factura.isPagada()) {
                        limpiarFormulario();
                    }
                    
                } catch (IOException e) {
                    e.printStackTrace();
                    AlertUtils.mostrarError("Error de navegación", 
                            "No se pudo abrir la pantalla de pagos: " + e.getMessage());
                }
            } else {
                // 8. Limpiar el formulario si no se va a pagar ahora
                limpiarFormulario();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error al Facturar");
            alert.setHeaderText("Ha ocurrido un error");
            alert.setContentText("No se pudo generar la factura: " + e.getMessage());
            
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String exceptionText = sw.toString();
            
            TextArea textArea = new TextArea(exceptionText);
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setMaxWidth(Double.MAX_VALUE);
            textArea.setMaxHeight(Double.MAX_VALUE);
            
            alert.getDialogPane().setExpandableContent(textArea);
            alert.getDialogPane().setExpanded(true);
            alert.showAndWait();
        }
    }
    
    /**
     * Limpia todos los campos del formulario y restaura el estado inicial
     */
    private void limpiarFormulario() {
        try {
            // 1. Resetear componentes de información general
            cmbCliente.getSelectionModel().clearSelection();
            cmbTipoVenta.getSelectionModel().selectFirst(); // Selecciona "Venta al Detalle"
            dpFecha.setValue(LocalDate.now());
            
            // 2. Limpiar búsqueda de productos
            txtBuscarProducto.clear();
            chkMostrarSoloDisponibles.setSelected(true); // Valor predeterminado en FXML
            productosFiltrados.setPredicate(producto -> mostrarProductoSegunStock(producto)); // Aplicar filtro original
            
            // 3. Limpiar tabla de detalle de cotización
            itemsCotizacion.clear();
            tblDetalleCotizacion.refresh();
            
            // 4. Restablecer botones de descuento
            btnAplicarPromocion.setDisable(promocionesDisponibles.isEmpty());
            btnAplicarDescuentoMayorista.setDisable(true);
            btnDescuentoManual.setDisable(true);
            
            // 5. Reiniciar totales y etiquetas
            subtotal = 0.0;
            descuentoTotal = 0.0;
            iva = 0.0;
            total = 0.0;
            
            lblSubtotal.setText(currencyFormat.format(0.0));
            lblDescuentoTotal.setText(currencyFormat.format(0.0));
            lblIva.setText(currencyFormat.format(0.0));
            lblTotal.setText(currencyFormat.format(0.0));
            
            // 6. Limpiar observaciones
            txtObservaciones.clear();
            
            // 7. Volver a cargar productos para actualizar stock
            cargarProductos();
            
            // 8. Restablecer estado de los botones de acción (opcional)
            btnGuardar.setDisable(false);
            btnGuardarYFacturar.setDisable(false);
            
            System.out.println("Formulario limpiado correctamente");
        } catch (Exception e) {
            System.err.println("Error al limpiar el formulario: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Valida los datos de la cotización
     */
    private boolean validarCotizacion() {
        if (cmbCliente.getSelectionModel().getSelectedItem() == null) {
            AlertUtils.mostrarAdvertencia("Cliente requerido", 
                    "Debe seleccionar un cliente para la cotización.");
            return false;
        }
        
        if (itemsCotizacion.isEmpty()) {
            AlertUtils.mostrarAdvertencia("Items requeridos", 
                    "Debe agregar al menos un producto a la cotización.");
            return false;
        }
        
        return true;
    }
    
    /**
     * Crea un objeto Cotizacion con los datos actuales
     */
    private Cotizacion crearCotizacion() {
        Cotizacion cotizacion = new Cotizacion();
        
        cotizacion.setCliente(cmbCliente.getSelectionModel().getSelectedItem());
        cotizacion.setVendedor(usuarioActual);
        cotizacion.setFecha(LocalDateTime.now());
        
        // En lugar de setTipoVenta, podemos guardar esta información en las observaciones
        String observaciones = "Tipo de venta: " + cmbTipoVenta.getValue() + "\n";
        if (txtObservaciones.getText() != null && !txtObservaciones.getText().isEmpty()) {
            observaciones += txtObservaciones.getText();
        }
        // Nota: El modelo Cotizacion no tiene campo para observaciones, 
        // podríamos agregar esta información a la numeración o crear el campo
        
        cotizacion.setSubtotal(subtotal);
        cotizacion.setDescuento(descuentoTotal); // Usar setDescuento en lugar de setDescuentoTotal
        cotizacion.setIva(iva);
        cotizacion.setTotal(total);
        
        // Generar número de cotización basado en la fecha y un identificador aleatorio
        String numeroCotizacion = "COT-" + 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + 
            "-" + String.format("%04d", (int)(Math.random() * 10000));
        cotizacion.setNumeroCotizacion(numeroCotizacion);
        
        // Agregar items
        for (ItemCotizacion item : itemsCotizacion) {
            cotizacion.agregarItem(item);
        }
        
        return cotizacion;
    }
    
    /**
     * Registra un movimiento contable para la transacción
     */
    private void registrarMovimientoContable(Object documento, String tipoDocumento) {
        try {
            if (tipoDocumento.equals("COTIZACION")) {
                Cotizacion cotizacion = (Cotizacion) documento;
                contabilidadService.registrarMovimientoCotizacion(cotizacion);
            } else if (tipoDocumento.equals("FACTURA")) {
                Factura factura = (Factura) documento;
                contabilidadService.registrarMovimientoFactura(factura);
            }
        } catch (Exception e) {
            System.err.println("Error al registrar movimiento contable: " + e.getMessage());
        }
    }
    
    /**
     * Cierra la ventana actual
     */
    private void cerrarVentana() {
        Stage stage = (Stage) txtVendedor.getScene().getWindow();
        stage.close();
    }
}