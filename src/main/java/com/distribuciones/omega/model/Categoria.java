package com.distribuciones.omega.model;

public enum Categoria {
    INSUMO_OFICINA("Insumo Oficina"),
    PRODUCTO_MOBILIARIO("Producto Mobiliario"),
    PRODUCTO_TECNOLOGICO("Producto Tecnológico");
    
    private final String displayName;
    
    Categoria(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}