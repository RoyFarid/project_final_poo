# Base de Datos

## Visión General

MySQL 8.0+ almacena usuarios, logs, transferencias y rooms. La base de datos se llama `whatsapp_clone` y usa InnoDB con codificación UTF-8 (utf8mb4).

## Esquema de Base de Datos

### Tabla: Usuario

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

**Columnas:**
- `Id`: Identificador único (AUTO_INCREMENT)
- `Username`: Nombre de usuario único
- `PasswordHash`: Hash BCrypt de la contraseña
- `Salt`: Salt usado para hashear
- `Email`: Correo electrónico
- `FechaCreacion`: Fecha de registro
- `UltimoLogin`: Última autenticación exitosa
- `Estado`: ACTIVO, INACTIVO, BLOQUEADO

**Índices:**
- PRIMARY KEY: `Id`
- UNIQUE: `Username`

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

**Columnas:**
- `Id`: Identificador único
- `Nivel`: INFO, WARN, ERROR, DEBUG
- `Mensaje`: Descripción del evento
- `Modulo`: Componente que generó el log
- `Fecha`: Timestamp del evento
- `TraceId`: ID para rastrear flujos
- `UserId`: Usuario relacionado (opcional)

**Índices:**
- PRIMARY KEY: `Id`
- FOREIGN KEY: `UserId` → `Usuario(Id)` ON DELETE SET NULL
- INDEX: `idx_fecha`, `idx_trace`

### Tabla: Transferencia

Registra todas las transferencias de archivos y mensajes.

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

**Columnas:**
- `Id`: Identificador único
- `Tipo`: MENSAJE, ARCHIVO, VIDEO, AUDIO
- `Nombre`: Nombre del archivo/mensaje
- `Tamano`: Tamaño en bytes
- `Checksum`: Hash SHA-256 para integridad
- `Estado`: PENDIENTE, EN_PROGRESO, COMPLETADA, FALLIDA, CANCELADA
- `Inicio`: Fecha/hora de inicio
- `Fin`: Fecha/hora de finalización
- `UserId`: Usuario que realizó la transferencia
- `PeerIp`: IP del destinatario/remitente

**Índices:**
- PRIMARY KEY: `Id`
- FOREIGN KEY: `UserId` → `Usuario(Id)` ON DELETE CASCADE
- INDEX: `idx_estado`, `idx_user`

### Tabla: Room

Información de salas de chat grupales.

