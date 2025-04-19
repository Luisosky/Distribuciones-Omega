package com.distribuciones.omega.model;

import java.util.ArrayList;
import java.util.List;

public class Empresa {
    private String nombre;
    private String id;
    private List<Producto> productos = new ArrayList<>(); 
    private List<Cliente> clientes = new ArrayList<>();
    private List<Usuario> empleados = new ArrayList<>();

    public Empresa(String nombre, String id) {
        this.nombre = nombre;
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<Producto> getProductos() {
        return productos;
    }

    public void setProductos(List<Producto> productos) {
        this.productos = productos;
    }

    public void agregarProducto(Producto producto) {
        this.productos.add(producto);
    }
    public void eliminarProducto(Producto producto) {
        this.productos.remove(producto);
    }
    public void eliminarProductoPorId(String id) {
        this.productos.removeIf(producto -> producto.getId().equals(id));
    }
    public void agregarCliente(Cliente cliente) {
        this.clientes.add(cliente);
    }
    public void eliminarCliente(Cliente cliente) {
        this.clientes.remove(cliente);
    }
    public void eliminarClientePorId(String id) {
        this.clientes.removeIf(cliente -> cliente.getId().equals(id));
    }
    public void agregarEmpleado(Usuario empleado) {
        this.empleados.add(empleado);
    }
    public void eliminarEmpleado(Usuario empleado) {
        this.empleados.remove(empleado);
    }
    public void eliminarEmpleadoPorId(String id) {
        this.empleados.removeIf(empleado -> empleado.getId().equals(id));
    }

    @Override
    public String toString() {
        return "Empresa{" +
                "nombre='" + nombre + '\'' +
                ", id='" + id + '\'' +
                ", productos=" + productos.toString() +
                ", clientes=" + clientes.toString() +
                ", empleados=" + empleados.toString() +
                ", totalProductos=" + productos.size() +
                ", totalClientes=" + clientes.size() +
                ", totalEmpleados=" + empleados.size() +
                '}';
    }

}
