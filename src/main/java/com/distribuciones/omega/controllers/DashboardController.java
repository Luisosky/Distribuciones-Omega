package com.distribuciones.omega.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;

public class DashboardController {

    @FXML private VBox menuBox;
    @FXML private Button btnClientes;
    @FXML private Button btnProductos;
    @FXML private Button btnInventario;
    @FXML private Button btnCotizacion;
    @FXML private AnchorPane contentArea;

    @FXML
    private void initialize() {
        btnClientes.setOnAction(e -> loadView("clientes-gestion.fxml"));
        btnProductos.setOnAction(e -> loadView("productos-gestion.fxml"));
        // carga inicial solo clientes
        loadView("clientes-gestion.fxml");
    }

    private void loadView(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + fxmlFile));
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
            // anclar al 100%
            AnchorPane.setTopAnchor(view, 0.0);
            AnchorPane.setBottomAnchor(view, 0.0);
            AnchorPane.setLeftAnchor(view, 0.0);
            AnchorPane.setRightAnchor(view, 0.0);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}