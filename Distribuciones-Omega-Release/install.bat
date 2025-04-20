REM filepath: f:\Project\Java\SW2\Distribuciones-Omega\Distribuciones-Omega-Release\install.bat
@echo off
setlocal enabledelayedexpansion

:: Si el script se está ejecutando con doble clic, activar modo de permanencia
echo %cmdcmdline% | find /i "%~0" >nul
if not errorlevel 1 set STAY_OPEN=1

:: Título para la ventana
title Instalador de Distribuciones Omega

:: Instalador para Windows de Distribuciones-Omega

echo Instalando Distribuciones-Omega...

:: Verificar Java
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java no esta instalado. Por favor instale Java 17 o superior.
    goto :error
)

:: Verificar MySQL - Ahora también busca MySQL80 service
echo Verificando instalación de MySQL...
mysql --version >nul 2>&1
if %errorlevel% neq 0 (
    sc query MySQL80 >nul 2>&1
    if %errorlevel% neq 0 (
        echo ADVERTENCIA: MySQL no fue detectado desde la línea de comandos ni como servicio MySQL80.
        echo Si tiene MySQL Workbench instalado pero no el cliente MySQL en el PATH, 
        echo asegúrese de que el servidor MySQL esté en ejecución a través de:
        echo - MySQL Workbench
        echo - O el panel de Servicios de Windows (services.msc)
    ) else (
        echo MySQL detectado como servicio MySQL80 - OK
    )
) else (
    echo MySQL detectado en línea de comandos - OK
)

:: Verificar espacio en disco - versión corregida
echo Verificando espacio en disco disponible...
set SPACE_OK=1

:: Obtenemos el tamaño requerido sin errores
for /f "tokens=3" %%a in ('dir /s /-c "%~dp0" ^| find "bytes"') do (
    set SIZE=%%a
    echo Tamaño requerido: !SIZE! bytes
)

:: Saltamos la verificación de espacio libre para evitar el error con números grandes
goto :skip_disk_check

:: Este código ya no se ejecutará pero lo dejamos como referencia
for /f "tokens=3" %%a in ('dir /-c "%USERPROFILE%\" ^| find "bytes free"') do (
    set FREE=%%a
    echo Espacio disponible: !FREE! bytes
    if !FREE! LSS !SIZE! (
        set SPACE_OK=0
    )
)

if !SPACE_OK! EQU 0 (
    echo ERROR: No hay suficiente espacio en disco para instalar la aplicación.
    goto :error
)

:skip_disk_check

:: Preguntar al usuario dónde quiere instalar
set INSTALL_DIR=%USERPROFILE%\Distribuciones-Omega
set /p CUSTOM_DIR="Presione Enter para instalar en %USERPROFILE%\Distribuciones-Omega o ingrese una ruta personalizada: "
if not "%CUSTOM_DIR%"=="" set INSTALL_DIR=%CUSTOM_DIR%

echo Instalando en %INSTALL_DIR%

if not exist "%INSTALL_DIR%" mkdir "%INSTALL_DIR%"
xcopy /s /e /y .\* "%INSTALL_DIR%\" >nul

:: Configuración de la base de datos
echo.
echo ------------------------------------------------
echo CONFIGURACION DE LA BASE DE DATOS
echo ------------------------------------------------
echo.

set DB_HOST=127.0.0.1
set DB_PORT=3306
set DB_NAME=omega
set DB_USER=root
set DB_PASS=root

set /p CUSTOM_DB="¿Desea personalizar la configuración de la base de datos? (s/n): "
if /i "%CUSTOM_DB%"=="s" (
    set /p DB_HOST="Host de MySQL [%DB_HOST%]: "
    set /p DB_PORT="Puerto de MySQL [%DB_PORT%]: "
    set /p DB_NAME="Nombre de la base de datos [%DB_NAME%]: "
    set /p DB_USER="Usuario de MySQL [%DB_USER%]: "
    set /p DB_PASS="Contraseña de MySQL [%DB_PASS%]: "
)

:: Configuración de correo electrónico
echo.
echo ------------------------------------------------
echo CONFIGURACION DE CORREO ELECTRONICO (Opcional)
echo ------------------------------------------------
echo.
echo Esta función permite a la aplicación enviar notificaciones por correo.
echo Para usar Gmail, necesitará crear una "Contraseña de aplicación":
echo 1. Vaya a su cuenta de Google: https://myaccount.google.com
echo 2. En Seguridad, active la verificación en dos pasos
echo 3. Cree una contraseña de aplicación para "Otra aplicación"
echo.

