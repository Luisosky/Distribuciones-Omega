@echo off
setlocal enabledelayedexpansion

:: Primero cambiamos al directorio donde se encuentra el script
cd /d "%~dp0"

:: Cambiar consola a Windows-1252 para acentos en español
chcp 1252 >nul

:: Si el script se está ejecutando con doble clic, activar modo de permanencia
echo %cmdcmdline% | find /i "%~0" >nul
if not errorlevel 1 set STAY_OPEN=1

:: Título para la ventana
title Instalador de Distribuciones Omega

:: Instalador para Windows de Distribuciones-Omega
echo Instalando Distribuciones-Omega...
echo Directorio actual: %CD%

:: Verificar si los archivos necesarios existen
echo.
echo Verificando archivos necesarios para la instalación...

set MISSING_FILES=0

if not exist "app-pos-0.0.1-SNAPSHOT.jar" (
    echo ERROR: No se encuentra el archivo app-pos-0.0.1-SNAPSHOT.jar en el directorio actual.
    echo Este archivo es NECESARIO para la instalación.
    set MISSING_FILES=1
)

if not exist "src\main\resources\images\logo.ico" (
    echo ADVERTENCIA: No se encuentra el archivo logo.ico en la ruta src\main\resources\images.
    echo El acceso directo se creará sin icono personalizado.
)

if %MISSING_FILES% equ 1 (
    echo.
    echo No se puede continuar con la instalación por falta de archivos esenciales.
    echo Por favor, asegúrese de que el archivo app-pos-0.0.1-SNAPSHOT.jar esté en la misma carpeta que este instalador.
    echo.
    echo Por favor, ejecute el instalador desde la carpeta que contiene todos los archivos necesarios.
    goto :error
)

:: Verificar Java
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java no está instalado. Por favor instale Java 17 o superior.
    goto :error
)

:: Verificar MySQL - Versión corregida
echo Verificando instalación de MySQL...
set MYSQL_FOUND=0

:: Verificar si MySQL está disponible en línea de comandos
mysql --version >nul 2>&1
if %errorlevel% equ 0 (
    set MYSQL_FOUND=1
    echo MySQL detectado en línea de comandos - OK
    goto :mysql_check_done
)

:: Verificar si existe el servicio MySQL80 (usado por MySQL Workbench)
sc query MySQL80 >nul 2>&1
if %errorlevel% equ 0 (
    set MYSQL_FOUND=1
    echo MySQL detectado como servicio MySQL80 - OK
    goto :mysql_check_done
)

:: Si llegamos aquí, MySQL no se detectó
echo ADVERTENCIA: MySQL no fue detectado desde la línea de comandos ni como servicio MySQL80.
echo Si tiene MySQL Workbench instalado pero no el cliente MySQL en el PATH, 
echo asegúrese de que el servidor MySQL esté en ejecución a través de:
echo - MySQL Workbench
echo - O el panel de Servicios de Windows (services.msc)

:mysql_check_done

:: Preguntar al usuario dónde quiere instalar
set INSTALL_DIR=%USERPROFILE%\Distribuciones-Omega
set /p CUSTOM_DIR="Presione Enter para instalar en %USERPROFILE%\Distribuciones-Omega o ingrese una ruta personalizada: "
if not "%CUSTOM_DIR%"=="" set INSTALL_DIR=%CUSTOM_DIR%

echo Instalando en %INSTALL_DIR%

:: Crear las carpetas necesarias
echo Creando estructura de directorios...
if not exist "%INSTALL_DIR%" (
    mkdir "%INSTALL_DIR%"
    if !errorlevel! neq 0 (
        echo ERROR: No se pudo crear el directorio de instalación.
        echo Verifique que tiene permisos de escritura en la ruta especificada.
        goto :error
    )
)

if not exist "%INSTALL_DIR%\src\main\resources\images" (
    mkdir "%INSTALL_DIR%\src\main\resources\images"
    if !errorlevel! neq 0 (
        echo ERROR: No se pudo crear la estructura de directorios.
        goto :error
    )
)

