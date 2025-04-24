package com.distribuciones.omega.model;

/**
 * Modelo para representar un item de factura
 */
public class ItemFactura {
    private Long id;
    private Factura factura; // Aquí es donde debe existir esta relación
    private ProductoInventario producto;
    private int cantidad;
    private double precioUnitario;
    private double subtotal;
    private double descuento;
    
    // Constructor por defecto
    public ItemFactura() {
    }
    
    // Getters y setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Factura getFactura() { return factura; }
    public void setFactura(Factura factura) { this.factura = factura; }
    
    public ProductoInventario getProducto() { return producto; }
    public void setProducto(ProductoInventario producto) { this.producto = producto; }
    
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    
    public double getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(double precioUnitario) { this.precioUnitario = precioUnitario; }
    
    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    public double getDescuento() { return descuento; }
    public void setDescuento(double descuento) { this.descuento = descuento; }

        /**
     * Establece el ID de la factura a la que pertenece este item
     * @param facturaId ID de la factura
     */
    public void setFacturaId(long facturaId) {
        // Si la factura no está inicializada, crearla
        if (this.factura == null) {
            this.factura = new Factura();
        }
        this.factura.setId(facturaId);
    }

    /**
     * Obtiene el ID de la factura a la que pertenece este item
     * @return ID de la factura
     */
    public long getFacturaId() {
        return factura != null ? factura.getId() : 0;
}
    
    /**
     * Calcula el subtotal del item
     */
    public void calcularSubtotal() {
        this.subtotal = this.cantidad * this.precioUnitario;
    }

    public String getNombre() {
        return producto != null ? producto.getDescripcion() : null;
    }
}