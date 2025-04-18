package com.distribuciones.omega;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Parent;

public class App extends Application {
  @Override
  public void start(Stage primaryStage) throws Exception {
    Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
    primaryStage.setScene(new Scene(root));
    primaryStage.show();
  }
  public static void main(String[] args) {
    launch(args);
  }
}