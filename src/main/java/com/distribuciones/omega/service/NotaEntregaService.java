package com.distribuciones.omega.service;

import com.distribuciones.omega.model.DetalleFactura;
import com.distribuciones.omega.model.Factura;
import com.distribuciones.omega.model.ItemFactura;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

/**
 * Servicio para manejar notas de entrega
 */
public class NotaEntregaService {
    
    private static final Logger logger = Logger.getLogger(NotaEntregaService.class.getName());
    
    /**
     * Obtiene el último número secuencial para generar una nueva nota de entrega
     * @return Último número secuencial
     */
    public int obtenerUltimoNumeroSecuencial() {
        // En una implementación real, esto consultaría a la base de datos
        // Por ahora retornamos un valor de muestra
        return 42; // Último número utilizado
    }
    
    /**
     * Genera un documento HTML con la nota de entrega
     * @param numeroNota Número de la nota de entrega
     * @param factura Factura asociada
     * @param observaciones Observaciones adicionales
     * @param responsableEntrega Nombre del responsable de entrega
     * @param responsableRecepcion Nombre del responsable de recepción
     * @return Documento HTML generado
     * @throws ParserConfigurationException Si hay error al crear el documento
     */
    public Document generarDocumentoNotaEntrega(
            String numeroNota, 
            Factura factura, 
            String observaciones,
            String responsableEntrega,
            String responsableRecepcion) throws ParserConfigurationException {

        // Convertir ItemFactura a DetalleFactura
        List<DetalleFactura> detalles = new ArrayList<>();
        for (ItemFactura item : factura.getItems()) {
            DetalleFactura detalle = new DetalleFactura();
            detalle.setCodigo(item.getProducto().getCodigo());
            detalle.setDescripcion(item.getProducto().getDescripcion());
            detalle.setCantidad(item.getCantidad());
            detalles.add(detalle);
        }
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.newDocument();
        
        // Elemento raíz
        Element html = document.createElement("html");
        document.appendChild(html);
        
        // Head
        Element head = document.createElement("head");
        html.appendChild(head);
        
        // Título y metadatos
        Element title = document.createElement("title");
        title.setTextContent("Nota de Entrega - " + numeroNota);
        head.appendChild(title);
        
        // Estilos
        Element style = document.createElement("style");
        style.setTextContent(
            "body { font-family: Arial, sans-serif; margin: 20px; }" +
            "table { width: 100%; border-collapse: collapse; margin-bottom: 20px; }" +
            "th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }" +
            "th { background-color: #f2f2f2; }" +
            ".header { display: flex; justify-content: space-between; margin-bottom: 20px; }" +
            ".company { font-size: 24px; font-weight: bold; }" +
            ".label { font-weight: bold; }" +
            ".title { font-size: 20px; font-weight: bold; text-align: center; margin: 20px 0; }" +
            ".signatures { display: flex; justify-content: space-between; margin-top: 50px; }" +
            ".signature { width: 40%; text-align: center; }" +
            ".signature-line { border-top: 1px solid #000; margin-top: 50px; padding-top: 5px; }"
        );
        head.appendChild(style);
        
        // Body
        Element body = document.createElement("body");
        html.appendChild(body);
        
        // Encabezado con logo y datos de empresa
        Element header = document.createElement("div");
        header.setAttribute("class", "header");
        body.appendChild(header);
        
        Element companyInfo = document.createElement("div");
        Element companyName = document.createElement("div");
        companyName.setAttribute("class", "company");
        companyName.setTextContent("DISTRIBUCIONES OMEGA");
        companyInfo.appendChild(companyName);
        
        Element companyAddress = document.createElement("div");
        companyAddress.setTextContent("Dirección: Av. Principal 123, Guayaquil");
        companyInfo.appendChild(companyAddress);
        
        Element companyContact = document.createElement("div");
        companyContact.setTextContent("Teléfono: (04) 555-1234 | Email: info@omega.com");
        companyInfo.appendChild(companyContact);
        
        header.appendChild(companyInfo);
        
        Element documentInfo = document.createElement("div");
        Element documentTitle = document.createElement("div");
        documentTitle.setAttribute("style", "font-size: 18px; font-weight: bold;");
        documentTitle.setTextContent("NOTA DE ENTREGA");
        documentInfo.appendChild(documentTitle);
        
        Element documentNumber = document.createElement("div");
        documentNumber.setTextContent("No. " + numeroNota);
        documentInfo.appendChild(documentNumber);
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        Element documentDate = document.createElement("div");
        documentDate.setTextContent("Fecha: " + dateFormat.format(new Date()));
        documentInfo.appendChild(documentDate);
        
        header.appendChild(documentInfo);
        
        // Título
        Element titleDiv = document.createElement("div");
        titleDiv.setAttribute("class", "title");
        titleDiv.setTextContent("NOTA DE ENTREGA DE PRODUCTOS");
        body.appendChild(titleDiv);
        
        // Datos del cliente
        Element clientSection = document.createElement("div");
        clientSection.setAttribute("style", "margin-bottom: 20px;");
        body.appendChild(clientSection);
        
        Element clientTitle = document.createElement("div");
        clientTitle.setAttribute("class", "label");
        clientTitle.setTextContent("DATOS DEL CLIENTE:");
        clientSection.appendChild(clientTitle);
        
        Element clientData = document.createElement("div");
        clientData.setAttribute("style", "display: flex; justify-content: space-between;");
        clientSection.appendChild(clientData);
        
        Element clientCol1 = document.createElement("div");
        clientCol1.setAttribute("style", "width: 48%;");
        
        // Crear el div para el cliente
        Element clientNameDiv = document.createElement("div");
        Element clientNameSpan = document.createElement("span");
        clientNameSpan.setAttribute("class", "label");
        clientNameSpan.setTextContent("Cliente: ");
        clientNameDiv.appendChild(clientNameSpan);
        clientNameDiv.appendChild(document.createTextNode(factura.getCliente().getNombre()));
        clientCol1.appendChild(clientNameDiv);
        
        // Crear el div para el ID/RUC
        Element idDiv = document.createElement("div");
        Element idSpan = document.createElement("span");
        idSpan.setAttribute("class", "label");
        idSpan.setTextContent("RUC/CI: ");
        idDiv.appendChild(idSpan);
        idDiv.appendChild(document.createTextNode(factura.getCliente().getId()));
        clientCol1.appendChild(idDiv);
        
        Element clientCol2 = document.createElement("div");
        clientCol2.setAttribute("style", "width: 48%;");
        
        // Crear el div para la dirección
        Element addressDiv = document.createElement("div");
        Element addressSpan = document.createElement("span");
        addressSpan.setAttribute("class", "label");
        addressSpan.setTextContent("Dirección: ");
        addressDiv.appendChild(addressSpan);
        addressDiv.appendChild(document.createTextNode(factura.getCliente().getDireccion()));
        clientCol2.appendChild(addressDiv);
        
        // Crear el div para el teléfono
        Element phoneDiv = document.createElement("div");
        Element phoneSpan = document.createElement("span");
        phoneSpan.setAttribute("class", "label");
        phoneSpan.setTextContent("Teléfono: ");
        phoneDiv.appendChild(phoneSpan);
        phoneDiv.appendChild(document.createTextNode(factura.getCliente().getTelefono()));
        clientCol2.appendChild(phoneDiv);
        
        clientData.appendChild(clientCol2);
        
        // Crear la referencia de factura
        Element facturaRef = document.createElement("div");
        Element facturaRefDiv = document.createElement("div");
        Element facturaRefSpan = document.createElement("span");
        facturaRefSpan.setAttribute("class", "label");
        facturaRefSpan.setTextContent("Referencia Factura: ");
        facturaRefDiv.appendChild(facturaRefSpan);
        facturaRefDiv.appendChild(document.createTextNode(factura.getNumeroFactura()));
        facturaRef.appendChild(facturaRefDiv);
        clientSection.appendChild(facturaRef);
        
        // Tabla de productos
        Element table = document.createElement("table");
        body.appendChild(table);
        
        Element tableHead = document.createElement("thead");
        table.appendChild(tableHead);
        
        Element headerRow = document.createElement("tr");
        tableHead.appendChild(headerRow);
        
        String[] columnHeaders = {"No.", "Código", "Descripción", "Cantidad"};
        for (String columnHeader : columnHeaders) {
            Element th = document.createElement("th");
            th.setTextContent(columnHeader);
            headerRow.appendChild(th);
        }
        
        Element tableBody = document.createElement("tbody");
        table.appendChild(tableBody);
        
        int itemCount = 1;
        for (DetalleFactura detalle : detalles) {
            Element row = document.createElement("tr");
            tableBody.appendChild(row);
            
            Element tdNum = document.createElement("td");
            tdNum.setTextContent(String.valueOf(itemCount++));
            row.appendChild(tdNum);
            
            Element tdCodigo = document.createElement("td");
            tdCodigo.setTextContent(detalle.getCodigo());
            row.appendChild(tdCodigo);
            
            Element tdDescripcion = document.createElement("td");
            tdDescripcion.setTextContent(detalle.getDescripcion());
            row.appendChild(tdDescripcion);
            
            Element tdCantidad = document.createElement("td");
            tdCantidad.setTextContent(String.valueOf(detalle.getCantidad()));
            row.appendChild(tdCantidad);
        }
        
        // Observaciones
        if (observaciones != null && !observaciones.trim().isEmpty()) {
            Element obsSection = document.createElement("div");
            obsSection.setAttribute("style", "margin: 20px 0;");
            body.appendChild(obsSection);
            
            Element obsTitle = document.createElement("div");
            obsTitle.setAttribute("class", "label");
            obsTitle.setTextContent("OBSERVACIONES:");
            obsSection.appendChild(obsTitle);
            
            Element obsContent = document.createElement("div");
            obsContent.setAttribute("style", "border: 1px solid #ddd; padding: 10px; min-height: 60px;");
            obsContent.setTextContent(observaciones);
            obsSection.appendChild(obsContent);
        }
        
        // Sección de firmas
        Element signaturesSection = document.createElement("div");
        signaturesSection.setAttribute("class", "signatures");
        body.appendChild(signaturesSection);
        
        Element signatureEntrega = document.createElement("div");
        signatureEntrega.setAttribute("class", "signature");
        
        Element signatureLineEntrega = document.createElement("div");
        signatureLineEntrega.setAttribute("class", "signature-line");
        signatureLineEntrega.setTextContent(responsableEntrega);
        signatureEntrega.appendChild(signatureLineEntrega);
        
        Element signatureLabelEntrega = document.createElement("div");
        signatureLabelEntrega.setTextContent("Responsable de Entrega");
        signatureEntrega.appendChild(signatureLabelEntrega);
        
        signaturesSection.appendChild(signatureEntrega);
        
        Element signatureRecepcion = document.createElement("div");
        signatureRecepcion.setAttribute("class", "signature");
        
        Element signatureLineRecepcion = document.createElement("div");
        signatureLineRecepcion.setAttribute("class", "signature-line");
        signatureLineRecepcion.setTextContent(responsableRecepcion);
        signatureRecepcion.appendChild(signatureLineRecepcion);
        
        Element signatureLabelRecepcion = document.createElement("div");
        signatureLabelRecepcion.setTextContent("Responsable de Recepción");
        signatureRecepcion.appendChild(signatureLabelRecepcion);
        
        signaturesSection.appendChild(signatureRecepcion);
        
        return document;
    }
    
    /**
     * Registra la impresión de una nota de entrega
     * @param numeroNota Número de nota de entrega
     * @param facturaId ID de la factura relacionada
     * @return true si se registró exitosamente
     */
    public boolean registrarImpresion(String numeroNota, Long facturaId) {
        // En una implementación real, esto guardaría un registro en la base de datos
        logger.info("Registrando impresión de nota de entrega: " + numeroNota + " para factura ID: " + facturaId);
        return true;
    }
}