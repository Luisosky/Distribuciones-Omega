package com.distribuciones.omega;

import com.distribuciones.omega.utils.DatabaseInitializer;
import com.distribuciones.omega.utils.InventarioMonitor;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.fxml.JavaFXBuilderFactory;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainApp extends Application {
    
    private static final Logger LOGGER = Logger.getLogger(MainApp.class.getName());

    @Override
    public void init() throws Exception {
        super.init();
        // Iniciar el monitor de inventario (verificar cada 12 horas)
        InventarioMonitor.getInstance().iniciarMonitoreo(12);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Mostrar pantalla de carga
        Label lblCarga = new Label("Inicializando base de datos...");
        ProgressIndicator progress = new ProgressIndicator();
        
        VBox splashLayout = new VBox(20);
        splashLayout.setAlignment(Pos.CENTER);
        
        // Intenta cargar el logo - si no está disponible, continúa sin él
        try {
            Image logoImage = new Image(getClass().getResourceAsStream("/images/logo.png"));
            ImageView logoView = new ImageView(logoImage);
            // Ajustar tamaño del logo
            logoView.setFitWidth(250);
            logoView.setPreserveRatio(true);
            logoView.setSmooth(true);
            splashLayout.getChildren().add(logoView);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "No se pudo cargar el logo", e);
        }
        
        splashLayout.getChildren().addAll(lblCarga, progress);
        // Ajustar tamaño de la ventana para acomodar el logo
        splashLayout.setStyle("-fx-background-color: white; -fx-padding: 30;");
        Scene splashScene = new Scene(splashLayout, 500, 400);
        
        primaryStage.setScene(splashScene);
        primaryStage.setTitle("Distribuciones Ómega - Iniciando");
        primaryStage.setResizable(false);
        primaryStage.centerOnScreen();
        primaryStage.show();
        
        // Inicializar DB en un hilo separado
        Task<Void> initTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try {
                    updateMessage("Conectando a la base de datos...");
                    // Primera fase de inicialización
                    Thread.sleep(500); // Breve pausa para mostrar los mensajes secuencialmente
                    
                    updateMessage("Verificando tablas existentes...");
                    Thread.sleep(500);
                    
                    updateMessage("Creando/actualizando estructura de datos...");
                    DatabaseInitializer.initialize();
                    
                    updateMessage("¡Inicialización completada!");
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    // Manejo de interrupción del hilo
                    Thread.currentThread().interrupt();
                }
                return null;
            }
        };
        
        // Actualizar la etiqueta de carga cuando cambie el mensaje
        initTask.messageProperty().addListener((obs, oldMsg, newMsg) -> {
            lblCarga.setText(newMsg);
        });
        
        initTask.setOnSucceeded(e -> {
            try {
                loadMainScene(primaryStage);
            } catch (Exception ex) {
                ex.printStackTrace();
                mostrarError(ex);
            }
        });
        
        initTask.setOnFailed(e -> {
            Throwable ex = initTask.getException();
            ex.printStackTrace();
            mostrarError(ex);
        });
        
        new Thread(initTask).start();
    }
    
    @Override
    public void stop() throws Exception {
        // Detener el monitor al cerrar la aplicación
        InventarioMonitor.getInstance().detenerMonitoreo();
        super.stop();
    }
    
    private void loadMainScene(Stage stage) throws Exception {
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
        stage.setScene(scene);
        
        // Añadir icono de la aplicación
        try {
            Image appIcon = new Image(getClass().getResourceAsStream("/images/logo.png"));
            stage.getIcons().add(appIcon);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "No se pudo cargar el icono de la aplicación", e);
        }
        
        stage.setTitle("Distribuciones Ómega – Login");
        stage.setResizable(true);
        stage.setMaximized(true);
    }
    
    private void mostrarError(Throwable ex) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error de inicialización");
            alert.setHeaderText("No se pudo inicializar la base de datos");
            alert.setContentText("Detalles: " + ex.getMessage());
            
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            
            TextArea textArea = new TextArea(sw.toString());
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setMaxWidth(Double.MAX_VALUE);
            textArea.setMaxHeight(Double.MAX_VALUE);
            
            alert.getDialogPane().setExpandableContent(textArea);
            alert.showAndWait();
            Platform.exit();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}