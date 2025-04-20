package com.distribuciones.omega.controllers;

import com.distribuciones.omega.model.Factura;
import com.distribuciones.omega.model.ItemFactura;
import com.distribuciones.omega.service.ConfiguracionService;
import com.distribuciones.omega.utils.AlertUtils;
import com.distribuciones.omega.utils.PrintUtil;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.print.PrinterJob;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Controlador para la visualización e impresión de facturas
 */
public class ImprimirFacturaController {

    @FXML private Label lblNumeroFactura;
    @FXML private Label lblFecha;
    @FXML private Label lblCliente;
    @FXML private Label lblDireccion;
    @FXML private Label lblRuc;
    @FXML private Label lblVendedor;
    
    @FXML private TableView<ItemFactura> tblItems;
    @FXML private TableColumn<ItemFactura, Integer> colCantidad;
    @FXML private TableColumn<ItemFactura, String> colDescripcion;
    @FXML private TableColumn<ItemFactura, Double> colPrecioUnitario;
    @FXML private TableColumn<ItemFactura, Double> colSubtotal;
    
    @FXML private Label lblSubtotal;
    @FXML private Label lblDescuento;
    @FXML private Label lblIva;
    @FXML private Label lblTotal;
    @FXML private Label lblEstadoPago;
    
    @FXML private VBox pnlContenido;
    @FXML private WebView webView;
    
    private Factura factura;
    private ConfiguracionService configService;
    private NumberFormat currencyFormat;
    private DateTimeFormatter dateFormatter;
    
