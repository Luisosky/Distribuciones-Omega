package com.distribuciones.omega.model;

public class ProductoTecnologico extends Producto{

    private String marca;
    private String modelo;
    private String numeroSerie;
    private int garantiaMeses;
    private String especificacionesTecnicas;

    public ProductoTecnologico(String nombre, String id, double precio, int cantidad, String marca, String modelo, String numeroSerie, int garantiaMeses, String especificacionesTecnicas) {
        super(nombre, id, precio, cantidad, Categoria.PRODUCTO_TECNOLOGICO);
        this.marca = marca;
        this.modelo = modelo;
        this.numeroSerie = numeroSerie;
        this.garantiaMeses = garantiaMeses;
        this.especificacionesTecnicas = especificacionesTecnicas;
    }
    public String getMarca() {
        return marca;
    }
    
    public String getModelo() {
        return modelo;
    }
     
    public String getNumeroSerie() {
        return numeroSerie;
    }

    public int getGarantiaMeses() {
        return garantiaMeses;
    }

    public String getEspecificacionesTecnicas() {
        return especificacionesTecnicas;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public void setNumeroSerie(String numeroSerie) {
        this.numeroSerie = numeroSerie;
    }

    public void setGarantiaMeses(int garantiaMeses) {
        this.garantiaMeses = garantiaMeses;
    }

    public void setEspecificacionesTecnicas(String especificacionesTecnicas) {
        this.especificacionesTecnicas = especificacionesTecnicas;
    }

    @Override
    public String toString() {
        return "ProductoTecnologico [marca=" + marca + ", modelo=" + modelo + ", numeroSerie=" + numeroSerie
                + ", garantiaMeses=" + garantiaMeses + ", especificacionesTecnicas=" + especificacionesTecnicas
                + ", nombre=" + getNombre() + ", id=" + getId() + ", precio=" + getPrecio() + ", cantidad="
                + getCantidad() + "]";
    }
        
}
