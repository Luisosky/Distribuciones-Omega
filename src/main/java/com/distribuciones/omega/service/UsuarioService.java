package com.distribuciones.omega.service;

import com.distribuciones.omega.model.Usuario;
import com.distribuciones.omega.repository.UsuarioRepository;
import com.distribuciones.omega.utils.SessionManager;

import java.util.List;

/**
 * Servicio para gestionar las operaciones relacionadas con usuarios
 */
public class UsuarioService {
    
    private final UsuarioRepository usuarioRepository;
    
    public UsuarioService() {
        this.usuarioRepository = new UsuarioRepository();
    }
    
    /**
     * Autentica un usuario
     * @param username Nombre de usuario
     * @param password Contraseña
     * @return Usuario autenticado o null si las credenciales son incorrectas
     */
    public Usuario autenticar(String username, String password) {
        Usuario usuario = usuarioRepository.findByUsername(username);
        
        if (usuario != null && verificarPassword(password, usuario.getPassword())) {
            // Guardar usuario en sesión
            SessionManager.getInstance().setUsuarioActual(usuario);
            return usuario;
        }
        
        return null;
    }
    
    /**
     * Obtiene el usuario actual (en sesión)
     * @return Usuario actual o null si no hay sesión
     */
    public Usuario getUsuarioActual() {
        return SessionManager.getInstance().getUsuarioActual();
    }
    
    /**
     * Cierra la sesión del usuario actual
     */
    public void cerrarSesion() {
        SessionManager.getInstance().clearSession();
    }
    
    /**
     * Registra un nuevo usuario
     * @param usuario Usuario a registrar
     * @return Usuario registrado con ID asignado
     */
    public Usuario registrarUsuario(Usuario usuario) {
        // Verificar que el username no exista
        if (usuarioRepository.findByUsername(usuario.getUsername()) != null) {
            return null;
        }
        
        // Encriptar contraseña
        usuario.setPassword(encriptarPassword(usuario.getPassword()));
        
        return usuarioRepository.save(usuario);
    }
    
    /**
     * Actualiza datos de un usuario
     * @param usuario Usuario con datos actualizados
     * @return true si la actualización fue exitosa
     */
    public boolean actualizarUsuario(Usuario usuario) {
        // Si se actualiza la contraseña, encriptarla
        if (usuario.getPassword() != null && !usuario.getPassword().isEmpty()) {
            usuario.setPassword(encriptarPassword(usuario.getPassword()));
        }
        
        return usuarioRepository.update(usuario);
    }
    
    /**
     * Obtiene todos los usuarios (solo administrador)
     * @return Lista de usuarios
     */
    public List<Usuario> obtenerTodosUsuarios() {
        return usuarioRepository.findAll();
    }
    
    /**
     * Busca un usuario por su ID
     * @param id ID del usuario
     * @return Usuario encontrado o null si no existe
     */
    public Usuario obtenerUsuarioPorId(Long id) {
        return usuarioRepository.findById(id);
    }
    
    /**
     * Obtiene los vendedores activos
     * @return Lista de vendedores
     */
    public List<Usuario> obtenerVendedores() {
        return usuarioRepository.findByRol("VENDEDOR");
    }
    
    /**
     * Encripta una contraseña (método simplificado, en producción usar BCrypt o similar)
     */
    private String encriptarPassword(String password) {
        // Implementación simple para pruebas - en producción usar BCrypt
        return "encrypted:" + password;
    }
    
    /**
     * Verifica si una contraseña coincide con su versión encriptada
     */
    private boolean verificarPassword(String plainPassword, String encryptedPassword) {
        // Implementación simple para pruebas - en producción usar BCrypt
        return encryptedPassword.equals("encrypted:" + plainPassword);
    }
}