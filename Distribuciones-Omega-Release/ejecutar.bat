@echo off
setlocal enabledelayedexpansion
chcp 1252 >nul

echo.
echo ================================================
echo      INICIANDO DISTRIBUCIONES OMEGA
echo ================================================
echo.

REM Cargar las credenciales desde el archivo .env
set DB_USER=root
set DB_PASS=root
set DB_NAME=omega
set DB_HOST=127.0.0.1
set DB_PORT=3306

if exist ".env" (
    for /F "tokens=1,2 delims==" %%a in (.env) do (
        if "%%a"=="DB_USER" set DB_USER=%%b
        if "%%a"=="DB_PASS" set DB_PASS=%%b
        if "%%a"=="DB_NAME" set DB_NAME=%%b
        if "%%a"=="DB_URL" (
            for /F "tokens=3 delims=:/" %%x in ("%%b") do set DB_HOST=%%x
            for /F "tokens=4 delims=:/" %%y in ("%%b") do (
                for /F "tokens=1 delims==" %%z in ("%%y") do set DB_PORT=%%z
            )
        )
    )
)

REM Crear archivo temporal SQL para crear la base de datos
echo CREATE DATABASE IF NOT EXISTS %DB_NAME% CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; > "%TEMP%\create_db.sql"

REM Intentar crear la base de datos
echo Intentando crear la base de datos %DB_NAME% si no existe...
mysql -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASS% < "%TEMP%\create_db.sql" 2>nul
if %errorlevel% neq 0 (
    echo ADVERTENCIA: No se pudo crear automáticamente la base de datos.
    echo Es posible que necesite crear manualmente la base de datos '%DB_NAME%'.
    echo Ejecute el siguiente comando en MySQL:
    echo CREATE DATABASE IF NOT EXISTS %DB_NAME% CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    echo.
) else (
    echo Base de datos verificada correctamente.
)

REM Eliminar el archivo temporal
del "%TEMP%\create_db.sql" >nul 2>&1

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