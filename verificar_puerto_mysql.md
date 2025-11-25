# Cómo Verificar el Puerto de MySQL

## Método 1: Desde MySQL (Recomendado)

Conéctate a MySQL y ejecuta:

```sql
SHOW VARIABLES LIKE 'port';
```

O también:

```sql
SELECT @@port;
```

## Método 2: Desde la Línea de Comandos

### Windows (CMD o PowerShell):
```cmd
netstat -an | findstr :3306
```

O para ver todos los puertos de MySQL:
```cmd
netstat -an | findstr mysql
```

### Linux/Mac:
```bash
netstat -an | grep :3306
```

O:
```bash
sudo lsof -i -P -n | grep mysql
```

## Método 3: Verificar el Archivo de Configuración de MySQL

### Windows:
Busca en: `C:\ProgramData\MySQL\MySQL Server X.X\my.ini`

Busca la línea:
```ini
port=3306
```

### Linux:
```bash
sudo cat /etc/mysql/my.cnf | grep port
```

O:
```bash
sudo cat /etc/mysql/mysql.conf.d/mysqld.cnf | grep port
```

### Mac (Homebrew):
```bash
cat /usr/local/etc/my.cnf | grep port
```

## Método 4: Desde el Administrador de Servicios

### Windows:
1. Abre "Servicios" (services.msc)
2. Busca "MySQL" o "MySQL80"
3. Haz clic derecho → Propiedades
4. Ve a la pestaña "Detalles" o revisa los argumentos de inicio

## Método 5: Probar Conexión

Puedes probar conectarte con diferentes puertos comunes:

- **3306** (puerto por defecto de MySQL)
- **3307** (alternativo común)
- **3308** (alternativo común)

## Nota Importante

**El puerto por defecto de MySQL es 3306**. Si no has cambiado la configuración, usa este puerto.

Si no estás seguro, prueba primero con **3306** en tu archivo `db.properties`.

