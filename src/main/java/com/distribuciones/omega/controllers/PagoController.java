package com.distribuciones.omega.controllers;

import com.distribuciones.omega.model.*;
import com.distribuciones.omega.service.*;
import com.distribuciones.omega.utils.AlertUtils;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.TextArea;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Controlador para la pantalla de pago de facturas
 */
public class PagoController {
    
    // Campos para mostrar la información de la factura
    @FXML private Label lblNumeroFactura;
    @FXML private Label lblFechaFactura;
    @FXML private Label lblClienteNombre;
    @FXML private Label lblVendedorNombre;
    @FXML private TextArea txtResumenCompra;
    @FXML private Label lblSubtotal;
    @FXML private Label lblDescuento;
    @FXML private Label lblIva;
    @FXML private Label lblTotal;
    @FXML private Label lblFechaPago;
    
    // Campos para el pago
    @FXML private ComboBox<String> cmbFormaPago;
    @FXML private TextField txtMontoPago;
    @FXML private Label lblCambio;
    @FXML private Button btnProcesarPago;
    @FXML private Button btnImprimir;
    @FXML private Button btnVolver;
    
    // Servicios
    private final FacturaService facturaService = new FacturaService();
    
    // Datos
    private Factura factura;
    private NumberFormat currencyFormat;
    
