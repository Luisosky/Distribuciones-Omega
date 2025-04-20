package com.distribuciones.omega.controllers;

import com.distribuciones.omega.model.DetalleFactura;
import com.distribuciones.omega.model.Factura;
import com.distribuciones.omega.service.ConfiguracionService;
import com.distribuciones.omega.service.NotaEntregaService;
import com.distribuciones.omega.utils.AlertUtils;
import com.distribuciones.omega.utils.PrintUtil;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.print.PrinterJob;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import org.w3c.dom.Document;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Optional;

/**
 * Controlador para la ventana de notas de entrega
 */
public class NotaEntregaController {

    @FXML private TextField txtNumeroNota;
    @FXML private Label lblCliente;
    @FXML private Label lblDireccion;
    @FXML private Label lblRuc;
    @FXML private Label lblTelefono;
    @FXML private Label lblFechaEmision;
    @FXML private Label lblFactura;
    
    @FXML private TableView<DetalleFactura> tblProductos;
    @FXML private TableColumn<DetalleFactura, String> colCodigo;
    @FXML private TableColumn<DetalleFactura, String> colDescripcion;
    @FXML private TableColumn<DetalleFactura, Integer> colCantidad;
    
    @FXML private TextArea txtObservaciones;
    @FXML private TextField txtResponsableEntrega;
    @FXML private TextField txtResponsableRecepcion;
    
    @FXML private WebView webView;
    @FXML private Button btnGenerar;
    @FXML private Button btnImprimir;
    @FXML private Button btnGuardar;
    @FXML private Button btnCancelar;
    
    private Factura factura;
    private NotaEntregaService notaEntregaService;
    private ConfiguracionService configuracionService;
    private Document documentoGenerado;
    
    /**
     * Inicializa el controlador
     */
    @FXML
    public void initialize() {
        notaEntregaService = new NotaEntregaService();
        configuracionService = new ConfiguracionService();
        
        // Configurar las columnas de la tabla
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        
        // Generar número de nota automáticamente
        generarNumeroNota();
        
        // Deshabilitar botones hasta que se genere la nota
        btnImprimir.setDisable(true);
        btnGuardar.setDisable(true);
    }
    
    /**
     * Establece la factura asociada a la nota de entrega
     */
    public void setFactura(Factura factura) {
        this.factura = factura;
        
        // Llenar datos del cliente
        lblCliente.setText(factura.getCliente().getNombre());
        lblDireccion.setText(factura.getCliente().getDireccion());
        lblRuc.setText(factura.getCliente().getId()); // Usar ID en lugar de RUC
        lblTelefono.setText(factura.getCliente().getTelefono());
        
        // Fecha y número de factura
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        lblFechaEmision.setText(factura.getFecha().format(formatter));
        lblFactura.setText(factura.getNumeroFactura());
        
        // Cargar productos usando el método getDetalles()
        tblProductos.setItems(FXCollections.observableArrayList(factura.getDetalles()));
    }
    
    /**
     * Genera un número de nota de entrega automáticamente
     */
    private void generarNumeroNota() {
        // Formato: NE-YYYYMMDD-XXX donde XXX es un número secuencial
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        String fecha = dateFormat.format(new Date());
        
        // Obtener el último número secuencial y sumar 1
        int ultimoNumero = notaEntregaService.obtenerUltimoNumeroSecuencial();
        String numeroSecuencial = String.format("%03d", ultimoNumero + 1);
        
        txtNumeroNota.setText("NE-" + fecha + "-" + numeroSecuencial);
    }
    
