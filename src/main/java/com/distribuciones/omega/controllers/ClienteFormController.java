package com.distribuciones.omega.controllers;

import com.distribuciones.omega.model.Cliente;
import com.distribuciones.omega.service.ClienteService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class ClienteFormController {

    @FXML private TextField tfId;
    @FXML private TextField tfNombre;
    @FXML private TextField tfEmail;
    @FXML private TextField tfTelefono;
    @FXML private TextField tfDireccion;
    @FXML private CheckBox chkMayorista;
    @FXML private TextField tfLimiteCredito;

    private Stage dialogStage;
    private Cliente cliente;
    private boolean okClicked = false;
    private boolean modoEdicion = false;
    
    // Servicio para validaciones adicionales
    private final ClienteService clienteService = new ClienteService();

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    public void setCliente(Cliente c, boolean isEdit) {
        this.cliente = c;
        this.modoEdicion = isEdit;
        
        if (isEdit) {
            tfId.setText(c.getId());
            tfId.setDisable(true); // No permitir cambiar ID en modo edición
            tfNombre.setText(c.getNombre());
            tfEmail.setText(c.getEmail());
            tfTelefono.setText(c.getTelefono());
            tfDireccion.setText(c.getDireccion());
            
            // Cargar opciones adicionales
            if (chkMayorista != null) {
                chkMayorista.setSelected(c.isMayorista());
            }
            
            if (tfLimiteCredito != null) {
                tfLimiteCredito.setText(String.valueOf(c.getLimiteCredito()));
            }
        }
    }

    @FXML
    private void onSave() {
        if (isInputValid()) {
            // Actualizar objeto cliente con valores del formulario
            cliente.setId(tfId.getText());
            cliente.setNombre(tfNombre.getText());
            cliente.setEmail(tfEmail.getText());
            cliente.setTelefono(tfTelefono.getText());
            cliente.setDireccion(tfDireccion.getText());
            
            // Actualizar campos adicionales si existen
            if (chkMayorista != null) {
                cliente.setMayorista(chkMayorista.isSelected());
            }
            
            if (tfLimiteCredito != null) {
                try {
                    double limiteCredito = Double.parseDouble(tfLimiteCredito.getText().trim());
                    cliente.setLimiteCredito(limiteCredito);
                } catch (NumberFormatException e) {
                    cliente.setLimiteCredito(0.0);
                }
            }
            
            okClicked = true;
            dialogStage.close();
        }
    }

    @FXML
    private void onCancel() {
        dialogStage.close();
    }
    
    /**
     * Valida la entrada del usuario en los campos
     * @return true si la entrada es válida
     */
    private boolean isInputValid() {
        String errorMessage = "";

        if (tfId.getText() == null || tfId.getText().trim().isEmpty()) {
            errorMessage += "ID no válido (RUC/Cédula requerido)\n";
        }
        
        if (tfNombre.getText() == null || tfNombre.getText().trim().isEmpty()) {
            errorMessage += "Nombre no válido (requerido)\n";
        }
        
        if (tfEmail.getText() != null && !tfEmail.getText().trim().isEmpty()) {
            // Validación simple de formato de email
            if (!tfEmail.getText().matches("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")) {
                errorMessage += "Email no válido (formato incorrecto)\n";
            }
        }
        
        // Validar que el ID no esté duplicado en creación
        if (!modoEdicion) {
            if (clienteService.existeCliente(tfId.getText().trim())) {
                errorMessage += "Ya existe un cliente con este ID\n";
            }
        }
        
        // Validar límite de crédito si el campo existe
        if (tfLimiteCredito != null && !tfLimiteCredito.getText().trim().isEmpty()) {
            try {
                double limite = Double.parseDouble(tfLimiteCredito.getText().trim());
                if (limite < 0) {
                    errorMessage += "El límite de crédito no puede ser negativo\n";
                }
            } catch (NumberFormatException e) {
                errorMessage += "El límite de crédito debe ser un número válido\n";
            }
        }

        if (errorMessage.isEmpty()) {
            return true;
        } else {
            // Mostrar mensaje de error
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.initOwner(dialogStage);
            alert.setTitle("Campos Inválidos");
            alert.setHeaderText("Por favor corrija los campos inválidos");
            alert.setContentText(errorMessage);
            alert.showAndWait();
            return false;
        }
    }

    public boolean isOkClicked() {
        return okClicked;
    }

    public Cliente getCliente() {
        return cliente;
    }
}