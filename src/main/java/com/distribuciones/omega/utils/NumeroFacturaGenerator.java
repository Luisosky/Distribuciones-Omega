package com.distribuciones.omega.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utilidad para generar números de factura únicos
 */
public class NumeroFacturaGenerator {
    
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final AtomicInteger secuencial = new AtomicInteger(1);
    
    /**
     * Genera un número de factura único en formato: 
     * FACT-YYYYMMDD-XXXX donde XXXX es un número secuencial
     * 
     * @return Número de factura generado
     */
    public static String generarNumeroFactura() {
        LocalDateTime now = LocalDateTime.now();
        String fecha = now.format(formatter);
        int numero = secuencial.getAndIncrement();
        
        return String.format("FACT-%s-%04d", fecha, numero);
    }
    
    /**
     * Reinicia el secuencial (para uso en testing)
     */
    public static void resetSecuencial() {
        secuencial.set(1);
    }
    
    /**
     * Establece un valor específico para el secuencial (para uso en testing)
     */
    public static void setSecuencial(int valor) {
        secuencial.set(valor);
    }
}