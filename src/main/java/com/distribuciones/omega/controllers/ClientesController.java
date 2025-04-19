package com.distribuciones.omega.controllers;

import com.distribuciones.omega.model.Cliente;
import com.distribuciones.omega.dao.ClienteDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.*;

public class ClientesController { 

    @FXML private TableView<Cliente> tableClientes;
    @FXML private TableColumn<Cliente,String> colId;
    @FXML private TableColumn<Cliente,String> colNombre;
    @FXML private TableColumn<Cliente,String> colEmail;
    @FXML private TableColumn<Cliente,String> colTelefono;
    @FXML private TableColumn<Cliente,String> colDireccion;

    private ClienteDAO dao;
    private ObservableList<Cliente> clientesList = FXCollections.observableArrayList();

    @FXML
    void initialize() {
        // 1) conectar y crear DAO
        try {
            // Cargar propiedades desde .env o usar configuración
            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPass = System.getenv("DB_PASS");
            
            // Registrar el driver explícitamente
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
            dao = new ClienteDAO(conn);
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
    
    private void loadClients() {
        clientesList.clear();
        try {
            clientesList.addAll(dao.getActiveClientes());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddCliente() {
        try {
            Cliente nuevo = new Cliente("","","","","");
            boolean ok = showFormDialog(nuevo, false);
            if (ok) {
                dao.addCliente(nuevo);
                clientesList.add(nuevo);
            }
        } catch (Exception e) {
            e.printStackTrace();
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
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
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
                dao.deactivateCliente(sel.getId());
                clientesList.remove(sel);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}