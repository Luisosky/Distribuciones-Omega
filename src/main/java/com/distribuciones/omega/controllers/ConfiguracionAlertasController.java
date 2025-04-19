package com.distribuciones.omega.controllers;

import com.distribuciones.omega.service.AlertaStockService;
import com.distribuciones.omega.utils.AlertUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Controlador para la pantalla de configuración de alertas
 */
public class ConfiguracionAlertasController {
    
    @FXML private Spinner<Integer> spnStockMinimo;
    @FXML private TextField txtEmailAdmin;
    @FXML private Button btnGuardar;
    @FXML private Button btnCancelar;
    
    private AlertaStockService alertaStockService;
    
    @FXML
    private void initialize() {
        alertaStockService = new AlertaStockService();
        
        // Configurar spinner de stock mínimo
        SpinnerValueFactory<Integer> valueFactory = 
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 50, alertaStockService.getStockMinimo());
        spnStockMinimo.setValueFactory(valueFactory);
        
        // Cargar email actual
        txtEmailAdmin.setText(alertaStockService.getEmailAdmin());
        
        // Configurar botones
        btnGuardar.setOnAction(e -> guardarConfiguracion());
        btnCancelar.setOnAction(e -> cerrarVentana());
    }
    
    /**
     * Guarda la configuración de alertas
     */
    private void guardarConfiguracion() {
        try {
            int stockMinimo = spnStockMinimo.getValue();
            String email = txtEmailAdmin.getText().trim();
            
            // Validar email
            if (!email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
                AlertUtils.mostrarError("Error de validación", "El correo electrónico no es válido");
                return;
            }
            
            // Guardar configuración
            alertaStockService.setStockMinimo(stockMinimo);
            alertaStockService.setEmailAdmin(email);
            
            AlertUtils.mostrarInformacion("Configuración guardada", 
                    "La configuración de alertas ha sido guardada correctamente.");
            
            cerrarVentana();
        } catch (Exception e) {
            AlertUtils.mostrarError("Error", "Ocurrió un error al guardar la configuración: " + e.getMessage());
        }
    }
    
    /**
     * Cierra la ventana actual
     */
    private void cerrarVentana() {
        Stage stage = (Stage) btnCancelar.getScene().getWindow();
        stage.close();
    }
}