package com.distribuciones.omega.service;

import com.distribuciones.omega.model.Factura;
import com.distribuciones.omega.model.MetodoPago;
import com.distribuciones.omega.model.Pago;
import com.distribuciones.omega.repository.FacturaRepository;
import com.distribuciones.omega.repository.PagoRepository;
import com.distribuciones.omega.utils.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio para gestionar las operaciones relacionadas con pagos
 */
public class PagoService {
    
    private final PagoRepository pagoRepository;
    private final FacturaRepository facturaRepository;
    private final ContabilidadService contabilidadService;
    
    public PagoService() {
        facturaRepository = new FacturaRepository();
        pagoRepository = new PagoRepository();
        
        // Primero crear tabla facturas
        facturaRepository.createTableIfNotExists();
        // Luego crear tabla pagos
        pagoRepository.createTableIfNotExists();
        
        this.contabilidadService = new ContabilidadService();
    }
    
    /**
     * Procesa un pago en efectivo
     * @param factura Factura a pagar
     * @param monto Monto entregado
     * @param observaciones Observaciones del pago
     * @return Map con resultado del pago (éxito, cambio, mensaje)
     */
    public Map<String, Object> procesarPagoEfectivo(Factura factura, double monto, String observaciones) {
        Map<String, Object> resultado = new HashMap<>();
        
        if (monto < factura.getTotal()) {
            resultado.put("exito", false);
            resultado.put("mensaje", "El monto entregado es insuficiente para cubrir el total de la factura");
            return resultado;
        }
        
        // Calcular cambio
        double cambio = monto - factura.getTotal();
        
        Pago pago = new Pago();
        pago.setFactura(factura);
        pago.setMonto(factura.getTotal()); // El monto registrado es el de la factura, no lo entregado
        pago.setMetodoPago(MetodoPago.EFECTIVO);
        pago.setObservaciones(observaciones);
        pago.setAprobado(true); // Pago en efectivo se aprueba automáticamente
        
        Pago pagoProcesado = pagoRepository.save(pago);
        
        // Actualizar factura como pagada
        factura.setPagada(true);
        facturaRepository.actualizarEstadoPago(factura.getId(), true);
        
        // Registrar en contabilidad
        contabilidadService.registrarIngreso(factura.getTotal(), "Pago de factura: " + factura.getNumeroFactura(), "EFECTIVO");
        
        resultado.put("exito", true);
        resultado.put("pago", pagoProcesado);
        resultado.put("cambio", cambio);
        resultado.put("mensaje", "Pago procesado correctamente. Cambio: $" + String.format("%.2f", cambio));
        
        return resultado;
    }
    
    /**
     * Procesa un pago con tarjeta (crédito o débito)
     * @param factura Factura a pagar
     * @param numeroTarjeta Número de tarjeta (últimos 4 dígitos)
     * @param tipoTarjeta Tipo (CREDITO o DEBITO)
     * @param autorizacion Código de autorización
     * @param observaciones Observaciones del pago
     * @return Map con resultado del pago (éxito, mensaje)
     */
    public Map<String, Object> procesarPagoTarjeta(
            Factura factura, 
            String numeroTarjeta, 
            MetodoPago tipoTarjeta, 
            String autorizacion,
            String observaciones) {
        
        Map<String, Object> resultado = new HashMap<>();
        
        if (tipoTarjeta != MetodoPago.TARJETA_CREDITO && tipoTarjeta != MetodoPago.TARJETA_DEBITO) {
            resultado.put("exito", false);
            resultado.put("mensaje", "Tipo de tarjeta inválido");
            return resultado;
        }
        
        // Simulamos la validación del pago
        boolean pagoAprobado = validarPagoTarjeta(numeroTarjeta, autorizacion, factura.getTotal());
        
        Pago pago = new Pago();
        pago.setFactura(factura);
        pago.setMonto(factura.getTotal());
        pago.setMetodoPago(tipoTarjeta);
        pago.setReferencia("Tarjeta: **** " + numeroTarjeta + " / Auth: " + autorizacion);
        pago.setObservaciones(observaciones);
        pago.setAprobado(pagoAprobado);
        
        Pago pagoProcesado = pagoRepository.save(pago);
        
        if (pagoAprobado) {
            // Actualizar factura como pagada
            factura.setPagada(true);
            facturaRepository.actualizarEstadoPago(factura.getId(), true);
            
            // Registrar en contabilidad
            contabilidadService.registrarIngreso(
                    factura.getTotal(), 
                    "Pago de factura: " + factura.getNumeroFactura(), 
                    tipoTarjeta.name());
            
            resultado.put("exito", true);
            resultado.put("pago", pagoProcesado);
            resultado.put("mensaje", "Pago con tarjeta procesado correctamente. Autorización: " + autorizacion);
        } else {
            resultado.put("exito", false);
            resultado.put("mensaje", "El pago con tarjeta fue rechazado. Por favor, intente con otro medio de pago.");
        }
        
        return resultado;
    }
    
