package com.distribuciones.omega.model;

public class ProductoMobilario extends Producto {
    private String tipoMobilario;
    private String material;
    private String color;
    private String dimensiones;

    public ProductoMobilario(String nombre, String id, double precio, int cantidad, String tipoMobilario, String material, String color, String dimensiones) {
        super(nombre, id, precio, cantidad, Categoria.PRODUCTO_MOBILIARIO);
        this.tipoMobilario = tipoMobilario;
        this.material = material;
        this.color = color;
        this.dimensiones = dimensiones;
    }

    public String getTipoMobilario() {
        return tipoMobilario;
    }

    public void setTipoMobilario(String tipoMobilario) {
        this.tipoMobilario = tipoMobilario;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getDimensiones() {
        return dimensiones;
    }

    public void setDimensiones(String dimensiones) {
        this.dimensiones = dimensiones;
    }

    @Override
    public String toString() {
        return "ProductoMobilario [tipoMobilario=" + tipoMobilario + ", material=" + material + ", color=" + color
                + ", dimensiones=" + dimensiones + ", nombre=" + getNombre() + ", id=" + getId() + ", precio="
                + getPrecio() + ", cantidad=" + getCantidad() + "]";
    }

    
    
}
