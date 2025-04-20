package com.distribuciones.omega.model;

/**
 * Modelo para representar un producto en el inventario
 */
public class ProductoInventario {
    private Long idProducto;
    private String codigo;
    private String descripcion;
    private double precio;
    private int cantidad; // Cambiado de "stock" a "cantidad" para coincidir con la BD
    private String numeroSerie;
    private String categoria;
    private String proveedor;
    private boolean activo = true;
    private String ubicacion = "Almacén General";
    private int cantidadMinima = 5; // Cambiado de "stockMinimo" a "cantidadMinima"
    private int cantidadMaxima = 100; // Cambiado de "stockMaximo" a "cantidadMaxima"
    
    // Constructor vacío
    public ProductoInventario() {
    }
    
    // Constructor con campos principales
    public ProductoInventario(String codigo, String descripcion, double precio, int cantidad) {
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.precio = precio;
        this.cantidad = cantidad;
    }

    // Getters y setters
    public Long getIdProducto() { return idProducto; }
    public void setIdProducto(Long idProducto) { this.idProducto = idProducto; }
    
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    
    public double getPrecio() { return precio; }
    public void setPrecio(double precio) { this.precio = precio; }
    
    // Mantenemos los métodos getStock/setStock por compatibilidad pero usan la variable cantidad
    public int getStock() { return cantidad; }
    public void setStock(int stock) { this.cantidad = stock; }
    
    // Nuevos métodos para trabajar directamente con cantidad
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    
    public String getNumeroSerie() { return numeroSerie; }
    public void setNumeroSerie(String numeroSerie) { this.numeroSerie = numeroSerie; }
    
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    
    public String getProveedor() { return proveedor; }
    public void setProveedor(String proveedor) { this.proveedor = proveedor; }
    
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    
    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }
    
    // Mantenemos los métodos para stockMinimo/Maximo pero usan las nuevas variables
    public int getStockMinimo() { return cantidadMinima; }
    public void setStockMinimo(int stockMinimo) { this.cantidadMinima = stockMinimo; }
    
    public int getStockMaximo() { return cantidadMaxima; }
    public void setStockMaximo(int stockMaximo) { this.cantidadMaxima = stockMaximo; }
    
    // Nuevos métodos para trabajar directamente con cantidadMinima/Maxima
    public int getCantidadMinima() { return cantidadMinima; }
    public void setCantidadMinima(int cantidadMinima) { this.cantidadMinima = cantidadMinima; }
    
    public int getCantidadMaxima() { return cantidadMaxima; }
    public void setCantidadMaxima(int cantidadMaxima) { this.cantidadMaxima = cantidadMaxima; }

    @Override
    public String toString() {
        return "ProductoInventario{" +
                "codigo='" + codigo + '\'' +
                ", descripcion='" + descripcion + '\'' +
                ", cantidad=" + cantidad +
                ", precio=" + precio +
                '}';
    }
}