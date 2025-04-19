package com.distribuciones.omega.service;

import com.distribuciones.omega.model.*;
import com.distribuciones.omega.repository.MovimientoContableRepository;
import com.distribuciones.omega.utils.DBUtil;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio para gestionar operaciones contables del sistema
 */
public class ContabilidadService {

    private final MovimientoContableRepository movimientoRepository;
    
    /**
     * Constructor que inicializa el repositorio
     */
    public ContabilidadService() {
        this.movimientoRepository = new MovimientoContableRepository();
    }
    
    /**
     * Registra un movimiento contable asociado a una cotización
     * 
     * @param cotizacion La cotización para la cual registrar el movimiento
     * @return El movimiento contable registrado
     * @throws Exception Si ocurre un error durante el registro
     */
    public MovimientoContable registrarMovimientoCotizacion(Cotizacion cotizacion) throws Exception {
        // Crear movimiento contable
        MovimientoContable movimiento = new MovimientoContable();
        
        // Datos básicos
        movimiento.setFecha(LocalDateTime.now());
        movimiento.setTipoDocumento("COTIZACION");
        movimiento.setNumeroDocumento(cotizacion.getNumeroCotizacion());
        movimiento.setDescripcion("Cotización para cliente: " + cotizacion.getCliente().getNombre());
        movimiento.setMonto(cotizacion.getTotal());
        
        // Datos específicos de cotización
        movimiento.setTipoMovimiento("CREDITO"); // Las cotizaciones no generan cargos reales
        movimiento.setUsuario(cotizacion.getVendedor().getNombre());
        movimiento.setEntidadRelacionada(cotizacion.getCliente().getNombre());
        movimiento.setReferencia("Cotización #" + cotizacion.getNumeroCotizacion());
        
        // Registrar detalle para cada item
        StringBuilder detalle = new StringBuilder();
        for (ItemCotizacion item : cotizacion.getItems()) {
            detalle.append(item.getProducto().getCodigo())
                   .append(" - ")
                   .append(item.getProducto().getDescripcion())
                   .append(" x ")
                   .append(item.getCantidad())
                   .append(" = ")
                   .append(item.getSubtotal())
                   .append("\n");
        }
        movimiento.setDetalle(detalle.toString());
        
        // Registrar en base de datos
        return movimientoRepository.save(movimiento);
    }
    
    /**
     * Registra un movimiento contable asociado a una factura
     * 
     * @param factura La factura para la cual registrar el movimiento
     * @return El movimiento contable registrado
     * @throws Exception Si ocurre un error durante el registro
     */
    public MovimientoContable registrarMovimientoFactura(Factura factura) throws Exception {
        // Crear movimiento contable
        MovimientoContable movimiento = new MovimientoContable();
        
        // Datos básicos
        movimiento.setFecha(LocalDateTime.now());
        movimiento.setTipoDocumento("FACTURA");
        movimiento.setNumeroDocumento(factura.getNumeroFactura());
        movimiento.setDescripcion("Factura para cliente: " + factura.getCliente().getNombre());
        movimiento.setMonto(factura.getTotal());
        
        // Datos específicos de factura
        movimiento.setTipoMovimiento("DEBITO"); // Las facturas generan ingresos
        movimiento.setUsuario(factura.getVendedor().getNombre());
        movimiento.setEntidadRelacionada(factura.getCliente().getNombre());
        movimiento.setReferencia("Factura #" + factura.getNumeroFactura());
        
        // Registrar detalle para cada item
        StringBuilder detalle = new StringBuilder();
        for (ItemFactura item : factura.getItems()) {
            detalle.append(item.getProducto().getCodigo())
                   .append(" - ")
                   .append(item.getProducto().getDescripcion())
                   .append(" x ")
                   .append(item.getCantidad())
                   .append(" = ")
                   .append(item.getSubtotal())
                   .append("\n");
        }
        movimiento.setDetalle(detalle.toString());
        
        // Efectuar también los movimientos de inventario
        registrarMovimientoInventario(factura);
        
        // Registrar en base de datos
        return movimientoRepository.save(movimiento);
    }
    
