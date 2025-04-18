package com.distribuciones.omega.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

public class LoginController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Button btnLogin;

    @FXML
    private void initialize() {
        btnLogin.setOnAction(evt -> doLogin());
    }

    private void doLogin() {
        String user = txtUsername.getText();
        String pass = txtPassword.getText();
        // TODO: validar credenciales contra DB
        boolean authenticated = !user.isEmpty() && !pass.isEmpty();

        if (authenticated) {
            try {
                FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/dashboard.fxml"));
                Parent root = loader.load();
                Stage stage = (Stage) btnLogin.getScene().getWindow();
                stage.setTitle("Distribuciones Ómega - Dashboard");
                Scene scene = new Scene(root);
                stage.setScene(scene);
                stage.setResizable(true);
                stage.setMaximized(true);              
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            // TODO: mostrar alerta de error
            System.err.println("Credenciales inválidas");
        }
    }
}