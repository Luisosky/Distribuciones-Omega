package com.distribuciones.omega.utils;

import com.distribuciones.omega.service.AlertaStockService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Monitor de inventario que verifica periódicamente el stock bajo
 */
public class InventarioMonitor {
    
    private static InventarioMonitor instance;
    private final AlertaStockService alertaStockService;
    private ScheduledExecutorService scheduler;
    private boolean running = false;
    
    private InventarioMonitor() {
        this.alertaStockService = new AlertaStockService();
    }
    
    /**
     * Obtiene la instancia única del monitor (Singleton)
     * @return Instancia del monitor
     */
    public static synchronized InventarioMonitor getInstance() {
        if (instance == null) {
            instance = new InventarioMonitor();
        }
        return instance;
    }
    
    /**
     * Inicia el monitoreo periódico del inventario
     * @param intervalHours Intervalo de verificación en horas
     */
    public void iniciarMonitoreo(int intervalHours) {
        if (running) {
            detenerMonitoreo();
        }
        
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(
            () -> alertaStockService.verificarStockBajo(),
            1,                 // Delay inicial (1 hora)
            intervalHours,     // Periodo entre ejecuciones
            TimeUnit.HOURS
        );
        
        running = true;
        System.out.println("Monitoreo de inventario iniciado con intervalo de " + intervalHours + " horas");
    }
    
    /**
     * Detiene el monitoreo del inventario
     */
    public void detenerMonitoreo() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            running = false;
            System.out.println("Monitoreo de inventario detenido");
        }
    }
    
    /**
     * Verifica inmediatamente el inventario
     */
    public void verificarAhora() {
        new Thread(() -> alertaStockService.verificarStockBajo()).start();
    }
}