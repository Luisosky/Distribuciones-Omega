package com.distribuciones.omega.service;

import com.distribuciones.omega.model.*;
import com.distribuciones.omega.repository.CotizacionRepository;
import com.distribuciones.omega.repository.OrdenRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio para gestionar las operaciones relacionadas con cotizaciones y órdenes
 */
public class CotizacionService {
    
    private final CotizacionRepository cotizacionRepository;
    private final OrdenRepository ordenRepository;
    
    public CotizacionService() {
        this.cotizacionRepository = new CotizacionRepository();
        this.ordenRepository = new OrdenRepository();
    }
    
    /**
     * Guarda una nueva cotización
     * @param cotizacion Cotización a guardar
     * @return Cotización guardada con ID asignado
     */
    public Cotizacion guardarCotizacion(Cotizacion cotizacion) {
        // Establecer fecha actual si no está definida
        if (cotizacion.getFecha() == null) {
            cotizacion.setFecha(LocalDateTime.now());
        }
        
        return cotizacionRepository.save(cotizacion);
    }
    
    /**
     * Busca una cotización por su ID
     * @param id ID de la cotización
     * @return Cotización encontrada o null si no existe
     */
    public Cotizacion obtenerCotizacionPorId(Long id) {
        return cotizacionRepository.findById(id);
    }
    
    /**
     * Obtiene todas las cotizaciones de un cliente
     * @param clienteId ID del cliente
     * @return Lista de cotizaciones del cliente
     */
    public List<Cotizacion> obtenerCotizacionesPorCliente(Long clienteId) {
        return cotizacionRepository.findByClienteId(clienteId);
    }
    
    /**
     * Obtiene todas las cotizaciones de un vendedor
     * @param vendedorId ID del vendedor
     * @return Lista de cotizaciones del vendedor
     */
    public List<Cotizacion> obtenerCotizacionesPorVendedor(Long vendedorId) {
        return cotizacionRepository.findByVendedorId(vendedorId);
    }
    
    /**
     * Obtiene la última cotización creada (para convertir a orden)
     * @return Última cotización o null si no hay cotizaciones
     */
    public Cotizacion obtenerUltimaCotizacion() {
        return cotizacionRepository.findLast();
    }
    
    /**
     * Convierte una cotización en una orden
     * @param cotizacion Cotización a convertir
     * @return Orden creada o null si hubo un error
     */
    public Orden convertirCotizacionAOrden(Cotizacion cotizacion) {
        if (cotizacion == null) {
            return null;
        }
        
        Orden orden = new Orden();
        orden.setCliente(cotizacion.getCliente());
        orden.setVendedor(cotizacion.getVendedor());
        orden.setFecha(LocalDateTime.now());
        orden.setCotizacionId(cotizacion.getId());
        orden.setSubtotal(cotizacion.getSubtotal());
        orden.setDescuento(cotizacion.getDescuento());
        orden.setIva(cotizacion.getIva());
        orden.setTotal(cotizacion.getTotal());
        
        // Copiar items
        List<ItemOrden> itemsOrden = new ArrayList<>();
        for (ItemCotizacion itemCotizacion : cotizacion.getItems()) {
            ItemOrden itemOrden = new ItemOrden();
            itemOrden.setProducto(itemCotizacion.getProducto());
            itemOrden.setCantidad(itemCotizacion.getCantidad());
            itemOrden.setPrecioUnitario(itemCotizacion.getPrecioUnitario());
            itemOrden.setSubtotal(itemCotizacion.getSubtotal());
            itemsOrden.add(itemOrden);
        }
        orden.setItems(itemsOrden);
        
        return ordenRepository.save(orden);
    }
    
    /**
     * Obtiene la última orden creada (para generar factura)
     * @return Última orden o null si no hay órdenes
     */
    public Orden obtenerUltimaOrden() {
        return ordenRepository.findLast();
    }
    
    /**
     * Obtiene todas las órdenes pendientes de facturación
     * @return Lista de órdenes pendientes
     */
    public List<Orden> obtenerOrdenesPendientes() {
        return ordenRepository.findPendientes();
    }
    
    /**
     * Marca una orden como facturada
     * @param ordenId ID de la orden
     * @param facturaId ID de la factura generada
     * @return true si la actualización fue exitosa
     */
    public boolean marcarOrdenComoFacturada(Long ordenId, Long facturaId) {
        Orden orden = ordenRepository.findById(ordenId);
        if (orden == null) {
            return false;
        }
        
        orden.setFacturada(true);
        orden.setFacturaId(facturaId);
        return ordenRepository.update(orden);
    }
}