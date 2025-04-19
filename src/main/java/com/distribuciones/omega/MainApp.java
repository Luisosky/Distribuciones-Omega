package com.distribuciones.omega;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.fxml.JavaFXBuilderFactory;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader();
        URL url = getClass().getResource("/fxml/login.fxml");
        // Si hay un error de formato de número, modifica el FXML en memoria
        String fxmlContent = new String(url.openStream().readAllBytes(), StandardCharsets.UTF_8);
        fxmlContent = fxmlContent.replace("width=\"100%\"", "width=\"-1\"")
                                 .replace("height=\"100%\"", "height=\"-1\"")
                                 .replace("javafx/23.0.1", "javafx/20");
        loader.setLocation(url);
        loader.setBuilderFactory(new JavaFXBuilderFactory());
        Parent root = loader.load(new ByteArrayInputStream(fxmlContent.getBytes(StandardCharsets.UTF_8)));
    
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Distribuciones Ómega – Login");
        primaryStage.setResizable(true);
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}