# Configuración de MySQL

## Requisitos Previos

1. **MySQL Server** instalado y ejecutándose
2. Usuario con permisos para crear bases de datos

## Pasos de Configuración

### 1. Crear la Base de Datos

Conéctate a MySQL y crea la base de datos:

```sql
CREATE DATABASE whatsapp_clone;
```

O déjala que se cree automáticamente (la aplicación la creará si no existe).

### 2. Configurar Credenciales

Crea un archivo `db.properties` en la raíz del proyecto con el siguiente contenido:

```properties
db.host=localhost
db.port=3306
db.database=whatsapp_clone
db.username=root
db.password=tu_contraseña
```

**Nota**: Si no creas el archivo `db.properties`, la aplicación usará valores por defecto:
- Host: localhost
- Puerto: 3306
- Base de datos: whatsapp_clone
- Usuario: root
- Contraseña: (vacía)

### 3. Verificar Conexión

Ejecuta la aplicación. Si hay problemas de conexión, verifica:

1. Que MySQL esté ejecutándose:
   ```bash
   # Windows
   net start MySQL80
   
   # Linux/Mac
   sudo systemctl status mysql
   ```

2. Que el usuario tenga permisos:
   ```sql
   GRANT ALL PRIVILEGES ON whatsapp_clone.* TO 'root'@'localhost';
   FLUSH PRIVILEGES;
   ```

3. Que el puerto 3306 esté abierto (por defecto)

### 4. Estructura de Tablas

La aplicación creará automáticamente las siguientes tablas:

- **Usuario**: Almacena información de usuarios
- **Log**: Registra eventos y logs del sistema
- **Transferencia**: Registra transferencias de archivos y mensajes

## Solución de Problemas

### Error: "Access denied for user"
- Verifica el usuario y contraseña en `db.properties`
- Asegúrate de que el usuario tenga permisos en la base de datos

### Error: "Unknown database 'whatsapp_clone'"
- Crea la base de datos manualmente o verifica que el usuario tenga permisos para crearla
- La aplicación intentará crearla automáticamente si tiene permisos

### Error: "Communications link failure"
- Verifica que MySQL esté ejecutándose
- Verifica que el puerto sea correcto (3306 por defecto)
- Verifica el firewall

### Error al guardar usuario
- Verifica que las tablas se hayan creado correctamente
- Revisa los logs de la aplicación para más detalles
- Asegúrate de que el campo Username sea único (no duplicados)

## Ejemplo de db.properties

```properties
# Configuración de Base de Datos MySQL
db.host=localhost
db.port=3306
db.database=whatsapp_clone
db.username=root
db.password=mipassword123
```