    /**
     * Genera la nota de entrega
     */
    @FXML
    private void generarNotaEntrega() {
        // Validar campos obligatorios
        if (txtResponsableEntrega.getText().isEmpty()) {
            AlertUtils.mostrarAdvertencia("Datos incompletos", 
                    "Por favor ingrese el nombre del responsable de entrega.");
            return;
        }
        
        if (txtResponsableRecepcion.getText().isEmpty()) {
            AlertUtils.mostrarAdvertencia("Datos incompletos", 
                    "Por favor ingrese el nombre del responsable de recepción.");
            return;
        }
        
        try {
            // Generar el documento HTML de la nota de entrega
            documentoGenerado = notaEntregaService.generarDocumentoNotaEntrega(
                    txtNumeroNota.getText(),
                    factura,
                    txtObservaciones.getText(),
                    txtResponsableEntrega.getText(),
                    txtResponsableRecepcion.getText()
            );
            
            // Mostrar en el WebView
            WebEngine webEngine = webView.getEngine();
            String htmlContent = PrintUtil.convertirDocumentoAHtml(documentoGenerado);
            webEngine.loadContent(htmlContent);
            
            // Habilitar botones
            btnImprimir.setDisable(false);
            btnGuardar.setDisable(false);
            
            // Deshabilitar edición
            txtNumeroNota.setEditable(false);
            txtObservaciones.setEditable(false);
            txtResponsableEntrega.setEditable(false);
            txtResponsableRecepcion.setEditable(false);
            btnGenerar.setDisable(true);
            
            // Mostrar mensaje de éxito
            AlertUtils.mostrarInformacion("Nota generada", 
                    "La nota de entrega ha sido generada exitosamente.");
            
        } catch (Exception e) {
            AlertUtils.mostrarError("Error", 
                    "No se pudo generar la nota de entrega: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Imprime la nota de entrega
     */
    @FXML
    private void imprimirNotaEntrega() {
        try {
            PrinterJob job = PrinterJob.createPrinterJob();
            
            if (job != null) {
                boolean showDialog = job.showPrintDialog(webView.getScene().getWindow());
                
                if (showDialog) {
                    // Configurar la impresión
                    PrintUtil.configurarImpresion(job, webView);
                    
                    // Ejecutar trabajo de impresión
                    boolean success = job.endJob();
                    
                    if (success) {
                        AlertUtils.mostrarInformacion("Impresión exitosa", 
                                "La nota de entrega se ha enviado a la impresora.");
                        
                        // Guardar registro de impresión
                        notaEntregaService.registrarImpresion(txtNumeroNota.getText(), factura.getId());
                    } else {
                        AlertUtils.mostrarAdvertencia("Error de impresión", 
                                "No se pudo completar la impresión.");
                    }
                }
            } else {
                AlertUtils.mostrarError("Error", "No se pudo iniciar el trabajo de impresión.");
            }
            
        } catch (Exception e) {
            AlertUtils.mostrarError("Error", 
                    "No se pudo imprimir la nota de entrega: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Guarda la nota de entrega como PDF
     */
    @FXML
    private void guardarNotaEntrega() {
        try {
            // Pedir ubicación para guardar
            String outputPath = PrintUtil.elegirUbicacionPDF("Nota_Entrega_" + txtNumeroNota.getText());
            
            if (outputPath != null) {
                // Convertir y guardar como PDF
                boolean resultado = PrintUtil.convertirHtmlAPdf(documentoGenerado, outputPath);
                
                if (resultado) {
                    AlertUtils.mostrarInformacion("PDF guardado", 
                            "La nota de entrega ha sido guardada como PDF.\n\n" +
                            "Ubicación: " + outputPath);
                    
                    // Preguntar si desea abrir el archivo
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Abrir archivo");
                    alert.setHeaderText("El PDF ha sido guardado");
                    alert.setContentText("¿Desea abrir el archivo ahora?");
                    
                    Optional<ButtonType> result = alert.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        PrintUtil.abrirEnNavegador(outputPath);
                    }
                } else {
                    AlertUtils.mostrarError("Error", 
                            "No se pudo guardar la nota de entrega como PDF.");
                }
            }
            
        } catch (IOException e) {
            AlertUtils.mostrarError("Error", 
                    "No se pudo guardar la nota de entrega: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Cierra la ventana
     */
    @FXML
    private void cancelar() {
        // Si se generó la nota, preguntar si desea salir sin guardar/imprimir
        if (documentoGenerado != null && (btnImprimir.isDisabled() == false || btnGuardar.isDisabled() == false)) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirmar");
            alert.setHeaderText("Cerrar sin imprimir/guardar");
            alert.setContentText("¿Está seguro de que desea cerrar sin imprimir o guardar la nota de entrega?");
            
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() != ButtonType.OK) {
                return;
            }
        }
        
        Stage stage = (Stage) btnCancelar.getScene().getWindow();
        stage.close();
    }
}