    /**
     * Inicializa el controlador
     */
    @FXML
    public void initialize() {
        configService = new ConfiguracionService();
        currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "EC"));
        dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        
        // Configurar tabla de items
        configurarTabla();
    }
    
    /**
     * Configura las columnas de la tabla de items
     */
    private void configurarTabla() {
        colCantidad.setCellValueFactory(cellData -> 
                new SimpleIntegerProperty(cellData.getValue().getCantidad()).asObject());
                
        colDescripcion.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getProducto().getDescripcion()));
                
        colPrecioUnitario.setCellValueFactory(cellData -> 
                new SimpleDoubleProperty(cellData.getValue().getPrecioUnitario()).asObject());
                
        colSubtotal.setCellValueFactory(cellData -> 
                new SimpleDoubleProperty(cellData.getValue().getSubtotal()).asObject());
                
        // Formato de moneda para columnas de precios
        colPrecioUnitario.setCellFactory(tc -> new TableCell<ItemFactura, Double>() {
            @Override
            protected void updateItem(Double precio, boolean empty) {
                super.updateItem(precio, empty);
                if (empty || precio == null) {
                    setText(null);
                } else {
                    setText(currencyFormat.format(precio));
                }
            }
        });
        
        colSubtotal.setCellFactory(tc -> new TableCell<ItemFactura, Double>() {
            @Override
            protected void updateItem(Double subtotal, boolean empty) {
                super.updateItem(subtotal, empty);
                if (empty || subtotal == null) {
                    setText(null);
                } else {
                    setText(currencyFormat.format(subtotal));
                }
            }
        });
    }
    
    /**
     * Establece la factura a mostrar e imprimir
     * @param factura Factura a procesar
     */
    public void setFactura(Factura factura) {
        this.factura = factura;
        
        // Actualizar UI con datos de la factura
        lblNumeroFactura.setText(factura.getNumeroFactura());
        lblFecha.setText(factura.getFecha().format(dateFormatter));
        lblCliente.setText(factura.getCliente().getNombre());
        lblDireccion.setText(factura.getCliente().getDireccion());
        lblRuc.setText(factura.getCliente().getId());
        lblVendedor.setText(factura.getVendedor().getNombre());
        
        // Cargar items
        ObservableList<ItemFactura> items = FXCollections.observableArrayList(factura.getItems());
        tblItems.setItems(items);
        
        // Actualizar totales
        lblSubtotal.setText(currencyFormat.format(factura.getSubtotal()));
        lblDescuento.setText(currencyFormat.format(factura.getDescuento()));
        lblIva.setText(currencyFormat.format(factura.getIva()));
        lblTotal.setText(currencyFormat.format(factura.getTotal()));
        
        // Estado de pago
        if (factura.isPagada()) {
            lblEstadoPago.setText("PAGADA");
            lblEstadoPago.getStyleClass().add("estado-pagado");
        } else {
            lblEstadoPago.setText("PENDIENTE DE PAGO");
            lblEstadoPago.getStyleClass().add("estado-pendiente");
        }
        
        // Preparar vista web para impresión
        prepararVistaWeb();
    }
    
    /**
     * Prepara la vista web con el contenido de la factura para imprimir
     */
    private void prepararVistaWeb() {
        try {
            // Cargar configuración de la empresa
            Map<String, String> configEmpresa = new HashMap<>();
            configEmpresa.put("nombre", configService.getConfiguracion("EMPRESA_NOMBRE", "Distribuciones Omega"));
            configEmpresa.put("direccion", configService.getConfiguracion("EMPRESA_DIRECCION", "Dirección no configurada"));
            configEmpresa.put("telefono", configService.getConfiguracion("EMPRESA_TELEFONO", "Teléfono no configurado"));
            configEmpresa.put("email", configService.getConfiguracion("EMPRESA_EMAIL", "Email no configurado"));
            configEmpresa.put("ruc", configService.getConfiguracion("EMPRESA_RUC", "RUC no configurado"));
            
            // Generar HTML para impresión
            String htmlContent = generarHtmlFactura(configEmpresa);
            
            // Cargar HTML en WebView
            webView.getEngine().loadContent(htmlContent);
            
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtils.mostrarError("Error", "No se pudo preparar la vista de impresión: " + e.getMessage());
        }
    }
    
    /**
     * Genera el HTML de la factura para impresión
     * @param configEmpresa Configuración de la empresa
     * @return HTML formateado de la factura
     */
    private String generarHtmlFactura(Map<String, String> configEmpresa) {
        StringBuilder html = new StringBuilder();
        
        // Estilo CSS
        html.append("<!DOCTYPE html><html><head><style>");
        html.append("body { font-family: Arial, sans-serif; font-size: 10pt; line-height: 1.3; }");
        html.append(".header { text-align: center; margin-bottom: 20px; }");
        html.append(".company-name { font-size: 14pt; font-weight: bold; margin-bottom: 5px; }");
        html.append(".invoice-title { font-size: 12pt; font-weight: bold; margin: 10px 0; text-align: center; }");
        html.append(".details { width: 100%; margin-bottom: 15px; }");
        html.append(".details td { padding: 2px 5px; }");
        html.append(".client-info, .invoice-info { width: 50%; vertical-align: top; }");
        html.append("table.items { width: 100%; border-collapse: collapse; margin: 15px 0; }");
        html.append("table.items th, table.items td { border: 1px solid #ddd; padding: 5px; text-align: left; }");
        html.append("table.items th { background-color: #f5f5f5; }");
        html.append(".totals { width: 100%; }");
        html.append(".totals td { padding: 3px 5px; }");
        html.append(".total-label { text-align: right; font-weight: bold; }");
        html.append(".footer { margin-top: 30px; font-size: 9pt; text-align: center; }");
        html.append(".paid { color: green; font-weight: bold; border: 1px solid green; padding: 5px; text-align: center; transform: rotate(-5deg); display: inline-block; }");
        html.append(".unpaid { color: red; font-weight: bold; border: 1px solid red; padding: 5px; text-align: center; transform: rotate(-5deg); display: inline-block; }");
        html.append(".stamp { position: absolute; top: 40%; left: 30%; opacity: 0.5; }");
        html.append("</style></head><body>");
        
        // Encabezado con datos de la empresa
        html.append("<div class='header'>");
        html.append("<div class='company-name'>").append(configEmpresa.get("nombre")).append("</div>");
        html.append("<div>RUC: ").append(configEmpresa.get("ruc")).append("</div>");
        html.append("<div>").append(configEmpresa.get("direccion")).append("</div>");
        html.append("<div>Tel: ").append(configEmpresa.get("telefono")).append(" | Email: ").append(configEmpresa.get("email")).append("</div>");
        html.append("</div>");
        
        // Título de la factura
        html.append("<div class='invoice-title'>FACTURA N° ").append(factura.getNumeroFactura()).append("</div>");
        
        // Información del cliente y la factura
        html.append("<table class='details'><tr>");
        
        // Información del cliente
        html.append("<td class='client-info'>");
        html.append("<strong>Cliente:</strong> ").append(factura.getCliente().getNombre()).append("<br/>");
        html.append("<strong>RUC/CI:</strong> ").append(factura.getCliente().getId()).append("<br/>");
        html.append("<strong>Dirección:</strong> ").append(factura.getCliente().getDireccion()).append("<br/>");
        if (factura.getCliente().getTelefono() != null && !factura.getCliente().getTelefono().isEmpty()) {
            html.append("<strong>Teléfono:</strong> ").append(factura.getCliente().getTelefono()).append("<br/>");
        }
        html.append("</td>");
        
        // Información de la factura
        html.append("<td class='invoice-info'>");
        html.append("<strong>Fecha:</strong> ").append(factura.getFecha().format(dateFormatter)).append("<br/>");
        html.append("<strong>Vendedor:</strong> ").append(factura.getVendedor().getNombre()).append("<br/>");
        if (factura.isPagada() && factura.getFechaPago() != null) {
            html.append("<strong>Fecha de Pago:</strong> ").append(factura.getFechaPago().format(dateFormatter)).append("<br/>");
        }
        html.append("</td>");
        
        html.append("</tr></table>");
        
        // Detalle de items
        html.append("<table class='items'>");
        html.append("<thead><tr>");
        html.append("<th>Cant.</th>");
        html.append("<th>Descripción</th>");
        html.append("<th>P. Unit.</th>");
        html.append("<th>Subtotal</th>");
        html.append("</tr></thead><tbody>");
        
        // Filas de items
        for (ItemFactura item : factura.getItems()) {
            html.append("<tr>");
            html.append("<td>").append(item.getCantidad()).append("</td>");
            html.append("<td>").append(item.getProducto().getDescripcion()).append("</td>");
            html.append("<td>").append(formatCurrency(item.getPrecioUnitario())).append("</td>");
            html.append("<td>").append(formatCurrency(item.getSubtotal())).append("</td>");
            html.append("</tr>");
        }
        
        html.append("</tbody></table>");
        
        // Totales
        html.append("<table class='totals' align='right' style='width: 250px;'>");
        html.append("<tr><td class='total-label'>Subtotal:</td><td>").append(formatCurrency(factura.getSubtotal())).append("</td></tr>");
        html.append("<tr><td class='total-label'>Descuento:</td><td>").append(formatCurrency(factura.getDescuento())).append("</td></tr>");
        html.append("<tr><td class='total-label'>IVA (12%):</td><td>").append(formatCurrency(factura.getIva())).append("</td></tr>");
        html.append("<tr><td class='total-label'>TOTAL:</td><td><strong>").append(formatCurrency(factura.getTotal())).append("</strong></td></tr>");
        html.append("</table>");
        
        // Sello de pagado/pendiente
        html.append("<div class='stamp'>");
        if (factura.isPagada()) {
            html.append("<div class='paid'>PAGADO</div>");
        } else {
            html.append("<div class='unpaid'>PENDIENTE DE PAGO</div>");
        }
        html.append("</div>");
        
        // Pie de página
        html.append("<div class='footer'>");
        html.append("<p>¡Gracias por su compra!</p>");
        html.append("<p>Este documento es un comprobante válido para su contabilidad.</p>");
        html.append("</div>");
        
        html.append("</body></html>");
        
        return html.toString();
    }
    
    /**
     * Formatea un valor como moneda
     * @param valor Valor a formatear
     * @return String formateado como moneda
     */
    private String formatCurrency(double valor) {
        return currencyFormat.format(valor);
    }
    
    /**
     * Imprime la factura
     */
    @FXML
    private void imprimirFactura() {
        try {
            PrinterJob job = PrinterJob.createPrinterJob();
            
            if (job != null && job.showPrintDialog(pnlContenido.getScene().getWindow())) {
                // Verificar si se pudo establecer la configuración de impresión
                if (PrintUtil.configurarImpresion(job, webView)) {
                    // Imprimir
                    boolean success = job.printPage(webView);
                    if (success) {
                        job.endJob();
                        AlertUtils.mostrarInformacion("Impresión exitosa", 
                                "La factura ha sido enviada a la impresora.");
                        cerrarVentana();
                    } else {
                        AlertUtils.mostrarError("Error de impresión", 
                                "No se pudo completar la impresión. Verifique la impresora.");
                    }
                } else {
                    AlertUtils.mostrarError("Error de configuración", 
                            "No se pudo configurar la impresión.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtils.mostrarError("Error", "Ocurrió un error durante la impresión: " + e.getMessage());
        }
    }
    
    /**
     * Visualiza la factura en el navegador predeterminado
     */
    @FXML
    private void visualizarEnNavegador() {
        try {
            // Guardar HTML en archivo temporal y abrirlo en navegador
            String tempFile = PrintUtil.guardarHtmlTemporal(webView.getEngine().getDocument());
            PrintUtil.abrirEnNavegador(tempFile);
            
            AlertUtils.mostrarInformacion("Visualización", 
                    "La factura ha sido abierta en su navegador predeterminado.");
            
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtils.mostrarError("Error", "No se pudo abrir en el navegador: " + e.getMessage());
        }
    }
    
    /**
     * Guarda la factura como PDF
     */
    @FXML
    private void guardarPDF() {
        try {
            // Elegir ubicación para guardar
            String filePath = PrintUtil.elegirUbicacionPDF("Factura_" + factura.getNumeroFactura());
            
            if (filePath != null) {
                // Convertir HTML a PDF
                if (PrintUtil.convertirHtmlAPdf(webView.getEngine().getDocument(), filePath)) {
                    AlertUtils.mostrarInformacion("PDF Guardado", 
                            "La factura ha sido guardada como PDF en:\n" + filePath);
                } else {
                    AlertUtils.mostrarError("Error", "No se pudo generar el archivo PDF.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtils.mostrarError("Error", "No se pudo guardar como PDF: " + e.getMessage());
        }
    }
    
    /**
     * Cierra la ventana actual
     */
    @FXML
    private void cerrarVentana() {
        Stage stage = (Stage) pnlContenido.getScene().getWindow();
        stage.close();
    }
}