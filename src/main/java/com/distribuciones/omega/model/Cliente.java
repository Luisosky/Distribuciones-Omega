package com.distribuciones.omega.model;

public class Cliente {
    private String nombre;
    private String id;
    private String emailString;
    private String telefono;
    private String direccion;
    private boolean active = true;


    public Cliente(String nombre, String id, String emailString, String telefono, String direccion) {
        this.nombre = nombre;
        this.id = id;
        this.emailString = emailString;
        this.telefono = telefono;
        this.direccion = direccion;
        this.active = true;
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
    public String getEmailString() {
        return emailString;
    }
    public void setEmailString(String emailString) {
        this.emailString = emailString;
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

    @Override
    public String toString() {
        return "Cliente{" +
                "nombre='" + nombre + '\'' +
                ", id='" + id + '\'' +
                ", emailString='" + emailString + '\'' +
                ", telefono='" + telefono + '\'' +
                ", direccion='" + direccion + '\'' +
                '}';
    }
}
