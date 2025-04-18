package com.distribuciones.omega.model;

import java.util.Date;


public class Factura {
    private String idFactura;
    private Date fechaEmision;
    private Empleado empleado;
    private String telefono;
    private String direccion;

    public Factura(String idFactura, Date fechaEmision, Empleado empleado, String telefono, String direccion) {
        this.idFactura = idFactura;
        this.fechaEmision = fechaEmision;
        this.empleado = empleado;
        this.telefono = telefono;
        this.direccion = direccion;
    }
    public String getIdFactura() {
        return idFactura;
    }
    public void setIdFactura(String idFactura) {
        this.idFactura = idFactura;
    }
    public Date getFechaEmision() {
        return fechaEmision;
    }
    public void setFechaEmision(Date fechaEmision) {
        this.fechaEmision = fechaEmision;
    }
    public Empleado getEmpleado() {
        return empleado;
    }
    public void setEmpleado(Empleado empleado) {
        this.empleado = empleado;
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

    public String toString() {
        return "Factura{" +
                "idFactura='" + idFactura + '\'' +
                ", fechaEmision=" + fechaEmision +
                ", empleado=" + empleado +
                ", telefono='" + telefono + '\'' +
                ", direccion='" + direccion + '\'' +
                ", total=" + calcularTotal() +
                '}';
    }

    public double calcularTotal() {

        return 0.0;
    }
}
