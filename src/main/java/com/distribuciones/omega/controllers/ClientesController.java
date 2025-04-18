/**
 * Sample Skeleton for 'clientes-gestion.fxml' Controller Class
 */

 package com.distribuciones.omega.controllers;

 import java.net.URL;
 import java.util.ResourceBundle;
 import javafx.fxml.FXML;
 import javafx.scene.control.TableView;
 
 public class ClientesController {
 
     @FXML // ResourceBundle that was given to the FXMLLoader
     private ResourceBundle resources;
 
     @FXML // URL location of the FXML file that was given to the FXMLLoader
     private URL location;
 
     @FXML // fx:id="tableClientes"
     private TableView<?> tableClientes; // Value injected by FXMLLoader
 
     @FXML // This method is called by the FXMLLoader when initialization is complete
     void initialize() {
         assert tableClientes != null : "fx:id=\"tableClientes\" was not injected: check your FXML file 'clientes-gestion.fxml'.";
 
     }
 
 }
 