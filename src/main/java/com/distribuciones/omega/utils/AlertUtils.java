package com.distribuciones.omega.utils;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import javafx.scene.control.DialogPane;

import java.util.Optional;

/**
 * Utilidad para mostrar diferentes tipos de alertas en la aplicación
 */
public class AlertUtils {

    /**
     * Muestra una alerta de error
     * 
     * @param titulo   Título de la alerta
     * @param mensaje  Mensaje de la alerta
     */
    public static void mostrarError(String titulo, String mensaje) {
        mostrarAlerta(AlertType.ERROR, titulo, mensaje);
    }
    
    /**
     * Muestra una alerta de advertencia
     * 
     * @param titulo   Título de la alerta
     * @param mensaje  Mensaje de la alerta
     */
    public static void mostrarAdvertencia(String titulo, String mensaje) {
        mostrarAlerta(AlertType.WARNING, titulo, mensaje);
    }
    
    /**
     * Muestra una alerta de información
     * 
     * @param titulo   Título de la alerta
     * @param mensaje  Mensaje de la alerta
     */
    public static void mostrarInformacion(String titulo, String mensaje) {
        mostrarAlerta(AlertType.INFORMATION, titulo, mensaje);
    }
    
    /**
     * Muestra una alerta de confirmación y retorna si fue confirmada
     * 
     * @param titulo   Título de la alerta
     * @param mensaje  Mensaje de la alerta
     * @return true si el usuario confirmó, false en caso contrario
     */
    public static boolean mostrarConfirmacion(String titulo, String mensaje) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        
        // Aplicar estilo CSS personalizado
        aplicarEstiloPersonalizado(alert);
        
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
    
    /**
     * Muestra una alerta de confirmación con botones personalizados
     * 
     * @param titulo    Título de la alerta
     * @param mensaje   Mensaje de la alerta
     * @param botones   Botones a mostrar en la alerta
     * @return El ButtonType seleccionado o empty si se cerró la ventana
     */
    public static Optional<ButtonType> mostrarConfirmacionPersonalizada(String titulo, String mensaje, ButtonType... botones) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.getButtonTypes().setAll(botones);
        
        // Aplicar estilo CSS personalizado
        aplicarEstiloPersonalizado(alert);
        
        return alert.showAndWait();
    }
    
    /**
     * Método interno para mostrar una alerta genérica
     * 
     * @param tipo      Tipo de alerta (ERROR, WARNING, INFORMATION)
     * @param titulo    Título de la alerta
     * @param mensaje   Mensaje de la alerta
     */
    private static void mostrarAlerta(AlertType tipo, String titulo, String mensaje) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        
        // Aplicar estilo CSS personalizado
        aplicarEstiloPersonalizado(alert);
        
        alert.showAndWait();
    }
    
    /**
     * Aplica estilos personalizados a una alerta
     * 
     * @param alert La alerta a la que aplicar el estilo
     */
    private static void aplicarEstiloPersonalizado(Alert alert) {
        DialogPane dialogPane = alert.getDialogPane();
        Stage stage = (Stage) dialogPane.getScene().getWindow();
        
        // Agregar estilos CSS si es necesario
        dialogPane.getStylesheets().add("/styles/alerts.css");
        
        // Personalizar el icono si es necesario
        // stage.getIcons().add(new Image("/img/app_icon.png"));
    }
}