:: Copiar archivos con mensajes de confirmación
echo.
echo Copiando archivos de la aplicación...

echo - Copiando JAR principal...
copy "app-pos-0.0.1-SNAPSHOT.jar" "%INSTALL_DIR%\"
if !errorlevel! neq 0 (
    echo   ERROR: No se pudo copiar app-pos-0.0.1-SNAPSHOT.jar
    goto :error
)

if exist "README.md" (
    echo - Copiando README.md...
    copy "README.md" "%INSTALL_DIR%\"
    if !errorlevel! neq 0 echo   ADVERTENCIA: No se pudo copiar README.md
)

if exist "src\main\resources\images\logo.ico" (
    echo - Copiando logo.ico...
    copy "src\main\resources\images\logo.ico" "%INSTALL_DIR%\src\main\resources\images\"
    if !errorlevel! neq 0 echo   ADVERTENCIA: No se pudo copiar el logo.ico
)

:: Configuración de la base de datos
echo.
echo ------------------------------------------------
echo CONFIGURACION DE LA BASE DE DATOS
echo ------------------------------------------------
echo.

set DB_HOST=localhost
set DB_PORT=3306
set DB_NAME=omega
set DB_USER=root
set DB_PASS=root123

echo Valores predeterminados para la base de datos:
echo - Host: %DB_HOST%
echo - Puerto: %DB_PORT%
echo - Base de datos: %DB_NAME%
echo - Usuario: %DB_USER%
echo - Contraseña: %DB_PASS%
echo.

set /p CUSTOM_DB="¿Desea personalizar la configuración de la base de datos? (s/n): "
if /i "%CUSTOM_DB%"=="s" (
    set /p DB_HOST="Host de MySQL [%DB_HOST%]: "
    set /p DB_PORT="Puerto de MySQL [%DB_PORT%]: "
    set /p DB_NAME="Nombre de la base de datos [%DB_NAME%]: "
    set /p DB_USER="Usuario de MySQL [%DB_USER%]: "
    set /p DB_PASS="Contraseña de MySQL [%DB_PASS%]: "
)

:: Crear archivo .env sin espacios adicionales
echo Creando archivo .env con la configuración...
(
echo DB_URL=jdbc:mysql://%DB_HOST%:%DB_PORT%/%DB_NAME%?allowPublicKeyRetrieval=true^^^&useSSL=false
echo DB_USER=%DB_USER%
echo DB_PASS=%DB_PASS%
echo EMAIL=%EMAIL%
echo APP_PASS=%APP_PASS%
) > "%INSTALL_DIR%\.env"

:: Intentar crear la base de datos directamente
echo.
echo Intentando crear la base de datos %DB_NAME% si no existe...
echo CREATE DATABASE IF NOT EXISTS %DB_NAME% CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; > "%TEMP%\create_db.sql"
mysql -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASS% < "%TEMP%\create_db.sql" 2>"%TEMP%\mysql_error.log"
if %errorlevel% neq 0 (
    echo ADVERTENCIA: No se pudo crear automáticamente la base de datos.
    echo Motivo:
    type "%TEMP%\mysql_error.log"
    echo.
    echo Es posible que necesite crear manualmente la base de datos '%DB_NAME%'.
    echo Ejecute el siguiente comando en MySQL:
    echo CREATE DATABASE IF NOT EXISTS %DB_NAME% CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    echo.
) else (
    echo Base de datos %DB_NAME% verificada/creada correctamente.
)
del "%TEMP%\create_db.sql" >nul 2>&1
del "%TEMP%\mysql_error.log" >nul 2>&1

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
    
    :: Actualizar el archivo .env con la información de correo
    echo Actualizando archivo .env con datos de correo...
    echo DB_URL=jdbc:mysql://%DB_HOST%:%DB_PORT%/%DB_NAME%?allowPublicKeyRetrieval=true^^^&useSSL=false > "%INSTALL_DIR%\.env"
    echo DB_USER=%DB_USER% >> "%INSTALL_DIR%\.env"
    echo DB_PASS=%DB_PASS% >> "%INSTALL_DIR%\.env"
    echo EMAIL=%EMAIL% >> "%INSTALL_DIR%\.env"
    echo APP_PASS=%APP_PASS% >> "%INSTALL_DIR%\.env"
    
    echo.
    echo Se ha configurado el correo electrónico para notificaciones.
) else (
    echo.
    echo Las notificaciones por correo electrónico no estarán disponibles.
)

