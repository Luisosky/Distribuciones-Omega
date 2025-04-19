package com.distribuciones.omega.service;

import com.distribuciones.omega.model.Cliente;
import com.distribuciones.omega.repository.ClienteRepository;

import java.util.List;

/**
 * Servicio para gestionar las operaciones relacionadas con los clientes
 */
public class ClienteService {
    
    private final ClienteRepository clienteRepository;
    
    public ClienteService() {
        this.clienteRepository = new ClienteRepository();
    }
    
    /**
     * Obtiene todos los clientes registrados
     * @return Lista de clientes
     */
    public List<Cliente> obtenerTodosClientes() {
        return clienteRepository.findAll();
    }
    
    /**
     * Busca un cliente por su ID
     * @param id ID del cliente
     * @return Cliente encontrado o null si no existe
     */
    public Cliente obtenerClientePorId(Long id) {
        return clienteRepository.findById(id);
    }
    
    /**
     * Busca un cliente por su número de identificación (RUC o CI)
     * @param numeroIdentificacion Número de identificación
     * @return Cliente encontrado o null si no existe
     */
    public Cliente obtenerClientePorIdentificacion(String numeroIdentificacion) {
        return clienteRepository.findByNumeroIdentificacion(numeroIdentificacion);
    }
    
    /**
     * Guarda un nuevo cliente
     * @param cliente Cliente a guardar
     * @return Cliente guardado con ID asignado
     */
    public Cliente guardarCliente(Cliente cliente) {
        return clienteRepository.save(cliente);
    }
    
    /**
     * Actualiza los datos de un cliente existente
     * @param cliente Cliente con datos actualizados
     * @return true si la actualización fue exitosa
     */
    public boolean actualizarCliente(Cliente cliente) {
        return clienteRepository.update(cliente);
    }
    
    /**
     * Obtiene la lista de clientes mayoristas
     * @return Lista de clientes mayoristas
     */
    public List<Cliente> obtenerClientesMayoristas() {
        return clienteRepository.findByMayorista(true);
    }
    
    /**
     * Verifica si un cliente existe por su identificación
     * @param numeroIdentificacion Número de identificación
     * @return true si el cliente ya existe
     */
    public boolean existeCliente(String numeroIdentificacion) {
        return clienteRepository.findByNumeroIdentificacion(numeroIdentificacion) != null;
    }

    /**
     * Elimina lógicamente un cliente (lo marca como inactivo)
     * @param id ID del cliente
     * @return true si la eliminación fue exitosa
     */
    public boolean eliminarCliente(Long id) {
        return clienteRepository.softDelete(id);
    }
}