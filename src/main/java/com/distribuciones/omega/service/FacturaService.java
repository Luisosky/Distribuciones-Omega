package com.distribuciones.omega.service;

import com.distribuciones.omega.model.*;
import com.distribuciones.omega.repository.FacturaRepository;
import com.distribuciones.omega.utils.NumeroFacturaGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio para gestionar las operaciones relacionadas con facturas
 */
public class FacturaService {
    
    private final FacturaRepository facturaRepository;
    private final CotizacionService cotizacionService;
    
    public FacturaService() {
        this.facturaRepository = new FacturaRepository();
        this.cotizacionService = new CotizacionService();
    }
    
    /**
     * Genera una factura a partir de una orden
     * @param orden Orden a partir de la cual se generará la factura
     * @return Factura generada o null si hubo un error
     */
    public Factura generarFacturaDesdeOrden(Orden orden) {
        if (orden == null) {
            return null;
        }
        
        // Verificar que la orden no esté ya facturada
        if (orden.isFacturada()) {
            return null;
        }
        
        // Crear nueva factura
        Factura factura = new Factura();
        factura.setNumeroFactura(NumeroFacturaGenerator.generarNumeroFactura());
        factura.setCliente(orden.getCliente());
        factura.setVendedor(orden.getVendedor());
        factura.setFecha(LocalDateTime.now());
        factura.setOrdenId(orden.getId());
        factura.setSubtotal(orden.getSubtotal());
        factura.setDescuento(orden.getDescuento());
        factura.setIva(orden.getIva());
        factura.setTotal(orden.getTotal());
        
        // Copiar items desde la orden
        List<ItemFactura> itemsFactura = new ArrayList<>();
        for (ItemOrden itemOrden : orden.getItems()) {
            ItemFactura itemFactura = new ItemFactura();
            itemFactura.setProducto(itemOrden.getProducto());
            itemFactura.setCantidad(itemOrden.getCantidad());
            itemFactura.setPrecioUnitario(itemOrden.getPrecioUnitario());
            itemFactura.setSubtotal(itemOrden.getSubtotal());
            itemsFactura.add(itemFactura);
        }
        factura.setItems(itemsFactura);
        
        // Guardar factura
        Factura facturaGuardada = facturaRepository.save(factura);
        
        // Marcar la orden como facturada
        if (facturaGuardada != null) {
            cotizacionService.marcarOrdenComoFacturada(orden.getId(), facturaGuardada.getId());
        }
        
        return facturaGuardada;
    }
    
    /**
     * Genera una factura a partir de una cotización
     * @param cotizacion La cotización de origen
     * @return La factura generada
     * @throws Exception Si ocurre algún error durante el proceso
     */
    public Factura generarFacturaDesdeContizacion(Cotizacion cotizacion) throws Exception {
        if (cotizacion == null) {
            throw new IllegalArgumentException("La cotización no puede ser nula");
        }
        
        if (cotizacion.getItems() == null || cotizacion.getItems().isEmpty()) {
            throw new IllegalArgumentException("La cotización no tiene items");
        }
        
        try {
            // 1. Crear la factura con datos de la cotización
            Factura factura = new Factura();
            
            // Asignar cliente, vendedor, fecha, totales, etc.
            factura.setCliente(cotizacion.getCliente());
            factura.setVendedor(cotizacion.getVendedor());
            factura.setFecha(LocalDateTime.now());
            factura.setOrdenId(cotizacion.getId()); // Referencia a la cotización
            factura.setSubtotal(cotizacion.getSubtotal());
            factura.setDescuento(cotizacion.getDescuento());
            factura.setIva(cotizacion.getIva());
            factura.setTotal(cotizacion.getTotal());
            factura.setAnulada(false);
            factura.setFormaPago("EFECTIVO"); // Valor predeterminado
            factura.setPagada(false);
            
            // 2. Generar número de factura
            String numeroFactura = generarNumeroFactura();
            factura.setNumeroFactura(numeroFactura);
            
            // 3. Guardar la factura en la base de datos
            factura = facturaRepository.save(factura);
            
            if (factura == null || factura.getId() == 0) {
                throw new Exception("Error al guardar la factura en la base de datos");
            }
            
            // 4. Copiar items de la cotización a la factura
            boolean itemsGuardados = copiarItemsCotizacionAFactura(cotizacion, factura);
            
            if (!itemsGuardados) {
                throw new Exception("Error al guardar los items de la factura");
            }
            
            return factura;
        } catch (Exception e) {
            System.err.println("Error al generar factura desde cotización: " + e.getMessage());
            e.printStackTrace();
            throw new Exception("No se pudo generar la factura: " + e.getMessage(), e);
        }
    }

