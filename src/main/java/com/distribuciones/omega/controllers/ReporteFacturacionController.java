package com.distribuciones.omega.controllers;

import com.distribuciones.omega.model.Factura;
import com.distribuciones.omega.model.Usuario;
import com.distribuciones.omega.service.FacturaService;
import com.distribuciones.omega.service.UsuarioService;
import com.distribuciones.omega.utils.AlertUtils;
import com.distribuciones.omega.utils.ExportarUtil;
import com.distribuciones.omega.utils.GraficoUtil;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controlador para generar reportes de facturación por vendedor
 */
public class ReporteFacturacionController {

    @FXML private ComboBox<Usuario> cmbVendedor;
    @FXML private DatePicker dpFechaInicio;
    @FXML private DatePicker dpFechaFin;
    @FXML private CheckBox chkTodosVendedores;
    
    @FXML private TableView<Factura> tblFacturas;
    @FXML private TableColumn<Factura, String> colNumeroFactura;
    @FXML private TableColumn<Factura, String> colFecha;
    @FXML private TableColumn<Factura, String> colCliente;
    @FXML private TableColumn<Factura, String> colVendedor;
    @FXML private TableColumn<Factura, Double> colTotal;
    @FXML private TableColumn<Factura, String> colEstado;
    
    @FXML private Label lblTotalFacturado;
    @FXML private Label lblTotalFacturas;
    @FXML private Label lblPromedioFactura;
    
    @FXML private VBox pnlGraficos;
    @FXML private BarChart<String, Number> chartFacturacion;
    
    private FacturaService facturaService;
    private UsuarioService usuarioService;
    private DateTimeFormatter dateFormatter;
    private NumberFormat currencyFormat;
    private ObservableList<Factura> facturasData;
    
