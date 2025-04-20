@echo off
echo Compilando y luego instalando Distribuciones-Omega...
call mvn clean package
if %errorlevel% neq 0 (
  echo Error al compilar. Instalación abortada.
  pause
  exit /b %errorlevel%
)
call install.bat