package com.distribuciones.omega.service;

import com.distribuciones.omega.model.*;
import com.distribuciones.omega.repository.FacturaRepository;
import com.distribuciones.omega.utils.NumeroFacturaGenerator;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
     * @param cotizacion Cotización origen
     * @return Factura generada
     */
    public Factura generarFacturaDesdeContizacion(Cotizacion cotizacion) {
        try {
            if (cotizacion == null) {
                throw new IllegalArgumentException("La cotización no puede ser nula");
            }
            
            // Crear la factura
            Factura factura = new Factura();
            factura.setCliente(cotizacion.getCliente());
            factura.setVendedor(cotizacion.getVendedor());
            factura.setFecha(LocalDateTime.now());
            factura.setSubtotal(cotizacion.getSubtotal());
            factura.setDescuento(cotizacion.getDescuento());
            factura.setIva(cotizacion.getIva());
            factura.setTotal(cotizacion.getTotal());
            factura.setPagada(false);
            
            // Generar número de factura
            String numeroFactura = "FACT-" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + 
                "-" + String.format("%04d", obtenerSiguienteNumeroFactura());
            factura.setNumeroFactura(numeroFactura);
            
            // Transferir items desde la cotización
            List<ItemFactura> itemsFactura = new ArrayList<>();
            
            // Esto es clave: Convertir los ítems de cotización a ítems de factura
            for (ItemCotizacion itemCotizacion : cotizacion.getItems()) {
                ItemFactura itemFactura = new ItemFactura();
                itemFactura.setProducto(itemCotizacion.getProducto());
                itemFactura.setCantidad(itemCotizacion.getCantidad());
                itemFactura.setPrecioUnitario(itemCotizacion.getPrecioUnitario());
                itemFactura.setDescuento(itemCotizacion.getDescuento());
                itemFactura.setSubtotal(itemCotizacion.getSubtotal());
                itemsFactura.add(itemFactura);
            }
            
            // Añadir los ítems a la factura
            factura.setItems(itemsFactura);
            
            // Guardar la factura
            FacturaRepository repo = new FacturaRepository();
            Factura facturaGuardada = repo.save(factura);
            
            // IMPORTANTE: Guardar los ítems en la base de datos
            System.out.println("Guardando ítems de factura: " + itemsFactura.size() + " ítems");
            for (ItemFactura item : itemsFactura) {
                System.out.println("Item: " + item.getProducto().getDescripcion() + 
                                " - Cantidad: " + item.getCantidad() + 
                                " - Precio: " + item.getPrecioUnitario());
            }
            
            // Guardar los ítems en la base de datos
            repo.guardarItemsFactura(facturaGuardada.getId(), itemsFactura);
            
            return facturaGuardada;
            
        } catch (Exception e) {
            System.err.println("Error al generar factura desde cotización: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

        /**
     * Genera una factura a partir de una cotización y lista de ítems preconvertidos
     * @param cotizacion Cotización origen
     * @param itemsFactura Ítems ya convertidos 
     * @return Factura generada
     */
    public Factura generarFacturaDesdeContizacion(Cotizacion cotizacion, List<ItemFactura> itemsFactura) {
        try {
            if (cotizacion == null) {
                throw new IllegalArgumentException("La cotización no puede ser nula");
            }
            
            // Crear la factura
            Factura factura = new Factura();
            factura.setCliente(cotizacion.getCliente());
            factura.setVendedor(cotizacion.getVendedor());
            factura.setFecha(LocalDateTime.now());
            factura.setSubtotal(cotizacion.getSubtotal());
            factura.setDescuento(cotizacion.getDescuento());
            factura.setIva(cotizacion.getIva());
            factura.setTotal(cotizacion.getTotal());
            factura.setPagada(false);
            
            // Generar número de factura
            String numeroFactura = "FACT-" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + 
                "-" + String.format("%04d", obtenerSiguienteNumeroFactura());
            factura.setNumeroFactura(numeroFactura);
            
            // Usar los ítems preconvertidos si se proporcionan, sino convertir desde la cotización
            if (itemsFactura == null || itemsFactura.isEmpty()) {
                itemsFactura = new ArrayList<>();
                
                // Convertir los ítems de cotización a ítems de factura
                for (ItemCotizacion itemCotizacion : cotizacion.getItems()) {
                    ItemFactura itemFactura = new ItemFactura();
                    itemFactura.setProducto(itemCotizacion.getProducto());
                    itemFactura.setCantidad(itemCotizacion.getCantidad());
                    itemFactura.setPrecioUnitario(itemCotizacion.getPrecioUnitario());
                    itemFactura.setDescuento(itemCotizacion.getDescuento());
                    itemFactura.setSubtotal(itemCotizacion.getSubtotal());
                    itemsFactura.add(itemFactura);
                }
            }
            
            // Añadir los ítems a la factura
            factura.setItems(itemsFactura);
            
            // Guardar la factura
            FacturaRepository repo = new FacturaRepository();
            Factura facturaGuardada = repo.save(factura);
            
            // IMPORTANTE: Guardar los ítems en la base de datos
            System.out.println("Guardando ítems de factura: " + itemsFactura.size() + " ítems");
            for (ItemFactura item : itemsFactura) {
                System.out.println("Item: " + item.getProducto().getDescripcion() + 
                                " - Cantidad: " + item.getCantidad() + 
                                " - Precio: " + item.getPrecioUnitario());
            }
            
            // Guardar los ítems en la base de datos
            repo.guardarItemsFactura(facturaGuardada.getId(), itemsFactura);
            
            return facturaGuardada;
            
        } catch (Exception e) {
            System.err.println("Error al generar factura desde cotización: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

        /**
     * Obtiene el siguiente número secuencial para facturas
     * @return Número secuencial
     */
    private int obtenerSiguienteNumeroFactura() {
        try {
            // Obtener la fecha actual en formato YYYYMMDD
            String fechaHoy = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            
            // Buscar la última factura del día
            String ultimoNumero = facturaRepository.obtenerUltimoNumeroFactura(fechaHoy);
            
            // Si hay una factura existente, incrementar el contador
            if (ultimoNumero != null && !ultimoNumero.isEmpty()) {
                try {
                    // Extraer el número secuencial del formato FACT-YYYYMMDD-XXXX
                    String[] partes = ultimoNumero.split("-");
                    if (partes.length == 3) {
                        return Integer.parseInt(partes[2]) + 1;
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Error al parsear número de secuencia: " + e.getMessage());
                }
            }
            
            // Si no hay facturas hoy o hubo error, empezar con 1
            return 1;
        } catch (Exception e) {
            System.err.println("Error al obtener siguiente número de factura: " + e.getMessage());
            // En caso de error, usar un número basado en el timestamp
            return (int)(System.currentTimeMillis() % 1000) + 1;
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
            // 1. Verificar que la factura existe
            Factura factura = facturaRepository.findById(facturaId);
            if (factura == null) {
                System.err.println("No se pudo actualizar el estado de pago: Factura no encontrada (ID: " + facturaId + ")");
                return false;
            }
            
            // 2. Actualizar el estado en BD
            boolean actualizado = facturaRepository.actualizarEstadoPago(facturaId, pagada);
            
            if (actualizado) {
                System.out.println("Estado de pago actualizado correctamente para factura ID: " + 
                                   facturaId + " - Pagada: " + pagada + " - Fecha de pago: " + 
                                   (pagada ? LocalDateTime.now() : "N/A"));
                return true;
            } else {
                System.err.println("Error técnico al actualizar el estado de pago para factura ID: " + facturaId);
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error al actualizar estado de pago: " + e.getMessage());
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

    /**
     * Diagnostica y repara los problemas con una factura específica
     * @param facturaId ID de la factura a diagnosticar y reparar
     * @return true si la reparación fue exitosa
     */
    public boolean diagnosticarYRepararFactura(Long facturaId) {
        try {
            FacturaRepository repo = new FacturaRepository();
            
            System.out.println("\n============= DIAGNÓSTICO Y REPARACIÓN DE FACTURA =============");
            System.out.println("Factura ID: " + facturaId);
            
            // 1. Diagnosticar tablas
            repo.diagnosticarTablas();
            
            // 2. Asegurar que la tabla items_factura existe
            repo.crearTablaItemsFactura();
            
            // 3. Verificar si la factura existe
            Factura factura = repo.findById(facturaId);
            if (factura == null) {
                System.out.println("ERROR: No se encontró la factura con ID " + facturaId);
                return false;
            }
            
            System.out.println("Factura encontrada: " + factura.getNumeroFactura());
            System.out.println("Cliente: " + factura.getCliente().getNombre());
            System.out.println("Fecha: " + factura.getFecha());
            System.out.println("Total: " + factura.getTotal());
            System.out.println("Items actuales: " + factura.getItems().size());
            
            // 4. Si no tiene ítems, crear datos de ejemplo
            if (factura.getItems() == null || factura.getItems().isEmpty()) {
                System.out.println("La factura no tiene ítems. Creando ítems de ejemplo...");
                
                boolean itemsCreados = repo.crearItemsEjemplo(facturaId);
                if (!itemsCreados) {
                    System.out.println("ERROR: No se pudieron crear ítems de ejemplo");
                    return false;
                }
                
                // 5. Recargar la factura para verificar los ítems creados
                factura = repo.findById(facturaId);
                if (factura == null) {
                    System.out.println("ERROR: No se pudo recargar la factura después de crear ítems");
                    return false;
                }
                
                System.out.println("Factura recargada después de crear ítems.");
                System.out.println("Items después de reparación: " + factura.getItems().size());
            }
            
            // 6. Mostrar resumen final
            System.out.println("\nRESUMEN FINAL:");
            System.out.println("Factura: " + factura.getNumeroFactura());
            System.out.println("Total de ítems: " + factura.getItems().size());
            
            if (factura.getItems().size() > 0) {
                System.out.println("\nDETALLE DE ÍTEMS:");
                for (int i = 0; i < factura.getItems().size(); i++) {
                    ItemFactura item = factura.getItems().get(i);
                    System.out.println("Ítem " + (i+1) + ": " + 
                                    "Producto=" + (item.getProducto() != null ? item.getProducto().getDescripcion() : "NULL") + 
                                    ", Código=" + (item.getProducto() != null ? item.getProducto().getCodigo() : "NULL") +
                                    ", Cantidad=" + item.getCantidad() + 
                                    ", Subtotal=" + item.getSubtotal());
                }
            }
            
            System.out.println("============================================================\n");
            return true;
            
        } catch (Exception e) {
            System.err.println("Error durante el diagnóstico y reparación: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Repara una factura utilizando ítems reales de la compra actual
     * @param facturaId ID de la factura a reparar
     * @param items Lista de ítems reales de la compra
     * @return true si la reparación fue exitosa
     */
    public boolean repararFacturaConItems(Long facturaId, List<ItemFactura> items) {
        try {
            if (facturaId == null || items == null || items.isEmpty()) {
                System.out.println("ERROR: Datos insuficientes para reparar la factura");
                return false;
            }
            
            FacturaRepository repo = new FacturaRepository();
            
            System.out.println("\n============= REPARACIÓN DE FACTURA CON ÍTEMS REALES =============");
            System.out.println("Factura ID: " + facturaId);
            System.out.println("Ítems a guardar: " + items.size());
            
            // Detalle de ítems a guardar
            System.out.println("\n============= DETALLE DE ÍTEMS A GUARDAR =============");
            for (ItemFactura item : items) {
                System.out.println("Item: " + item.getProducto().getDescripcion() + 
                                  " (ID: " + item.getProducto().getIdProducto() + 
                                  ", Cantidad: " + item.getCantidad() + 
                                  ", Precio: " + item.getPrecioUnitario() + 
                                  ", Subtotal: " + item.getSubtotal() + ")");
            }
            System.out.println("=====================================================\n");
            
            // 1. Verificar si la factura existe
            Factura factura = repo.findById(facturaId);
            if (factura == null) {
                System.out.println("ERROR: No se encontró la factura con ID " + facturaId);
                return false;
            }
            
            // 2. Limpiar ítems existentes si hay alguno
            boolean limpiezaExitosa = repo.limpiarItemsFactura(facturaId);
            if (!limpiezaExitosa) {
                System.out.println("Advertencia: No se pudieron limpiar ítems previos");
            }
            
            // 3. Guardar los ítems reales
            boolean guardadoExitoso = repo.guardarItemsFactura(facturaId, items);
            if (!guardadoExitoso) {
                System.out.println("ERROR: No se pudieron guardar los ítems reales");
                return false;
            }
            
            // 4. Recalcular totales de la factura si es necesario
            double subtotal = 0;
            for (ItemFactura item : items) {
                subtotal += item.getSubtotal();
            }
            
            // Si el subtotal de los ítems no coincide con el de la factura, actualizar
            if (Math.abs(factura.getSubtotal() - subtotal) > 0.01) {
                System.out.println("Actualizando totales de factura para reflejar los ítems reales");
                
                factura.setSubtotal(subtotal);
                double iva = subtotal * 0.12; // 12% de IVA (ajustar según tu lógica)
                factura.setIva(iva);
                factura.setTotal(subtotal + iva - factura.getDescuento());
                
                boolean actualizacionExitosa = repo.update(factura);
                if (!actualizacionExitosa) {
                    System.out.println("Advertencia: No se pudieron actualizar los totales de la factura");
                }
            }
            
            System.out.println("Reparación exitosa: Se guardaron " + items.size() + " ítems reales");
            System.out.println("============================================================\n");
            
            return true;
            
        } catch (Exception e) {
            System.err.println("Error durante la reparación con ítems reales: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}