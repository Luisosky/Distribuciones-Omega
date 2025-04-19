package com.distribuciones.omega.model;

import java.time.LocalDate;

public class Promocion {
    private Long id;
    private String descripcion;
    private String tipo;  // "PORCENTAJE", "2X1", "PRECIO_FIJO"
    private double valor; // Porcentaje de descuento o precio fijo
    private String codigoProducto; // Producto al que aplica la promoción
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private boolean activa = true;
    
    public Promocion() {
        // Constructor vacío necesario para frameworks
    }
    
    public Promocion(String descripcion, String tipo, double valor, String codigoProducto, 
                    LocalDate fechaInicio, LocalDate fechaFin) {
        this.descripcion = descripcion;
        this.tipo = tipo;
        this.valor = valor;
        this.codigoProducto = codigoProducto;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
    }
    
    public Promocion(Long id, String descripcion, String tipo, double valor, String codigoProducto, 
                    LocalDate fechaInicio, LocalDate fechaFin, boolean activa) {
        this.id = id;
        this.descripcion = descripcion;
        this.tipo = tipo;
        this.valor = valor;
        this.codigoProducto = codigoProducto;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.activa = activa;
    }
    
    // Getters y setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
    
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
    
    public String getTipo() {
        return tipo;
    }
    
    public void setTipo(String tipo) {
        this.tipo = tipo;
    }
    
    public double getValor() {
        return valor;
    }
    
    public void setValor(double valor) {
        this.valor = valor;
    }
    
    public String getCodigoProducto() {
        return codigoProducto;
    }
    
    public void setCodigoProducto(String codigoProducto) {
        this.codigoProducto = codigoProducto;
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
    
    /**
     * Verifica si la promoción está vigente en la fecha actual
     */
    public boolean estaVigente() {
        LocalDate hoy = LocalDate.now();
        return activa && 
               (hoy.isEqual(fechaInicio) || hoy.isAfter(fechaInicio)) && 
               (hoy.isEqual(fechaFin) || hoy.isBefore(fechaFin));
    }
    
    /**
     * Verifica si la promoción está vigente en una fecha específica
     */
    public boolean estaVigenteEn(LocalDate fecha) {
        return activa && 
               (fecha.isEqual(fechaInicio) || fecha.isAfter(fechaInicio)) && 
               (fecha.isEqual(fechaFin) || fecha.isBefore(fechaFin));
    }
    
    @Override
    public String toString() {
        return descripcion + " - " + tipo + " (" + valor + ")";
    }
}