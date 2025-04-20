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
     * Actualiza el stock de un producto
     * @param producto Producto a actualizar
     * @param cantidadVendida Cantidad a reducir del stock
     * @return true si la actualización fue exitosa
     */
    public boolean actualizarStockProducto(ProductoInventario producto, int cantidadVendida) {
        try {
            if (producto == null) {
                System.err.println("ERROR: Producto nulo al intentar actualizar stock");
                return false;
            }
            
            // Verificar disponibilidad primero
            System.out.println("Verificando disponibilidad para producto: " + producto.getDescripcion() + 
                              " (Cantidad disponible: " + producto.getStock() + ", Solicitado: " + cantidadVendida + ")");
            
            if (!verificarDisponibilidad(producto, cantidadVendida)) {
                System.err.println("ERROR: Stock insuficiente para el producto: " + producto.getDescripcion());
                return false;
            }
            
            // Calcular nuevo stock
            int nuevoStock = producto.getStock() - cantidadVendida;
            System.out.println("Actualizando cantidad para producto: " + producto.getDescripcion() + 
                              " (ID: " + producto.getIdProducto() + ", Código: " + producto.getCodigo() + "), Nueva cantidad: " + nuevoStock);
            
            // Actualizar el objeto producto
            producto.setStock(nuevoStock);
            
            // Imprimir la estructura de la tabla para diagnosticar el problema
            System.out.println("======= DIAGNÓSTICO DE ESTRUCTURA DE BASE DE DATOS =======");
            inventarioRepository.imprimirEstructuraTablaProductos();
            System.out.println("==========================================================");
            
            // Intentar actualizar usando el código
            System.out.println("Actualizando producto por código: " + producto.getCodigo());
            boolean resultado = inventarioRepository.update(producto);
            
            if (!resultado) {
                System.err.println("Error al actualizar stock para: " + producto.getDescripcion());
            }
            
            return resultado;
            
        } catch (Exception e) {
            System.err.println("Error en actualizarStockProducto: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Actualiza el stock de un producto (resta las unidades vendidas)
     * @param productoId ID o código del producto
     * @param cantidad Cantidad vendida (se resta del stock)
     * @return true si la actualización fue exitosa
     */
    public boolean actualizarStockProducto(String productoId, int cantidad) {
        try {
            // 1. Obtener el producto actual
            ProductoInventario producto = null;
            
            // Intentar convertir a Long para buscar por ID
            try {
                Long idProducto = Long.parseLong(productoId);
                producto = inventarioRepository.findById(idProducto);
            } catch (NumberFormatException e) {
                // No es un número, buscar por código
                producto = inventarioRepository.findByCodigo(productoId);
            }
            
            if (producto == null) {
                System.err.println("Producto no encontrado: " + productoId);
                return false;
            }
            
            // 2. Verificar stock suficiente
            if (producto.getStock() < cantidad) {
                System.err.println("Stock insuficiente para el producto: " + productoId);
                return false;
            }
            
            // 3. Actualizar stock
            int nuevoStock = producto.getStock() - cantidad;
            producto.setStock(nuevoStock);
            
            // 4. Guardar cambios
            return inventarioRepository.update(producto);
            
        } catch (Exception e) {
            System.err.println("Error al actualizar stock: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Verifica la disponibilidad de cantidad (stock) de un producto
     * @param producto Objeto producto completo
     * @param cantidad Cantidad solicitada
     * @return true si hay suficiente cantidad
     */
    public boolean verificarDisponibilidad(ProductoInventario producto, int cantidad) {
        if (producto == null) {
            System.err.println("Verificación de disponibilidad: producto es null");
            return false;
        }
        
        System.out.println("Verificando disponibilidad para producto: " + 
                        producto.getDescripcion() + 
                        " (Cantidad disponible: " + producto.getCantidad() + 
                        ", Solicitado: " + cantidad + ")");
        
        return producto.getCantidad() >= cantidad;  // Usamos getCantidad()
    }

    
    /**
     * Verifica si hay suficiente stock para un producto y cantidad
     * @param productoId ID del producto a verificar
     * @param cantidad Cantidad requerida
     * @return true si hay suficiente stock
     */
    public boolean verificarDisponibilidad(String productoId, int cantidad) {
        try {
            // Convertir el ID a Long si es posible
            Long idProductoLong;
            try {
                idProductoLong = Long.parseLong(productoId);
            } catch (NumberFormatException e) {
                System.out.println("ID de producto no es un número válido: " + productoId);
                
                // Si no es un Long válido, intentar buscar por código
                ProductoInventario producto = inventarioRepository.findByCodigo(productoId);
                if (producto == null) {
                    System.out.println("Producto con código " + productoId + " no encontrado");
                    return false;
                }
                
                System.out.println("Verificando disponibilidad para producto con código " + productoId + 
                    ": Disponible=" + producto.getStock() + ", Solicitado=" + cantidad);
                
                return producto.getStock() >= cantidad;
            }
            
            // Buscar por ID
            ProductoInventario producto = inventarioRepository.findById(idProductoLong);
            
            if (producto == null) {
                System.out.println("Producto con ID " + idProductoLong + " no encontrado");
                return false;
            }
            
            System.out.println("Verificando disponibilidad para producto con ID " + idProductoLong + 
                ": Disponible=" + producto.getStock() + ", Solicitado=" + cantidad);
            
            return producto.getStock() >= cantidad;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
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