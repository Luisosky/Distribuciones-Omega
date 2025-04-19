package com.distribuciones.omega.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Orden {
    private Long id;
    private String numeroOrden;
    private Cliente cliente;
    private Usuario vendedor;
    private LocalDateTime fecha;
    private Long cotizacionId;
    private List<ItemOrden> items = new ArrayList<>();
    private double subtotal;
    private double descuento;
    private double iva;
    private double total;
    private boolean facturada = false;
    private Long facturaId;
    
    public Orden() {
        // Constructor vacío necesario para frameworks
    }
    
    // Getters y setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getNumeroOrden() {
        return numeroOrden;
    }
    
    public void setNumeroOrden(String numeroOrden) {
        this.numeroOrden = numeroOrden;
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
    
    public Long getCotizacionId() {
        return cotizacionId;
    }
    
    public void setCotizacionId(Long cotizacionId) {
        this.cotizacionId = cotizacionId;
    }
    
    public List<ItemOrden> getItems() {
        return items;
    }
    
    public void setItems(List<ItemOrden> items) {
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
    
    public boolean isFacturada() {
        return facturada;
    }
    
    public void setFacturada(boolean facturada) {
        this.facturada = facturada;
    }
    
    public Long getFacturaId() {
        return facturaId;
    }
    
    public void setFacturaId(Long facturaId) {
        this.facturaId = facturaId;
    }
    
    /**
     * Agrega un item a la orden
     */
    public void agregarItem(ItemOrden item) {
        items.add(item);
    }
    
    /**
     * Calcula los totales de la orden
     */
    public void calcularTotales() {
        // Calcular subtotal
        subtotal = items.stream()
                .mapToDouble(ItemOrden::getSubtotal)
                .sum();
        
        // Para descuentos e IVA se usarían los valores ya establecidos
        
        // Calcular total
        total = subtotal - descuento + iva;
    }
    
    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return "Orden #" + numeroOrden + " - " + 
               (fecha != null ? fecha.format(formatter) : "N/A") + 
               " - Cliente: " + (cliente != null ? cliente.getNombre() : "N/A");
    }
}