    /**
     * Genera un número de factura único
     * @return Número de factura generado
     */
    private String generarNumeroFactura() {
        // Formato: FACT-YYYYMMDD-XXXX donde XXXX es un número secuencial
        LocalDateTime now = LocalDateTime.now();
        String fecha = String.format("%04d%02d%02d", now.getYear(), now.getMonthValue(), now.getDayOfMonth());
        
        try {
            // Obtener el último número de factura del día
            String ultimoNumero = facturaRepository.obtenerUltimoNumeroFactura(fecha);
            
            int secuencia = 1;
            if (ultimoNumero != null && !ultimoNumero.isEmpty()) {
                try {
                    // Extraer el número secuencial del formato FACT-YYYYMMDD-XXXX
                    String[] partes = ultimoNumero.split("-");
                    if (partes.length == 3) {
                        secuencia = Integer.parseInt(partes[2]) + 1;
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Error al parsear número de secuencia: " + e.getMessage());
                    // Si hay error, usar una secuencia basada en el tiempo como respaldo
                    secuencia = (int)(System.currentTimeMillis() % 10000);
                }
            }
            
            return String.format("FACT-%s-%04d", fecha, secuencia);
        } catch (Exception e) {
            System.err.println("Error al generar número de factura: " + e.getMessage());
            // En caso de error, usar timestamp como secuencia
            return String.format("FACT-%s-%04d", fecha, (int)(System.currentTimeMillis() % 10000));
        }
    }

    /**
     * Actualiza el estado de pago de una factura
     * @param facturaId ID de la factura
     * @param pagada Indica si la factura está pagada (true) o no (false)
     * @return true si la actualización fue exitosa
     */
    public boolean actualizarEstadoPago(Long facturaId, boolean pagada) {
        try {
            // Imprimir estructura antes de realizar la actualización
            System.out.println("==== DIAGNÓSTICO DE LA TABLA FACTURAS ====");
            facturaRepository.imprimirEstructuraTablaFacturas();
            System.out.println("========================================");
            
            // 1. Obtener la factura actual
            Factura factura = obtenerFacturaPorId(facturaId);
            if (factura == null) {
                System.err.println("No se pudo actualizar el estado de pago: Factura no encontrada (ID: " + facturaId + ")");
                return false;
            }
            
            // 2. Actualizar el estado de pago - esto también establecerá la fecha de pago internamente
            factura.setPagada(pagada);
            
            // 3. Guardar los cambios en la base de datos
            boolean actualizado = facturaRepository.update(factura);
            
            if (actualizado) {
                System.out.println("Estado de pago actualizado correctamente para factura ID: " + facturaId + 
                                " - Pagada: " + pagada +
                                " - Fecha de pago: " + (factura.getFechaPago() != null ? factura.getFechaPago() : "N/A"));
            } else {
                System.err.println("Error al actualizar estado de pago en la base de datos para factura ID: " + facturaId);
            }
            
            return actualizado;
            
        } catch (Exception e) {
            System.err.println("Error al actualizar estado de pago para factura ID " + facturaId + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Copia los items de una cotización a una factura
     * @param cotizacion Cotización origen
     * @param factura Factura destino
     * @return true si los items se copiaron exitosamente
     */
    private boolean copiarItemsCotizacionAFactura(Cotizacion cotizacion, Factura factura) {
        try {
            for (ItemCotizacion itemCotizacion : cotizacion.getItems()) {
                ItemFactura itemFactura = new ItemFactura();
                
                // Asignar factura al item
                // Usar método agregador en lugar de setter si está disponible
                factura.agregarItem(itemFactura);
                
                // O si prefieres usar el setter, asegúrate de que esté definido:
                // itemFactura.setFactura(factura);
                
                itemFactura.setProducto(itemCotizacion.getProducto());
                itemFactura.setCantidad(itemCotizacion.getCantidad());
                itemFactura.setPrecioUnitario(itemCotizacion.getPrecioUnitario());
                itemFactura.setSubtotal(itemCotizacion.getSubtotal());
                
                // Si prefieres hacer el guardado directo en lugar de usar factura.agregarItem:
                // boolean guardado = facturaRepository.guardarItemFactura(itemFactura);
                // if (!guardado) {
                //     return false;
                // }
            }
            return true;
        } catch (Exception e) {
            System.err.println("Error al copiar items de cotización a factura: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    
    /**
     * Busca una factura por su ID
     * @param id ID de la factura
     * @return Factura encontrada o null si no existe
     */
    public Factura obtenerFacturaPorId(Long id) {
        return facturaRepository.findById(id);
    }
    
    /**
     * Busca una factura por su número
     * @param numeroFactura Número de factura
     * @return Factura encontrada o null si no existe
     */
    public Factura obtenerFacturaPorNumero(String numeroFactura) {
        return facturaRepository.findByNumero(numeroFactura);
    }
    
    /**
     * Obtiene todas las facturas de un cliente
     * @param clienteId ID del cliente
     * @return Lista de facturas del cliente
     */
    public List<Factura> obtenerFacturasPorCliente(Long clienteId) {
        return facturaRepository.findByClienteId(clienteId);
    }
    
    /**
     * Obtiene todas las facturas emitidas por un vendedor
     * @param vendedorId ID del vendedor
     * @return Lista de facturas del vendedor
     */
    public List<Factura> obtenerFacturasPorVendedor(Long vendedorId) {
        return facturaRepository.findByVendedorId(vendedorId);
    }
    
    /**
     * Obtiene las facturas emitidas en un rango de fechas
     * @param fechaInicio Fecha de inicio
     * @param fechaFin Fecha de fin
     * @return Lista de facturas en el rango
     */
    public List<Factura> obtenerFacturasPorRangoFechas(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        return facturaRepository.findByFechaBetween(fechaInicio, fechaFin);
    }
    
    /**
     * Obtiene las facturas filtradas por vendedor y rango de fechas
     * @param vendedorId ID del vendedor
     * @param fechaInicio Fecha inicio del rango
     * @param fechaFin Fecha fin del rango
     * @return Lista de facturas que cumplen con los criterios
     */
    public List<Factura> obtenerFacturasPorVendedorYRango(Long vendedorId, LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        return facturaRepository.buscarFacturasPorVendedorYRango(vendedorId, fechaInicio, fechaFin);
    }
    
    /**
     * Obtiene las facturas filtradas por rango de fechas
     * @param fechaInicio Fecha inicio del rango
     * @param fechaFin Fecha fin del rango
     * @return Lista de facturas que cumplen con los criterios
     */
    public List<Factura> obtenerFacturasPorRango(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        return facturaRepository.buscarFacturasPorRango(fechaInicio, fechaFin);
    }
    
    /**
     * Anula una factura (en casos de devolución)
     * @param facturaId ID de la factura a anular
     * @param motivo Motivo de la anulación
     * @return true si la anulación fue exitosa
     */
    public boolean anularFactura(Long facturaId, String motivo) {
        Factura factura = obtenerFacturaPorId(facturaId);
        if (factura == null) {
            return false;
        }
        
        factura.setAnulada(true);
        factura.setMotivoAnulacion(motivo);
        factura.setFechaAnulacion(LocalDateTime.now());
        
        return facturaRepository.update(factura);
    }
}