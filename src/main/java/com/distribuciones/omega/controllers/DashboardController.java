package com.distribuciones.omega.controllers;

import com.distribuciones.omega.model.Usuario;
import com.distribuciones.omega.model.Factura;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import com.distribuciones.omega.utils.AlertUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Timer;
import java.util.TimerTask;

public class DashboardController {

    @FXML private VBox menuBoxCollapsed;
    @FXML private VBox menuBoxExpanded;
    @FXML private AnchorPane contentArea;
    @FXML private Label lblCurrentDateTime;
    @FXML private Label lblUsername;
    @FXML private Button btnLockMenu;
    @FXML private Button btnLockMenuExpanded;
    
    // Botones del menú expandido
    @FXML private Button btnClientes;
    @FXML private Button btnProductos;
    @FXML private Button btnInventario;
    @FXML private Button btnCotizacion;
    @FXML private Button btnVentas;
    @FXML private Button btnReportes;
    
    // Botones del menú contraído (los mismos, pero solo con íconos)
    @FXML private Button btnClientesCollapsed;
    @FXML private Button btnProductosCollapsed;
    @FXML private Button btnInventarioCollapsed;
    @FXML private Button btnCotizacionCollapsed;
    @FXML private Button btnVentasCollapsed;
    @FXML private Button btnReportesCollapsed;

    @FXML private MenuItem menuConfiguracionAlertas;
    @FXML private Button btnConfiguracionAlertas;

    private boolean menuLocked = false;
    private boolean menuExpanded = false;
    private Timer clockTimer;
    private Usuario usuarioActual;

    // Método existente para compatibilidad
    public void setUsername(String username) {
        if (username != null && !username.isEmpty()) {
            lblUsername.setText(username);
        }
    }
    
    /**
     * Establece el usuario actual y actualiza la interfaz
     * @param usuario Usuario autenticado
     */
    public void setUsuario(Usuario usuario) {
        this.usuarioActual = usuario;
        
        if (usuario != null) {
            // Mostrar el nombre del usuario en la etiqueta
            lblUsername.setText(usuario.getNombre());
            
            // Configurar permisos según el rol del usuario
            configurarPermisosSegunRol(usuario.getRol());
        }
    }
    
    /**
     * Configura la visibilidad de elementos según el rol del usuario
     * @param rol Rol del usuario autenticado
     */
    private void configurarPermisosSegunRol(String rol) {
        // Ejemplo de implementación según roles
        boolean esAdmin = "ADMIN".equalsIgnoreCase(rol);
        
        // Los botones de reportes solo son visibles para administradores
        btnReportes.setVisible(esAdmin);
        btnReportes.setManaged(esAdmin);
        btnReportesCollapsed.setVisible(esAdmin);
        btnReportesCollapsed.setManaged(esAdmin);
        
        // Aquí puedes añadir más configuraciones según los roles
    }
    
    /**
     * Devuelve el usuario actualmente autenticado
     * @return Usuario actual o null si no hay usuario
     */
    public Usuario getUsuarioActual() {
        return usuarioActual;
    }

    @FXML
    private void initialize() {
        // Configurar el listener para el hover del menú
        setupMenuHoverEffects();
        
        // Iniciar el reloj en tiempo real
        startClock();

        // Configurar acciones para los botones del menú expandido
        btnClientes.setOnAction(e -> loadView("clientes-gestion.fxml"));
        btnProductos.setOnAction(e -> loadView("productos-gestion.fxml"));
        btnInventario.setOnAction(e -> loadView("gestion.fxml"));
        btnCotizacion.setOnAction(e -> loadView("cotizacion.fxml"));
        btnVentas.setOnAction(e -> loadView("pago.fxml"));
        btnReportes.setOnAction(e -> loadView("reporte-facturacion.fxml"));

        // Configurar acciones para los botones del menú contraído (mismas acciones)
        btnClientesCollapsed.setOnAction(e -> loadView("clientes-gestion.fxml"));
        btnProductosCollapsed.setOnAction(e -> loadView("productos-gestion.fxml"));
        btnInventarioCollapsed.setOnAction(e -> loadView("gestion.fxml"));
        btnCotizacionCollapsed.setOnAction(e -> loadView("cotizacion.fxml"));
        btnVentasCollapsed.setOnAction(e -> loadView("pago.fxml"));
        btnReportesCollapsed.setOnAction(e -> loadView("reporte-facturacion.fxml"));

        // Configurar acción para el menú de configuración de alertas
        if (menuConfiguracionAlertas != null) {
            menuConfiguracionAlertas.setOnAction(e -> mostrarConfiguracionAlertas());
        } else {
            System.err.println("Advertencia: menuConfiguracionAlertas no encontrado en el FXML");
        }

        // Configurar acción para el botón de configuración de alertas
        if (btnConfiguracionAlertas != null) {
            btnConfiguracionAlertas.setOnAction(e -> mostrarConfiguracionAlertas());
        }

        // Carga inicial de la vista de clientes
        loadView("clientes-gestion.fxml");
    }

