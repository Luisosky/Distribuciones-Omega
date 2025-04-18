/**
 * Sample Skeleton for 'productos-gestion.fxml' Controller Class
 */

 package com.distribuciones.omega.controllers;

 import java.net.URL;
 import java.util.ResourceBundle;
 import javafx.fxml.FXML;
 import javafx.scene.control.TableColumn;
 
 public class ProductosController {
 
     @FXML // ResourceBundle that was given to the FXMLLoader
     private ResourceBundle resources;
 
     @FXML // URL location of the FXML file that was given to the FXMLLoader
     private URL location;
 
     @FXML // fx:id="tableProductos"
     private TableColumn<?, ?> tableProductos; // Value injected by FXMLLoader
 
     @FXML // This method is called by the FXMLLoader when initialization is complete
     void initialize() {
         assert tableProductos != null : "fx:id=\"tableProductos\" was not injected: check your FXML file 'productos-gestion.fxml'.";
 
     }
 
 }
 