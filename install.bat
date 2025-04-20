@echo off
:: Instalador para Windows de Distribuciones-Omega

echo Instalando Distribuciones-Omega...

:: Verificar Java
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java no esta instalado. Por favor instale Java 17 o superior.
    pause
    exit /b 1
)

:: Verificar MySQL
mysql --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ADVERTENCIA: MySQL no fue detectado. Asegurese de tener MySQL instalado.
)

:: Crear directorio de aplicación
set INSTALL_DIR=%USERPROFILE%\Distribuciones-Omega
echo Instalando en %INSTALL_DIR%

if not exist "%INSTALL_DIR%" mkdir "%INSTALL_DIR%"
xcopy /s /e /y .\* "%INSTALL_DIR%\" >nul

:: Crear acceso directo en escritorio - CORREGIDO
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
    echo REM Verificar que existe el JAR >> "%INSTALL_DIR%\ejecutar.bat"
    echo if not exist "target\app-pos-0.0.1-SNAPSHOT.jar" ^( >> "%INSTALL_DIR%\ejecutar.bat"
    echo     echo ERROR: No se encuentra el archivo JAR. >> "%INSTALL_DIR%\ejecutar.bat"
    echo     echo. >> "%INSTALL_DIR%\ejecutar.bat"
    echo     echo Compilando el proyecto... >> "%INSTALL_DIR%\ejecutar.bat"
    echo     call mvn clean package -DskipTests >> "%INSTALL_DIR%\ejecutar.bat"
    echo. >> "%INSTALL_DIR%\ejecutar.bat"
    echo     if %%errorlevel%% neq 0 ^( >> "%INSTALL_DIR%\ejecutar.bat"
    echo         echo. >> "%INSTALL_DIR%\ejecutar.bat"
    echo         echo ERROR: Falló la compilación del proyecto. >> "%INSTALL_DIR%\ejecutar.bat"
    echo         pause >> "%INSTALL_DIR%\ejecutar.bat"
    echo         exit /b 1 >> "%INSTALL_DIR%\ejecutar.bat"
    echo     ^) >> "%INSTALL_DIR%\ejecutar.bat"
    echo ^) >> "%INSTALL_DIR%\ejecutar.bat"
    echo. >> "%INSTALL_DIR%\ejecutar.bat"
    echo echo. >> "%INSTALL_DIR%\ejecutar.bat"
    echo echo Iniciando aplicación... >> "%INSTALL_DIR%\ejecutar.bat"
    echo echo. >> "%INSTALL_DIR%\ejecutar.bat"
    echo. >> "%INSTALL_DIR%\ejecutar.bat"
    echo REM Ejecutar la aplicación directamente con Spring Boot >> "%INSTALL_DIR%\ejecutar.bat"
    echo java -jar target\app-pos-0.0.1-SNAPSHOT.jar >> "%INSTALL_DIR%\ejecutar.bat"
    echo. >> "%INSTALL_DIR%\ejecutar.bat"
    echo if %%errorlevel%% neq 0 ^( >> "%INSTALL_DIR%\ejecutar.bat"
    echo     echo. >> "%INSTALL_DIR%\ejecutar.bat"
    echo     echo Error al iniciar la aplicación. Código: %%errorlevel%% >> "%INSTALL_DIR%\ejecutar.bat"
    echo     echo. >> "%INSTALL_DIR%\ejecutar.bat"
    echo     echo Posibles soluciones: >> "%INSTALL_DIR%\ejecutar.bat"
    echo     echo 1. Asegúrese de tener MySQL instalado y en ejecución >> "%INSTALL_DIR%\ejecutar.bat"
    echo     echo 2. Verifique la conexión a la base de datos >> "%INSTALL_DIR%\ejecutar.bat"
    echo     echo 3. Verifique permisos de escritura en la carpeta actual >> "%INSTALL_DIR%\ejecutar.bat"
    echo     echo. >> "%INSTALL_DIR%\ejecutar.bat"
    echo     pause >> "%INSTALL_DIR%\ejecutar.bat"
    echo ^) >> "%INSTALL_DIR%\ejecutar.bat"
)

echo.
echo Instalacion completada! Puede encontrar un acceso directo en su escritorio.
echo.
pause