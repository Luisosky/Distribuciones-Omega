package com.distribuciones.omega.service;

import com.distribuciones.omega.model.ProductoInventario;
import com.distribuciones.omega.utils.EmailUtil;
import io.github.cdimascio.dotenv.Dotenv;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Servicio para gestionar alertas de stock bajo
 */
public class AlertaStockService {
    
    private static final Dotenv dotenv = Dotenv.configure()
                                           .directory(".")
                                           .ignoreIfMissing()
                                           .load();
    private static final String EMAIL = dotenv.get("EMAIL");
    
    private static int stockMinimo = 1; // Valor por defecto
    private static String emailAdmin = EMAIL; // Valor por defecto
    
    private final InventarioService inventarioService;
    private final Set<String> alertasEnviadas; // Para evitar duplicados
    
    public AlertaStockService() {
        this.inventarioService = new InventarioService();
        this.alertasEnviadas = new HashSet<>();
    }
    
    /**
     * Verifica si hay productos con stock bajo y genera alertas
     */
    public void verificarStockBajo() {
        // Usando el método específico de InventarioService para obtener productos con stock bajo
        List<ProductoInventario> productosStockBajo = inventarioService.obtenerProductosStockBajo(stockMinimo);
        
        if (!productosStockBajo.isEmpty()) {
            // Filtrar productos que no han sido alertados aún
            productosStockBajo.removeIf(p -> alertasEnviadas.contains(p.getCodigo()));
            
            if (!productosStockBajo.isEmpty()) {
                // Mostrar alerta visual
                mostrarAlertaVisual(productosStockBajo);
                
                // Enviar correo electrónico
                enviarAlertaPorCorreo(productosStockBajo);
                
                // Registrar las alertas enviadas
                productosStockBajo.forEach(p -> alertasEnviadas.add(p.getCodigo()));
            }
        }
    }
    
    /**
     * Muestra una alerta visual para productos con stock bajo
     */
    private void mostrarAlertaVisual(List<ProductoInventario> productos) {
        Platform.runLater(() -> {
            StringBuilder mensaje = new StringBuilder("Los siguientes productos tienen stock bajo:\n\n");
            
            for (ProductoInventario producto : productos) {
                mensaje.append("- ")
                       .append(producto.getCodigo())
                       .append(" (Código: ")
                       .append(producto.getIdProducto())
                       .append(") - Stock actual: ")
                       .append(producto.getStock())
                       .append("\n");
            }
            
            Alert alert = new Alert(Alert.AlertType.WARNING, mensaje.toString(), ButtonType.OK);
            alert.setTitle("Alerta de Stock Bajo");
            alert.setHeaderText("¡Atención! Productos con stock bajo detectados");
            alert.showAndWait();
        });
    }
    
    /**
     * Envía alerta por correo electrónico
     */
    private void enviarAlertaPorCorreo(List<ProductoInventario> productos) {
        String subject = "ALERTA: Productos con stock bajo";
        
        StringBuilder body = new StringBuilder("Se han detectado los siguientes productos con stock por debajo del mínimo (" + stockMinimo + "):\n\n");
        
        for (ProductoInventario producto : productos) {
            body.append("- ")
                .append(producto.getCodigo())
                .append(" (Código: ")
                .append(producto.getIdProducto())
                .append(")\n")
                .append("  Stock actual: ")
                .append(producto.getStock())
                .append("\n");
        }
        
        body.append("\n\nPor favor, reponga estos productos pronto para evitar problemas de inventario.")
            .append("\n\nEste es un mensaje automático del sistema de inventario de Distribuciones Omega.");
        
        // Enviar en un hilo separado para no bloquear la interfaz
        new Thread(() -> {
            EmailUtil.sendEmail(emailAdmin, subject, body.toString());
        }).start();
    }
    
    /**
     * Verifica un producto específico al actualizar el inventario
     */
    public void verificarProducto(ProductoInventario producto) {
        if (producto.getStock() < stockMinimo && !alertasEnviadas.contains(producto.getCodigo())) {
            // Mostrar alerta visual
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.WARNING,
                        "El producto " + producto.getCodigo() + " (Código: " + producto.getIdProducto() + 
                        ") tiene un stock bajo: " + producto.getStock() + " unidades.", 
                        ButtonType.OK);
                alert.setTitle("Alerta de Stock Bajo");
                alert.setHeaderText("¡Atención! Stock bajo detectado");
                alert.showAndWait();
            });
            
            // Enviar correo
            final String subject = "ALERTA: Stock bajo para " + producto.getCodigo();
            final String body = "El producto " + producto.getCodigo() + " (Código: " + producto.getIdProducto() + 
                    ") tiene un stock bajo: " + producto.getStock() + " unidades.\n\n" +
                    "Por favor, reponga este producto pronto para evitar problemas de inventario.";
            
            new Thread(() -> {
                EmailUtil.sendEmail(emailAdmin, subject, body);
            }).start();
            
            // Registrar alerta
            alertasEnviadas.add(producto.getCodigo());
        }
    }
    
    /**
     * Verifica un producto por su código
     */
    public void verificarProductoPorCodigo(String codigo) {
        ProductoInventario producto = inventarioService.obtenerProductoPorCodigo(codigo);
        if (producto != null) {
            verificarProducto(producto);
        }
    }
    
    /**
     * Reinicia la lista de alertas enviadas (útil para casos de prueba o reinicio diario)
     */
    public void limpiarAlertasEnviadas() {
        alertasEnviadas.clear();
    }
    
    // Getters y setters para la configuración
    public int getStockMinimo() {
        return stockMinimo;
    }
    
    public void setStockMinimo(int stockMinimo) {
        AlertaStockService.stockMinimo = stockMinimo;
    }
    
    public String getEmailAdmin() {
        return emailAdmin;
    }
    
    public void setEmailAdmin(String emailAdmin) {
        AlertaStockService.emailAdmin = emailAdmin;
    }
}