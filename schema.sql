-- Esquema de la base de datos whatsapp_clone
-- Ejecuta este script en MySQL para crear todas las tablas necesarias.
-- Nota: La aplicación crea las tablas automáticamente al iniciar, pero este script
-- puede ser útil para setup manual o migraciones.

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

CREATE TABLE IF NOT EXISTS Room (
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

CREATE TABLE IF NOT EXISTS RoomMember (
    RoomId INT NOT NULL,
    ConnectionId VARCHAR(255) NOT NULL,
    PRIMARY KEY (RoomId, ConnectionId),
    FOREIGN KEY (RoomId) REFERENCES Room(Id) ON DELETE CASCADE,
    INDEX idx_connection (ConnectionId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

