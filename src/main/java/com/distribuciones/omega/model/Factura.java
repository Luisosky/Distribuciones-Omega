package com.distribuciones.omega.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Factura {
    private Long id;
    private String numeroFactura;
    private Cliente cliente;
    private Usuario vendedor;
    private LocalDateTime fecha;
    private Long ordenId;
    private Long cotizacionId; 
    private List<ItemFactura> items = new ArrayList<>();
    private double subtotal;
    private double descuento;
    private double iva;
    private double total;
    private boolean anulada = false;
    private String motivoAnulacion;
    private LocalDateTime fechaAnulacion;
    private String formaPago;
    private boolean pagada = false;
    private LocalDateTime fechaPago;
    
    public Factura() {
        // Constructor vacío necesario para frameworks
    }
    
    // Getters y setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getNumeroFactura() {
        return numeroFactura;
    }
    
    public void setNumeroFactura(String numeroFactura) {
        this.numeroFactura = numeroFactura;
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
    
    public Long getOrdenId() {
        return ordenId;
    }
    
    public void setOrdenId(Long ordenId) {
        this.ordenId = ordenId;
    }
    
    public Long getCotizacionId() {
        return cotizacionId;
    }
    
    public void setCotizacionId(Long cotizacionId) {
        this.cotizacionId = cotizacionId;
    }
    
    public List<ItemFactura> getItems() {
        return items;
    }
    
    public void setItems(List<ItemFactura> items) {
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
    
    public boolean isAnulada() {
        return anulada;
    }
    
    public void setAnulada(boolean anulada) {
        this.anulada = anulada;
    }
    
    public String getMotivoAnulacion() {
        return motivoAnulacion;
    }
    
    public void setMotivoAnulacion(String motivoAnulacion) {
        this.motivoAnulacion = motivoAnulacion;
    }
    
    public LocalDateTime getFechaAnulacion() {
        return fechaAnulacion;
    }
    
    public void setFechaAnulacion(LocalDateTime fechaAnulacion) {
        this.fechaAnulacion = fechaAnulacion;
    }
    
    public String getFormaPago() {
        return formaPago;
    }
    
    public void setFormaPago(String formaPago) {
        this.formaPago = formaPago;
    }
    
    public boolean isPagada() {
        return pagada;
    }
    
    public void setPagada(boolean pagada) {
        this.pagada = pagada;
        if (pagada) {
            // Si se marca como pagada, registrar la fecha actual
            this.fechaPago = LocalDateTime.now();
        } else {
            // Si se desmarca, eliminar la fecha de pago
            this.fechaPago = null;
        }
    }
    
    public LocalDateTime getFechaPago() {
        return fechaPago;
    }
    
    public void setFechaPago(LocalDateTime fechaPago) {
        this.fechaPago = fechaPago;
        // Si se establece una fecha, se considera como pagada
        this.pagada = (fechaPago != null);
    }
    
    /**
     * Agrega un item a la factura
     */
    public void agregarItem(ItemFactura item) {
        items.add(item);
    }
    
    /**
     * Calcula los totales de la factura
     */
    public void calcularTotales() {
        // Calcular subtotal
        subtotal = items.stream()
                .mapToDouble(ItemFactura::getSubtotal)
                .sum();
        
        // Para descuentos e IVA se usarían los valores ya establecidos
        
        // Calcular total
        total = subtotal - descuento + iva;
    }
    
    /**
     * Convierte los ItemFactura en DetalleFactura para uso en NotaEntrega
     * @return Lista de DetalleFactura
     */
    public List<DetalleFactura> getDetalles() {
        List<DetalleFactura> detalles = new ArrayList<>();
        for (ItemFactura item : this.items) {
            DetalleFactura detalle = new DetalleFactura();
            detalle.setCodigo(item.getProducto().getCodigo());
            detalle.setDescripcion(item.getProducto().getDescripcion());
            detalle.setCantidad(item.getCantidad());
            detalles.add(detalle);
        }
        return detalles;
    }
    
    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return "Factura #" + numeroFactura + " - " + 
               (fecha != null ? fecha.format(formatter) : "N/A") + 
               " - Cliente: " + (cliente != null ? cliente.getNombre() : "N/A");
    }
}