package com.distribuciones.omega.model;

/**
 * Enum que representa los diferentes métodos de pago disponibles
 */
public enum MetodoPago {
    EFECTIVO("Efectivo"),
    TARJETA_CREDITO("Tarjeta de Crédito"),
    TARJETA_DEBITO("Tarjeta de Débito"),
    TRANSFERENCIA("Transferencia Bancaria"),
    CHEQUE("Cheque");
    
    private final String nombre;
    
    MetodoPago(String nombre) {
        this.nombre = nombre;
    }
    
    public String getNombre() {
        return nombre;
    }
    
    @Override
    public String toString() {
        return nombre;
    }
}