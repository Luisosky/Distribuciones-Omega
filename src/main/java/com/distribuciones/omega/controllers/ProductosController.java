package com.distribuciones.omega.controllers;

import com.distribuciones.omega.dao.ProductoDAO;
import com.distribuciones.omega.model.Producto;
import com.distribuciones.omega.model.Categoria;
import com.distribuciones.omega.model.InsumoOficina;
import com.distribuciones.omega.model.ProductoMobilario;
import com.distribuciones.omega.model.ProductoTecnologico;

import io.github.cdimascio.dotenv.Dotenv;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Optional;

public class ProductosController {

    @FXML private TableView<Producto> tableProductos;
    @FXML private TableColumn<Producto, String> colId;
    @FXML private TableColumn<Producto, String> colNombre;
    @FXML private TableColumn<Producto, Double> colPrecio;
    @FXML private TableColumn<Producto, Integer> colCantidad;
    @FXML private TableColumn<Producto, Categoria> colCategoria;
    
    @FXML private TextField txtBuscar;
    @FXML private Button btnBuscar;
    @FXML private Button btnAgregar;
    @FXML private Button btnEditar;
    @FXML private Button btnEliminar;
    
    @FXML private Label lblTotalProductos;
    @FXML private Label lblValorInventario;
    
    private ProductoDAO dao;
    private ObservableList<Producto> productosList = FXCollections.observableArrayList();
    private FilteredList<Producto> filteredProductos;
    
    @FXML
    void initialize() {
        try {
            // Inicializar conexión a la base de datos
            Dotenv dotenv = Dotenv.configure().load();
            String dbUrl = dotenv.get("DB_URL");
            String dbUser = dotenv.get("DB_USER");
            String dbPass = dotenv.get("DB_PASS");

            System.out.println("Intentando conectar a: " + dbUrl);
            System.out.println("Usuario: " + dbUser);
            
            // Registrar el driver explícitamente
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
            dao = new ProductoDAO(conn);
            
            // Crear la tabla si no existe
            dao.createTableIfNotExists();
            
            // Configurar la tabla
            setupTable();
            
            // Cargar datos
            loadProductos();
            
            // Configurar eventos de botones
            setupButtons();
            
            // Configurar búsqueda
            setupSearch();
            
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Error de conexión", "No se pudo conectar a la base de datos", e.getMessage());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            showError("Error de driver", "No se encontró el driver de MySQL", e.getMessage());
        }
    }
    
