package com.distribuciones.omega.model;

public class DetalleFactura {
    private int id;
    private String codigo;
    private String descripcion;
    private int cantidad;
    private Double valor;
    private Double descuento;
    private Double valorTotal;
    
    // Constructor existente
    public DetalleFactura(int cantidad, Double valor, Double descuento) {
        this.cantidad = cantidad;
        this.valor = valor;
        this.descuento = descuento;
        this.valorTotal = calcularValorTotal();
    }
    
    // Nuevo constructor para NotaEntrega
    public DetalleFactura(String codigo, String descripcion, int cantidad) {
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.cantidad = cantidad;
        this.valor = 0.0;
        this.descuento = 0.0;
        this.valorTotal = 0.0;
    }
    
    // Constructor vacío
    public DetalleFactura() {
    }

    // Getters y setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getCodigo() {
        return codigo;
    }
    
    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
    
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
    
    public int getCantidad() {
        return cantidad;
    }
    
    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public Double getValor() {
        return valor;
    }
    
    public void setValor(Double valor) {
        this.valor = valor;
    }

    public Double getDescuento() {
        return descuento;
    }
    
    public void setDescuento(Double descuento) {
        this.descuento = descuento;
    }

    public Double getValorTotal() {
        return valorTotal;
    }
    
    public void setValorTotal(Double valorTotal) {
        this.valorTotal = valorTotal;
    }

    private Double calcularValorTotal() {
        return (valor * cantidad) - descuento;
    }
}