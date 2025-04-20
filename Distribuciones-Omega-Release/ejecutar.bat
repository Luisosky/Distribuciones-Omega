@echo off
setlocal enabledelayedexpansion

echo.
echo ================================================
echo      INICIANDO DISTRIBUCIONES OMEGA
echo ================================================
echo.

REM Verificar que existe el JAR en la ubicación actual
if not exist "app-pos-0.0.1-SNAPSHOT.jar" (
    echo ERROR: No se encuentra el archivo JAR en la carpeta actual.
    echo.
    echo Por favor, asegúrese de que el archivo JAR está en la misma carpeta que este script.
    pause
    exit /b 1
)

echo.
echo Iniciando aplicación...
echo.

REM Ejecutar la aplicación directamente con Spring Boot
java -jar app-pos-0.0.1-SNAPSHOT.jar

if %errorlevel% neq 0 (
    echo.
    echo Error al iniciar la aplicación. Código: %errorlevel%
    echo.
    echo Posibles soluciones:
    echo 1. Asegúrese de tener MySQL instalado y en ejecución
    echo 2. Verifique la conexión a la base de datos
    echo 3. Verifique permisos de escritura en la carpeta actual
    echo.
    pause
)