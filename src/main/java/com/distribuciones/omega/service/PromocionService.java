package com.distribuciones.omega.service;

import com.distribuciones.omega.model.Promocion;
import com.distribuciones.omega.repository.PromocionRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Servicio para gestionar las operaciones relacionadas con promociones
 */
public class PromocionService {
    
    private final PromocionRepository promocionRepository;
    
    public PromocionService() {
        this.promocionRepository = new PromocionRepository();
    }
    
    /**
     * Busca una promoción vigente para un producto específico
     * @param codigoProducto Código del producto
     * @return Promoción vigente o null si no hay
     */
    public Promocion buscarPromocionPorProducto(String codigoProducto) {
        // Obtener todas las promociones vigentes para el producto
        List<Promocion> promociones = promocionRepository.findByProductoAndFechas(
                codigoProducto, 
                LocalDate.now(), 
                LocalDate.now());
        
        // Devolver la primera promoción encontrada (si hay varias, se podría implementar lógica para elegir la mejor)
        return promociones.isEmpty() ? null : promociones.get(0);
    }
    
    /**
     * Guarda una nueva promoción
     * @param promocion Promoción a guardar
     * @return Promoción guardada con ID asignado
     */
    public Promocion guardarPromocion(Promocion promocion) {
        return promocionRepository.save(promocion);
    }
    
    /**
     * Obtiene todas las promociones vigentes
     * @return Lista de promociones vigentes
     */
    public List<Promocion> obtenerPromocionesVigentes() {
        return promocionRepository.findByFechas(LocalDate.now(), LocalDate.now());
    }
    
    /**
     * Obtiene todas las promociones (vigentes y no vigentes)
     * @return Lista de todas las promociones
     */
    public List<Promocion> obtenerTodasPromociones() {
        return promocionRepository.findAll();
    }
    
    /**
     * Actualiza una promoción existente
     * @param promocion Promoción con datos actualizados
     * @return true si la actualización fue exitosa
     */
    public boolean actualizarPromocion(Promocion promocion) {
        return promocionRepository.update(promocion);
    }
    
    /**
     * Elimina una promoción
     * @param id ID de la promoción a eliminar
     * @return true si la eliminación fue exitosa
     */
    public boolean eliminarPromocion(int id) {  // Cambiado de Long a int
        return promocionRepository.delete(id);
    }
    
    /**
     * Verifica si un producto tiene una promoción activa de tipo 2x1
     * @param codigoProducto Código del producto
     * @return true si hay promoción 2x1 activa
     */
    public boolean tienePromocion2x1(String codigoProducto) {
        Promocion promocion = buscarPromocionPorProducto(codigoProducto);
        // Verificar si es una promoción 2x1 basada en la descripción
        return promocion != null && promocion.getDescripcion().toUpperCase().contains("2X1");
    }
    
    /**
     * Obtiene el porcentaje de descuento para un producto (si aplica)
     * @param codigoProducto Código del producto
     * @return Porcentaje de descuento o 0 si no hay promoción
     */
    public double obtenerPorcentajeDescuento(String codigoProducto) {
        Promocion promocion = buscarPromocionPorProducto(codigoProducto);
        if (promocion != null && promocion.isPorcentaje()) {
            return promocion.getValor();
        }
        return 0;
    }
    
    /**
     * Verifica si un producto o categoría tiene alguna promoción aplicable
     * @param codigoProducto Código del producto
     * @param categoria Categoría del producto
     * @return Promoción aplicable o null si no hay
     */
    public Promocion buscarPromocionAplicable(String codigoProducto, String categoria) {
        // Primero buscar por producto específico
        Promocion promocionProducto = buscarPromocionPorProducto(codigoProducto);
        if (promocionProducto != null) {
            return promocionProducto;
        }
        
        // Si no hay promoción específica para el producto, buscar por su categoría
        List<Promocion> todasPromociones = obtenerPromocionesVigentes();
        for (Promocion promocion : todasPromociones) {
            String categoriasAplicables = promocion.getCategoriasAplicables();
            
            // Si aplica a todas las categorías o a la categoría específica
            if (categoriasAplicables != null && 
                (categoriasAplicables.equals("TODAS") || 
                 categoriasAplicables.contains(categoria))) {
                return promocion;
            }
        }
        
        return null;
    }
}