:: Crear script de ejecución con parámetros explícitos
echo.
echo Creando archivo ejecutar.bat...

echo @echo off > "%INSTALL_DIR%\ejecutar.bat"
echo setlocal enabledelayedexpansion >> "%INSTALL_DIR%\ejecutar.bat"
echo chcp 1252 ^>nul >> "%INSTALL_DIR%\ejecutar.bat"
echo. >> "%INSTALL_DIR%\ejecutar.bat"
echo echo. >> "%INSTALL_DIR%\ejecutar.bat"
echo echo ================================================ >> "%INSTALL_DIR%\ejecutar.bat"
echo echo      INICIANDO DISTRIBUCIONES OMEGA >> "%INSTALL_DIR%\ejecutar.bat"
echo echo ================================================ >> "%INSTALL_DIR%\ejecutar.bat"
echo echo. >> "%INSTALL_DIR%\ejecutar.bat"
echo. >> "%INSTALL_DIR%\ejecutar.bat"

echo REM Cargar las credenciales desde el archivo .env >> "%INSTALL_DIR%\ejecutar.bat"
echo set DB_URL=jdbc:mysql://localhost:3306/omega >> "%INSTALL_DIR%\ejecutar.bat"
echo set DB_USER=root >> "%INSTALL_DIR%\ejecutar.bat"
echo set DB_PASS=root123 >> "%INSTALL_DIR%\ejecutar.bat"
echo set DB_NAME=omega >> "%INSTALL_DIR%\ejecutar.bat"
echo set DB_HOST=localhost >> "%INSTALL_DIR%\ejecutar.bat"
echo set DB_PORT=3306 >> "%INSTALL_DIR%\ejecutar.bat"
echo. >> "%INSTALL_DIR%\ejecutar.bat"
echo if exist ".env" ^( >> "%INSTALL_DIR%\ejecutar.bat"
echo     for /F "tokens=1,2 delims==" %%%%a in ^(.env^) do ^( >> "%INSTALL_DIR%\ejecutar.bat"
echo         if "%%%%a"=="DB_USER" set DB_USER=%%%%b >> "%INSTALL_DIR%\ejecutar.bat"
echo         if "%%%%a"=="DB_PASS" set DB_PASS=%%%%b >> "%INSTALL_DIR%\ejecutar.bat"
echo         if "%%%%a"=="DB_URL" ^( >> "%INSTALL_DIR%\ejecutar.bat"
echo             set DB_URL=%%%%b >> "%INSTALL_DIR%\ejecutar.bat"
echo             for /F "tokens=3 delims=:/" %%%%x in ^("%%%%b"^) do set DB_HOST=%%%%x >> "%INSTALL_DIR%\ejecutar.bat"
echo             for /F "tokens=4 delims=:/" %%%%y in ^("%%%%b"^) do ^( >> "%INSTALL_DIR%\ejecutar.bat"
echo                 for /F "tokens=1 delims=?" %%%%z in ^("%%%%y"^) do set DB_PORT=%%%%z >> "%INSTALL_DIR%\ejecutar.bat"
echo             ^) >> "%INSTALL_DIR%\ejecutar.bat"
echo             for /F "tokens=5 delims=:/" %%%%w in ^("%%%%b"^) do ^( >> "%INSTALL_DIR%\ejecutar.bat"
echo                 for /F "tokens=1 delims=?" %%%%v in ^("%%%%w"^) do set DB_NAME=%%%%v >> "%INSTALL_DIR%\ejecutar.bat"
echo             ^) >> "%INSTALL_DIR%\ejecutar.bat"
echo         ^) >> "%INSTALL_DIR%\ejecutar.bat"
echo     ^) >> "%INSTALL_DIR%\ejecutar.bat"
echo ^) >> "%INSTALL_DIR%\ejecutar.bat"
echo. >> "%INSTALL_DIR%\ejecutar.bat"
echo echo Usando las siguientes credenciales de base de datos: >> "%INSTALL_DIR%\ejecutar.bat"
echo echo URL: %%DB_URL%% >> "%INSTALL_DIR%\ejecutar.bat"
echo echo Host: %%DB_HOST%% >> "%INSTALL_DIR%\ejecutar.bat"
echo echo Puerto: %%DB_PORT%% >> "%INSTALL_DIR%\ejecutar.bat"
echo echo Base de datos: %%DB_NAME%% >> "%INSTALL_DIR%\ejecutar.bat"
echo echo Usuario: %%DB_USER%% >> "%INSTALL_DIR%\ejecutar.bat"
echo. >> "%INSTALL_DIR%\ejecutar.bat"

