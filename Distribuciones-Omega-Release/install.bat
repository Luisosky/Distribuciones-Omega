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

:: Verificar espacio en disco
for /f "tokens=3" %%a in ('dir /s /-c "%~dp0" ^| find "bytes"') do set SIZE=%%a
for /f "tokens=3" %%a in ('dir /-c "%USERPROFILE%\" ^| find "bytes free"') do set FREE=%%a
if %FREE% LSS %SIZE% (
    echo ERROR: No hay suficiente espacio en disco para instalar la aplicación.
    pause
    exit /b 1
)

:: Preguntar al usuario dónde quiere instalar
set INSTALL_DIR=%USERPROFILE%\Distribuciones-Omega
set /p CUSTOM_DIR="Presione Enter para instalar en %USERPROFILE%\Distribuciones-Omega o ingrese una ruta personalizada: "
if not "%CUSTOM_DIR%"=="" set INSTALL_DIR=%CUSTOM_DIR%

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
echo Instalacion completada! Puede encontrar accesos directos en:
echo - Escritorio
echo - Menú de inicio
echo.
pause