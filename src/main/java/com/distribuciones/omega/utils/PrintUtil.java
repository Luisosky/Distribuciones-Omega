package com.distribuciones.omega.utils;

import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.PrinterJob;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.itextpdf.html2pdf.HtmlConverter;

/**
 * Utilidad para operaciones de impresión y exportación
 */
public class PrintUtil {

    /**
     * Configura un trabajo de impresión para un WebView
     * @param job Trabajo de impresión a configurar
     * @param webView WebView que contiene el contenido a imprimir
     * @return true si la configuración fue exitosa
     */
    public static boolean configurarImpresion(PrinterJob job, WebView webView) {
        // Configurar para impresión en papel carta (letter)
        PageLayout pageLayout = job.getPrinter().createPageLayout(
                Paper.NA_LETTER, 
                PageOrientation.PORTRAIT, 
                10, 10, 10, 10); // Márgenes
                
        job.getJobSettings().setPageLayout(pageLayout);
        
        // Configurar para imprimir todo el contenido del WebView
        webView.getEngine().print(job);
        
        return true;
    }
    
    /**
     * Guarda el contenido HTML de un documento en un archivo temporal
     * @param document Documento HTML
     * @return Ruta al archivo temporal
     * @throws IOException Si ocurre un error al guardar
     */
    public static String guardarHtmlTemporal(Document document) throws IOException {
        // Convertir documento a string HTML
        String html = getOuterHTML(document.getDocumentElement());
        
        // Crear archivo temporal
        Path tempFile = Files.createTempFile("factura_", ".html");
        
        // Escribir HTML al archivo
        try (FileWriter writer = new FileWriter(tempFile.toFile())) {
            writer.write(html);
        }
        
        return tempFile.toString();
    }
    
    /**
     * Convierte un documento DOM a una cadena HTML
     * @param document Documento DOM
     * @return Cadena HTML
     * @throws TransformerException Si ocurre un error durante la transformación
     */
    public static String convertirDocumentoAHtml(Document document) throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.METHOD, "html");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        
        return writer.toString();
    }
    
    /**
     * Convierte un elemento DOM a su representación HTML como cadena
     * @param element Elemento DOM a convertir
     * @return Representación HTML del elemento
     */
    private static String getOuterHTML(Element element) {
        try {
            TransformerFactory transFactory = TransformerFactory.newInstance();
            Transformer transformer = transFactory.newTransformer();
            StringWriter buffer = new StringWriter();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.transform(new DOMSource(element), new StreamResult(buffer));
            return buffer.toString();
        } catch (TransformerException e) {
            throw new RuntimeException("Error al convertir Element a cadena HTML", e);
        }
    }
    
    /**
     * Abre un archivo en el navegador predeterminado
     * @param filePath Ruta al archivo
     * @throws IOException Si ocurre un error al abrir el archivo
     */
    public static void abrirEnNavegador(String filePath) throws IOException {
        File file = new File(filePath);
        Desktop.getDesktop().browse(file.toURI());
    }
    
    /**
     * Muestra un diálogo para elegir dónde guardar un PDF
     * @param defaultName Nombre predeterminado para el archivo
     * @return Ruta seleccionada o null si se canceló la operación
     */
    public static String elegirUbicacionPDF(String defaultName) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar como PDF");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Archivos PDF", "*.pdf"));
        fileChooser.setInitialFileName(defaultName + ".pdf");
        
        File file = fileChooser.showSaveDialog(new Stage());
        return (file != null) ? file.getAbsolutePath() : null;
    }
    
    /**
     * Convierte un documento HTML a PDF y lo guarda en la ruta especificada
     * @param document Documento HTML
     * @param outputPath Ruta donde guardar el PDF
     * @return true si la conversión fue exitosa
     */
    public static boolean convertirHtmlAPdf(Document document, String outputPath) {
        try {
            // Convertir documento a string HTML
            String html = getOuterHTML(document.getDocumentElement());
            
            // Guardar en archivo temporal primero
            Path tempHtmlFile = Files.createTempFile("factura_temp_", ".html");
            try (FileWriter writer = new FileWriter(tempHtmlFile.toFile())) {
                writer.write(html);
            }
            
            // Convertir HTML a PDF usando iText
            HtmlConverter.convertToPdf(
                    new File(tempHtmlFile.toString()), 
                    new File(outputPath));
            
            // Eliminar archivo temporal
            Files.deleteIfExists(tempHtmlFile);
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Imprime un archivo utilizando el visor predeterminado del sistema
     * @param rutaArchivo Ruta al archivo a imprimir
     * @return true si se envió a imprimir correctamente
     * @throws IOException Si ocurre un error al abrir el archivo
     */
    public static boolean imprimirArchivo(String rutaArchivo) throws IOException {
        File file = new File(rutaArchivo);
        
        if (!file.exists()) {
            throw new IOException("El archivo no existe: " + rutaArchivo);
        }
        
        // En Windows, podemos usar la impresión predeterminada
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            String comando = "rundll32 url.dll,FileProtocolHandler " + file.getAbsolutePath();
            Runtime.getRuntime().exec(comando);
            return true;
        } else {
            // En otros sistemas operativos, simplemente abrimos el archivo
            Desktop.getDesktop().open(file);
            return true;
        }
    }
}