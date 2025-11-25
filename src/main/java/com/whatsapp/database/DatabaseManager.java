package com.whatsapp.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static DatabaseManager instance;
    private final DatabaseConfig config;
    private Connection connection;

    private DatabaseManager() {
        this.config = new DatabaseConfig();
        initializeDatabase();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(
                config.getConnectionUrl(),
                config.getUsername(),
                config.getPassword()
            );
        }
        return connection;
    }

    private void initializeDatabase() {
        try {
            logger.info("Intentando conectar a MySQL en {}:{} con usuario: {}", 
                       config.getHost(), config.getPort(), config.getUsername());
            
            // Primero crear la base de datos si no existe
            String urlWithoutDb = String.format("jdbc:mysql://%s:%s?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
                config.getHost(), config.getPort());
            
            try (Connection conn = DriverManager.getConnection(urlWithoutDb, config.getUsername(), config.getPassword());
                 Statement stmt = conn.createStatement()) {
                logger.info("Conexión exitosa a MySQL. Creando base de datos si no existe...");
                stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + config.getDatabase());
                logger.info("Base de datos '{}' verificada/creada", config.getDatabase());
            } catch (SQLException e) {
                logger.error("Error al conectar a MySQL o crear la base de datos", e);
                logger.error("Verifica que:");
                logger.error("1. MySQL esté ejecutándose");
                logger.error("2. El usuario '{}' y contraseña sean correctos", config.getUsername());
                logger.error("3. El puerto {} sea correcto", config.getPort());
                logger.error("4. El archivo db.properties tenga la configuración correcta");
                throw new RuntimeException("No se pudo conectar a MySQL: " + e.getMessage(), e);
            }

            // Ahora crear las tablas
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {
                logger.info("Creando tablas en la base de datos...");

                // Tabla Usuario
                String createUsuarioTable = """
                    CREATE TABLE IF NOT EXISTS Usuario (
                        Id INT AUTO_INCREMENT PRIMARY KEY,
                        Username VARCHAR(50) UNIQUE NOT NULL,
                        PasswordHash VARCHAR(255) NOT NULL,
                        Salt VARCHAR(255) NOT NULL,
                        Email VARCHAR(100) NOT NULL,
                        FechaCreacion DATETIME NOT NULL,
                        UltimoLogin DATETIME,
                        Estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO'
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """;

                // Tabla Log
                String createLogTable = """
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
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """;

                // Tabla Transferencia
                String createTransferenciaTable = """
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
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """;

                stmt.execute(createUsuarioTable);
                stmt.execute(createLogTable);
                stmt.execute(createTransferenciaTable);

                // Crear índices adicionales (MySQL no soporta IF NOT EXISTS en CREATE INDEX)
                // El índice único ya está definido en la tabla Usuario con UNIQUE
                // Los otros índices ya están definidos en las tablas

                logger.info("Base de datos inicializada correctamente");
            } catch (SQLException e) {
                logger.error("Error al crear las tablas", e);
                throw new RuntimeException("No se pudieron crear las tablas: " + e.getMessage(), e);
            }
        } catch (RuntimeException e) {
            // Re-lanzar RuntimeException
            throw e;
        } catch (Exception e) {
            logger.error("Error inesperado al inicializar la base de datos", e);
            throw new RuntimeException("No se pudo inicializar la base de datos: " + e.getMessage(), e);
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            logger.error("Error al cerrar la conexión", e);
        }
    }
}

