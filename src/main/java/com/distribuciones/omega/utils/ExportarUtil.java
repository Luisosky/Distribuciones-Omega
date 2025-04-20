package com.distribuciones.omega.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * Utilidad para exportar datos en diferentes formatos
 */
public class ExportarUtil {
    
    /**
     * Exporta datos a un archivo CSV
     * @param datos Lista de datos a exportar
     * @param encabezados Encabezados para el CSV
     * @return Ruta del archivo generado o null si hubo un error
     */
    public static String exportarCSV(List<List<String>> datos, List<String> encabezados) {
        try {
            // Mostrar diálogo para elegir ubicación
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Exportar a CSV");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Archivos CSV", "*.csv"));
            fileChooser.setInitialFileName("reporte.csv");
            
            File file = fileChooser.showSaveDialog(new Stage());
            if (file == null) {
                return null;
            }
            
            // Generar contenido CSV
            StringBuilder csv = new StringBuilder();
            
            // Agregar encabezados
            csv.append(String.join(",", encabezados)).append("\n");
            
            // Agregar datos
            for (List<String> fila : datos) {
                csv.append(String.join(",", fila)).append("\n");
            }
            
            // Escribir a archivo
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(csv.toString());
            }
            
            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Exporta datos a un archivo Excel (XLSX)
     * @param rutaArchivo Ruta donde se guardará el archivo
     * @param titulo Título del reporte
     * @param subtitulo Subtítulo o descripción del reporte
     * @param estadisticas Mapa de estadísticas a incluir (clave-valor)
     * @param columnas Nombres de las columnas
     * @param datos Lista de filas con datos (array de objetos)
     * @return true si la exportación fue exitosa
     * @throws Exception Si ocurre un error durante la exportación
     */
    public static boolean exportarExcel(
            String rutaArchivo, 
            String titulo, 
            String subtitulo, 
            Map<String, String> estadisticas, 
            String[] columnas, 
            List<Object[]> datos) throws Exception {
        
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Crear hoja de cálculo
            XSSFSheet sheet = workbook.createSheet("Reporte");
            
            // Estilos
            CellStyle estiloTitulo = crearEstiloTitulo(workbook);
            CellStyle estiloSubtitulo = crearEstiloSubtitulo(workbook);
            CellStyle estiloEncabezado = crearEstiloEncabezado(workbook);
            CellStyle estiloDato = crearEstiloDato(workbook);
            CellStyle estiloMoneda = crearEstiloMoneda(workbook);
            CellStyle estiloEstadistica = crearEstiloEstadistica(workbook);
            
            int filaActual = 0;
            
            // Título
            Row filaTitulo = sheet.createRow(filaActual++);
            Cell celdaTitulo = filaTitulo.createCell(0);
            celdaTitulo.setCellValue(titulo);
            celdaTitulo.setCellStyle(estiloTitulo);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, columnas.length - 1));
            
            // Subtítulo
            Row filaSubtitulo = sheet.createRow(filaActual++);
            Cell celdaSubtitulo = filaSubtitulo.createCell(0);
            celdaSubtitulo.setCellValue(subtitulo);
            celdaSubtitulo.setCellStyle(estiloSubtitulo);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, columnas.length - 1));
            
            filaActual++; // Espacio en blanco
            
            // Estadísticas
            if (estadisticas != null && !estadisticas.isEmpty()) {
                Row filaEstadisticasTitulo = sheet.createRow(filaActual++);
                Cell celdaEstadisticasTitulo = filaEstadisticasTitulo.createCell(0);
                celdaEstadisticasTitulo.setCellValue("Estadísticas:");
                celdaEstadisticasTitulo.setCellStyle(estiloEncabezado);
                
                for (Map.Entry<String, String> estadistica : estadisticas.entrySet()) {
                    Row filaEstadistica = sheet.createRow(filaActual++);
                    
                    Cell celdaNombreEstadistica = filaEstadistica.createCell(0);
                    celdaNombreEstadistica.setCellValue(estadistica.getKey() + ":");
                    celdaNombreEstadistica.setCellStyle(estiloEstadistica);
                    
                    Cell celdaValorEstadistica = filaEstadistica.createCell(1);
                    celdaValorEstadistica.setCellValue(estadistica.getValue());
                    celdaValorEstadistica.setCellStyle(estiloEstadistica);
                }
                
                filaActual++; // Espacio en blanco
            }
            
            // Encabezados de columna
            Row filaEncabezados = sheet.createRow(filaActual++);
            for (int i = 0; i < columnas.length; i++) {
                Cell celda = filaEncabezados.createCell(i);
                celda.setCellValue(columnas[i]);
                celda.setCellStyle(estiloEncabezado);
            }
            
            // Datos
            for (Object[] fila : datos) {
                Row filaExcel = sheet.createRow(filaActual++);
                
                for (int i = 0; i < fila.length; i++) {
                    Cell celda = filaExcel.createCell(i);
                    
                    if (fila[i] == null) {
                        celda.setCellValue("");
                    } else if (fila[i] instanceof String) {
                        celda.setCellValue((String) fila[i]);
                    } else if (fila[i] instanceof Number) {
                        celda.setCellValue(((Number) fila[i]).doubleValue());
                        
                        // Si es la columna de monto (asumiendo que es la quinta columna, índice 4)
                        if (i == 4) {
                            celda.setCellStyle(estiloMoneda);
                        }
                    } else if (fila[i] instanceof Boolean) {
                        celda.setCellValue((Boolean) fila[i]);
                    } else {
                        celda.setCellValue(fila[i].toString());
                    }
                    
                    if (i != 4) { // No aplicar estilo a columnas con formato especial
                        celda.setCellStyle(estiloDato);
                    }
                }
            }
            
            // Ajustar ancho de columnas
            for (int i = 0; i < columnas.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // Guardar archivo
            try (FileOutputStream outputStream = new FileOutputStream(rutaArchivo)) {
                workbook.write(outputStream);
            }
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * Exporta datos a un archivo PDF
     * @param rutaArchivo Ruta donde se guardará el archivo
     * @param titulo Título del reporte
     * @param subtitulo Subtítulo o descripción del reporte
     * @param estadisticas Mapa de estadísticas a incluir (clave-valor)
     * @param columnas Nombres de las columnas
     * @param datos Lista de filas con datos (array de objetos)
     * @return true si la exportación fue exitosa
     * @throws Exception Si ocurre un error durante la exportación
     */
    public static boolean exportarPDF(
            String rutaArchivo, 
            String titulo, 
            String subtitulo, 
            Map<String, String> estadisticas, 
            String[] columnas, 
            List<Object[]> datos) throws Exception {
        
        // Crear HTML para el reporte
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html><head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<title>").append(titulo).append("</title>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; margin: 20px; }");
        html.append("h1 { color: #333366; text-align: center; }");
        html.append("h2 { color: #666699; margin-top: 0; }");
        html.append(".estadisticas { margin: 20px 0; padding: 10px; background-color: #f5f5f5; border-radius: 5px; }");
        html.append(".estadisticas h3 { margin-top: 0; color: #333366; }");
        html.append(".estadisticas table { width: 100%; }");
        html.append(".estadisticas td { padding: 5px; }");
        html.append("table.datos { width: 100%; border-collapse: collapse; margin-top: 20px; }");
        html.append("table.datos th { background-color: #333366; color: white; padding: 8px; text-align: left; }");
        html.append("table.datos td { padding: 8px; border-bottom: 1px solid #ddd; }");
        html.append("table.datos tr:nth-child(even) { background-color: #f2f2f2; }");
        html.append(".moneda { text-align: right; }");
        html.append(".estado-pagada { color: green; font-weight: bold; }");
        html.append(".estado-pendiente { color: red; font-weight: bold; }");
        html.append("</style>");
        html.append("</head><body>");
        
        // Título y subtítulo
        html.append("<h1>").append(titulo).append("</h1>");
        html.append("<h2>").append(subtitulo).append("</h2>");
        
        // Estadísticas
        if (estadisticas != null && !estadisticas.isEmpty()) {
            html.append("<div class='estadisticas'>");
            html.append("<h3>Estadísticas</h3>");
            html.append("<table>");
            
            for (Map.Entry<String, String> entry : estadisticas.entrySet()) {
                html.append("<tr>");
                html.append("<td><strong>").append(entry.getKey()).append(":</strong></td>");
                html.append("<td>").append(entry.getValue()).append("</td>");
                html.append("</tr>");
            }
            
            html.append("</table>");
            html.append("</div>");
        }
        
        // Tabla de datos
        html.append("<table class='datos'>");
        
        // Encabezados
        html.append("<thead><tr>");
        for (String columna : columnas) {
            html.append("<th>").append(columna).append("</th>");
        }
        html.append("</tr></thead>");
        
        // Datos
        html.append("<tbody>");
        for (Object[] fila : datos) {
            html.append("<tr>");
            
            for (int i = 0; i < fila.length; i++) {
                String valor = (fila[i] != null) ? fila[i].toString() : "";
                
                // Aplicar clases según el tipo de dato
                if (i == 4) { // Columna Total (asumiendo que es la quinta columna)
                    html.append("<td class='moneda'>").append(valor).append("</td>");
                } else if (i == 5) { // Columna Estado (asumiendo que es la sexta columna)
                    String clase = "estado-pendiente";
                    if ("Pagada".equals(valor)) {
                        clase = "estado-pagada";
                    }
                    html.append("<td class='").append(clase).append("'>").append(valor).append("</td>");
                } else {
                    html.append("<td>").append(valor).append("</td>");
                }
            }
            
            html.append("</tr>");
        }
        html.append("</tbody>");
        html.append("</table>");
        
        html.append("</body></html>");
        
        // Convertir HTML a PDF usando iText
        try (FileOutputStream fileOutputStream = new FileOutputStream(rutaArchivo)) {
            com.itextpdf.html2pdf.HtmlConverter.convertToPdf(
                    html.toString(), 
                    fileOutputStream);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * Imprime un reporte
     * @param titulo Título del reporte
     * @param subtitulo Subtítulo o descripción del reporte
     * @param estadisticas Mapa de estadísticas a incluir (clave-valor)
     * @param columnas Nombres de las columnas
     * @param datos Lista de filas con datos (array de objetos)
     * @return true si la impresión fue exitosa
     * @throws Exception Si ocurre un error durante la impresión
     */
    public static boolean imprimirReporte(
            String titulo, 
            String subtitulo, 
            Map<String, String> estadisticas, 
            String[] columnas, 
            List<Object[]> datos) throws Exception {
        
        // Crear archivo PDF temporal
        File tempFile = File.createTempFile("reporte_", ".pdf");
        String rutaTemporal = tempFile.getAbsolutePath();
        
        // Exportar a PDF
        exportarPDF(rutaTemporal, titulo, subtitulo, estadisticas, columnas, datos);
        
        // Imprimir PDF
        PrintUtil.imprimirArchivo(rutaTemporal);
        
        // Eliminar archivo temporal
        tempFile.deleteOnExit();
        
        return true;
    }
    
    // Métodos auxiliares para crear estilos en Excel
    
    private static CellStyle crearEstiloTitulo(XSSFWorkbook workbook) {
        CellStyle estilo = workbook.createCellStyle();
        XSSFFont fuente = workbook.createFont();
        fuente.setBold(true);
        fuente.setFontHeightInPoints((short) 16);
        estilo.setFont(fuente);
        estilo.setAlignment(HorizontalAlignment.CENTER);
        return estilo;
    }
    
    private static CellStyle crearEstiloSubtitulo(XSSFWorkbook workbook) {
        CellStyle estilo = workbook.createCellStyle();
        XSSFFont fuente = workbook.createFont();
        fuente.setFontHeightInPoints((short) 12);
        estilo.setFont(fuente);
        estilo.setAlignment(HorizontalAlignment.CENTER);
        return estilo;
    }
    
    private static CellStyle crearEstiloEncabezado(XSSFWorkbook workbook) {
        CellStyle estilo = workbook.createCellStyle();
        XSSFFont fuente = workbook.createFont();
        fuente.setBold(true);
        estilo.setFont(fuente);
        estilo.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        estilo.setBorderBottom(BorderStyle.THIN);
        estilo.setBorderTop(BorderStyle.THIN);
        estilo.setBorderLeft(BorderStyle.THIN);
        estilo.setBorderRight(BorderStyle.THIN);
        return estilo;
    }
    
    private static CellStyle crearEstiloDato(XSSFWorkbook workbook) {
        CellStyle estilo = workbook.createCellStyle();
        estilo.setBorderBottom(BorderStyle.THIN);
        estilo.setBorderTop(BorderStyle.THIN);
        estilo.setBorderLeft(BorderStyle.THIN);
        estilo.setBorderRight(BorderStyle.THIN);
        return estilo;
    }
    
    private static CellStyle crearEstiloMoneda(XSSFWorkbook workbook) {
        CellStyle estilo = workbook.createCellStyle();
        estilo.setBorderBottom(BorderStyle.THIN);
        estilo.setBorderTop(BorderStyle.THIN);
        estilo.setBorderLeft(BorderStyle.THIN);
        estilo.setBorderRight(BorderStyle.THIN);
        estilo.setDataFormat(workbook.createDataFormat().getFormat("$#,##0.00"));
        return estilo;
    }
    
    private static CellStyle crearEstiloEstadistica(XSSFWorkbook workbook) {
        CellStyle estilo = workbook.createCellStyle();
        XSSFFont fuente = workbook.createFont();
        fuente.setBold(true);
        estilo.setFont(fuente);
        return estilo;
    }
}