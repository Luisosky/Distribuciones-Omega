package com.distribuciones.omega.controllers;

import com.distribuciones.omega.model.Cliente;
import com.distribuciones.omega.dao.ClienteDAO;

import java.util.List;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import io.github.cdimascio.dotenv.Dotenv;

import java.sql.*;

public class ClientesController { 

    @FXML private TableView<Cliente> tableClientes;
    @FXML private TableColumn<Cliente,String> colId;
    @FXML private TableColumn<Cliente,String> colNombre;
    @FXML private TableColumn<Cliente,String> colEmail;
    @FXML private TableColumn<Cliente,String> colTelefono;
    @FXML private TableColumn<Cliente,String> colDireccion;
    
    @FXML private TextField txtBuscar;
    @FXML private Button btnBuscar;
    @FXML private Button btnLimpiarBusqueda;

    private ClienteDAO dao;
    private ObservableList<Cliente> clientesList = FXCollections.observableArrayList();
    private ObservableList<Cliente> clientesCompletos = FXCollections.observableArrayList();
    private FilteredList<Cliente> filteredList;

    @FXML
    void initialize() {
        // 1) conectar y crear DAO
        try {
            // Cargar variables desde .env usando la biblioteca
            Dotenv dotenv = Dotenv.configure().load();
            String dbUrl = dotenv.get("DB_URL");
            String dbUser = dotenv.get("DB_USER");
            String dbPass = dotenv.get("DB_PASS");
            
            System.out.println("Intentando conectar a: " + dbUrl);
            System.out.println("Usuario: " + dbUser);
            
            // Registrar el driver explícitamente
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
            dao = new ClienteDAO(conn);

            // Crear la tabla si no existe y cargar datos de ejemplo si está vacía
            dao.createTableIfNotExists();
            
            // Continuar con la inicialización
            setupTable();
            loadClients();
            setupSearch();
            
        } catch (SQLException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error de Conexión");
            alert.setHeaderText("No se pudo conectar a la base de datos");
            alert.setContentText("Detalles: " + e.getMessage());
            alert.showAndWait();
            return;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error de Driver");
            alert.setHeaderText("No se encontró el driver de MySQL");
            alert.setContentText("Asegúrate de tener la dependencia de MySQL JDBC en tu proyecto");
            alert.showAndWait();
            return;
        }
    }

