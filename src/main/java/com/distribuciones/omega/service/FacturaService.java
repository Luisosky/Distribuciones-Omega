package com.distribuciones.omega.service;

import com.distribuciones.omega.model.*;
import com.distribuciones.omega.repository.FacturaRepository;
import com.distribuciones.omega.utils.NumeroFacturaGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio para gestionar las operaciones relacionadas con facturas
 */
public class FacturaService {
    
    private final FacturaRepository facturaRepository;
    private final CotizacionService cotizacionService;
    
    public FacturaService() {
        this.facturaRepository = new FacturaRepository();
        this.cotizacionService = new CotizacionService();
    }
    
    /**
     * Genera una factura a partir de una orden
     * @param orden Orden a partir de la cual se generará la factura
     * @return Factura generada o null si hubo un error
     */
    public Factura generarFacturaDesdeOrden(Orden orden) {
        if (orden == null) {
            return null;
        }
        
        // Verificar que la orden no esté ya facturada
        if (orden.isFacturada()) {
            return null;
        }
        
        // Crear nueva factura
        Factura factura = new Factura();
        factura.setNumeroFactura(NumeroFacturaGenerator.generarNumeroFactura());
        factura.setCliente(orden.getCliente());
        factura.setVendedor(orden.getVendedor());
        factura.setFecha(LocalDateTime.now());
        factura.setOrdenId(orden.getId());
        factura.setSubtotal(orden.getSubtotal());
        factura.setDescuento(orden.getDescuento());
        factura.setIva(orden.getIva());
        factura.setTotal(orden.getTotal());
        
        // Copiar items desde la orden
        List<ItemFactura> itemsFactura = new ArrayList<>();
        for (ItemOrden itemOrden : orden.getItems()) {
            ItemFactura itemFactura = new ItemFactura();
            itemFactura.setProducto(itemOrden.getProducto());
            itemFactura.setCantidad(itemOrden.getCantidad());
            itemFactura.setPrecioUnitario(itemOrden.getPrecioUnitario());
            itemFactura.setSubtotal(itemOrden.getSubtotal());
            itemsFactura.add(itemFactura);
        }
        factura.setItems(itemsFactura);
        
        // Guardar factura
        Factura facturaGuardada = facturaRepository.save(factura);
        
        // Marcar la orden como facturada
        if (facturaGuardada != null) {
            cotizacionService.marcarOrdenComoFacturada(orden.getId(), facturaGuardada.getId());
        }
        
        return facturaGuardada;
    }
    
    /**
     * Genera una factura a partir de una cotización
     * @param cotizacion Cotización a partir de la cual se generará la factura
     * @return Factura generada o null si hubo un error
     */
    public Factura generarFacturaDesdeContizacion(Cotizacion cotizacion) {
        if (cotizacion == null) {
            return null;
        }
        
        // Verificar que la cotización no esté ya convertida a orden/factura
        if (cotizacion.isConvertidaAOrden()) {
            return null;
        }
        
        // Crear nueva factura
        Factura factura = new Factura();
        factura.setNumeroFactura(NumeroFacturaGenerator.generarNumeroFactura());
        factura.setCliente(cotizacion.getCliente());
        factura.setVendedor(cotizacion.getVendedor());
        factura.setFecha(LocalDateTime.now());
        factura.setCotizacionId(cotizacion.getId());
        factura.setSubtotal(cotizacion.getSubtotal());
        factura.setDescuento(cotizacion.getDescuento());
        factura.setIva(cotizacion.getIva());
        factura.setTotal(cotizacion.getTotal());
        
        // Copiar items desde la cotización
        List<ItemFactura> itemsFactura = new ArrayList<>();
        for (ItemCotizacion itemCotizacion : cotizacion.getItems()) {
            ItemFactura itemFactura = new ItemFactura();
            itemFactura.setProducto(itemCotizacion.getProducto());
            itemFactura.setCantidad(itemCotizacion.getCantidad());
            itemFactura.setPrecioUnitario(itemCotizacion.getPrecioUnitario());
            itemFactura.setDescuento(itemCotizacion.getDescuento());
            itemFactura.setSubtotal(itemCotizacion.getSubtotal());
            itemsFactura.add(itemFactura);
        }
        factura.setItems(itemsFactura);
        
        // Guardar factura
        Factura facturaGuardada = facturaRepository.save(factura);
        
        // Marcar la cotización como convertida a factura
        if (facturaGuardada != null) {
            cotizacion.setConvertidaAOrden(true);
            cotizacionService.actualizarCotizacion(cotizacion);
        }
        
        return facturaGuardada;
    }
    
    /**
     * Busca una factura por su ID
     * @param id ID de la factura
     * @return Factura encontrada o null si no existe
     */
    public Factura obtenerFacturaPorId(Long id) {
        return facturaRepository.findById(id);
    }
    
    /**
     * Busca una factura por su número
     * @param numeroFactura Número de factura
     * @return Factura encontrada o null si no existe
     */
    public Factura obtenerFacturaPorNumero(String numeroFactura) {
        return facturaRepository.findByNumero(numeroFactura);
    }
    
    /**
     * Obtiene todas las facturas de un cliente
     * @param clienteId ID del cliente
     * @return Lista de facturas del cliente
     */
    public List<Factura> obtenerFacturasPorCliente(Long clienteId) {
        return facturaRepository.findByClienteId(clienteId);
    }
    
    /**
     * Obtiene todas las facturas emitidas por un vendedor
     * @param vendedorId ID del vendedor
     * @return Lista de facturas del vendedor
     */
    public List<Factura> obtenerFacturasPorVendedor(Long vendedorId) {
        return facturaRepository.findByVendedorId(vendedorId);
    }
    
    /**
     * Obtiene las facturas emitidas en un rango de fechas
     * @param fechaInicio Fecha de inicio
     * @param fechaFin Fecha de fin
     * @return Lista de facturas en el rango
     */
    public List<Factura> obtenerFacturasPorRangoFechas(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        return facturaRepository.findByFechaBetween(fechaInicio, fechaFin);
    }
    
    /**
     * Anula una factura (en casos de devolución)
     * @param facturaId ID de la factura a anular
     * @param motivo Motivo de la anulación
     * @return true si la anulación fue exitosa
     */
    public boolean anularFactura(Long facturaId, String motivo) {
        Factura factura = obtenerFacturaPorId(facturaId);
        if (factura == null) {
            return false;
        }
        
        factura.setAnulada(true);
        factura.setMotivoAnulacion(motivo);
        factura.setFechaAnulacion(LocalDateTime.now());
        
        return facturaRepository.update(factura);
    }
}