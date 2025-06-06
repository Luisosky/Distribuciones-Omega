@echo off
setlocal enabledelayedexpansion
chcp 1252 >nul

echo.
echo ================================================
echo      INICIANDO DISTRIBUCIONES OMEGA
echo ================================================
echo.

REM Cargar las credenciales desde el archivo .env
set DB_URL=jdbc:mysql://localhost:3306/omega
set DB_USER=root
set DB_PASS=root123
set DB_NAME=omega
set DB_HOST=localhost
set DB_PORT=3306

if exist ".env" (
    for /F "tokens=1,2 delims==" %%a in (.env) do (
        set "VAR_NAME=%%a"
        set "VAR_VALUE=%%b"
        REM Eliminar espacios en blanco al principio y al final
        set "VAR_NAME=!VAR_NAME: =!"
        set "VAR_VALUE=!VAR_VALUE: =!"
        
        if "!VAR_NAME!"=="DB_USER" set DB_USER=!VAR_VALUE!
        if "!VAR_NAME!"=="DB_PASS" set DB_PASS=!VAR_VALUE!
        if "!VAR_NAME!"=="DB_URL" (
            set DB_URL=!VAR_VALUE!
            for /F "tokens=3 delims=:/" %%x in ("!VAR_VALUE!") do set DB_HOST=%%x
            for /F "tokens=4 delims=:/" %%y in ("!VAR_VALUE!") do (
                for /F "tokens=1 delims=?" %%z in ("%%y") do set DB_PORT=%%z
            )
            for /F "tokens=5 delims=:/" %%w in ("!VAR_VALUE!") do (
                for /F "tokens=1 delims=?" %%v in ("%%w") do set DB_NAME=%%v
            )
        )
    )
)

echo Credenciales de base de datos (limpias de espacios):
echo URL: %DB_URL%
echo Host: %DB_HOST%
echo Puerto: %DB_PORT%
echo Base de datos: %DB_NAME%
echo Usuario: [%DB_USER%]

REM Crear archivo temporal SQL para crear la base de datos
echo CREATE DATABASE IF NOT EXISTS %DB_NAME% CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; > "%TEMP%\create_db.sql"

REM Intentar crear la base de datos
echo Intentando crear la base de datos %DB_NAME% si no existe...
mysql -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASS% --protocol=tcp < "%TEMP%\create_db.sql" 2>nul
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

REM Ejecutar la aplicación con los parámetros correctamente formateados
echo Ejecutando con estos parámetros:
echo java -jar app-pos-0.0.1-SNAPSHOT.jar --spring.datasource.url="[%DB_URL%]" --spring.datasource.username="[%DB_USER%]" --spring.datasource.password="[%DB_PASS%]"
java -jar app-pos-0.0.1-SNAPSHOT.jar --spring.datasource.url="%DB_URL%" --spring.datasource.username="%DB_USER%" --spring.datasource.password="%DB_PASS%"

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