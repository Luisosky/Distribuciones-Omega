package com.distribuciones.omega.model;

public class Cliente {
    private Long idCliente; // Añadido para base de datos
    private String nombre;
    private String id; // Número de identificación (RUC o CI)
    private String emailString;
    private String telefono;
    private String direccion;
    private boolean active = true;
    private boolean mayorista = false; // Nueva propiedad para clientes mayoristas
    private double limiteCredito = 0.0; // Límite de crédito para clientes

    public Cliente() {
        // Constructor vacío necesario para frameworks
    }

    public Cliente(String nombre, String id, String emailString, String telefono, String direccion) {
        this.nombre = nombre;
        this.id = id;
        this.emailString = emailString;
        this.telefono = telefono;
        this.direccion = direccion;
        this.active = true;
    }

    // Constructor completo con todos los campos
    public Cliente(Long idCliente, String nombre, String id, String emailString, String telefono, 
                  String direccion, boolean active, boolean mayorista, double limiteCredito) {
        this.idCliente = idCliente;
        this.nombre = nombre;
        this.id = id;
        this.emailString = emailString;
        this.telefono = telefono;
        this.direccion = direccion;
        this.active = active;
        this.mayorista = mayorista;
        this.limiteCredito = limiteCredito;
    }

    // Getters y setters existentes
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
    public String getEmailString() {
        return emailString;
    }
    public void setEmailString(String emailString) {
        this.emailString = emailString;
    }
    public String getEmail() {
        return emailString;
    }
    public void setEmail(String email) {
        this.emailString = email;
    }
    public String getTelefono() {
        return telefono;
    }
    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }
    public String getDireccion() {
        return direccion;
    }
    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }
    public boolean isActive() {
        return active;
    }
    public void setActive(boolean active) {
        this.active = active;
    }
    
    // Nuevos getters y setters
    public Long getIdCliente() {
        return idCliente;
    }
    public void setIdCliente(Long idCliente) {
        this.idCliente = idCliente;
    }
    public boolean isMayorista() {
        return mayorista;
    }
    public void setMayorista(boolean mayorista) {
        this.mayorista = mayorista;
    }
    public double getLimiteCredito() {
        return limiteCredito;
    }
    public void setLimiteCredito(double limiteCredito) {
        this.limiteCredito = limiteCredito;
    }

    @Override
    public String toString() {
        return nombre + " (" + id + ")";
    }
}