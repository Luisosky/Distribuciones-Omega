package com.distribuciones.omega.model;



public class Producto {

    private String nombre;
    private String id;
    private Double precio;
    private int cantidad;
    
    public Producto(String nombre, String id, Double precio, int cantidad) {
        this.nombre = nombre;
        this.id = id;
        this.precio = precio;
        this.cantidad = cantidad;
    }
    public String getNombre() {
        return nombre;
    }
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public Double getPrecio() {
        return precio;
    }
    public void setPrecio(Double precio) {
        this.precio = precio;
    }
    public int getCantidad() {
        return cantidad;
    }
    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    @Override
    public String toString() {
        return "Producto [nombre=" + nombre + ", id=" + id + ", precio=" + precio + ", cantidad=" + cantidad + "]";
    }
    

    public Double calcularPrecioTotal() {
        return precio * cantidad;
    }
    
}
