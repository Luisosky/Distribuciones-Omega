package com.distribuciones.omega.service;

import com.distribuciones.omega.model.ProductoInventario;
import com.distribuciones.omega.repository.InventarioRepository;

import java.util.List;

/**
 * Servicio para gestionar las operaciones relacionadas con el inventario
 */
public class InventarioService {
    
    private final InventarioRepository inventarioRepository;
    
    public InventarioService() {
        this.inventarioRepository = new InventarioRepository();
    }
    
    /**
     * Obtiene todos los productos disponibles en inventario
     * @return Lista de productos en inventario
     */
    public List<ProductoInventario> obtenerProductosDisponibles() {
        return inventarioRepository.findAll();
    }
    
    /**
     * Busca un producto específico por su código
     * @param codigo Código único del producto
     * @return Producto encontrado o null si no existe
     */
    public ProductoInventario obtenerProductoPorCodigo(String codigo) {
        return inventarioRepository.findByCodigo(codigo);
    }
    
    /**
     * Actualiza el stock de un producto después de una venta
     * @param codigo Código del producto
     * @param cantidadVendida Cantidad a restar del stock
     * @return true si la actualización fue exitosa
     */
    public boolean actualizarStockProducto(String codigo, int cantidadVendida) {
        ProductoInventario producto = obtenerProductoPorCodigo(codigo);
        if (producto == null || producto.getStock() < cantidadVendida) {
            return false;
        }
        
        producto.setStock(producto.getStock() - cantidadVendida);
        return inventarioRepository.update(producto);
    }
    
    /**
     * Verifica si hay suficiente stock para un producto
     * @param codigo Código del producto
     * @param cantidadRequerida Cantidad necesaria
     * @return true si hay suficiente stock
     */
    public boolean verificarDisponibilidad(String codigo, int cantidadRequerida) {
        ProductoInventario producto = obtenerProductoPorCodigo(codigo);
        return producto != null && producto.getStock() >= cantidadRequerida;
    }
    
    /**
     * Obtiene productos con stock bajo (para alertas)
     * @param umbral Nivel mínimo de stock
     * @return Lista de productos con stock bajo
     */
    public List<ProductoInventario> obtenerProductosStockBajo(int umbral) {
        return inventarioRepository.findByStockLessThan(umbral);
    }
    
    /**
     * Actualiza un producto en el inventario
     * @param producto Producto con datos actualizados
     * @return true si la actualización fue exitosa
     */
    public boolean actualizarProducto(ProductoInventario producto) {
        return inventarioRepository.update(producto);
    }
}