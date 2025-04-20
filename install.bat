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

:: Crear acceso directo en escritorio
echo Set oWS = WScript.CreateObject("WScript.Shell") > "%TEMP%\CreateShortcut.vbs"
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

echo.
echo Instalacion completada! Puede encontrar un acceso directo en su escritorio.
echo.
pause