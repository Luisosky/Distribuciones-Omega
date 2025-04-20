@echo off
setlocal enabledelayedexpansion

echo.
echo ================================================
echo      INICIANDO DISTRIBUCIONES OMEGA
echo ================================================
echo.

REM Verificar que existe el JAR
if not exist "target\app-pos-0.0.1-SNAPSHOT.jar" (
    echo ERROR: No se encuentra el archivo JAR. 
    echo.
    echo Compilando el proyecto...
    call mvn clean package -DskipTests
    
    if %errorlevel% neq 0 (
        echo.
        echo ERROR: Falló la compilación del proyecto.
        pause
        exit /b 1
    )
)

echo.
echo Iniciando aplicación...
echo.

REM Crear archivo para ejecutar la aplicación
echo java -jar app-pos-0.0.1-SNAPSHOT.jar > Distribuciones-Omega-Release\ejecutar.bat

REM Ejecutar la aplicación directamente con Spring Boot
java -jar target\app-pos-0.0.1-SNAPSHOT.jar

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