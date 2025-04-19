package com.distribuciones.omega.utils;

import com.distribuciones.omega.model.Usuario;

/**
 * Singleton para manejar la sesión del usuario actual
 */
public class SessionManager {
    
    private static SessionManager instance;
    private Usuario usuarioActual;
    
    private SessionManager() {
        // Constructor privado para singleton
    }
    
    /**
     * Obtiene la instancia única del session manager
     * @return Instancia del session manager
     */
    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }
    
    /**
     * Guarda el usuario actual en sesión
     * @param usuario Usuario a guardar en sesión
     */
    public void setUsuarioActual(Usuario usuario) {
        this.usuarioActual = usuario;
    }
    
    /**
     * Obtiene el usuario actual en sesión
     * @return Usuario actual o null si no hay sesión
     */
    public Usuario getUsuarioActual() {
        return usuarioActual;
    }
    
    /**
     * Limpia la sesión actual
     */
    public void clearSession() {
        this.usuarioActual = null;
    }
    
    /**
     * Verifica si hay un usuario con sesión activa
     * @return true si hay sesión activa
     */
    public boolean isSessionActive() {
        return usuarioActual != null;
    }
}