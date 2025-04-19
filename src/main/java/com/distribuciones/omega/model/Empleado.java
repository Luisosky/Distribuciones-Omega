package com.distribuciones.omega.model;


public class Empleado {
    private String nombre;
    private int edad;
    private String id;
    private String direccion;
    private String telefono;
    private String emailString;
    private Double salario;
    private Enum<?> contrato;
    private String rol;


    public Empleado(String nombre, int edad, String id, String direccion, String telefono, String emailString, Double salario, Enum<?> contrato, String rol) {
        this.nombre = nombre;
        this.edad = edad;
        this.id = id;
        this.direccion = direccion;
        this.telefono = telefono;
        this.emailString = emailString;
        this.salario = salario;
        this.contrato = contrato;
        this.rol = rol;
    }

    public String getNombre() {
        return nombre;
    }
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
    public int getEdad() {
        return edad;
    }
    public void setEdad(int edad) {
        this.edad = edad;
    }
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getDireccion() {
        return direccion;
    }
    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }
    public String getTelefono() {
        return telefono;
    }
    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }
    public String getEmail() {
        return emailString;
    }
    public void setEmail(String emailString) {
        this.emailString = emailString;
    }
    public Double getSalario() {
        return salario;
    }
    public void setSalario(Double salario) {
        this.salario = salario;
    }
    public Enum<?> getContrato() {
        return contrato;
    }
    public void setContrato(Enum<?> contrato) {
        this.contrato = contrato;
    }
    public String getRol() {
        return rol;
    }
    public void setRol(String rol) {
        this.rol = rol;
    }
    @Override
    public String toString() {
        return "Empleado{" +
                "nombre='" + nombre + '\'' +
                ", edad=" + edad +
                ", id='" + id + '\'' +
                ", direccion='" + direccion + '\'' +
                ", telefono='" + telefono + '\'' +
                ", emailString='" + emailString + '\'' +
                ", salario=" + salario +
                ", contrato=" + contrato +
                ", rol='" + rol + '\'' +
                ", contratoType='" + contrato.getClass().getSimpleName() + '\'' +
                '}';
    }

}
