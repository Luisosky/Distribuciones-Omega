package com.distribuciones.omega.model;

public class InsumoOficina extends Producto {
    private String presentacion;
    private String tipoPapel;
    private int cantidadPorPaquete;

    public InsumoOficina(String nombre, String id, double precio, int cantidad, String presentacion, String tipoPapel, int cantidadPorPaquete) {
        super(nombre, id, precio, cantidad, Categoria.INSUMO_OFICINA);
        this.presentacion = presentacion;
        this.tipoPapel = tipoPapel;
        this.cantidadPorPaquete = cantidadPorPaquete;
    }

    public String getPresentacion() {
        return presentacion;
    }

    public void setPresentacion(String presentacion) {
        this.presentacion = presentacion;
    }

    public String getTipoPapel() {
        return tipoPapel;
    }

    public void setTipoPapel(String tipoPapel) {
        this.tipoPapel = tipoPapel;
    }

    public int getCantidadPorPaquete() {
        return cantidadPorPaquete;
    }

    public void setCantidadPorPaquete(int cantidadPorPaquete) {
        this.cantidadPorPaquete = cantidadPorPaquete;
    }

    @Override
    public String toString() {
        return "InsumoOficina [presentacion=" + presentacion + ", tipoPapel=" + tipoPapel + ", cantidadPorPaquete="
                + cantidadPorPaquete + ", nombre=" + getNombre() + ", id=" + getId() + ", precio=" + getPrecio()
                + ", cantidad=" + getCantidad() + "]";
    }
    
}