echo REM Crear archivo temporal SQL para crear la base de datos >> "%INSTALL_DIR%\ejecutar.bat"
echo echo CREATE DATABASE IF NOT EXISTS %%DB_NAME%% CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; ^> "%%TEMP%%\create_db.sql" >> "%INSTALL_DIR%\ejecutar.bat"
echo. >> "%INSTALL_DIR%\ejecutar.bat"
echo REM Intentar crear la base de datos >> "%INSTALL_DIR%\ejecutar.bat"
echo echo Intentando crear la base de datos %%DB_NAME%% si no existe... >> "%INSTALL_DIR%\ejecutar.bat"
echo mysql -h%%DB_HOST%% -P%%DB_PORT%% -u%%DB_USER%% -p%%DB_PASS%% --protocol=tcp ^< "%%TEMP%%\create_db.sql" 2^>nul >> "%INSTALL_DIR%\ejecutar.bat"
echo if %%errorlevel%% neq 0 ^( >> "%INSTALL_DIR%\ejecutar.bat"
echo     echo ADVERTENCIA: No se pudo crear automáticamente la base de datos. >> "%INSTALL_DIR%\ejecutar.bat"
echo     echo Es posible que necesite crear manualmente la base de datos '%%DB_NAME%%'. >> "%INSTALL_DIR%\ejecutar.bat"
echo     echo Ejecute el siguiente comando en MySQL: >> "%INSTALL_DIR%\ejecutar.bat"
echo     echo CREATE DATABASE IF NOT EXISTS %%DB_NAME%% CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; >> "%INSTALL_DIR%\ejecutar.bat"
echo     echo. >> "%INSTALL_DIR%\ejecutar.bat"
echo ^) else ^( >> "%INSTALL_DIR%\ejecutar.bat"
echo     echo Base de datos verificada correctamente. >> "%INSTALL_DIR%\ejecutar.bat"
echo ^) >> "%INSTALL_DIR%\ejecutar.bat"
echo. >> "%INSTALL_DIR%\ejecutar.bat"
echo REM Eliminar el archivo temporal >> "%INSTALL_DIR%\ejecutar.bat"
echo del "%%TEMP%%\create_db.sql" ^>nul 2^>^&1 >> "%INSTALL_DIR%\ejecutar.bat"
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

echo REM Ejecutar la aplicación con parámetros explícitos >> "%INSTALL_DIR%\ejecutar.bat"
echo java -jar app-pos-0.0.1-SNAPSHOT.jar --spring.datasource.url="%%DB_URL%%" --spring.datasource.username="%%DB_USER%%" --spring.datasource.password="%%DB_PASS%%" >> "%INSTALL_DIR%\ejecutar.bat"
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

