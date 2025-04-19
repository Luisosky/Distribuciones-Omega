package com.distribuciones.omega.model;

public class ItemFactura {
    private Long id;
    private ProductoInventario producto;
    private int cantidad;
    private double precioUnitario;
    private double subtotal;
    
    public ItemFactura() {
        // Constructor vacío necesario para frameworks
    }
    
    public ItemFactura(ProductoInventario producto, int cantidad, double precioUnitario) {
        this.producto = producto;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        actualizarSubtotal();
    }
    
    public void actualizarSubtotal() {
        this.subtotal = this.cantidad * this.precioUnitario;
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
        actualizarSubtotal();
    }
    
    public double getPrecioUnitario() {
        return precioUnitario;
    }
    
    public void setPrecioUnitario(double precioUnitario) {
        this.precioUnitario = precioUnitario;
        actualizarSubtotal();
    }
    
    public double getSubtotal() {
        return subtotal;
    }
    
    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }
}