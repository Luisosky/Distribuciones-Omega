package com.distribuciones.omega.service;

import com.distribuciones.omega.model.Producto;
import com.distribuciones.omega.model.ProductoInventario;
import com.distribuciones.omega.model.Categoria;
import com.distribuciones.omega.repository.ProductoRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para la gestión de productos
 */
public class ProductoService {
    
    private final ProductoRepository repository;
    
    public ProductoService() {
        this.repository = new ProductoRepository();
    }
    
    /**
     * Obtiene todos los productos
     * @return Lista de productos
     */
    public List<Producto> getAllProductos() {
        return repository.findAll();
    }
    
    /**
     * Guarda un nuevo producto
     * @param producto Producto a guardar
     * @return Producto guardado con ID asignado
     */
    public Producto saveProducto(Producto producto) {
        return repository.save(producto);
    }
    
    /**
     * Actualiza un producto existente
     * @param producto Producto a actualizar
     * @return true si la actualización fue exitosa
     */
    public boolean updateProducto(Producto producto) {
        return repository.update(producto);
    }
    
    /**
     * Elimina un producto por su ID
     * @param id ID del producto
     * @return true si la eliminación fue exitosa
     */
    public boolean deleteProducto(String id) {
        return repository.delete(id);
    }
    
    /**
     * Busca un producto por su ID
     * @param id ID del producto
     * @return Producto encontrado o null si no existe
     */
    public Producto findById(String id) {
        return repository.findById(id);
    }
    
    /**
     * Genera un nuevo ID para un producto según su categoría
     * @param categoria Categoría del producto
     * @return Nuevo ID generado
     */
    public String generateNewId(Categoria categoria) {
        return repository.generateNewId(categoria);
    }
    
    /**
     * Busca productos por nombre (búsqueda parcial)
     * @param nombre Fragmento del nombre a buscar
     * @return Lista de productos que coinciden
     */
    public List<Producto> findByNombreContaining(String nombre) {
        return repository.findByNombreContaining(nombre);
    }
    
    /**
     * Busca productos por categoría
     * @param categoria Categoría a buscar
     * @return Lista de productos de la categoría
     */
    public List<Producto> findByCategoria(Categoria categoria) {
        return repository.findByCategoria(categoria);
    }
    
    /**
     * Inicializa la base de datos si es necesario
     */
    public void initializeDatabase() {
        repository.createTableIfNotExists();
    }

    /**
     * Obtiene todos los productos del inventario
     * @return Lista de productos con información de inventario
     */
    public List<ProductoInventario> obtenerTodosProductos() {
        // Necesitamos un InventarioRepository para obtener productos del inventario
        InventarioService inventarioService = new InventarioService();
        return inventarioService.obtenerProductosDisponibles();
    }

    /**
     * Obtiene productos con stock por debajo del mínimo especificado
     * @param stockMinimo Nivel mínimo de stock
     * @return Lista de productos con stock bajo
     */
    public List<ProductoInventario> obtenerProductosStockBajo(int stockMinimo) {
        List<ProductoInventario> todosProductos = obtenerTodosProductos();
        return todosProductos.stream()
                .filter(p -> p.getStock() < stockMinimo)
                .collect(Collectors.toList());
    }
}