:: Verificar si se creó correctamente el ejecutar.bat
if not exist "%INSTALL_DIR%\ejecutar.bat" (
    echo ERROR: No se pudo crear el archivo ejecutar.bat.
    goto :error
)

:: Crear acceso directo en escritorio con método alternativo
echo Creando acceso directo en escritorio...
echo @echo off > "%TEMP%\CreateDesktopShortcut.cmd"
echo setlocal enabledelayedexpansion >> "%TEMP%\CreateDesktopShortcut.cmd"
echo set SHORTCUT="%USERPROFILE%\Desktop\Distribuciones Omega.lnk" >> "%TEMP%\CreateDesktopShortcut.cmd"
echo set TARGET="%INSTALL_DIR%\ejecutar.bat" >> "%TEMP%\CreateDesktopShortcut.cmd"
echo set ICON="%INSTALL_DIR%\src\main\resources\images\logo.ico" >> "%TEMP%\CreateDesktopShortcut.cmd"
echo set PWD="%INSTALL_DIR%" >> "%TEMP%\CreateDesktopShortcut.cmd"
echo set SCRIPT="%TEMP%\tempVBS.vbs" >> "%TEMP%\CreateDesktopShortcut.cmd"
echo. >> "%TEMP%\CreateDesktopShortcut.cmd"
echo echo Set oWS = WScript.CreateObject^("WScript.Shell"^) ^> %%SCRIPT%% >> "%TEMP%\CreateDesktopShortcut.cmd"
echo echo Set oLink = oWS.CreateShortcut^(%%SHORTCUT%%^) ^>^> %%SCRIPT%% >> "%TEMP%\CreateDesktopShortcut.cmd"
echo echo oLink.TargetPath = %%TARGET%% ^>^> %%SCRIPT%% >> "%TEMP%\CreateDesktopShortcut.cmd"
echo echo oLink.WorkingDirectory = %%PWD%% ^>^> %%SCRIPT%% >> "%TEMP%\CreateDesktopShortcut.cmd"
echo echo oLink.Description = "Distribuciones Omega" ^>^> %%SCRIPT%% >> "%TEMP%\CreateDesktopShortcut.cmd"
echo echo oLink.WindowStyle = 1 ^>^> %%SCRIPT%% >> "%TEMP%\CreateDesktopShortcut.cmd"
echo if exist "%INSTALL_DIR%\src\main\resources\images\logo.ico" echo echo oLink.IconLocation = %%ICON%% ^>^> %%SCRIPT%% >> "%TEMP%\CreateDesktopShortcut.cmd"
echo echo oLink.Save ^>^> %%SCRIPT%% >> "%TEMP%\CreateDesktopShortcut.cmd"
echo cscript //nologo %%SCRIPT%% >> "%TEMP%\CreateDesktopShortcut.cmd"
echo del %%SCRIPT%% >> "%TEMP%\CreateDesktopShortcut.cmd"

:: Ejecutar el script con derechos elevados para crear el acceso directo
echo Set UAC = CreateObject^("Shell.Application"^) > "%TEMP%\ElevateShortcut.vbs"
echo UAC.ShellExecute "%TEMP%\CreateDesktopShortcut.cmd", "", "", "runas", 1 >> "%TEMP%\ElevateShortcut.vbs"
cscript //nologo "%TEMP%\ElevateShortcut.vbs"
del "%TEMP%\ElevateShortcut.vbs"

:: Esperar un poco para que se complete la creación del acceso directo
timeout /t 2 >nul

