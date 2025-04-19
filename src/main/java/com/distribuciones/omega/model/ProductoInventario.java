package com.distribuciones.omega.model;

public class ProductoInventario {
    private Long idProducto;
    private String codigo;
    private String descripcion;
    private double precio;
    private int stock;
    private String numeroSerie;
    private String categoria;
    private String proveedor;
    private boolean activo = true;
    
    public ProductoInventario() {
        // Constructor vacío necesario para frameworks
    }
    
    public ProductoInventario(String codigo, String descripcion, double precio, int stock, String numeroSerie) {
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.precio = precio;
        this.stock = stock;
        this.numeroSerie = numeroSerie;
    }
    
    public ProductoInventario(Long idProducto, String codigo, String descripcion, double precio, 
                              int stock, String numeroSerie, String categoria, String proveedor, boolean activo) {
        this.idProducto = idProducto;
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.precio = precio;
        this.stock = stock;
        this.numeroSerie = numeroSerie;
        this.categoria = categoria;
        this.proveedor = proveedor;
        this.activo = activo;
    }
    
    // Getters y setters
    public Long getIdProducto() {
        return idProducto;
    }
    
    public void setIdProducto(Long idProducto) {
        this.idProducto = idProducto;
    }
    
    public String getCodigo() {
        return codigo;
    }
    
    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
    
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
    
    public double getPrecio() {
        return precio;
    }
    
    public void setPrecio(double precio) {
        this.precio = precio;
    }
    
    public int getStock() {
        return stock;
    }
    
    public void setStock(int stock) {
        this.stock = stock;
    }
    
    public String getNumeroSerie() {
        return numeroSerie;
    }
    
    public void setNumeroSerie(String numeroSerie) {
        this.numeroSerie = numeroSerie;
    }
    
    public String getCategoria() {
        return categoria;
    }
    
    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }
    
    public String getProveedor() {
        return proveedor;
    }
    
    public void setProveedor(String proveedor) {
        this.proveedor = proveedor;
    }
    
    public boolean isActivo() {
        return activo;
    }
    
    public void setActivo(boolean activo) {
        this.activo = activo;
    }
    
    @Override
    public String toString() {
        return codigo + " - " + descripcion;
    }
}