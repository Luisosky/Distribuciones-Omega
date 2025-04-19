package com.distribuciones.omega.model;

import java.time.LocalDateTime;

/**
 * Modelo que representa un movimiento contable en el sistema
 */
public class MovimientoContable {
    private Long id;
    private LocalDateTime fecha;
    private String tipoDocumento;     // FACTURA, COTIZACION, MOVIMIENTO_INVENTARIO, CIERRE_PERIODO, etc.
    private String numeroDocumento;   // Número de referencia del documento original
    private String descripcion;
    private double monto;
    private String tipoMovimiento;    // DEBITO o CREDITO
    private String usuario;           // Usuario que generó el movimiento
    private String entidadRelacionada; // Cliente, Proveedor, Inventario, etc.
    private String referencia;        // Información adicional de referencia
    private String detalle;           // Detalles completos del movimiento
    
    // Constructores
    public MovimientoContable() {
    }
    
    public MovimientoContable(LocalDateTime fecha, String tipoDocumento, String numeroDocumento, 
                             String descripcion, double monto, String tipoMovimiento) {
        this.fecha = fecha;
        this.tipoDocumento = tipoDocumento;
        this.numeroDocumento = numeroDocumento;
        this.descripcion = descripcion;
        this.monto = monto;
        this.tipoMovimiento = tipoMovimiento;
    }
    
    // Getters y setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public LocalDateTime getFecha() {
        return fecha;
    }
    
    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }
    
    public String getTipoDocumento() {
        return tipoDocumento;
    }
    
    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }
    
    public String getNumeroDocumento() {
        return numeroDocumento;
    }
    
    public void setNumeroDocumento(String numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
    
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
    
    public double getMonto() {
        return monto;
    }
    
    public void setMonto(double monto) {
        this.monto = monto;
    }
    
    public String getTipoMovimiento() {
        return tipoMovimiento;
    }
    
    public void setTipoMovimiento(String tipoMovimiento) {
        this.tipoMovimiento = tipoMovimiento;
    }
    
    public String getUsuario() {
        return usuario;
    }
    
    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }
    
    public String getEntidadRelacionada() {
        return entidadRelacionada;
    }
    
    public void setEntidadRelacionada(String entidadRelacionada) {
        this.entidadRelacionada = entidadRelacionada;
    }
    
    public String getReferencia() {
        return referencia;
    }
    
    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }
    
    public String getDetalle() {
        return detalle;
    }
    
    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }
    
    @Override
    public String toString() {
        return "MovimientoContable{" +
                "id=" + id +
                ", fecha=" + fecha +
                ", tipoDocumento='" + tipoDocumento + '\'' +
                ", numeroDocumento='" + numeroDocumento + '\'' +
                ", descripcion='" + descripcion + '\'' +
                ", monto=" + monto +
                ", tipoMovimiento='" + tipoMovimiento + '\'' +
                '}';
    }
}