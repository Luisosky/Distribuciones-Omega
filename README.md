# Distribuciones-Omega

Sistema integral de gestión para distribuidoras mayoristas.

## Características principales

- Gestión de inventario y productos
- Control de ventas y facturación
- Administración de clientes y proveedores
- Reportes y estadísticas de ventas
- Control de usuarios y permisos

## Instalación

### Requisitos previos
- Java 17 o superior
- MySQL 5.7 o superior

### Opciones de instalación

#### 1. Instalación rápida con script (Linux/Mac)
```bash
chmod +x install.sh
./install.sh
```

#### 2. Instalación para Windows
Ejecute el archivo install.bat con doble clic o mediante la consola:
```bash
install.bat
```

#### Configuración
Base de datos
- Host: localhost (por defecto)
- Puerto: 3306 (por defecto)
- Usuario: root
- Contraseña: root
- Base de datos: omega_db (se crea automáticamente)
- Para modificar estos valores, edita el archivo application.properties.

#### Uso inicial
Al iniciar por primera vez, utilice las siguientes credenciales:

- Usuario: admin
- Contraseña: admin123

#### 3. Instalación con Docker
Si tienes Docker y Docker Compose instalados, puedes ejecutar:
```bash
docker-compose up -d
```

Esto iniciará tanto la aplicación como una base de datos MySQL en contenedores separados. Para acceder a la aplicación visita: http://localhost:8080