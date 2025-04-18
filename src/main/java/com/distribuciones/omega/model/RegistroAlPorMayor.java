package com.distribuciones.omega.model;

public class RegistroAlPorMayor {
    private String cuenta;
    private String deudor;
    private String proveedor;

    public RegistroAlPorMayor(String cuenta, String deudor, String proveedor) {
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
    
}
