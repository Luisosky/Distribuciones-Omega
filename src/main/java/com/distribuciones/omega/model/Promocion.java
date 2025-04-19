package com.distribuciones.omega.model;

import java.time.LocalDate;

/**
 * Modelo simplificado para promociones
 */
public class Promocion {
    private int id;
    private String descripcion;
    private double valor;
    private boolean porcentaje; // true si es un porcentaje, false si es un valor fijo
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private boolean activa;
    private String categoriasAplicables; // Categorías a las que aplica la promoción, separadas por comas
    
    // Constructor
    public Promocion() {
    }
    
    // Getters y Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
    
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
    
    public double getValor() {
        return valor;
    }
    
    public void setValor(double valor) {
        this.valor = valor;
    }
    
    public boolean isPorcentaje() {
        return porcentaje;
    }
    
    public void setPorcentaje(boolean porcentaje) {
        this.porcentaje = porcentaje;
    }
    
    public LocalDate getFechaInicio() {
        return fechaInicio;
    }
    
    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }
    
    public LocalDate getFechaFin() {
        return fechaFin;
    }
    
    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }
    
    public boolean isActiva() {
        return activa;
    }
    
    public void setActiva(boolean activa) {
        this.activa = activa;
    }
    
    public String getCategoriasAplicables() {
        return categoriasAplicables;
    }
    
    public void setCategoriasAplicables(String categoriasAplicables) {
        this.categoriasAplicables = categoriasAplicables;
    }
}