:: Crear acceso directo en el menú inicio (usando el mismo método mejorado)
echo Creando acceso directo en menú inicio...
echo @echo off > "%TEMP%\CreateStartMenuShortcut.cmd"
echo setlocal enabledelayedexpansion >> "%TEMP%\CreateStartMenuShortcut.cmd"
echo set SHORTCUT="%APPDATA%\Microsoft\Windows\Start Menu\Programs\Distribuciones Omega.lnk" >> "%TEMP%\CreateStartMenuShortcut.cmd"
echo set TARGET="%INSTALL_DIR%\ejecutar.bat" >> "%TEMP%\CreateStartMenuShortcut.cmd"
echo set ICON="%INSTALL_DIR%\src\main\resources\images\logo.ico" >> "%TEMP%\CreateStartMenuShortcut.cmd"
echo set PWD="%INSTALL_DIR%" >> "%TEMP%\CreateStartMenuShortcut.cmd"
echo set SCRIPT="%TEMP%\tempStartVBS.vbs" >> "%TEMP%\CreateStartMenuShortcut.cmd"
echo. >> "%TEMP%\CreateStartMenuShortcut.cmd"
echo echo Set oWS = WScript.CreateObject^("WScript.Shell"^) ^> %%SCRIPT%% >> "%TEMP%\CreateStartMenuShortcut.cmd"
echo echo Set oLink = oWS.CreateShortcut^(%%SHORTCUT%%^) ^>^> %%SCRIPT%% >> "%TEMP%\CreateStartMenuShortcut.cmd"
echo echo oLink.TargetPath = %%TARGET%% ^>^> %%SCRIPT%% >> "%TEMP%\CreateStartMenuShortcut.cmd"
echo echo oLink.WorkingDirectory = %%PWD%% ^>^> %%SCRIPT%% >> "%TEMP%\CreateStartMenuShortcut.cmd"
echo echo oLink.Description = "Distribuciones Omega" ^>^> %%SCRIPT%% >> "%TEMP%\CreateStartMenuShortcut.cmd"
echo echo oLink.WindowStyle = 1 ^>^> %%SCRIPT%% >> "%TEMP%\CreateStartMenuShortcut.cmd"
echo if exist "%INSTALL_DIR%\src\main\resources\images\logo.ico" echo echo oLink.IconLocation = %%ICON%% ^>^> %%SCRIPT%% >> "%TEMP%\CreateStartMenuShortcut.cmd"
echo echo oLink.Save ^>^> %%SCRIPT%% >> "%TEMP%\CreateStartMenuShortcut.cmd"
echo cscript //nologo %%SCRIPT%% >> "%TEMP%\CreateStartMenuShortcut.cmd"
echo del %%SCRIPT%% >> "%TEMP%\CreateStartMenuShortcut.cmd"

:: Ejecutar el script con derechos elevados para crear el acceso directo
echo Set UAC = CreateObject^("Shell.Application"^) > "%TEMP%\ElevateStartMenuShortcut.vbs"
echo UAC.ShellExecute "%TEMP%\CreateStartMenuShortcut.cmd", "", "", "runas", 1 >> "%TEMP%\ElevateStartMenuShortcut.vbs"
cscript //nologo "%TEMP%\ElevateStartMenuShortcut.vbs"
del "%TEMP%\ElevateStartMenuShortcut.vbs"

:: Esperar un poco para que se complete la creación del acceso directo
timeout /t 2 >nul

echo.
echo Instalación completada exitosamente!
echo.
echo NOTA: Si los accesos directos no se crearon correctamente,
echo puede ejecutar manualmente el programa desde:
echo %INSTALL_DIR%\ejecutar.bat
echo.
echo Base de datos configurada:
echo - Host: %DB_HOST%:%DB_PORT%
echo - Base de datos: %DB_NAME%
echo - Usuario: %DB_USER%
echo - Contraseña: %DB_PASS%
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
echo Presione cualquier tecla para salir...
pause >nul
exit /b 1

:end
:: Si se ejecutó con doble clic, mantener abierto
if defined STAY_OPEN (
    echo.
    echo Presione cualquier tecla para cerrar esta ventana...
    pause >nul
)