```sql
CREATE TABLE Room (
    Id INT AUTO_INCREMENT PRIMARY KEY,
    Name VARCHAR(100) NOT NULL,
    CreatorConnectionId VARCHAR(255) NOT NULL,
    CreatorUsername VARCHAR(50) NOT NULL,
    Estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    FechaCreacion DATETIME NOT NULL,
    ServerUsername VARCHAR(50) NOT NULL,
    RequestMessage TEXT,
    IncludeServer BOOLEAN DEFAULT FALSE,
    INDEX idx_server (ServerUsername),
    INDEX idx_estado (Estado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**Columnas:**
- `Id`: Identificador único
- `Name`: Nombre del room
- `CreatorConnectionId`: ConnectionId del creador
- `CreatorUsername`: Username del creador
- `Estado`: PENDIENTE, ACTIVO, RECHAZADO, CERRADO
- `FechaCreacion`: Fecha de creación
- `ServerUsername`: Username del servidor que gestiona el room
- `RequestMessage`: Mensaje opcional de solicitud
- `IncludeServer`: Si el servidor participa en el room

**Índices:**
- PRIMARY KEY: `Id`
- INDEX: `idx_server`, `idx_estado`

### Tabla: RoomMember

Relación muchos-a-muchos entre rooms y miembros.

```sql
CREATE TABLE RoomMember (
    RoomId INT NOT NULL,
    ConnectionId VARCHAR(255) NOT NULL,
    PRIMARY KEY (RoomId, ConnectionId),
    FOREIGN KEY (RoomId) REFERENCES Room(Id) ON DELETE CASCADE,
    INDEX idx_connection (ConnectionId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**Columnas:**
- `RoomId`: ID del room
- `ConnectionId`: ConnectionId del miembro

**Índices:**
- PRIMARY KEY: `(RoomId, ConnectionId)`
- FOREIGN KEY: `RoomId` → `Room(Id)` ON DELETE CASCADE
- INDEX: `idx_connection`

## Configuración

### Archivo: db.properties

```properties
db.host=localhost
db.port=3306
db.database=whatsapp_clone
db.username=whatsapp_user
db.password=tu_password_seguro
```

### Script SQL de Esquema

El archivo `schema.sql` en la raíz del proyecto contiene el esquema completo de la base de datos. Puede ejecutarse manualmente si es necesario:

```bash
mysql -u whatsapp_user -p whatsapp_clone < schema.sql
```

**Nota:** La aplicación crea las tablas automáticamente al iniciar mediante `DatabaseManager`, por lo que normalmente no es necesario ejecutar este script manualmente.

### Crear Base de Datos y Usuario

```sql
CREATE DATABASE IF NOT EXISTS whatsapp_clone
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

CREATE USER 'whatsapp_user'@'localhost' IDENTIFIED BY 'tu_password_seguro';

GRANT ALL PRIVILEGES ON whatsapp_clone.* TO 'whatsapp_user'@'localhost';
FLUSH PRIVILEGES;
```

## Consultas Comunes

### Usuarios

```sql
-- Buscar usuario por username
SELECT * FROM Usuario WHERE Username = 'juanito';

-- Listar usuarios activos
SELECT Id, Username, Email, FechaCreacion, UltimoLogin
FROM Usuario
WHERE Estado = 'ACTIVO'
ORDER BY FechaCreacion DESC;

-- Actualizar último login
UPDATE Usuario SET UltimoLogin = NOW() WHERE Id = 1;
```

### Logs

```sql
-- Logs de un usuario específico
SELECT * FROM Log
WHERE UserId = 1
ORDER BY Fecha DESC
LIMIT 100;

-- Logs por nivel de severidad
SELECT Nivel, COUNT(*) AS Total FROM Log GROUP BY Nivel;

-- Logs de las últimas 24 horas
SELECT * FROM Log
WHERE Fecha >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
ORDER BY Fecha DESC;

-- Seguir un flujo por TraceId
SELECT * FROM Log
WHERE TraceId = 'TRC-1234567890'
ORDER BY Fecha ASC;
```

### Transferencias

```sql
-- Transferencias de un usuario
SELECT * FROM Transferencia
WHERE UserId = 1
ORDER BY Inicio DESC;

-- Transferencias completadas
SELECT Nombre, Tamano / 1024 / 1024 AS TamanoMB, Inicio, Fin,
       TIMESTAMPDIFF(SECOND, Inicio, Fin) AS DuracionSegundos
FROM Transferencia
WHERE Estado = 'COMPLETADA'
ORDER BY Inicio DESC;

-- Total de datos transferidos por usuario
SELECT u.Username, COUNT(t.Id) AS TotalTransferencias,
       SUM(t.Tamano) / 1024 / 1024 AS TotalMB
FROM Transferencia t
JOIN Usuario u ON t.UserId = u.Id
GROUP BY u.Username
ORDER BY TotalMB DESC;
```

### Rooms

```sql
-- Rooms activos de un servidor
SELECT * FROM Room
WHERE ServerUsername = 'servidor1' AND Estado = 'ACTIVO'
ORDER BY FechaCreacion DESC;

-- Miembros de un room
SELECT rm.ConnectionId, r.Name
FROM RoomMember rm
JOIN Room r ON rm.RoomId = r.Id
WHERE rm.RoomId = 1;

-- Rooms con número de miembros
SELECT r.Id, r.Name, r.Estado, COUNT(rm.ConnectionId) AS NumMiembros
FROM Room r
LEFT JOIN RoomMember rm ON r.Id = rm.RoomId
WHERE r.ServerUsername = 'servidor1'
GROUP BY r.Id, r.Name, r.Estado;
```

## Mantenimiento

### Backup

```bash
mysqldump -u whatsapp_user -p whatsapp_clone > backup_whatsapp_$(date +%Y%m%d).sql
```

### Restaurar

```bash
mysql -u whatsapp_user -p whatsapp_clone < backup_whatsapp_20251125.sql
```

### Limpiar Logs Antiguos

```sql
DELETE FROM Log WHERE Fecha < DATE_SUB(NOW(), INTERVAL 90 DAY);
```

### Optimizar Tablas

```sql
OPTIMIZE TABLE Usuario;
OPTIMIZE TABLE Log;
OPTIMIZE TABLE Transferencia;
OPTIMIZE TABLE Room;
OPTIMIZE TABLE RoomMember;
```

## Seguridad

### Buenas Prácticas

1. **Contraseñas**: Nunca almacenar en texto plano. Usar BCrypt con al menos 12 rounds.
2. **Permisos**: Usuario de aplicación con permisos limitados. No usar root en producción.
3. **Conexiones**: Usar SSL/TLS para conexiones remotas.
4. **Auditoría**: Registrar todos los accesos y monitorear intentos fallidos de login.

