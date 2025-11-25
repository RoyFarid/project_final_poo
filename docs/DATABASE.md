# Base de Datos

## Visión General

MySQL 8.0+ para almacenar usuarios, logs y transferencias.

**Base de datos:** `whatsapp_clone`  
**Motor:** InnoDB  
**Codificación:** UTF-8 (utf8mb4)  
**Timezone:** UTC

---

## Esquema

### Diagrama Entidad-Relación

```

    Usuario      

 Id (PK)         
 Username        
 PasswordHash               
 Salt                       
 Email                      
 FechaCreacion              
 UltimoLogin                
 Estado                     
           
                             
                             
                             
         FK                   FK
                             
   
      Log             Transferencia   
   
 Id (PK)             Id (PK)          
 Nivel               Tipo             
 Mensaje             Nombre           
 Modulo              Tamano           
 Fecha               Checksum         
 TraceId             Estado           
 UserId (FK)         Inicio           
    Fin              
                       UserId (FK)      
                       PeerIp           
                      
```

---

## Tablas

### Usuario

Información de usuarios registrados.

```sql
CREATE TABLE Usuario (
    Id INT AUTO_INCREMENT PRIMARY KEY,
    Username VARCHAR(50) UNIQUE NOT NULL,
    PasswordHash VARCHAR(255) NOT NULL,
    Salt VARCHAR(255) NOT NULL,
    Email VARCHAR(100) NOT NULL,
    FechaCreacion DATETIME NOT NULL,
    UltimoLogin DATETIME,
    Estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### Columnas

| Campo | Tipo | Null | Default | Descripción |
|-------|------|------|---------|-------------|
| **Id** | INT | NO | AUTO_INCREMENT | Identificador único del usuario |
| **Username** | VARCHAR(50) | NO | - | Nombre de usuario único |
| **PasswordHash** | VARCHAR(255) | NO | - | Hash BCrypt de la contraseña |
| **Salt** | VARCHAR(255) | NO | - | Salt usado para hashear |
| **Email** | VARCHAR(100) | NO | - | Correo electrónico |
| **FechaCreacion** | DATETIME | NO | - | Fecha de registro |
| **UltimoLogin** | DATETIME | YES | NULL | Última autenticación exitosa |
| **Estado** | VARCHAR(20) | NO | 'ACTIVO' | Estado del usuario |

#### Restricciones

- **PRIMARY KEY**: `Id`
- **UNIQUE**: `Username`

#### Valores de Estado

- `ACTIVO`: Usuario puede iniciar sesión
- `INACTIVO`: Usuario temporalmente deshabilitado
- `BLOQUEADO`: Usuario permanentemente bloqueado

#### Índices

```sql
-- Índice único en Username (automático por UNIQUE)
CREATE UNIQUE INDEX idx_username ON Usuario(Username);
```

#### Ejemplo de Datos

```sql
INSERT INTO Usuario (Username, PasswordHash, Salt, Email, FechaCreacion, Estado) 
VALUES ('juanito', '$2a$12$...', '$2a$12$...', 'juan@email.com', NOW(), 'ACTIVO');
```

---

### Tabla: Log

Registra eventos y actividades del sistema para auditoría y debugging.

```sql
CREATE TABLE Log (
    Id INT AUTO_INCREMENT PRIMARY KEY,
    Nivel VARCHAR(10) NOT NULL,
    Mensaje TEXT NOT NULL,
    Modulo VARCHAR(50) NOT NULL,
    Fecha DATETIME NOT NULL,
    TraceId VARCHAR(100),
    UserId INT,
    FOREIGN KEY (UserId) REFERENCES Usuario(Id) ON DELETE SET NULL,
    INDEX idx_fecha (Fecha),
    INDEX idx_trace (TraceId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### Columnas

| Campo | Tipo | Null | Default | Descripción |
|-------|------|------|---------|-------------|
| **Id** | INT | NO | AUTO_INCREMENT | Identificador único del log |
| **Nivel** | VARCHAR(10) | NO | - | Nivel de severidad |
| **Mensaje** | TEXT | NO | - | Descripción del evento |
| **Modulo** | VARCHAR(50) | NO | - | Componente que generó el log |
| **Fecha** | DATETIME | NO | - | Timestamp del evento |
| **TraceId** | VARCHAR(100) | YES | NULL | ID para rastrear flujos |
| **UserId** | INT | YES | NULL | Usuario relacionado (opcional) |

#### Restricciones

- **PRIMARY KEY**: `Id`
- **FOREIGN KEY**: `UserId` → `Usuario(Id)` ON DELETE SET NULL

#### Niveles de Log

- `INFO`: Información general
- `WARN`: Advertencias
- `ERROR`: Errores
- `DEBUG`: Información de depuración

#### Índices

```sql
CREATE INDEX idx_fecha ON Log(Fecha);
CREATE INDEX idx_trace ON Log(TraceId);
```

#### Ejemplo de Datos

```sql
INSERT INTO Log (Nivel, Mensaje, Modulo, Fecha, TraceId, UserId)
VALUES ('INFO', 'Usuario autenticado', 'AuthService', NOW(), 'TRC-123', 1);
```

---

### Tabla: Transferencia

Registra todas las transferencias de archivos y mensajes del sistema.

```sql
CREATE TABLE Transferencia (
    Id INT AUTO_INCREMENT PRIMARY KEY,
    Tipo VARCHAR(20) NOT NULL,
    Nombre VARCHAR(255) NOT NULL,
    Tamano BIGINT NOT NULL,
    Checksum VARCHAR(64) NOT NULL,
    Estado VARCHAR(20) NOT NULL,
    Inicio DATETIME NOT NULL,
    Fin DATETIME,
    UserId INT NOT NULL,
    PeerIp VARCHAR(45) NOT NULL,
    FOREIGN KEY (UserId) REFERENCES Usuario(Id) ON DELETE CASCADE,
    INDEX idx_estado (Estado),
    INDEX idx_user (UserId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### Columnas

| Campo | Tipo | Null | Default | Descripción |
|-------|------|------|---------|-------------|
| **Id** | INT | NO | AUTO_INCREMENT | Identificador único |
| **Tipo** | VARCHAR(20) | NO | - | Tipo de transferencia |
| **Nombre** | VARCHAR(255) | NO | - | Nombre del archivo/mensaje |
| **Tamano** | BIGINT | NO | - | Tamaño en bytes |
| **Checksum** | VARCHAR(64) | NO | - | Hash SHA-256 para integridad |
| **Estado** | VARCHAR(20) | NO | - | Estado de la transferencia |
| **Inicio** | DATETIME | NO | - | Fecha/hora de inicio |
| **Fin** | DATETIME | YES | NULL | Fecha/hora de finalización |
| **UserId** | INT | NO | - | Usuario que realizó la transferencia |
| **PeerIp** | VARCHAR(45) | NO | - | IP del destinatario/remitente |

#### Restricciones

- **PRIMARY KEY**: `Id`
- **FOREIGN KEY**: `UserId` → `Usuario(Id)` ON DELETE CASCADE

#### Tipos de Transferencia

- `MENSAJE`: Mensaje de chat
- `ARCHIVO`: Archivo transferido
- `VIDEO`: Stream de video
- `AUDIO`: Stream de audio

#### Estados de Transferencia

- `PENDIENTE`: Esperando inicio
- `EN_PROGRESO`: Transferencia activa
- `COMPLETADA`: Transferencia exitosa
- `FALLIDA`: Error en transferencia
- `CANCELADA`: Cancelada por usuario

#### Índices

```sql
CREATE INDEX idx_estado ON Transferencia(Estado);
CREATE INDEX idx_user ON Transferencia(UserId);
```

#### Ejemplo de Datos

```sql
INSERT INTO Transferencia (Tipo, Nombre, Tamano, Checksum, Estado, Inicio, UserId, PeerIp)
VALUES ('ARCHIVO', 'documento.pdf', 2048576, 'a3f5b2c1...', 'COMPLETADA', NOW(), 1, '192.168.1.100');
```

---

##  Configuración

### Archivo: db.properties

```properties
# Configuración de conexión MySQL
db.host=localhost
db.port=3306
db.database=whatsapp_clone
db.username=whatsapp_user
db.password=tu_password_seguro
```

### Crear Base de Datos y Usuario

```sql
-- Crear base de datos
CREATE DATABASE IF NOT EXISTS whatsapp_clone
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

-- Crear usuario
CREATE USER 'whatsapp_user'@'localhost' IDENTIFIED BY 'tu_password_seguro';

-- Otorgar permisos
GRANT ALL PRIVILEGES ON whatsapp_clone.* TO 'whatsapp_user'@'localhost';
FLUSH PRIVILEGES;
```

---

##  Consultas Comunes

### Usuarios

#### Buscar usuario por username
```sql
SELECT * FROM Usuario 
WHERE Username = 'juanito';
```

#### Listar usuarios activos
```sql
SELECT Id, Username, Email, FechaCreacion, UltimoLogin
FROM Usuario
WHERE Estado = 'ACTIVO'
ORDER BY FechaCreacion DESC;
```

#### Actualizar último login
```sql
UPDATE Usuario
SET UltimoLogin = NOW()
WHERE Id = 1;
```

#### Usuarios registrados por mes
```sql
SELECT 
    DATE_FORMAT(FechaCreacion, '%Y-%m') AS Mes,
    COUNT(*) AS TotalUsuarios
FROM Usuario
GROUP BY DATE_FORMAT(FechaCreacion, '%Y-%m')
ORDER BY Mes DESC;
```

### Logs

#### Logs de un usuario específico
```sql
SELECT * FROM Log
WHERE UserId = 1
ORDER BY Fecha DESC
LIMIT 100;
```

#### Logs por nivel de severidad
```sql
SELECT Nivel, COUNT(*) AS Total
FROM Log
GROUP BY Nivel;
```

#### Logs de las últimas 24 horas
```sql
SELECT * FROM Log
WHERE Fecha >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
ORDER BY Fecha DESC;
```

#### Seguir un flujo por TraceId
```sql
SELECT * FROM Log
WHERE TraceId = 'TRC-1234567890'
ORDER BY Fecha ASC;
```

#### Logs de errores
```sql
SELECT Fecha, Modulo, Mensaje, UserId
FROM Log
WHERE Nivel = 'ERROR'
ORDER BY Fecha DESC
LIMIT 50;
```

### Transferencias

#### Transferencias de un usuario
```sql
SELECT * FROM Transferencia
WHERE UserId = 1
ORDER BY Inicio DESC;
```

#### Transferencias completadas
```sql
SELECT 
    Nombre,
    Tamano / 1024 / 1024 AS TamanoMB,
    Inicio,
    Fin,
    TIMESTAMPDIFF(SECOND, Inicio, Fin) AS DuracionSegundos
FROM Transferencia
WHERE Estado = 'COMPLETADA'
ORDER BY Inicio DESC;
```

#### Total de datos transferidos por usuario
```sql
SELECT 
    u.Username,
    COUNT(t.Id) AS TotalTransferencias,
    SUM(t.Tamano) / 1024 / 1024 AS TotalMB
FROM Transferencia t
JOIN Usuario u ON t.UserId = u.Id
GROUP BY u.Username
ORDER BY TotalMB DESC;
```

#### Transferencias fallidas
```sql
SELECT 
    t.Nombre,
    t.Tipo,
    t.Inicio,
    u.Username,
    t.PeerIp
FROM Transferencia t
JOIN Usuario u ON t.UserId = u.Id
WHERE t.Estado = 'FALLIDA'
ORDER BY t.Inicio DESC;
```

#### Estadísticas por tipo de transferencia
```sql
SELECT 
    Tipo,
    COUNT(*) AS Total,
    AVG(Tamano / 1024) AS PromedioKB,
    MAX(Tamano / 1024 / 1024) AS MaximoMB
FROM Transferencia
WHERE Estado = 'COMPLETADA'
GROUP BY Tipo;
```

---

##  Estadísticas y Reportes

### Dashboard de Actividad

```sql
SELECT 
    (SELECT COUNT(*) FROM Usuario WHERE Estado = 'ACTIVO') AS UsuariosActivos,
    (SELECT COUNT(*) FROM Log WHERE Fecha >= DATE_SUB(NOW(), INTERVAL 1 DAY)) AS LogsUltimas24h,
    (SELECT COUNT(*) FROM Transferencia WHERE Estado = 'COMPLETADA') AS TransferenciasCompletadas,
    (SELECT SUM(Tamano) / 1024 / 1024 / 1024 FROM Transferencia WHERE Estado = 'COMPLETADA') AS TotalGB;
```

### Usuarios Más Activos

```sql
SELECT 
    u.Username,
    COUNT(t.Id) AS TotalTransferencias,
    COUNT(l.Id) AS TotalLogs,
    MAX(u.UltimoLogin) AS UltimoAcceso
FROM Usuario u
LEFT JOIN Transferencia t ON u.Id = t.UserId
LEFT JOIN Log l ON u.Id = l.UserId
WHERE u.Estado = 'ACTIVO'
GROUP BY u.Id
ORDER BY TotalTransferencias DESC
LIMIT 10;
```

### Actividad por Hora del Día

```sql
SELECT 
    HOUR(Fecha) AS Hora,
    COUNT(*) AS TotalEventos
FROM Log
WHERE Fecha >= DATE_SUB(NOW(), INTERVAL 7 DAY)
GROUP BY HOUR(Fecha)
ORDER BY Hora;
```

---

##  Mantenimiento

### Backup de Base de Datos

```bash
# Backup completo
mysqldump -u whatsapp_user -p whatsapp_clone > backup_whatsapp_$(date +%Y%m%d).sql

# Backup solo estructura
mysqldump -u whatsapp_user -p --no-data whatsapp_clone > schema_whatsapp.sql

# Backup solo datos
mysqldump -u whatsapp_user -p --no-create-info whatsapp_clone > data_whatsapp.sql
```

### Restaurar Base de Datos

```bash
mysql -u whatsapp_user -p whatsapp_clone < backup_whatsapp_20251125.sql
```

### Limpiar Logs Antiguos

```sql
-- Eliminar logs mayores a 90 días
DELETE FROM Log 
WHERE Fecha < DATE_SUB(NOW(), INTERVAL 90 DAY);
```

### Optimizar Tablas

```sql
OPTIMIZE TABLE Usuario;
OPTIMIZE TABLE Log;
OPTIMIZE TABLE Transferencia;
```

### Analizar Rendimiento

```sql
-- Ver tamaño de tablas
SELECT 
    table_name AS Tabla,
    ROUND((data_length + index_length) / 1024 / 1024, 2) AS TamanoMB
FROM information_schema.TABLES
WHERE table_schema = 'whatsapp_clone'
ORDER BY (data_length + index_length) DESC;

-- Ver índices de una tabla
SHOW INDEX FROM Usuario;
```

---

##  Seguridad

### Buenas Prácticas

1. **Contraseñas**:
   - Nunca almacenar contraseñas en texto plano
   - Usar BCrypt con al menos 12 rounds
   - Almacenar salt junto con hash

2. **Permisos**:
   - Usuario de aplicación con permisos limitados
   - No usar root en producción
   - Principio de mínimo privilegio

3. **Conexiones**:
   - Usar SSL/TLS para conexiones remotas
   - Limitar conexiones por IP
   - Timeout de conexiones inactivas

4. **Auditoría**:
   - Registrar todos los accesos
   - Monitorear intentos fallidos de login
   - Revisar logs regularmente

### Encriptación de Datos Sensibles

```sql
-- Ejemplo de uso de encriptación (MySQL 8.0+)
-- Para columnas que requieran encriptación adicional

-- Encriptar
INSERT INTO Usuario (Username, Email, ...) 
VALUES ('user', AES_ENCRYPT('email@test.com', 'encryption_key'), ...);

-- Desencriptar
SELECT Username, AES_DECRYPT(Email, 'encryption_key') AS Email
FROM Usuario;
```

---

##  Modelo de Datos en Java

### Clase Usuario

```java
public class Usuario {
    private Long id;
    private String username;
    private String passwordHash;
    private String salt;
    private String email;
    private LocalDateTime fechaCreacion;
    private LocalDateTime ultimoLogin;
    private EstadoUsuario estado;
    
    public enum EstadoUsuario {
        ACTIVO, INACTIVO, BLOQUEADO
    }
}
```

### Clase Log

```java
public class Log {
    private Long id;
    private String nivel;
    private String mensaje;
    private String modulo;
    private LocalDateTime fecha;
    private String traceId;
    private Long userId;
}
```

### Clase Transferencia

```java
public class Transferencia {
    private Long id;
    private TipoTransferencia tipo;
    private String nombre;
    private Long tamano;
    private String checksum;
    private EstadoTransferencia estado;
    private LocalDateTime inicio;
    private LocalDateTime fin;
    private Long userId;
    private String peerIp;
    
    public enum TipoTransferencia {
        MENSAJE, ARCHIVO, VIDEO, AUDIO
    }
    
    public enum EstadoTransferencia {
        PENDIENTE, EN_PROGRESO, COMPLETADA, FALLIDA, CANCELADA
    }
}
```

---

##  Migraciones

### Script de Inicialización Completo

```sql
-- Crear base de datos
CREATE DATABASE IF NOT EXISTS whatsapp_clone
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE whatsapp_clone;

-- Tabla Usuario
CREATE TABLE IF NOT EXISTS Usuario (
    Id INT AUTO_INCREMENT PRIMARY KEY,
    Username VARCHAR(50) UNIQUE NOT NULL,
    PasswordHash VARCHAR(255) NOT NULL,
    Salt VARCHAR(255) NOT NULL,
    Email VARCHAR(100) NOT NULL,
    FechaCreacion DATETIME NOT NULL,
    UltimoLogin DATETIME,
    Estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tabla Log
CREATE TABLE IF NOT EXISTS Log (
    Id INT AUTO_INCREMENT PRIMARY KEY,
    Nivel VARCHAR(10) NOT NULL,
    Mensaje TEXT NOT NULL,
    Modulo VARCHAR(50) NOT NULL,
    Fecha DATETIME NOT NULL,
    TraceId VARCHAR(100),
    UserId INT,
    FOREIGN KEY (UserId) REFERENCES Usuario(Id) ON DELETE SET NULL,
    INDEX idx_fecha (Fecha),
    INDEX idx_trace (TraceId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tabla Transferencia
CREATE TABLE IF NOT EXISTS Transferencia (
    Id INT AUTO_INCREMENT PRIMARY KEY,
    Tipo VARCHAR(20) NOT NULL,
    Nombre VARCHAR(255) NOT NULL,
    Tamano BIGINT NOT NULL,
    Checksum VARCHAR(64) NOT NULL,
    Estado VARCHAR(20) NOT NULL,
    Inicio DATETIME NOT NULL,
    Fin DATETIME,
    UserId INT NOT NULL,
    PeerIp VARCHAR(45) NOT NULL,
    FOREIGN KEY (UserId) REFERENCES Usuario(Id) ON DELETE CASCADE,
    INDEX idx_estado (Estado),
    INDEX idx_user (UserId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

Esta documentación proporciona una guía completa del esquema de base de datos del proyecto. Para gestión programática, ver `DatabaseManager.java` y las clases en `com.whatsapp.repository`.
