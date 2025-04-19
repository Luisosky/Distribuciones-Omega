package com.distribuciones.omega.model;

public class DetalleFactura {
    private int cantidad;
    private Double valor;
    private Double descuento;
    private Double valortTotal;

    public DetalleFactura(int cantidad, Double valor, Double descuento) {
        this.cantidad = cantidad;
        this.valor = valor;
        this.descuento = descuento;
        this.valortTotal = calcularValorTotal();
    }

    public int getCantidad() {
        return cantidad;
    }

    public Double getValor() {
        return valor;
    }

    public Double getDescuento() {
        return descuento;
    }

    public Double getValortTotal() {
        return valortTotal;
    }

    private Double calcularValorTotal() {
        return (valor * cantidad) - descuento;
    }

}