    /**
     * Inicializa el controlador
     */
    @FXML
    public void initialize() {
        lblFechaPago.setVisible(false);

        // Inicializar formato de moneda
        currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "EC"));
        
        // Configurar formas de pago
        cmbFormaPago.getItems().addAll("EFECTIVO", "TARJETA DE CRÉDITO", "TARJETA DE DÉBITO", "TRANSFERENCIA", "CHEQUE");
        cmbFormaPago.getSelectionModel().selectFirst();
        
        // Configurar event listeners
        txtMontoPago.textProperty().addListener((obs, oldVal, newVal) -> {
            calcularCambio();
        });
        
        cmbFormaPago.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean esEfectivo = "EFECTIVO".equals(newVal);
            txtMontoPago.setDisable(!esEfectivo);
            lblCambio.setVisible(esEfectivo);
            
            if (!esEfectivo) {
                txtMontoPago.setText("");
                lblCambio.setText("Cambio: " + currencyFormat.format(0));
            }
        });
        
        // Botones inicialmente deshabilitados hasta que se cargue una factura
        btnProcesarPago.setDisable(true);
        btnImprimir.setDisable(true);
    }
    
    /**
     * Inicializa los datos con la factura que se va a pagar
     * @param factura Factura a pagar
     */
    public void inicializarDatos(Factura factura) {
        if (factura == null) {
            AlertUtils.mostrarError("Error", "No se recibió información de factura válida");
            return;
        }
        
        this.factura = factura;
        
        // Formatear fecha
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String fechaFormateada = factura.getFecha().format(formatter);
        
        // Llenar información de la factura
        lblNumeroFactura.setText(factura.getNumeroFactura());
        lblFechaFactura.setText(fechaFormateada);
        lblClienteNombre.setText(factura.getCliente().getNombre());
        lblVendedorNombre.setText(factura.getVendedor().getNombre());
        
        // Mostrar resumen de compra
        StringBuilder resumen = new StringBuilder();
        for (ItemFactura item : factura.getItems()) {
            resumen.append(item.getCantidad())
                  .append(" x ")
                  .append(item.getProducto().getDescripcion())
                  .append(" - ")
                  .append(currencyFormat.format(item.getSubtotal()))
                  .append("\n");
        }
        txtResumenCompra.setText(resumen.toString());
        
        // Mostrar totales
        lblSubtotal.setText(currencyFormat.format(factura.getSubtotal()));
        lblDescuento.setText(currencyFormat.format(factura.getDescuento()));
        lblIva.setText(currencyFormat.format(factura.getIva()));
        lblTotal.setText(currencyFormat.format(factura.getTotal()));
        
        // Habilitar botones
        btnProcesarPago.setDisable(false);
        txtMontoPago.setDisable(false);
        
        // Establecer el monto sugerido
        txtMontoPago.setText(String.valueOf(factura.getTotal()));
    }
    
    /**
     * Calcula el cambio basado en el monto pagado
     */
    private void calcularCambio() {
        try {
            if (txtMontoPago.getText() == null || txtMontoPago.getText().trim().isEmpty()) {
                lblCambio.setText("Cambio: " + currencyFormat.format(0));
                return;
            }
            
            double montoPagado = Double.parseDouble(txtMontoPago.getText());
            double cambio = montoPagado - factura.getTotal();
            
            lblCambio.setText("Cambio: " + currencyFormat.format(cambio));
            
            // Habilitar botón de procesar solo si el monto es suficiente
            btnProcesarPago.setDisable(cambio < 0);
            
        } catch (NumberFormatException e) {
            lblCambio.setText("Valor inválido");
            btnProcesarPago.setDisable(true);
        }
    }
    
    /**
     * Procesa el pago de la factura
     */
    @FXML
    private void procesarPago() {
        try {
            // Validar entrada
            if (cmbFormaPago.getValue() == null) {
                AlertUtils.mostrarError("Error", "Debe seleccionar una forma de pago");
                return;
            }

            
            // 1. Obtener forma de pago seleccionada
            String formaPago = cmbFormaPago.getValue();
            if (formaPago == null || formaPago.isEmpty()) {
                AlertUtils.mostrarAdvertencia("Forma de Pago", "Debe seleccionar una forma de pago");
                return;
            }
            
            // 2. Actualizar la factura local
            factura.setFormaPago(formaPago);
            
            // 3. Para pagos en efectivo, verificar que el monto sea suficiente
            if ("EFECTIVO".equals(formaPago)) {
                try {
                    double montoPagado = Double.parseDouble(txtMontoPago.getText().trim());
                    if (montoPagado < factura.getTotal()) {
                        AlertUtils.mostrarAdvertencia("Monto Insuficiente", 
                                "El monto pagado debe ser igual o mayor al total de la factura");
                        return;
                    }
                } catch (NumberFormatException e) {
                    AlertUtils.mostrarAdvertencia("Monto Inválido", 
                            "Por favor ingrese un monto válido");
                    return;
                }
            }
            
            // 4. Actualizar la factura en la base de datos
            factura.setFormaPago(formaPago);
            boolean actualizado = facturaService.actualizarEstadoPago(factura.getId(), true);
            
            if (actualizado) {
                AlertUtils.mostrarInformacion("Pago Procesado", 
                        "El pago ha sido procesado correctamente");
                
                // 5. Actualizar la interfaz
                btnImprimir.setDisable(false);
                btnProcesarPago.setDisable(true);
                txtMontoPago.setDisable(true);
                cmbFormaPago.setDisable(true);
                
                // Mostrar fecha de pago
                if (factura.getFechaPago() != null) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
                    String fechaPagoFormateada = factura.getFechaPago().format(formatter);
                    lblFechaPago.setText("Fecha de Pago: " + fechaPagoFormateada);
                    lblFechaPago.setVisible(true);
                }
            } else {
                AlertUtils.mostrarError("Error", 
                        "No se pudo procesar el pago. Intente nuevamente");
            }
            
        } catch (Exception e) {
            AlertUtils.mostrarError("Error", 
                    "Error al procesar el pago: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Imprime la factura
     */
    @FXML
    private void imprimirFactura() {
        try {
            // Obtener la factura actualizada de la base de datos
            Factura facturaActualizada = facturaService.obtenerFacturaPorId(factura.getId());
            
            if (facturaActualizada == null) {
                AlertUtils.mostrarError("Error", "No se pudo obtener la información actualizada de la factura");
                return;
            }
            
            // Cargar el controlador de impresión de factura
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/imprimir-factura.fxml"));
            Parent root = loader.load();
            
            // Obtener el controlador y configurar la factura
            ImprimirFacturaController controller = loader.getController();
            controller.setFactura(facturaActualizada);
            
            // Crear una nueva escena y ventana
            Scene scene = new Scene(root);
            Stage stage = new Stage();
            stage.setTitle("Imprimir Factura - " + facturaActualizada.getNumeroFactura());
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL); 
            stage.showAndWait();
            
        } catch (Exception e) {
            AlertUtils.mostrarError("Error", 
                    "Error al preparar la impresión: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Vuelve a la pantalla principal
     */
    @FXML
    private void volver() {
        try {
            // Navegar a la pantalla principal o cerrar esta ventana
            // Implementar según tu navegación
        } catch (Exception e) {
            AlertUtils.mostrarError("Error", 
                    "Error al volver: " + e.getMessage());
        }
    }
}