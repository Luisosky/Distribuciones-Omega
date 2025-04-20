package com.distribuciones.omega.model;

import java.time.LocalDateTime;

/**
 * Modelo que representa un pago realizado
 */
public class Pago {
    private Long id;
    private Factura factura;
    private Double monto;
    private MetodoPago metodoPago;
    private String referencia;
    private LocalDateTime fechaPago;
    private boolean aprobado;
    private String observaciones;
    
    // Constructor vacío
    public Pago() {
        this.fechaPago = LocalDateTime.now();
    }
    
    // Constructor con campos principales
    public Pago(Factura factura, Double monto, MetodoPago metodoPago) {
        this.factura = factura;
        this.monto = monto;
        this.metodoPago = metodoPago;
        this.fechaPago = LocalDateTime.now();
    }
    
    // Getters y Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Factura getFactura() {
        return factura;
    }
    
    public void setFactura(Factura factura) {
        this.factura = factura;
    }
    
    public Double getMonto() {
        return monto;
    }
    
    public void setMonto(Double monto) {
        this.monto = monto;
    }
    
    public MetodoPago getMetodoPago() {
        return metodoPago;
    }
    
    public void setMetodoPago(MetodoPago metodoPago) {
        this.metodoPago = metodoPago;
    }
    
    public String getReferencia() {
        return referencia;
    }
    
    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }
    
    public LocalDateTime getFechaPago() {
        return fechaPago;
    }
    
    public void setFechaPago(LocalDateTime fechaPago) {
        this.fechaPago = fechaPago;
    }
    
    public boolean isAprobado() {
        return aprobado;
    }
    
    public void setAprobado(boolean aprobado) {
        this.aprobado = aprobado;
    }
    
    public String getObservaciones() {
        return observaciones;
    }
    
    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
    
    @Override
    public String toString() {
        return "Pago{" +
                "id=" + id +
                ", monto=" + monto +
                ", metodoPago=" + metodoPago +
                ", aprobado=" + aprobado +
                '}';
    }
}