# Distribuciones-Omega

Sistema integral de gestión para distribuidoras mayoristas.

## Características principales

- Gestión de inventario y productos
- Control de ventas y facturación
- Administración de clientes y proveedores
- Reportes y estadísticas de ventas
- Control de usuarios y permisos

## Requisitos previos

- Java 17 o superior
- MySQL 5.7 o superior
- Maven 3.6 o superior (solo necesario para desarrollo o compilación desde código fuente)

## Instalación

### Opción 1: Instalador para Windows (Recomendado)

1. Descargue el [instalador desde la página de releases](https://github.com/Luisosky/Distribuciones-Omega/releases)
2. Ejecute `install.bat` haciendo doble clic o desde la consola
3. Siga las instrucciones en pantalla para completar la instalación
4. Al finalizar, encontrará accesos directos en el Escritorio y en el Menú de Inicio

### Opción 2: Instalación desde código fuente

1. Clone el repositorio:
   ```
   git clone https://github.com/luisosky/Distribuciones-Omega.git
   ```

2. Acceda al directorio del proyecto:
   ```
   cd Distribuciones-Omega
   ```

3. Compile el proyecto con Maven:
   ```
   mvn clean package -DskipTests
   ```

4. Ejecute el instalador:
   ```
   install.bat
   ```

### Opción 3: Instalación en Linux/Mac

1. Clone o descargue el repositorio
2. Otorgue permisos de ejecución al script de instalación:
   ```bash
   chmod +x install.sh
   ```
3. Ejecute el instalador:
   ```bash
   ./install.sh
   ```

### Opción 4: Instalación con Docker

Si prefiere utilizar contenedores Docker:

```bash
docker-compose up -d
```

Esto iniciará tanto la aplicación como una base de datos MySQL en contenedores separados.
Para acceder a la aplicación visite: http://localhost:8080

## Configuración

### Base de datos
- **Host**: localhost (por defecto)
- **Puerto**: 3306 (por defecto)
- **Usuario**: root
- **Contraseña**: root
- **Base de datos**: omega_db (se crea automáticamente)

Para modificar estos valores, edite el archivo `application.properties`.

## Primer inicio

Al iniciar la aplicación por primera vez:

1. Se creará automáticamente la estructura de la base de datos
2. Se insertarán datos de ejemplo para facilitar las pruebas
3. Utilice las siguientes credenciales para ingresar:
   - **Usuario**: admin
   - **Contraseña**: admin123

## Solución de problemas

Si encuentra problemas durante la instalación o ejecución:

1. **Error de Java no encontrado**: Asegúrese de tener Java 17 o superior instalado y en el PATH
2. **Error de MySQL**: Verifique que MySQL esté instalado y en ejecución
3. **Error de compilación**: Si compila desde el código fuente, asegúrese de tener Maven instalado
4. **Error de conexión**: Verifique que los datos de conexión en `application.properties` sean correctos

## Soporte

Si necesita ayuda, puede:
- Crear un [issue en GitHub](https://github.com/Luisosky/Distribuciones-Omega/issues)
- Contactar al equipo de soporte en: luisc.calderonc@uqvirtual.edu.co

## Licencia

Este proyecto está disponible bajo la licencia MIT.

