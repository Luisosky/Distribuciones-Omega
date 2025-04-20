package com.distribuciones.omega.utils;

import java.util.List;
import java.util.Map;

import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;

/**
 * Utilidad para crear y configurar gráficos
 */
public class GraficoUtil {
    
    /**
     * Crea un gráfico de barras con los datos proporcionados
     * @param titulo Título del gráfico
     * @param ejeX Título del eje X
     * @param ejeY Título del eje Y
     * @param datos Mapa de datos (categoría -> valor)
     * @return Gráfico de barras configurado
     */
    public static BarChart<String, Number> crearGraficoBarras(
            String titulo, String ejeX, String ejeY, Map<String, Number> datos) {
        
        // Crear ejes
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel(ejeX);
        yAxis.setLabel(ejeY);
        
        // Crear gráfico
        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle(titulo);
        
        // Crear serie
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(titulo);
        
        // Agregar datos
        for (Map.Entry<String, Number> entry : datos.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        
        barChart.getData().add(series);
        return barChart;
    }

    /**
     * Configura un gráfico de barras existente con los datos proporcionados
     * @param chart Gráfico de barras a configurar
     * @param titulo Título del gráfico
     * @param ejeX Título del eje X
     * @param ejeY Título del eje Y
     * @param datos Mapa de datos (categoría -> valor)
     */
    public static void generarGraficoBarras(
            BarChart<String, Number> chart, 
            String titulo, 
            String ejeX, 
            String ejeY, 
            Map<String, Double> datos) {
        
        // Limpiar datos existentes
        chart.getData().clear();
        
        // Configurar ejes
        CategoryAxis xAxis = (CategoryAxis) chart.getXAxis();
        NumberAxis yAxis = (NumberAxis) chart.getYAxis();
        xAxis.setLabel(ejeX);
        yAxis.setLabel(ejeY);
        
        // Configurar gráfico
        chart.setTitle(titulo);
        
        // Crear serie
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(titulo);
        
        // Agregar datos
        for (Map.Entry<String, Double> entry : datos.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        
        chart.getData().add(series);
        
        // Hacer visible
        chart.setAnimated(true);
        chart.setLegendVisible(false);
    }
    
    /**
     * Crea un gráfico de líneas con los datos proporcionados
     * @param titulo Título del gráfico
     * @param ejeX Título del eje X
     * @param ejeY Título del eje Y
     * @param datos Mapa de datos (categoría -> valor)
     * @return Gráfico de líneas configurado
     */
    public static LineChart<String, Number> crearGraficoLineas(
            String titulo, String ejeX, String ejeY, Map<String, Number> datos) {
        
        // Crear ejes
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel(ejeX);
        yAxis.setLabel(ejeY);
        
        // Crear gráfico
        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle(titulo);
        
        // Crear serie
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(titulo);
        
        // Agregar datos
        for (Map.Entry<String, Number> entry : datos.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        
        lineChart.getData().add(series);
        return lineChart;
    }
    
    /**
     * Crea un gráfico circular con los datos proporcionados
     * @param titulo Título del gráfico
     * @param datos Mapa de datos (categoría -> valor)
     * @return Gráfico circular configurado
     */
    public static PieChart crearGraficoCircular(String titulo, Map<String, Number> datos) {
        // Crear gráfico
        PieChart pieChart = new PieChart();
        pieChart.setTitle(titulo);
        
        // Agregar datos
        for (Map.Entry<String, Number> entry : datos.entrySet()) {
            pieChart.getData().add(new PieChart.Data(
                    entry.getKey(), entry.getValue().doubleValue()));
        }
        
        return pieChart;
    }
    
    /**
     * Exporta un gráfico como imagen
     * @param chart El gráfico a exportar
     * @return Ruta de la imagen exportada o null si hubo un error
     */
    public static String exportarGrafico(javafx.scene.Node chart) {
        // Implementar lógica para exportar gráfico como imagen
        // Requiere snapshot y JavaFX utilities
        return null;
    }
}