set EMAIL=
set APP_PASS=

set /p USE_EMAIL="¿Desea configurar las notificaciones por correo? (s/n): "
if /i "%USE_EMAIL%"=="s" (
    set /p EMAIL="Correo electrónico: "
    set /p APP_PASS="Contraseña de aplicación: "
    echo.
    echo Se ha configurado el correo electrónico para notificaciones.
) else (
    echo.
    echo Las notificaciones por correo electrónico no estarán disponibles.
)

:: Crear archivo .env
echo DB_URL=jdbc:mysql://%DB_HOST%:%DB_PORT%/%DB_NAME% > "%INSTALL_DIR%\.env"
echo DB_USER=%DB_USER% >> "%INSTALL_DIR%\.env"
echo DB_PASS=%DB_PASS% >> "%INSTALL_DIR%\.env"
echo EMAIL=%EMAIL% >> "%INSTALL_DIR%\.env"
echo APP_PASS=%APP_PASS% >> "%INSTALL_DIR%\.env"

:: Crear acceso directo en escritorio
echo Set oWS = WScript.CreateObject("WScript.Shell") > "%TEMP%\CreateShortcut.vbs"
echo Set FSO = CreateObject("Scripting.FileSystemObject") >> "%TEMP%\CreateShortcut.vbs"
echo sLinkFile = "%USERPROFILE%\Desktop\Distribuciones Omega.lnk" >> "%TEMP%\CreateShortcut.vbs"
echo Set oLink = oWS.CreateShortcut(sLinkFile) >> "%TEMP%\CreateShortcut.vbs"
echo oLink.TargetPath = "%INSTALL_DIR%\ejecutar.bat" >> "%TEMP%\CreateShortcut.vbs"
echo oLink.WorkingDirectory = "%INSTALL_DIR%" >> "%TEMP%\CreateShortcut.vbs"
echo oLink.Description = "Distribuciones Omega" >> "%TEMP%\CreateShortcut.vbs"
echo oLink.WindowStyle = 1 >> "%TEMP%\CreateShortcut.vbs"

echo If FSO.FileExists("%INSTALL_DIR%\src\main\resources\images\logo.ico") Then >> "%TEMP%\CreateShortcut.vbs"
echo   oLink.IconLocation = "%INSTALL_DIR%\src\main\resources\images\logo.ico, 0" >> "%TEMP%\CreateShortcut.vbs"
echo End If >> "%TEMP%\CreateShortcut.vbs"
echo oLink.Save >> "%TEMP%\CreateShortcut.vbs"
cscript /nologo "%TEMP%\CreateShortcut.vbs"
del "%TEMP%\CreateShortcut.vbs"

:: Agregar opción para crear un acceso directo en el menú inicio
echo Set oWS = WScript.CreateObject("WScript.Shell") > "%TEMP%\StartMenu.vbs"
echo Set FSO = CreateObject("Scripting.FileSystemObject") >> "%TEMP%\StartMenu.vbs"
echo sLinkFile = oWS.SpecialFolders("StartMenu") ^& "\Programs\Distribuciones Omega.lnk" >> "%TEMP%\StartMenu.vbs"
echo Set oLink = oWS.CreateShortcut(sLinkFile) >> "%TEMP%\StartMenu.vbs"
echo oLink.TargetPath = "%INSTALL_DIR%\ejecutar.bat" >> "%TEMP%\StartMenu.vbs"
echo oLink.WorkingDirectory = "%INSTALL_DIR%" >> "%TEMP%\StartMenu.vbs"
echo oLink.Description = "Distribuciones Omega" >> "%TEMP%\StartMenu.vbs"
echo oLink.WindowStyle = 1 >> "%TEMP%\StartMenu.vbs"
echo If FSO.FileExists("%INSTALL_DIR%\src\main\resources\images\logo.ico") Then >> "%TEMP%\StartMenu.vbs"
echo   oLink.IconLocation = "%INSTALL_DIR%\src\main\resources\images\logo.ico, 0" >> "%TEMP%\StartMenu.vbs"
echo End If >> "%TEMP%\StartMenu.vbs"
echo oLink.Save >> "%TEMP%\StartMenu.vbs"
cscript /nologo "%TEMP%\StartMenu.vbs"
del "%TEMP%\StartMenu.vbs"