    private void setupTable() {
        colId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getId()));
        colNombre.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNombre()));
        colPrecio.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getPrecio()).asObject());
        colPrecio.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "MX"));
                    setText(currencyFormat.format(item));
                }
            }
        });
        colCantidad.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getCantidad()).asObject());
        colCategoria.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getCategoria()));
        
        tableProductos.setItems(productosList);
    }
    
    private void loadProductos() {
        try {
            productosList.clear();
            productosList.addAll(dao.getAllProductos());
            
            updateStats();
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Error de carga", "No se pudieron cargar los productos", e.getMessage());
        }
    }
    
    private void updateStats() {
        lblTotalProductos.setText("Total productos: " + productosList.size());
        
        double valorTotal = productosList.stream()
                .mapToDouble(p -> p.getPrecio() * p.getCantidad())
                .sum();
        
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "MX"));
        lblValorInventario.setText("Valor inventario: " + currencyFormat.format(valorTotal));
    }
    
    private void setupButtons() {
        btnAgregar.setOnAction(e -> handleNuevoProducto());
        btnEditar.setOnAction(e -> handleEditarProducto());
        btnEliminar.setOnAction(e -> handleEliminarProducto());
    }
    
    private void setupSearch() {
        filteredProductos = new FilteredList<>(productosList, p -> true);
        
        txtBuscar.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredProductos.setPredicate(producto -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                
                String lowerCaseFilter = newValue.toLowerCase();
                
                return producto.getNombre().toLowerCase().contains(lowerCaseFilter) ||
                       producto.getId().toLowerCase().contains(lowerCaseFilter) ||
                       producto.getCategoria().toString().toLowerCase().contains(lowerCaseFilter);
            });
            
            tableProductos.setItems(filteredProductos);
            updateStats();
        });
        
        btnBuscar.setOnAction(e -> {
            String texto = txtBuscar.getText();
            txtBuscar.setText(""); // Limpiar y volver a poner para desencadenar el listener
            txtBuscar.setText(texto);
        });
    }
    
    private void handleNuevoProducto() {
        // Primero, preguntar la categoría
        Categoria categoriaSeleccionada = mostrarDialogoSeleccionCategoria();
        if (categoriaSeleccionada == null) {
            return; // El usuario canceló
        }
        
        // Crear el diálogo según la categoría
        Dialog<Producto> dialog = crearDialogoProducto(null, categoriaSeleccionada);
        
        Optional<Producto> result = dialog.showAndWait();
        result.ifPresent(producto -> {
            try {
                dao.addProducto(producto);
                productosList.add(producto);
                updateStats();
                
                // Opción "añadir otro del mismo tipo"
                boolean agregarOtro = mostrarConfirmacion(
                    "Producto agregado", 
                    "El producto " + producto.getNombre() + " ha sido agregado exitosamente.",
                    "¿Desea agregar otro producto de tipo " + categoriaSeleccionada.getDisplayName() + "?"
                );
                
                if (agregarOtro) {
                    handleNuevoProductoMismaCategoria(categoriaSeleccionada);
                }
                
            } catch (SQLException e) {
                e.printStackTrace();
                showError("Error al agregar", "No se pudo agregar el producto", e.getMessage());
            }
        });
    }
    
    private void handleNuevoProductoMismaCategoria(Categoria categoria) {
        Dialog<Producto> dialog = crearDialogoProducto(null, categoria);
        
        Optional<Producto> result = dialog.showAndWait();
        result.ifPresent(producto -> {
            try {
                dao.addProducto(producto);
                productosList.add(producto);
                updateStats();
                
                boolean agregarOtro = mostrarConfirmacion(
                    "Producto agregado", 
                    "El producto " + producto.getNombre() + " ha sido agregado exitosamente.",
                    "¿Desea agregar otro producto de tipo " + categoria.getDisplayName() + "?"
                );
                
                if (agregarOtro) {
                    handleNuevoProductoMismaCategoria(categoria);
                }
                
            } catch (SQLException e) {
                e.printStackTrace();
                showError("Error al agregar", "No se pudo agregar el producto", e.getMessage());
            }
        });
    }
    
    private void handleEditarProducto() {
        Producto productoSeleccionado = tableProductos.getSelectionModel().getSelectedItem();
        
        if (productoSeleccionado == null) {
            showError("Selección vacía", "No hay producto seleccionado", "Por favor, seleccione un producto para editar.");
            return;
        }
        
        Dialog<Producto> dialog = crearDialogoProducto(productoSeleccionado, productoSeleccionado.getCategoria());
        
        Optional<Producto> result = dialog.showAndWait();
        result.ifPresent(productoActualizado -> {
            try {
                dao.updateProducto(productoActualizado);
                
                // Actualizar en la lista
                int index = productosList.indexOf(productoSeleccionado);
                if (index >= 0) {
                    productosList.set(index, productoActualizado);
                }
                
                updateStats();
                
            } catch (SQLException e) {
                e.printStackTrace();
                showError("Error al actualizar", "No se pudo actualizar el producto", e.getMessage());
            }
        });
    }
    
    private void handleEliminarProducto() {
        Producto productoSeleccionado = tableProductos.getSelectionModel().getSelectedItem();
        
        if (productoSeleccionado == null) {
            showError("Selección vacía", "No hay producto seleccionado", "Por favor, seleccione un producto para eliminar.");
            return;
        }
        
        boolean confirmar = mostrarConfirmacion(
            "Confirmar eliminación", 
            "¿Está seguro de eliminar el producto?", 
            "Producto: " + productoSeleccionado.getNombre() + " (ID: " + productoSeleccionado.getId() + ")"
        );
        
        if (confirmar) {
            try {
                dao.deleteProducto(productoSeleccionado.getId());
                productosList.remove(productoSeleccionado);
                updateStats();
            } catch (SQLException e) {
                e.printStackTrace();
                showError("Error al eliminar", "No se pudo eliminar el producto", e.getMessage());
            }
        }
    }
    
    private Categoria mostrarDialogoSeleccionCategoria() {
        Dialog<Categoria> dialog = new Dialog<>();
        dialog.setTitle("Seleccionar Categoría");
        dialog.setHeaderText("Seleccione la categoría del producto");
        
        // Botones
        ButtonType confirmarButton = new ButtonType("Confirmar", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelarButton = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmarButton, cancelarButton);
        
        // Contenido
        VBox content = new VBox(10);
        ComboBox<Categoria> comboCategoria = new ComboBox<>();
        comboCategoria.getItems().addAll(Categoria.values());
        comboCategoria.setPromptText("Seleccione una categoría");
        comboCategoria.setPrefWidth(300);
        
        content.getChildren().add(new Label("Categoría:"));
        content.getChildren().add(comboCategoria);
        dialog.getDialogPane().setContent(content);
        
        // Validación
        Node confirmarBtn = dialog.getDialogPane().lookupButton(confirmarButton);
        confirmarBtn.setDisable(true);
        
        comboCategoria.valueProperty().addListener((obs, oldVal, newVal) -> {
            confirmarBtn.setDisable(newVal == null);
        });
        
        // Convertidor de resultado
        dialog.setResultConverter(buttonType -> {
            if (buttonType == confirmarButton) {
                return comboCategoria.getValue();
            }
            return null;
        });
        
        return dialog.showAndWait().orElse(null);
    }
    
    private Dialog<Producto> crearDialogoProducto(Producto producto, Categoria categoria) {
        Dialog<Producto> dialog = new Dialog<>();
        
        if (producto == null) {
            dialog.setTitle("Nuevo Producto");
            dialog.setHeaderText("Agregar " + categoria.getDisplayName());
        } else {
            dialog.setTitle("Editar Producto");
            dialog.setHeaderText("Modificar " + categoria.getDisplayName());
        }
        
        // Botones
        ButtonType guardarButton = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelarButton = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(guardarButton, cancelarButton);
        
        // Crear grid para formulario
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));
        
        // Campos comunes
        TextField txtId = new TextField();
        TextField txtNombre = new TextField();
        TextField txtPrecio = new TextField();
        TextField txtCantidad = new TextField();
        ComboBox<Categoria> cboCategoria = new ComboBox<>();
        
        cboCategoria.getItems().addAll(Categoria.values());
        cboCategoria.setValue(categoria);
        
        // Campos específicos por categoría
        TextField txtPresentacion = null;
        TextField txtTipoPapel = null;
        TextField txtCantidadPorPaquete = null;
        
        TextField txtTipoMobiliario = null;
        TextField txtMaterial = null;
        TextField txtColor = null;
        TextField txtDimensiones = null;
        
        TextField txtMarca = null;
        TextField txtModelo = null;
        TextField txtNumeroSerie = null;
        TextField txtGarantiaMeses = null;
        TextField txtEspecificacionesTecnicas = null;
        
        int rowIndex = 5; // Comenzar campos específicos después de los comunes
        
        // Configuración por categoría
        switch(categoria) {
            case INSUMO_OFICINA:
                // Campos específicos para insumos
                txtPresentacion = new TextField();
                txtTipoPapel = new TextField();
                txtCantidadPorPaquete = new TextField();
                
                grid.add(new Label("Presentación:"), 0, rowIndex);
                grid.add(txtPresentacion, 1, rowIndex++);
                grid.add(new Label("Tipo de Papel:"), 0, rowIndex);
                grid.add(txtTipoPapel, 1, rowIndex++);
                grid.add(new Label("Cantidad por Paquete:"), 0, rowIndex);
                grid.add(txtCantidadPorPaquete, 1, rowIndex++);
                
                // Si es edición, llenar campos específicos
                if (producto != null && producto instanceof InsumoOficina) {
                    InsumoOficina insumo = (InsumoOficina) producto;
                    txtPresentacion.setText(insumo.getPresentacion());
                    txtTipoPapel.setText(insumo.getTipoPapel());
                    txtCantidadPorPaquete.setText(String.valueOf(insumo.getCantidadPorPaquete()));
                }
                break;
                
            case PRODUCTO_MOBILIARIO:
                // Campos específicos para mobiliario
                txtTipoMobiliario = new TextField();
                txtMaterial = new TextField();
                txtColor = new TextField();
                txtDimensiones = new TextField();
                
                grid.add(new Label("Tipo de Mobiliario:"), 0, rowIndex);
                grid.add(txtTipoMobiliario, 1, rowIndex++);
                grid.add(new Label("Material:"), 0, rowIndex);
                grid.add(txtMaterial, 1, rowIndex++);
                grid.add(new Label("Color:"), 0, rowIndex);
                grid.add(txtColor, 1, rowIndex++);
                grid.add(new Label("Dimensiones:"), 0, rowIndex);
                grid.add(txtDimensiones, 1, rowIndex++);
                
                // Si es edición, llenar campos específicos
                if (producto != null && producto instanceof ProductoMobilario) {
                    ProductoMobilario mobiliario = (ProductoMobilario) producto;
                    txtTipoMobiliario.setText(mobiliario.getTipoMobilario());
                    txtMaterial.setText(mobiliario.getMaterial());
                    txtColor.setText(mobiliario.getColor());
                    txtDimensiones.setText(mobiliario.getDimensiones());
                }
                break;
                
            case PRODUCTO_TECNOLOGICO:
                // Campos específicos para tecnología
                txtMarca = new TextField();
                txtModelo = new TextField();
                txtNumeroSerie = new TextField();
                txtGarantiaMeses = new TextField();
                txtEspecificacionesTecnicas = new TextField();
                
                grid.add(new Label("Marca:"), 0, rowIndex);
                grid.add(txtMarca, 1, rowIndex++);
                grid.add(new Label("Modelo:"), 0, rowIndex);
                grid.add(txtModelo, 1, rowIndex++);
                grid.add(new Label("Número de Serie:"), 0, rowIndex);
                grid.add(txtNumeroSerie, 1, rowIndex++);
                grid.add(new Label("Garantía (meses):"), 0, rowIndex);
                grid.add(txtGarantiaMeses, 1, rowIndex++);
                grid.add(new Label("Especificaciones:"), 0, rowIndex);
                grid.add(txtEspecificacionesTecnicas, 1, rowIndex++);
                
                // Si es edición, llenar campos específicos
                if (producto != null && producto instanceof ProductoTecnologico) {
                    ProductoTecnologico tecnologico = (ProductoTecnologico) producto;
                    txtMarca.setText(tecnologico.getMarca());
                    txtModelo.setText(tecnologico.getModelo());
                    txtNumeroSerie.setText(tecnologico.getNumeroSerie());
                    txtGarantiaMeses.setText(String.valueOf(tecnologico.getGarantiaMeses()));
                    txtEspecificacionesTecnicas.setText(tecnologico.getEspecificacionesTecnicas());
                }
                break;
        }
        
        // Añadir campos comunes al grid
        grid.add(new Label("ID:"), 0, 0);
        grid.add(txtId, 1, 0);
        grid.add(new Label("Nombre:"), 0, 1);
        grid.add(txtNombre, 1, 1);
        grid.add(new Label("Precio:"), 0, 2);
        grid.add(txtPrecio, 1, 2);
        grid.add(new Label("Cantidad:"), 0, 3);
        grid.add(txtCantidad, 1, 3);
        grid.add(new Label("Categoría:"), 0, 4);
        grid.add(cboCategoria, 1, 4);
        
        // Si es edición, llenar los campos comunes
        if (producto != null) {
            txtId.setText(producto.getId());
            txtId.setEditable(false); // No permitir cambiar el ID
            txtNombre.setText(producto.getNombre());
            txtPrecio.setText(String.valueOf(producto.getPrecio()));
            txtCantidad.setText(String.valueOf(producto.getCantidad()));
            cboCategoria.setValue(producto.getCategoria());
        } else {
            // Generar ID automático para productos nuevos
            try {
                String newId = dao.generateNewId(categoria);
                txtId.setText(newId);
                txtId.setEditable(false);
            } catch (SQLException e) {
                e.printStackTrace();
                txtId.setPromptText("Ingrese un ID único");
            }
        }
        
        dialog.getDialogPane().setContent(grid);
        
        // Validación
        Node guardarBtn = dialog.getDialogPane().lookupButton(guardarButton);
        
        // Validar al cambiar los campos
        txtNombre.textProperty().addListener((obs, old, newVal) -> validarCampos(guardarBtn, txtId, txtNombre, txtPrecio, txtCantidad));
        txtPrecio.textProperty().addListener((obs, old, newVal) -> validarCampos(guardarBtn, txtId, txtNombre, txtPrecio, txtCantidad));
        txtCantidad.textProperty().addListener((obs, old, newVal) -> validarCampos(guardarBtn, txtId, txtNombre, txtPrecio, txtCantidad));
        
        // Validación inicial
        validarCampos(guardarBtn, txtId, txtNombre, txtPrecio, txtCantidad);
        
        // Pedir foco al primer campo
        txtNombre.requestFocus();
        
        // Convertidor de resultado
        final TextField finalTxtPresentacion = txtPresentacion;
        final TextField finalTxtTipoPapel = txtTipoPapel;
        final TextField finalTxtCantidadPorPaquete = txtCantidadPorPaquete;
        
        final TextField finalTxtTipoMobiliario = txtTipoMobiliario;
        final TextField finalTxtMaterial = txtMaterial;
        final TextField finalTxtColor = txtColor;
        final TextField finalTxtDimensiones = txtDimensiones;
        
        final TextField finalTxtMarca = txtMarca;
        final TextField finalTxtModelo = txtModelo;
        final TextField finalTxtNumeroSerie = txtNumeroSerie;
        final TextField finalTxtGarantiaMeses = txtGarantiaMeses;
        final TextField finalTxtEspecificacionesTecnicas = txtEspecificacionesTecnicas;
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == guardarButton) {
                try {
                    String id = txtId.getText();
                    String nombre = txtNombre.getText();
                    double precio = Double.parseDouble(txtPrecio.getText());
                    int cantidad = Integer.parseInt(txtCantidad.getText());
                    Categoria cat = cboCategoria.getValue();
                    
                    // Crear el tipo específico de producto según la categoría
                    switch(cat) {
                        case INSUMO_OFICINA:
                            String presentacion = finalTxtPresentacion.getText();
                            String tipoPapel = finalTxtTipoPapel.getText();
                            int cantidadPorPaquete = Integer.parseInt(finalTxtCantidadPorPaquete.getText());
                            return new InsumoOficina(nombre, id, precio, cantidad, presentacion, tipoPapel, cantidadPorPaquete);
                            
                        case PRODUCTO_MOBILIARIO:
                            String tipoMobiliario = finalTxtTipoMobiliario.getText();
                            String material = finalTxtMaterial.getText();
                            String color = finalTxtColor.getText();
                            String dimensiones = finalTxtDimensiones.getText();
                            return new ProductoMobilario(nombre, id, precio, cantidad, tipoMobiliario, material, color, dimensiones);
                            
                        case PRODUCTO_TECNOLOGICO:
                            String marca = finalTxtMarca.getText();
                            String modelo = finalTxtModelo.getText();
                            String numeroSerie = finalTxtNumeroSerie.getText();
                            int garantiaMeses = Integer.parseInt(finalTxtGarantiaMeses.getText());
                            String especificacionesTecnicas = finalTxtEspecificacionesTecnicas.getText();
                            return new ProductoTecnologico(nombre, id, precio, cantidad, marca, modelo, numeroSerie, garantiaMeses, especificacionesTecnicas);
                            
                        default:
                            return new Producto(nombre, id, precio, cantidad, cat);
                    }
                } catch (NumberFormatException e) {
                    showError("Error de formato", "Datos incorrectos", "Verifique que precio, cantidad y otros campos numéricos sean valores válidos.");
                    return null;
                }
            }
            return null;
        });
        
        return dialog;
    }
    
    private void validarCampos(Node guardarBtn, TextField txtId, TextField txtNombre, TextField txtPrecio, TextField txtCantidad) {
        boolean idValido = !txtId.getText().trim().isEmpty();
        boolean nombreValido = !txtNombre.getText().trim().isEmpty();
        boolean precioValido = false;
        boolean cantidadValida = false;
        
        try {
            double precio = Double.parseDouble(txtPrecio.getText());
            precioValido = precio > 0;
        } catch (NumberFormatException e) {
            precioValido = false;
        }
        
        try {
            int cantidad = Integer.parseInt(txtCantidad.getText());
            cantidadValida = cantidad >= 0;
        } catch (NumberFormatException e) {
            cantidadValida = false;
        }
        
        guardarBtn.setDisable(!(idValido && nombreValido && precioValido && cantidadValida));
    }
    
    private void showError(String titulo, String header, String contenido) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(header);
        alert.setContentText(contenido);
        alert.showAndWait();
    }
    
    private boolean mostrarConfirmacion(String titulo, String header, String contenido) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(header);
        alert.setContentText(contenido);
        
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}