    /**
     * Inicializa el controlador
     */
    @FXML
    public void initialize() {
        facturaService = new FacturaService();
        usuarioService = new UsuarioService();
        dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "EC"));
        facturasData = FXCollections.observableArrayList();
        
        // Configurar controles de fecha
        dpFechaInicio.setValue(LocalDate.now().minusMonths(1));
        dpFechaFin.setValue(LocalDate.now());
        
        // Configurar tabla
        configurarTabla();
        
        // Cargar vendedores
        cargarVendedores();
        
        // Configurar eventos
        chkTodosVendedores.selectedProperty().addListener((obs, oldVal, newVal) -> {
            cmbVendedor.setDisable(newVal);
            if (newVal) {
                cmbVendedor.getSelectionModel().clearSelection();
            }
        });
    }
    
    /**
     * Configura las columnas de la tabla de facturas
     */
    private void configurarTabla() {
        colNumeroFactura.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getNumeroFactura()));
                
        colFecha.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getFecha().format(dateFormatter)));
                
        colCliente.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getCliente().getNombre()));
                
        colVendedor.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getVendedor().getNombre()));
                
        colTotal.setCellValueFactory(cellData -> 
                new SimpleDoubleProperty(cellData.getValue().getTotal()).asObject());
                
        colEstado.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().isPagada() ? "Pagada" : "Pendiente"));
                
        // Formato de moneda para columna de total
        colTotal.setCellFactory(tc -> new TableCell<Factura, Double>() {
            @Override
            protected void updateItem(Double total, boolean empty) {
                super.updateItem(total, empty);
                if (empty || total == null) {
                    setText(null);
                } else {
                    setText(currencyFormat.format(total));
                }
            }
        });
        
        // Color según estado de pago
        colEstado.setCellFactory(tc -> new TableCell<Factura, String>() {
            @Override
            protected void updateItem(String estado, boolean empty) {
                super.updateItem(estado, empty);
                if (empty || estado == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(estado);
                    if ("Pagada".equals(estado)) {
                        setStyle("-fx-text-fill: green;");
                    } else {
                        setStyle("-fx-text-fill: red;");
                    }
                }
            }
        });
        
        tblFacturas.setItems(facturasData);
    }
    
    /**
     * Carga los vendedores al ComboBox
     */
    private void cargarVendedores() {
        try {
            List<Usuario> vendedores = usuarioService.obtenerVendedores();
            cmbVendedor.setItems(FXCollections.observableArrayList(vendedores));
            
            // Configurar visualización de vendedores
            cmbVendedor.setCellFactory(lv -> new ListCell<Usuario>() {
                @Override
                protected void updateItem(Usuario usuario, boolean empty) {
                    super.updateItem(usuario, empty);
                    if (empty || usuario == null) {
                        setText(null);
                    } else {
                        setText(usuario.getNombre());
                    }
                }
            });
            
            cmbVendedor.setButtonCell(new ListCell<Usuario>() {
                @Override
                protected void updateItem(Usuario usuario, boolean empty) {
                    super.updateItem(usuario, empty);
                    if (empty || usuario == null) {
                        setText(null);
                    } else {
                        setText(usuario.getNombre());
                    }
                }
            });
            
        } catch (Exception e) {
            AlertUtils.mostrarError("Error", "No se pudieron cargar los vendedores: " + e.getMessage());
        }
    }
    
    /**
     * Genera el reporte según los filtros seleccionados
     */
    @FXML
    private void generarReporte() {
        // Validar fechas
        if (dpFechaInicio.getValue() == null || dpFechaFin.getValue() == null) {
            AlertUtils.mostrarAdvertencia("Datos incompletos", 
                    "Por favor seleccione un rango de fechas.");
            return;
        }
        
        // Validar vendedor (si no se seleccionó "Todos los vendedores")
        if (!chkTodosVendedores.isSelected() && cmbVendedor.getSelectionModel().getSelectedItem() == null) {
            AlertUtils.mostrarAdvertencia("Datos incompletos", 
                    "Por favor seleccione un vendedor o marque 'Todos los vendedores'.");
            return;
        }
        
        try {
            // Preparar filtros
            LocalDateTime fechaInicio = dpFechaInicio.getValue().atStartOfDay();
            LocalDateTime fechaFin = dpFechaFin.getValue().plusDays(1).atStartOfDay(); // Incluir todo el día final
            
            Usuario vendedor = null;
            if (!chkTodosVendedores.isSelected()) {
                vendedor = cmbVendedor.getSelectionModel().getSelectedItem();
            }
            
            // Obtener facturas según filtros
            List<Factura> facturas;
            if (vendedor != null) {
                facturas = facturaService.obtenerFacturasPorVendedorYRango(vendedor.getIdUsuario(), fechaInicio, fechaFin);
            } else {
                facturas = facturaService.obtenerFacturasPorRango(fechaInicio, fechaFin);
            }
            
            facturasData.clear();
            facturasData.addAll(facturas);
            
            // Calcular estadísticas
            actualizarEstadisticasReporte(facturas);
            
            // Generar gráficos
            generarGraficos(facturas);
            
        } catch (Exception e) {
            AlertUtils.mostrarError("Error al generar reporte", 
                    "No se pudo generar el reporte: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Actualiza las estadísticas del reporte
     * @param facturas Lista de facturas
     */
    private void actualizarEstadisticasReporte(List<Factura> facturas) {
        if (facturas.isEmpty()) {
            lblTotalFacturado.setText(currencyFormat.format(0));
            lblTotalFacturas.setText("0");
            lblPromedioFactura.setText(currencyFormat.format(0));
            return;
        }
        
        // Calcular total facturado
        double totalFacturado = facturas.stream()
                .mapToDouble(Factura::getTotal)
                .sum();
                
        // Calcular promedio por factura
        double promedioFactura = totalFacturado / facturas.size();
        
        // Actualizar etiquetas
        lblTotalFacturado.setText(currencyFormat.format(totalFacturado));
        lblTotalFacturas.setText(String.valueOf(facturas.size()));
        lblPromedioFactura.setText(currencyFormat.format(promedioFactura));
    }
    
    /**
     * Genera gráficos según los datos
     * @param facturas Lista de facturas
     */
    private void generarGraficos(List<Factura> facturas) {
        if (facturas.isEmpty()) {
            pnlGraficos.setVisible(false);
            return;
        }
        
        pnlGraficos.setVisible(true);
        
        // Generar gráfico de barras por vendedor
        if (chkTodosVendedores.isSelected()) {
            generarGraficoVendedores(facturas);
        } else {
            generarGraficoVentasDiarias(facturas);
        }
    }
    
    /**
     * Genera gráfico comparativo de vendedores
     * @param facturas Lista de facturas
     */
    private void generarGraficoVendedores(List<Factura> facturas) {
        // Agrupar por vendedor
        Map<String, Double> ventasPorVendedor = facturas.stream()
                .collect(Collectors.groupingBy(
                        f -> f.getVendedor().getNombre(),
                        Collectors.summingDouble(Factura::getTotal)));
        
        // Generar gráfico
        GraficoUtil.generarGraficoBarras(
                chartFacturacion, 
                "Facturación por Vendedor", 
                "Vendedor", 
                "Total Facturado ($)",
                ventasPorVendedor);
    }
    
    /**
     * Genera gráfico de ventas diarias para un vendedor
     * @param facturas Lista de facturas
     */
    private void generarGraficoVentasDiarias(List<Factura> facturas) {
        // Agrupar por fecha
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        Map<String, Double> ventasPorFecha = facturas.stream()
                .collect(Collectors.groupingBy(
                        f -> f.getFecha().toLocalDate().format(formatter),
                        Collectors.summingDouble(Factura::getTotal)));
        
        // Generar gráfico
        GraficoUtil.generarGraficoBarras(
                chartFacturacion, 
                "Ventas Diarias", 
                "Fecha", 
                "Total Facturado ($)",
                ventasPorFecha);
    }
    
    /**
     * Exporta el reporte a Excel
     */
    @FXML
    private void exportarReporte() {
        if (facturasData.isEmpty()) {
            AlertUtils.mostrarAdvertencia("Sin datos", 
                    "No hay datos para exportar. Primero genere un reporte.");
            return;
        }
        
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Guardar Reporte");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Archivos Excel", "*.xlsx"));
            
            // Sugerir nombre de archivo
            String nombreArchivo = "Reporte_Facturacion_";
            if (!chkTodosVendedores.isSelected() && cmbVendedor.getValue() != null) {
                nombreArchivo += cmbVendedor.getValue().getNombre().replace(" ", "_") + "_";
            }
            nombreArchivo += dpFechaInicio.getValue().toString() + "_a_" + dpFechaFin.getValue().toString();
            
            fileChooser.setInitialFileName(nombreArchivo + ".xlsx");
            
            File file = fileChooser.showSaveDialog(pnlGraficos.getScene().getWindow());
            if (file == null) {
                return;
            }
            
            // Preparar datos para exportar
            String titulo = "Reporte de Facturación";
            String subtitulo = "Período: " + dpFechaInicio.getValue().toString() + " al " + dpFechaFin.getValue().toString();
            if (!chkTodosVendedores.isSelected() && cmbVendedor.getValue() != null) {
                subtitulo += " - Vendedor: " + cmbVendedor.getValue().getNombre();
            }
            
            // Preparar estadísticas
            Map<String, String> estadisticas = new HashMap<>();
            estadisticas.put("Total Facturado", lblTotalFacturado.getText());
            estadisticas.put("Cantidad de Facturas", lblTotalFacturas.getText());
            estadisticas.put("Promedio por Factura", lblPromedioFactura.getText());
            
            // Columnas
            String[] columnas = {"Nº Factura", "Fecha", "Cliente", "Vendedor", "Total", "Estado"};
            
            // Convertir datos a formato para exportar
            List<Object[]> datos = facturasData.stream().map(f -> new Object[]{
                f.getNumeroFactura(),
                f.getFecha().format(dateFormatter),
                f.getCliente().getNombre(),
                f.getVendedor().getNombre(),
                f.getTotal(),
                f.isPagada() ? "Pagada" : "Pendiente"
            }).collect(Collectors.toList());
            
            // Exportar
            ExportarUtil.exportarExcel(file.getAbsolutePath(), titulo, subtitulo, estadisticas, columnas, datos);
            
            AlertUtils.mostrarInformacion("Exportación exitosa", 
                    "El reporte ha sido exportado correctamente a:\n" + file.getAbsolutePath());
            
        } catch (Exception e) {
            AlertUtils.mostrarError("Error al exportar", 
                    "No se pudo exportar el reporte: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Exporta el reporte a PDF
     */
    @FXML
    private void exportarPDF() {
        if (facturasData.isEmpty()) {
            AlertUtils.mostrarAdvertencia("Sin datos", 
                    "No hay datos para exportar. Primero genere un reporte.");
            return;
        }
        
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Guardar Reporte PDF");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Archivos PDF", "*.pdf"));
            
            // Sugerir nombre de archivo
            String nombreArchivo = "Reporte_Facturacion_";
            if (!chkTodosVendedores.isSelected() && cmbVendedor.getValue() != null) {
                nombreArchivo += cmbVendedor.getValue().getNombre().replace(" ", "_") + "_";
            }
            nombreArchivo += dpFechaInicio.getValue().toString() + "_a_" + dpFechaFin.getValue().toString();
            
            fileChooser.setInitialFileName(nombreArchivo + ".pdf");
            
            File file = fileChooser.showSaveDialog(pnlGraficos.getScene().getWindow());
            if (file == null) {
                return;
            }
            
            // Preparar datos para exportar
            String titulo = "Reporte de Facturación";
            String subtitulo = "Período: " + dpFechaInicio.getValue().toString() + " al " + dpFechaFin.getValue().toString();
            if (!chkTodosVendedores.isSelected() && cmbVendedor.getValue() != null) {
                subtitulo += " - Vendedor: " + cmbVendedor.getValue().getNombre();
            }
            
            // Preparar estadísticas
            Map<String, String> estadisticas = new HashMap<>();
            estadisticas.put("Total Facturado", lblTotalFacturado.getText());
            estadisticas.put("Cantidad de Facturas", lblTotalFacturas.getText());
            estadisticas.put("Promedio por Factura", lblPromedioFactura.getText());
            
            // Columnas
            String[] columnas = {"Nº Factura", "Fecha", "Cliente", "Vendedor", "Total", "Estado"};
            
            // Convertir datos a formato para exportar
            List<Object[]> datos = facturasData.stream().map(f -> new Object[]{
                f.getNumeroFactura(),
                f.getFecha().format(dateFormatter),
                f.getCliente().getNombre(),
                f.getVendedor().getNombre(),
                f.getTotal(),
                f.isPagada() ? "Pagada" : "Pendiente"
            }).collect(Collectors.toList());
            
            // Exportar
            ExportarUtil.exportarPDF(file.getAbsolutePath(), titulo, subtitulo, estadisticas, columnas, datos);
            
            AlertUtils.mostrarInformacion("Exportación exitosa", 
                    "El reporte ha sido exportado correctamente a:\n" + file.getAbsolutePath());
            
        } catch (Exception e) {
            AlertUtils.mostrarError("Error al exportar", 
                    "No se pudo exportar el reporte: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Imprime el reporte directamente
     */
    @FXML
    private void imprimirReporte() {
        if (facturasData.isEmpty()) {
            AlertUtils.mostrarAdvertencia("Sin datos", 
                    "No hay datos para imprimir. Primero genere un reporte.");
            return;
        }
        
        try {
            // Preparar datos para imprimir
            String titulo = "Reporte de Facturación";
            String subtitulo = "Período: " + dpFechaInicio.getValue().toString() + " al " + dpFechaFin.getValue().toString();
            if (!chkTodosVendedores.isSelected() && cmbVendedor.getValue() != null) {
                subtitulo += " - Vendedor: " + cmbVendedor.getValue().getNombre();
            }
            
            // Preparar estadísticas
            Map<String, String> estadisticas = new HashMap<>();
            estadisticas.put("Total Facturado", lblTotalFacturado.getText());
            estadisticas.put("Cantidad de Facturas", lblTotalFacturas.getText());
            estadisticas.put("Promedio por Factura", lblPromedioFactura.getText());
            
            // Columnas
            String[] columnas = {"Nº Factura", "Fecha", "Cliente", "Vendedor", "Total", "Estado"};
            
            // Convertir datos a formato para exportar
            List<Object[]> datos = facturasData.stream().map(f -> new Object[]{
                f.getNumeroFactura(),
                f.getFecha().format(dateFormatter),
                f.getCliente().getNombre(),
                f.getVendedor().getNombre(),
                f.getTotal(),
                f.isPagada() ? "Pagada" : "Pendiente"
            }).collect(Collectors.toList());
            
            // Imprimir usando utilidad de impresión
            ExportarUtil.imprimirReporte(titulo, subtitulo, estadisticas, columnas, datos);
            
        } catch (Exception e) {
            AlertUtils.mostrarError("Error al imprimir", 
                    "No se pudo imprimir el reporte: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Analiza los métodos de pago utilizados en las facturas
     */
    @FXML
    private void analizarMetodosPago() {
        if (facturasData.isEmpty()) {
            AlertUtils.mostrarAdvertencia("Sin datos", 
                    "No hay datos para analizar. Primero genere un reporte.");
            return;
        }
        
        try {
            // Obtenemos las IDs de las facturas para consultar sus pagos
            List<Long> idsFacturas = facturasData.stream()
                    .map(Factura::getId)
                    .collect(Collectors.toList());
            
            // Aquí deberíamos obtener los pagos para estas facturas
            // En un caso real, se consultaría a PagoService para obtener esta información
            
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Análisis de Métodos de Pago");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(tblFacturas.getScene().getWindow());
            
            VBox content = new VBox(10);
            content.setPadding(new javafx.geometry.Insets(20));
            
            Label titulo = new Label("Métodos de Pago Utilizados");
            titulo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
            
            // Este es un ejemplo simulado de distribución de métodos de pago
            // En un caso real, estos datos vendrían de la base de datos
            Map<String, Double> distribucionPagos = new HashMap<>();
            distribucionPagos.put("Efectivo", 45.0);
            distribucionPagos.put("Tarjeta de Crédito", 30.0);
            distribucionPagos.put("Transferencia", 20.0);
            distribucionPagos.put("Cheque", 5.0);
            
            // Crear gráfico de torta para métodos de pago
            javafx.scene.chart.PieChart pieChart = new javafx.scene.chart.PieChart();
            pieChart.setTitle("Distribución de Métodos de Pago");
            
            for (Map.Entry<String, Double> entry : distribucionPagos.entrySet()) {
                javafx.scene.chart.PieChart.Data slice = 
                        new javafx.scene.chart.PieChart.Data(entry.getKey() + " (" + entry.getValue() + "%)", entry.getValue());
                pieChart.getData().add(slice);
            }
            
            // Tabla con detalles
            TableView<Map.Entry<String, Double>> tablaPagos = new TableView<>();
            
            TableColumn<Map.Entry<String, Double>, String> colMetodo = new TableColumn<>("Método de Pago");
            colMetodo.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getKey()));
            
            TableColumn<Map.Entry<String, Double>, String> colPorcentaje = new TableColumn<>("Porcentaje");
            colPorcentaje.setCellValueFactory(data -> 
                    new SimpleStringProperty(data.getValue().getValue() + "%"));
            
            tablaPagos.getColumns().addAll(colMetodo, colPorcentaje);
            tablaPagos.setItems(FXCollections.observableArrayList(distribucionPagos.entrySet()));
            
            // Agregar a la vista
            content.getChildren().addAll(titulo, pieChart, tablaPagos);
            
            // Botón para cerrar
            Button btnCerrar = new Button("Cerrar");
            btnCerrar.setOnAction(e -> dialogStage.close());
            content.getChildren().add(btnCerrar);
            
            Scene scene = new Scene(content, 600, 500);
            dialogStage.setScene(scene);
            dialogStage.show();
            
        } catch (Exception e) {
            AlertUtils.mostrarError("Error", 
                    "No se pudo analizar los métodos de pago: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Cierra la ventana actual
     */
    @FXML
    private void cerrar() {
        Stage stage = (Stage) tblFacturas.getScene().getWindow();
        stage.close();
    }
}