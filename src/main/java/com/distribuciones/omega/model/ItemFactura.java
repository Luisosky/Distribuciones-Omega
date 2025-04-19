package com.distribuciones.omega.model;

/**
 * Modelo para representar un item de factura
 */
public class ItemFactura {
    private Long id;
    private ProductoInventario producto;
    private int cantidad;
    private double precioUnitario;
    private double descuento;
    private double subtotal;
    
    /**
     * Constructor vacío
     */
    public ItemFactura() {
    }
    
    /**
     * Constructor con parámetros
     */
    public ItemFactura(ProductoInventario producto, int cantidad, double precioUnitario) {
        this.producto = producto;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.descuento = 0.0;
        calcularSubtotal();
    }
    
    /**
     * Calcula el subtotal considerando cantidad, precio unitario y descuento
     */
    public void calcularSubtotal() {
        this.subtotal = (this.cantidad * this.precioUnitario) - this.descuento;
        if (this.subtotal < 0) {
            this.subtotal = 0;
        }
    }
    
    /**
     * Método de compatibilidad con código existente
     */
    public void actualizarSubtotal() {
        calcularSubtotal();
    }
    
    // Getters y setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public ProductoInventario getProducto() {
        return producto;
    }
    
    public void setProducto(ProductoInventario producto) {
        this.producto = producto;
    }
    
    public int getCantidad() {
        return cantidad;
    }
    
    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
        calcularSubtotal();
    }
    
    public double getPrecioUnitario() {
        return precioUnitario;
    }
    
    public void setPrecioUnitario(double precioUnitario) {
        this.precioUnitario = precioUnitario;
        calcularSubtotal();
    }
    
    public double getDescuento() {
        return descuento;
    }
    
    public void setDescuento(double descuento) {
        this.descuento = descuento;
        calcularSubtotal();
    }
    
    public double getSubtotal() {
        return subtotal;
    }
    
    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }
}