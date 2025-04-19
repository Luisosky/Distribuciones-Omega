package com.distribuciones.omega.controllers;

import com.distribuciones.omega.model.Usuario;
import com.distribuciones.omega.service.UsuarioService;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.util.Duration;

public class LoginController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Button btnLogin;
    @FXML private Label lblError;
    
    // Instancia del servicio
    private final UsuarioService usuarioService = new UsuarioService();

    @FXML
    private void initialize() {
        btnLogin.setOnAction(evt -> doLogin());
        
        // Permitir presionar Enter para iniciar sesión
        txtPassword.setOnAction(evt -> doLogin());
        
        // Ocultar mensaje de error inicialmente
        lblError.setVisible(false);
        lblError.setManaged(false);
    }

    private void doLogin() {
        String user = txtUsername.getText().trim();
        String pass = txtPassword.getText().trim();
        
        // Validación básica
        if (user.isEmpty() || pass.isEmpty()) {
            showError("Por favor complete todos los campos");
            return;
        }
        
        // Intento de autenticación usando el servicio
        Usuario usuario = usuarioService.autenticar(user, pass);

        if (usuario != null) {
            try {
                FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/dashboard.fxml"));
                Parent root = loader.load();
                
                DashboardController dashboardController = loader.getController();
                dashboardController.setUsuario(usuario); 

                Stage stage = (Stage) btnLogin.getScene().getWindow();
                stage.setTitle("Distribuciones Ómega - Dashboard");
                Scene scene = new Scene(root);
                stage.setScene(scene);
                stage.setResizable(true);
                stage.setMaximized(true);
            } catch (Exception ex) {
                ex.printStackTrace();
                showError("Error al cargar el dashboard");
            }
        } else {
            showError("Usuario o contraseña incorrectos");
        }
    }
    
    /**
     * Muestra un mensaje de error con animación
     * @param message El mensaje a mostrar
     */
    private void showError(String message) {
        lblError.setText(message);
        lblError.setVisible(true);
        lblError.setManaged(true);
        
        // Efecto de fade-in para el mensaje de error
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), lblError);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
        
        // Efecto visual en los campos
        txtUsername.setStyle("-fx-background-radius: 5; -fx-border-color: #e74c3c; -fx-border-radius: 5;");
        txtPassword.setStyle("-fx-background-radius: 5; -fx-border-color: #e74c3c; -fx-border-radius: 5;");
        
        // Restaurar estilo cuando el usuario comience a corregir
        txtUsername.setOnKeyPressed(e -> resetFieldStyle(txtUsername));
        txtPassword.setOnKeyPressed(e -> resetFieldStyle(txtPassword));
    }
    
    /**
     * Resetea el estilo de un campo cuando el usuario comienza a editarlo
     * @param field El campo a resetear
     */
    private void resetFieldStyle(TextField field) {
        field.setStyle("-fx-background-radius: 5; -fx-border-color: #e0e0e0; -fx-border-radius: 5;");
        // Quitar este handler para evitar múltiples registros
        field.setOnKeyPressed(null);
        
        // Ocultar mensaje de error si ambos campos tienen estilo normal
        if (!txtUsername.getStyle().contains("#e74c3c") && !txtPassword.getStyle().contains("#e74c3c")) {
            // Efecto de fade-out para el mensaje de error
            FadeTransition fadeOut = new FadeTransition(Duration.millis(200), lblError);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(e -> {
                lblError.setVisible(false);
                lblError.setManaged(false);
            });
            fadeOut.play();
        }
    }
}