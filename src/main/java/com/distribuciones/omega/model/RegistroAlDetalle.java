package com.distribuciones.omega.model;

public class RegistroAlDetalle {
    private String cuenta;
    private String deudor;
    private String proveedor;

    public RegistroAlDetalle(String cuenta, String deudor, String proveedor) {
        this.cuenta = cuenta;
        this.deudor = deudor;
        this.proveedor = proveedor;
    }

    public String getCuenta() {
        return cuenta;
    }

    public String getDeudor() {
        return deudor;
    }

    public String getProveedor() {
        return proveedor;
    }

    public void setCuenta(String cuenta) {
        this.cuenta = cuenta;
    }

    public void setDeudor(String deudor) {
        this.deudor = deudor;
    }

    public void setProveedor(String proveedor) {
        this.proveedor = proveedor;
    }
}
