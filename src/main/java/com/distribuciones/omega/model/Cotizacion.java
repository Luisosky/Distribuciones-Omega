package com.distribuciones.omega.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Cotizacion {
    private Long id;
    private String numeroCotizacion;
    private Cliente cliente;
    private Usuario vendedor;
    private LocalDateTime fecha;
    private List<ItemCotizacion> items = new ArrayList<>();
    private double subtotal;
    private double descuento;
    private double iva;
    private double total;
    private boolean convertidaAOrden = false;
    private String observaciones;
    
    public Cotizacion() {
        // Constructor vacío necesario para frameworks
    }
    
    // Getters y setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getNumeroCotizacion() {
        return numeroCotizacion;
    }
    
    public void setNumeroCotizacion(String numeroCotizacion) {
        this.numeroCotizacion = numeroCotizacion;
    }
    
    public Cliente getCliente() {
        return cliente;
    }
    
    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }
    
    public Usuario getVendedor() {
        return vendedor;
    }
    
    public void setVendedor(Usuario vendedor) {
        this.vendedor = vendedor;
    }
    
    public LocalDateTime getFecha() {
        return fecha;
    }
    
    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }
    
    public List<ItemCotizacion> getItems() {
        return items;
    }
    
    public void setItems(List<ItemCotizacion> items) {
        this.items = items;
    }
    
    public double getSubtotal() {
        return subtotal;
    }
    
    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }
    
    public double getDescuento() {
        return descuento;
    }
    
    public void setDescuento(double descuento) {
        this.descuento = descuento;
    }
    
    public double getIva() {
        return iva;
    }
    
    public void setIva(double iva) {
        this.iva = iva;
    }
    
    public double getTotal() {
        return total;
    }
    
    public void setTotal(double total) {
        this.total = total;
    }
    
    public boolean isConvertidaAOrden() {
        return convertidaAOrden;
    }
    
    public void setConvertidaAOrden(boolean convertidaAOrden) {
        this.convertidaAOrden = convertidaAOrden;
    }
    
    public String getObservaciones() {
        return observaciones;
    }
    
    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
    
    /**
     * Agrega un item a la cotización
     */
    public void agregarItem(ItemCotizacion item) {
        items.add(item);
    }
    
    /**
     * Elimina un item de la cotización
     */
    public void eliminarItem(ItemCotizacion item) {
        items.remove(item);
    }
    
    /**
     * Calcula los totales de la cotización
     */
    public void calcularTotales() {
        // Calcular subtotal
        subtotal = items.stream()
                .mapToDouble(ItemCotizacion::getSubtotal)
                .sum();
        
        // Para descuentos e IVA se usarían los valores ya establecidos
        
        // Calcular total
        total = subtotal - descuento + iva;
    }
    
    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return "Cotización #" + numeroCotizacion + " - " + 
               (fecha != null ? fecha.format(formatter) : "N/A") + 
               " - Cliente: " + (cliente != null ? cliente.getNombre() : "N/A");
    }
}