    // Método para configurar las columnas de la tabla
    private void setupTable() {
        colId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getId()));
        colNombre.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNombre()));
        colEmail.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEmail()));
        colTelefono.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTelefono()));
        colDireccion.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDireccion()));
    }
    
    private void loadClients() {
        clientesList.clear();
        clientesCompletos.clear();
        try {
            List<Cliente> clientes = dao.getActiveClientes();
            clientesCompletos.addAll(clientes); // Guardamos todos los clientes
            
            // Crear la lista filtrada basada en la lista completa
            filteredList = new FilteredList<>(clientesCompletos, p -> true);
            
            // Establecer la lista filtrada como fuente de datos para la tabla
            tableClientes.setItems(filteredList);
            
        } catch (SQLException e) {
            e.printStackTrace();
            mostrarError("Error al cargar clientes", "No se pudieron cargar los clientes desde la base de datos", e.getMessage());
        }
    }
    
    // Configurar la búsqueda en tiempo real
    private void setupSearch() {
        // Usar un listener para detectar cambios en el texto de búsqueda
        txtBuscar.textProperty().addListener((observable, oldValue, newValue) -> {
            filtrarClientesEnTiempoReal(newValue);
        });
        
        // Configurar eventos para los botones
        btnBuscar.setOnAction(e -> handleBuscarCliente());
        btnLimpiarBusqueda.setOnAction(e -> handleLimpiarBusqueda());
    }
    
    // Método para filtrar clientes en tiempo real mientras se escribe
    private void filtrarClientesEnTiempoReal(String texto) {
        if (filteredList == null) return;
        
        filteredList.setPredicate(cliente -> {
            // Si el texto está vacío, mostrar todos los clientes
            if (texto == null || texto.isEmpty()) {
                return true;
            }
            
            // Convertir texto de búsqueda a minúsculas para comparación sin distinción de mayúsculas
            String lowerCaseFilter = texto.toLowerCase();
            
            // Filtrar por ID o nombre
            if (cliente.getId().toLowerCase().contains(lowerCaseFilter)) {
                return true; // Coincide con ID
            } else if (cliente.getNombre().toLowerCase().contains(lowerCaseFilter)) {
                return true; // Coincide con nombre
            }
            
            // No hay coincidencia
            return false;
        });
        
        // Actualizar información sobre resultados encontrados
        if (filteredList.isEmpty() && !texto.isEmpty()) {
            // Podríamos mostrar un mensaje, pero mejor lo dejamos para cuando se presiona el botón,
            // para no interrumpir la escritura con alertas constantes
        }
    }

    @FXML
    private void handleAddCliente() {
        try {
            Cliente nuevo = new Cliente("","","","","");
            boolean ok = showFormDialog(nuevo, false);
            if (ok) {
                dao.addCliente(nuevo);
                clientesCompletos.add(nuevo);
                // No es necesario añadir a clientesList ya que usamos FilteredList
                filtrarClientesEnTiempoReal(txtBuscar.getText()); // Actualizar filtro
            }
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error al agregar cliente", "No se pudo agregar el cliente", e.getMessage());
        }
    }

    @FXML
    private void handleEditCliente() {
        Cliente sel = tableClientes.getSelectionModel().getSelectedItem();
        if (sel != null) {
            try {
                boolean ok = showFormDialog(sel, true);
                if (ok) {
                    dao.updateCliente(sel);
                    tableClientes.refresh();
                    // La FilteredList se actualiza automáticamente
                }
            } catch (Exception e) {
                e.printStackTrace();
                mostrarError("Error al editar cliente", "No se pudo editar el cliente", e.getMessage());
            }
        } else {
            mostrarAlerta("Selección", "Debe seleccionar un cliente para editar");
        }
    }
    
    private boolean showFormDialog(Cliente cliente, boolean isEdit) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/cliente-form.fxml"));
        Parent root = loader.load();

        ClienteFormController ctrl = loader.getController();
        Stage dialog = new Stage();
        dialog.setTitle(isEdit ? "Editar Cliente" : "Nuevo Cliente");
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setScene(new Scene(root));
        ctrl.setDialogStage(dialog);
        ctrl.setCliente(cliente, isEdit);
        dialog.showAndWait();
        return ctrl.isOkClicked();
    }

    @FXML
    private void handleDeleteCliente() {
        Cliente sel = tableClientes.getSelectionModel().getSelectedItem();
        if (sel != null) {
            try {
                // Confirmación antes de eliminar
                boolean confirmar = mostrarConfirmacion(
                    "Confirmar eliminación", 
                    "¿Está seguro de eliminar este cliente?", 
                    "Esta acción no se puede deshacer."
                );
                
                if (confirmar) {
                    dao.deactivateCliente(sel.getId());
                    clientesCompletos.remove(sel);
                }
            } catch (SQLException e) {
                e.printStackTrace();
                mostrarError("Error al eliminar", "No se pudo eliminar el cliente", e.getMessage());
            }
        } else {
            mostrarAlerta("Selección", "Debe seleccionar un cliente para eliminar");
        }
    }
    
    @FXML
    private void handleBuscarCliente() {
        String criterio = txtBuscar.getText().trim();
        
        if (criterio.isEmpty()) {
            handleLimpiarBusqueda();
            return;
        }
        
        try {
            // Aplicar filtro (no es necesario consultar la base de datos gracias a la búsqueda en memoria)
            filtrarClientesEnTiempoReal(criterio);
            
            // Mostrar mensaje si no hay resultados
            if (filteredList.isEmpty()) {
                mostrarAlerta("Búsqueda", "No se encontraron clientes que coincidan con: " + criterio);
            }
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error en búsqueda", "No se pudo realizar la búsqueda", e.getMessage());
        }
    }
    
    @FXML
    private void handleLimpiarBusqueda() {
        txtBuscar.clear();
        filtrarClientesEnTiempoReal(""); // Restaurar todos los clientes
    }
    
    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
    
    private void mostrarError(String titulo, String header, String detalle) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(header);
        alert.setContentText(detalle);
        alert.showAndWait();
    }
    
    private boolean mostrarConfirmacion(String titulo, String header, String contenido) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(header);
        alert.setContentText(contenido);
        
        return alert.showAndWait().filter(r -> r == ButtonType.OK).isPresent();
    }
}