package com.distribuciones.omega.model;

public class ItemCotizacion {
    private Long id;
    private ProductoInventario producto;
    private int cantidad;
    private double precioUnitario;
    private double descuento;
    private double subtotal;
    
    public ItemCotizacion() {
        // Constructor vacío necesario para frameworks
    }
    
    public ItemCotizacion(ProductoInventario producto, int cantidad, double precioUnitario) {
        this.producto = producto;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.descuento = 0.0;
        calcularSubtotal();
    }
    
    /**
     * Calcula el subtotal considerando la cantidad, precio unitario y descuento
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