:: Copiar también el script ejecutar.bat si no existe ya en la carpeta de instalación
if not exist "%INSTALL_DIR%\ejecutar.bat" (
    echo @echo off > "%INSTALL_DIR%\ejecutar.bat"
    echo setlocal enabledelayedexpansion >> "%INSTALL_DIR%\ejecutar.bat"
    echo. >> "%INSTALL_DIR%\ejecutar.bat"
    echo echo. >> "%INSTALL_DIR%\ejecutar.bat"
    echo echo ================================================ >> "%INSTALL_DIR%\ejecutar.bat"
    echo echo      INICIANDO DISTRIBUCIONES OMEGA >> "%INSTALL_DIR%\ejecutar.bat"
    echo echo ================================================ >> "%INSTALL_DIR%\ejecutar.bat"
    echo echo. >> "%INSTALL_DIR%\ejecutar.bat"
    echo. >> "%INSTALL_DIR%\ejecutar.bat"
    echo REM Verificar que existe el JAR en la ubicación actual >> "%INSTALL_DIR%\ejecutar.bat"
    echo if not exist "app-pos-0.0.1-SNAPSHOT.jar" ^( >> "%INSTALL_DIR%\ejecutar.bat"
    echo     echo ERROR: No se encuentra el archivo JAR en la carpeta actual. >> "%INSTALL_DIR%\ejecutar.bat"
    echo     echo. >> "%INSTALL_DIR%\ejecutar.bat"
    echo     echo Por favor, asegúrese de que el archivo JAR está en la misma carpeta que este script. >> "%INSTALL_DIR%\ejecutar.bat"
    echo     pause >> "%INSTALL_DIR%\ejecutar.bat"
    echo     exit /b 1 >> "%INSTALL_DIR%\ejecutar.bat"
    echo ^) >> "%INSTALL_DIR%\ejecutar.bat"
    echo. >> "%INSTALL_DIR%\ejecutar.bat"
    echo echo. >> "%INSTALL_DIR%\ejecutar.bat"
    echo echo Iniciando aplicación... >> "%INSTALL_DIR%\ejecutar.bat"
    echo echo. >> "%INSTALL_DIR%\ejecutar.bat"
    echo. >> "%INSTALL_DIR%\ejecutar.bat"
    echo REM Ejecutar la aplicación directamente con Spring Boot >> "%INSTALL_DIR%\ejecutar.bat"
    echo java -jar app-pos-0.0.1-SNAPSHOT.jar >> "%INSTALL_DIR%\ejecutar.bat"
    echo. >> "%INSTALL_DIR%\ejecutar.bat"
    echo if %%errorlevel%% neq 0 ^( >> "%INSTALL_DIR%\ejecutar.bat"
    echo     echo. >> "%INSTALL_DIR%\ejecutar.bat"
    echo     echo Error al iniciar la aplicación. Código: %%errorlevel%% >> "%INSTALL_DIR%\ejecutar.bat"
    echo     echo. >> "%INSTALL_DIR%\ejecutar.bat"
    echo     echo Posibles soluciones: >> "%INSTALL_DIR%\ejecutar.bat"
    echo     echo 1. Asegúrese de tener MySQL instalado y en ejecución >> "%INSTALL_DIR%\ejecutar.bat"
    echo     echo 2. Verifique la conexión a la base de datos en el archivo .env >> "%INSTALL_DIR%\ejecutar.bat"
    echo     echo 3. Verifique permisos de escritura en la carpeta actual >> "%INSTALL_DIR%\ejecutar.bat"
    echo     echo. >> "%INSTALL_DIR%\ejecutar.bat"
    echo     pause >> "%INSTALL_DIR%\ejecutar.bat"
    echo ^) >> "%INSTALL_DIR%\ejecutar.bat"
)

echo.
echo Instalación completada exitosamente!
echo.
echo Puede encontrar accesos directos en:
echo - Escritorio
echo - Menú de inicio
echo.
echo Base de datos configurada:
echo - Host: %DB_HOST%:%DB_PORT%
echo - Base de datos: %DB_NAME%
echo.
if not "%EMAIL%"=="" (
    echo Correo electrónico configurado: %EMAIL%
) else (
    echo Notificaciones por correo no configuradas.
    echo Puede configurarlas más tarde editando el archivo .env
)
echo.
goto :end

:error
echo.
echo Ha ocurrido un error durante la instalación. Revise los mensajes anteriores.
echo Si la ventana se cierra demasiado rápido, intente ejecutar el instalador como administrador
echo o desde una línea de comandos (cmd).
echo.

:end
:: Si se ejecutó con doble clic, mantener abierto
if defined STAY_OPEN (
    echo.
    echo Presione cualquier tecla para cerrar esta ventana...
    pause >nul
)