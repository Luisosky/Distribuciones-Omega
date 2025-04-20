#!/bin/bash
# install.sh - Instalador para Distribuciones-Omega

echo "Instalando Distribuciones-Omega..."

# Verificar Java
if ! command -v java &> /dev/null; then
    echo "ERROR: Java no está instalado. Por favor instale Java 11 o superior."
    exit 1
fi

# Verificar MySQL
if ! command -v mysql &> /dev/null; then
    echo "ADVERTENCIA: MySQL no fue detectado. Asegúrese de tener MySQL instalado."
fi

# Crear directorio de aplicación
mkdir -p ~/distribuciones-omega
cp -r ./* ~/distribuciones-omega/

# Crear acceso directo en escritorio
echo "[Desktop Entry]
Name=Distribuciones Omega
Exec=java -jar ~/distribuciones-omega/distribuciones-omega.jar
Icon=~/distribuciones-omega/src/main/resources/images/logo.png
Type=Application
Terminal=false" > ~/Desktop/DistribucionesOmega.desktop

chmod +x ~/Desktop/DistribucionesOmega.desktop

echo "¡Instalación completada! Puede encontrar un acceso directo en su escritorio."