    /**
     * Procesa un pago por transferencia bancaria
     * @param factura Factura a pagar
     * @param numeroReferencia Número de referencia de la transferencia
     * @param banco Nombre del banco origen
     * @param observaciones Observaciones del pago
     * @return Map con resultado del pago (éxito, mensaje)
     */
    public Map<String, Object> procesarPagoTransferencia(
            Factura factura, 
            String numeroReferencia, 
            String banco,
            String observaciones) {
        
        Map<String, Object> resultado = new HashMap<>();
        
        if (numeroReferencia == null || numeroReferencia.trim().isEmpty()) {
            resultado.put("exito", false);
            resultado.put("mensaje", "El número de referencia es requerido");
            return resultado;
        }
        
        // Los pagos por transferencia pueden requerir verificación manual
        boolean verificacionPendiente = true;
        
        Pago pago = new Pago();
        pago.setFactura(factura);
        pago.setMonto(factura.getTotal());
        pago.setMetodoPago(MetodoPago.TRANSFERENCIA);
        pago.setReferencia("Ref: " + numeroReferencia + " / Banco: " + banco);
        pago.setObservaciones(observaciones);
        pago.setAprobado(!verificacionPendiente); // Inicialmente no aprobado hasta verificación
        
        Pago pagoProcesado = pagoRepository.save(pago);
        
        if (verificacionPendiente) {
            resultado.put("exito", true);
            resultado.put("pendienteVerificacion", true);
            resultado.put("pago", pagoProcesado);
            resultado.put("mensaje", "Pago registrado pero pendiente de verificación. Se notificará cuando se confirme.");
        } else {
            // Si no requiere verificación (podría ser el caso para algunos clientes de confianza)
            factura.setPagada(true);
            facturaRepository.actualizarEstadoPago(factura.getId(), true);
            
            contabilidadService.registrarIngreso(
                    factura.getTotal(), 
                    "Pago de factura: " + factura.getNumeroFactura(), 
                    "TRANSFERENCIA");
            
            resultado.put("exito", true);
            resultado.put("pendienteVerificacion", false);
            resultado.put("pago", pagoProcesado);
            resultado.put("mensaje", "Pago por transferencia procesado correctamente.");
        }
        
        return resultado;
    }
    
    /**
     * Aprueba manualmente un pago que estaba pendiente de verificación
     * @param pagoId ID del pago
     * @param observaciones Observaciones adicionales
     * @return true si la aprobación fue exitosa
     */
    public boolean aprobarPago(Long pagoId, String observaciones) {
        Pago pago = pagoRepository.findById(pagoId);
        if (pago == null) {
            return false;
        }
        
        // Actualizar el estado del pago
        pago.setAprobado(true);
        pago.setObservaciones((pago.getObservaciones() != null ? pago.getObservaciones() + "\n" : "") + 
                             "APROBADO: " + observaciones);
        
        boolean actualizado = pagoRepository.updateEstado(pago);
        
        if (actualizado) {
            // Actualizar la factura como pagada
            facturaRepository.actualizarEstadoPago(pago.getFactura().getId(), true);
            
            // Registrar en contabilidad
            contabilidadService.registrarIngreso(
                    pago.getMonto(), 
                    "Pago verificado de factura: " + pago.getFactura().getNumeroFactura(), 
                    pago.getMetodoPago().name());
        }
        
        return actualizado;
    }
    
    /**
     * Rechaza un pago que estaba pendiente de verificación
     * @param pagoId ID del pago
     * @param motivo Motivo del rechazo
     * @return true si el rechazo fue exitoso
     */
    public boolean rechazarPago(Long pagoId, String motivo) {
        Pago pago = pagoRepository.findById(pagoId);
        if (pago == null) {
            return false;
        }
        
        // Actualizar el estado del pago
        pago.setAprobado(false);
        pago.setObservaciones((pago.getObservaciones() != null ? pago.getObservaciones() + "\n" : "") + 
                             "RECHAZADO: " + motivo);
        
        return pagoRepository.updateEstado(pago);
    }
    
    /**
     * Obtiene los pagos asociados a una factura
     * @param facturaId ID de la factura
     * @return Lista de pagos
     */
    public List<Pago> obtenerPagosPorFactura(Long facturaId) {
        return pagoRepository.findByFacturaId(facturaId);
    }
    
    /**
     * Verifica si una factura está totalmente pagada
     * @param facturaId ID de la factura
     * @return true si la factura está pagada
     */
    public boolean verificarFacturaPagada(Long facturaId) {
        List<Pago> pagos = pagoRepository.findByFacturaId(facturaId);
        
        if (pagos.isEmpty()) {
            return false;
        }
        
        // Verificar si existe al menos un pago aprobado
        return pagos.stream().anyMatch(Pago::isAprobado);
    }
    
