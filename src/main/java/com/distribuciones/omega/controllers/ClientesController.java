package com.distribuciones.omega.controllers;

import com.distribuciones.omega.model.Cliente;
import com.distribuciones.omega.service.ClienteService;

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

    // Reemplazar DAO por Service
    private ClienteService clienteService;
    private ObservableList<Cliente> clientesCompletos = FXCollections.observableArrayList();
    private FilteredList<Cliente> filteredList;

    @FXML
    void initialize() {
        try {
            // Inicializar el servicio
            clienteService = new ClienteService();
            
            // Configurar tabla y cargar datos
            setupTable();
            loadClients();
            setupSearch();
            
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error de Inicialización", 
                         "No se pudo inicializar el controlador", 
                         e.getMessage());
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
        clientesCompletos.clear();
        try {
            // Usar el servicio para obtener todos los clientes
            List<Cliente> clientes = clienteService.obtenerTodosClientes();
            clientesCompletos.addAll(clientes);
            
            // Crear la lista filtrada basada en la lista completa
            filteredList = new FilteredList<>(clientesCompletos, p -> true);
            
            // Establecer la lista filtrada como fuente de datos para la tabla
            tableClientes.setItems(filteredList);
            
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error al cargar clientes", 
                         "No se pudieron cargar los clientes", 
                         e.getMessage());
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
    }

    @FXML
    private void handleAddCliente() {
        try {
            Cliente nuevo = new Cliente("","","","","");
            boolean ok = showFormDialog(nuevo, false);
            if (ok) {
                // Usar el servicio para guardar el cliente
                Cliente clienteGuardado = clienteService.guardarCliente(nuevo);
                if (clienteGuardado != null) {
                    clientesCompletos.add(clienteGuardado);
                    filtrarClientesEnTiempoReal(txtBuscar.getText()); // Actualizar filtro
                } else {
                    mostrarError("Error al guardar", "No se pudo guardar el cliente", 
                                "Verifique que los datos sean correctos y que el cliente no exista");
                }
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
                    // Usar el servicio para actualizar el cliente
                    boolean actualizado = clienteService.actualizarCliente(sel);
                    if (actualizado) {
                        tableClientes.refresh();
                    } else {
                        mostrarError("Error al actualizar", "No se pudo actualizar el cliente", 
                                    "Verifique que los datos sean correctos");
                    }
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
                    // Usar el servicio para eliminar (soft delete) el cliente
                    boolean eliminado = clienteService.eliminarCliente(sel.getIdCliente());
                    if (eliminado) {
                        clientesCompletos.remove(sel);
                    } else {
                        mostrarError("Error al eliminar", "No se pudo eliminar el cliente", 
                                    "El cliente podría estar asociado a registros existentes");
                    }
                }
            } catch (Exception e) {
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