    /**
     * Registra los movimientos contables relacionados con el inventario para una factura
     * 
     * @param factura La factura que modificó el inventario
     * @throws Exception Si ocurre un error durante el registro
     */
    private void registrarMovimientoInventario(Factura factura) throws Exception {
        // Crear movimiento contable de inventario
        MovimientoContable movimiento = new MovimientoContable();
        
        // Datos básicos
        movimiento.setFecha(LocalDateTime.now());
        movimiento.setTipoDocumento("MOVIMIENTO_INVENTARIO");
        movimiento.setNumeroDocumento(factura.getNumeroFactura());
        movimiento.setDescripcion("Salida de inventario por factura: " + factura.getNumeroFactura());
        
        // Calcular costo total de productos vendidos
        double costoTotal = 0.0;
        StringBuilder detalle = new StringBuilder();
        
        for (ItemFactura item : factura.getItems()) {
            ProductoInventario producto = item.getProducto();
            double costoItem = producto.getPrecio() * 0.7 * item.getCantidad(); // Estimación de costo como 70% del precio
            costoTotal += costoItem;
            
            detalle.append("Salida: ")
                   .append(producto.getCodigo())
                   .append(" - ")
                   .append(producto.getDescripcion())
                   .append(" x ")
                   .append(item.getCantidad())
                   .append(" = ")
                   .append(costoItem)
                   .append("\n");
        }
        
        movimiento.setMonto(costoTotal);
        movimiento.setTipoMovimiento("CREDITO"); // Salida de inventario
        movimiento.setUsuario(factura.getVendedor().getNombre());
        movimiento.setEntidadRelacionada("INVENTARIO");
        movimiento.setReferencia("Salida inventario por Factura #" + factura.getNumeroFactura());
        movimiento.setDetalle(detalle.toString());
        
        // Registrar en base de datos
        movimientoRepository.save(movimiento);
    }
    
    /**
     * Consulta los movimientos contables según tipo y rango de fechas
     * 
     * @param tipoMovimiento El tipo de movimiento (DEBITO/CREDITO)
     * @param fechaInicio La fecha de inicio del rango
     * @param fechaFin La fecha de fin del rango
     * @return Lista de movimientos contables
     * @throws Exception Si ocurre un error durante la consulta
     */
    public List<MovimientoContable> consultarMovimientos(
            String tipoMovimiento, LocalDateTime fechaInicio, LocalDateTime fechaFin) throws Exception {
        return movimientoRepository.buscarPorTipoYFechas(tipoMovimiento, fechaInicio, fechaFin);
    }
    
    /**
     * Obtiene un resumen de la situación contable actual
     * 
     * @return Un objeto con el resumen contable
     * @throws Exception Si ocurre un error al generar el resumen
     */
    public ResumenContable generarResumenContable() throws Exception {
        ResumenContable resumen = new ResumenContable();
        
        // Obtener totales de movimientos
        double totalDebitos = movimientoRepository.calcularTotalPorTipo("DEBITO");
        double totalCreditos = movimientoRepository.calcularTotalPorTipo("CREDITO");
        
        resumen.setTotalIngresos(totalDebitos);
        resumen.setTotalEgresos(totalCreditos);
        resumen.setSaldoActual(totalDebitos - totalCreditos);
        resumen.setFechaGeneracion(LocalDateTime.now());
        
        // Obtener conteos por tipo de documento
        resumen.setTotalFacturas(movimientoRepository.contarPorTipoDocumento("FACTURA"));
        resumen.setTotalCotizaciones(movimientoRepository.contarPorTipoDocumento("COTIZACION"));
        
        return resumen;
    }
    