    /**
     * Configura los efectos de hover para expandir/contraer el menú
     */
    private void setupMenuHoverEffects() {
        // Evento para expandir al poner el cursor sobre el menú contraído
        menuBoxCollapsed.setOnMouseEntered(event -> {
            if (!menuLocked) {
                expandMenu();
            }
        });
        
        // Evento para contraer al quitar el cursor del menú expandido
        menuBoxExpanded.setOnMouseExited(event -> {
            if (!menuLocked) {
                collapseMenu();
            }
        });
    }
    
    /**
     * Expande el menú lateral con animación
     */
    private void expandMenu() {
        menuExpanded = true;
        menuBoxCollapsed.setVisible(false);
        menuBoxExpanded.setVisible(true);
        
        // Animación de transición para suavizar la expansión
        TranslateTransition transition = new TranslateTransition(Duration.millis(200), menuBoxExpanded);
        transition.setFromX(-220);
        transition.setToX(0);
        transition.play();
    }
    
    /**
     * Contrae el menú lateral con animación
     */
    private void collapseMenu() {
        menuExpanded = false;
        menuBoxCollapsed.setVisible(true);
        menuBoxExpanded.setVisible(false);
        
        // Animación de transición para suavizar la contracción
        TranslateTransition transition = new TranslateTransition(Duration.millis(200), menuBoxCollapsed);
        transition.setFromX(220);
        transition.setToX(0);
        transition.play();
    }
    
    /**
     * Alterna entre bloquear y desbloquear el estado del menú
     */
    @FXML
    public void toggleMenuLock() {
        menuLocked = !menuLocked;
        
        // Actualizar iconos en ambos botones
        String lockIcon = menuLocked ? "🔒" : "🔓";
        ((Label)btnLockMenu.getGraphic()).setText(lockIcon);
        ((Label)btnLockMenuExpanded.getGraphic()).setText(lockIcon);
        
        // Si se desbloquea y el menú estaba expandido, colapsar
        if (!menuLocked && menuExpanded) {
            collapseMenu();
        }
    }
    
    /**
     * Inicia el reloj que muestra la fecha y hora actuales
     */
    private void startClock() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        // Actualizar la hora inicialmente
        lblCurrentDateTime.setText(LocalDateTime.now().format(formatter));
        
        // Configurar actualización periódica
        clockTimer = new Timer(true);
        clockTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // Actualizar en el hilo de JavaFX
                javafx.application.Platform.runLater(() -> 
                    lblCurrentDateTime.setText(LocalDateTime.now().format(formatter))
                );
            }
        }, 0, 1000); // Actualizar cada segundo
    }
    
    /**
     * Carga una vista FXML en el área de contenido
     * @param fxmlFile nombre del archivo FXML a cargar
     */
    private void loadView(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + fxmlFile));
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
            
            // Anclar al 100% del área
            AnchorPane.setTopAnchor(view, 0.0);
            AnchorPane.setBottomAnchor(view, 0.0);
            AnchorPane.setLeftAnchor(view, 0.0);
            AnchorPane.setRightAnchor(view, 0.0);
        } catch (IOException ex) {
            ex.printStackTrace();
            AlertUtils.mostrarError("Error de carga", 
                "No se pudo cargar la vista: " + fxmlFile + "\n" + 
                "Detalles: " + ex.getMessage());
        }
    }
    
    /**
     * Muestra la ventana de configuración de alertas
     */
    private void mostrarConfiguracionAlertas() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/configuracion-alertas.fxml"));
            Parent root = loader.load();
            
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Configuración de Alertas");
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            AlertUtils.mostrarError("Error", "No se pudo abrir la ventana de configuración: " + e.getMessage());
        }
    }
    
    /**
     * Método para cargar la pantalla de pago
     * @param factura La factura a pagar
     */
    private void cargarPantallaPago(Factura factura) {
        try {
            // Cargar el FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/pago.fxml"));
            Parent root = loader.load();
            
            // Obtener el controlador
            PagoController pagoController = loader.getController();
            
            // Inicializar datos en el controlador
            pagoController.inicializarDatos(factura, null);
            
            // Crear una nueva escena y aplicar el CSS
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/pago.css").toExternalForm());
            
            // Configurar el Stage
            Stage stage = new Stage();
            stage.setTitle("Pago de Factura");
            stage.setScene(scene);
            stage.setResizable(false);
            
            // Mostrar la ventana de forma modal
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
            
            // Actualizar la vista principal si es necesario después de cerrar el diálogo
            // actualizarVistaFacturas();
            
        } catch (IOException e) {
            AlertUtils.mostrarError("Error", "No se pudo cargar la pantalla de pago: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Método para liberar recursos cuando se cierra la aplicación
     * Debe ser llamado cuando se cierra la ventana principal
     */
    public void shutdown() {
        if (clockTimer != null) {
            clockTimer.cancel();
        }
    }
}