    /**
     * Simula la validación de un pago con tarjeta con un procesador externo
     */
    private boolean validarPagoTarjeta(String numeroTarjeta, String autorizacion, double monto) {
        // Simulación simple: 
        // - Si el monto es mayor a 10000, rechazar el pago (para pruebas)
        // - Si el número de autorización es vacío, rechazar el pago
        // - De lo contrario, aprobar
        
        if (monto > 10000) {
            return false;
        }
        
        if (autorizacion == null || autorizacion.trim().isEmpty()) {
            return false;
        }
        
        // En un caso real, aquí se llamaría a un servicio externo
        return true;
    }

        /**
     * Analiza los métodos de pago utilizados en un rango de fechas
     * @param fechaInicio Fecha de inicio
     * @param fechaFin Fecha de fin
     * @return Mapa con la distribución de métodos de pago (método -> porcentaje)
     */
    public Map<String, Double> analizarMetodosPago(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        Map<String, Double> distribucion = new HashMap<>();
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "SELECT metodo_pago, COUNT(*) as cantidad FROM pagos " +
                 "WHERE fecha_pago BETWEEN ? AND ? AND aprobado = true " +
                 "GROUP BY metodo_pago")) {
            
            pstmt.setTimestamp(1, Timestamp.valueOf(fechaInicio));
            pstmt.setTimestamp(2, Timestamp.valueOf(fechaFin));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                // Primero contamos el total de pagos
                int totalPagos = 0;
                Map<String, Integer> conteo = new HashMap<>();
                
                while (rs.next()) {
                    String metodoPago = rs.getString("metodo_pago");
                    int cantidad = rs.getInt("cantidad");
                    conteo.put(metodoPago, cantidad);
                    totalPagos += cantidad;
                }
                
                // Calculamos los porcentajes
                if (totalPagos > 0) {
                    for (Map.Entry<String, Integer> entry : conteo.entrySet()) {
                        double porcentaje = (entry.getValue() * 100.0) / totalPagos;
                        distribucion.put(entry.getKey(), Math.round(porcentaje * 100.0) / 100.0); // Redondear a 2 decimales
                    }
                }
            }
            
            // Si no hay datos, establecemos valores por defecto
            if (distribucion.isEmpty()) {
                for (MetodoPago metodo : MetodoPago.values()) {
                    distribucion.put(metodo.name(), 0.0);
                }
            }
            
            return distribucion;
            
        } catch (SQLException e) {
            throw new RuntimeException("Error al analizar métodos de pago: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene la distribución de métodos de pago para un conjunto de facturas
     * @param facturaIds Lista de IDs de facturas a analizar
     * @return Mapa con la distribución de métodos de pago (método -> porcentaje)
     */
    public Map<String, Double> analizarMetodosPagoPorFacturas(List<Long> facturaIds) {
        if (facturaIds == null || facturaIds.isEmpty()) {
            return new HashMap<>();
        }
        
        Map<String, Double> distribucion = new HashMap<>();
        
        try (Connection conn = DBUtil.getConnection()) {
            // Construir la consulta dinámica con los IDs de facturas
            StringBuilder queryBuilder = new StringBuilder(
                "SELECT metodo_pago, COUNT(*) as cantidad FROM pagos " +
                "WHERE factura_id IN (");
            
            for (int i = 0; i < facturaIds.size(); i++) {
                if (i > 0) {
                    queryBuilder.append(",");
                }
                queryBuilder.append("?");
            }
            
            queryBuilder.append(") AND aprobado = true GROUP BY metodo_pago");
            
            try (PreparedStatement pstmt = conn.prepareStatement(queryBuilder.toString())) {
                // Establecer los parámetros
                for (int i = 0; i < facturaIds.size(); i++) {
                    pstmt.setLong(i + 1, facturaIds.get(i));
                }
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    // Primero contamos el total de pagos
                    int totalPagos = 0;
                    Map<String, Integer> conteo = new HashMap<>();
                    
                    while (rs.next()) {
                        String metodoPago = rs.getString("metodo_pago");
                        int cantidad = rs.getInt("cantidad");
                        conteo.put(metodoPago, cantidad);
                        totalPagos += cantidad;
                    }
                    
                    // Calculamos los porcentajes
                    if (totalPagos > 0) {
                        for (Map.Entry<String, Integer> entry : conteo.entrySet()) {
                            double porcentaje = (entry.getValue() * 100.0) / totalPagos;
                            distribucion.put(entry.getKey(), Math.round(porcentaje * 100.0) / 100.0);
                        }
                    }
                }
            }
            
            // Si no hay datos, establecemos valores por defecto
            if (distribucion.isEmpty()) {
                for (MetodoPago metodo : MetodoPago.values()) {
                    distribucion.put(metodo.name(), 0.0);
                }
            }
            
            return distribucion;
            
        } catch (SQLException e) {
            throw new RuntimeException("Error al analizar métodos de pago por facturas: " + e.getMessage(), e);
        }
    }
}