    /**
     * Genera un libro diario con todos los movimientos de una fecha específica
     * 
     * @param fecha La fecha para la cual generar el libro diario
     * @return Lista de movimientos de la fecha
     * @throws Exception Si ocurre un error durante la generación
     */
    public List<MovimientoContable> generarLibroDiario(LocalDateTime fecha) throws Exception {
        LocalDateTime inicioDia = fecha.toLocalDate().atStartOfDay();
        LocalDateTime finDia = inicioDia.plusDays(1).minusSeconds(1);
        
        return movimientoRepository.buscarPorFechas(inicioDia, finDia);
    }
    
    /**
     * Realiza el cierre contable de un período
     * 
     * @param fechaCierre La fecha en que se realiza el cierre
     * @param observaciones Observaciones adicionales sobre el cierre
     * @return El movimiento de cierre generado
     * @throws Exception Si ocurre un error durante el cierre
     */
    public MovimientoContable realizarCierrePeriodo(LocalDateTime fechaCierre, String observaciones) throws Exception {
        Connection conn = null;
        
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);
            
            // Obtener saldos
            double totalDebitos = movimientoRepository.calcularTotalPorTipo("DEBITO");
            double totalCreditos = movimientoRepository.calcularTotalPorTipo("CREDITO");
            double saldoFinal = totalDebitos - totalCreditos;
            
            // Crear movimiento de cierre
            MovimientoContable movimientoCierre = new MovimientoContable();
            movimientoCierre.setFecha(fechaCierre);
            movimientoCierre.setTipoDocumento("CIERRE_PERIODO");
            movimientoCierre.setNumeroDocumento("CIERRE-" + fechaCierre.getYear() + "-" + fechaCierre.getMonthValue());
            movimientoCierre.setDescripcion("Cierre contable de período: " + 
                    fechaCierre.getMonth() + "/" + fechaCierre.getYear());
            movimientoCierre.setMonto(saldoFinal);
            movimientoCierre.setTipoMovimiento(saldoFinal >= 0 ? "DEBITO" : "CREDITO");
            movimientoCierre.setUsuario("SISTEMA");
            movimientoCierre.setEntidadRelacionada("SISTEMA");
            movimientoCierre.setReferencia("Cierre automático");
            movimientoCierre.setDetalle("Saldo final del período: " + saldoFinal + "\n" +
                    "Total Débitos: " + totalDebitos + "\n" +
                    "Total Créditos: " + totalCreditos + "\n\n" +
                    "Observaciones: " + observaciones);
            
            // Guardar movimiento de cierre
            MovimientoContable cierreGuardado = movimientoRepository.save(movimientoCierre);
            
            // Confirmar transacción
            conn.commit();
            
            return cierreGuardado;
        } catch (Exception e) {
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }
    
    /**
     * Clase interna para manejar resúmenes contables
     */
    public static class ResumenContable {
        private double totalIngresos;
        private double totalEgresos;
        private double saldoActual;
        private LocalDateTime fechaGeneracion;
        private int totalFacturas;
        private int totalCotizaciones;
        
        // Getters y setters
        public double getTotalIngresos() {
            return totalIngresos;
        }
        
        public void setTotalIngresos(double totalIngresos) {
            this.totalIngresos = totalIngresos;
        }
        
        public double getTotalEgresos() {
            return totalEgresos;
        }
        
        public void setTotalEgresos(double totalEgresos) {
            this.totalEgresos = totalEgresos;
        }
        
        public double getSaldoActual() {
            return saldoActual;
        }
        
        public void setSaldoActual(double saldoActual) {
            this.saldoActual = saldoActual;
        }
        
        public LocalDateTime getFechaGeneracion() {
            return fechaGeneracion;
        }
        
        public void setFechaGeneracion(LocalDateTime fechaGeneracion) {
            this.fechaGeneracion = fechaGeneracion;
        }
        
        public int getTotalFacturas() {
            return totalFacturas;
        }
        
        public void setTotalFacturas(int totalFacturas) {
            this.totalFacturas = totalFacturas;
        }
        
        public int getTotalCotizaciones() {
            return totalCotizaciones;
        }
        
        public void setTotalCotizaciones(int totalCotizaciones) {
            this.totalCotizaciones = totalCotizaciones;
        }
    }
}