package com.whatsapp.database;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class DatabaseConfig {
    private static final String CONFIG_FILE = "db.properties";
    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_PORT = "3306";
    private static final String DEFAULT_DB = "whatsapp_clone";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";

    private String host;
    private String port;
    private String database;
    private String username;
    private String password;
    private String serverUsername; // Username del servidor para crear BD única

    public DatabaseConfig() {
        loadConfig();
    }

    public DatabaseConfig(String serverUsername) {
        this.serverUsername = serverUsername;
        loadConfig();
        // Si se proporciona un username de servidor, usar ese para la BD
        if (serverUsername != null && !serverUsername.isEmpty()) {
            this.database = sanitizeDatabaseName(serverUsername + "_db");
        }
    }

    private String sanitizeDatabaseName(String name) {
        // Limpiar el nombre para que sea válido como nombre de BD
        return name.replaceAll("[^a-zA-Z0-9_]", "_").toLowerCase();
    }

    private void loadConfig() {
        Properties props = new Properties();
        
        // Intentar cargar desde archivo
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            props.load(fis);
            this.host = props.getProperty("db.host", DEFAULT_HOST);
            this.port = props.getProperty("db.port", DEFAULT_PORT);
            this.database = props.getProperty("db.database", DEFAULT_DB);
            this.username = props.getProperty("db.username", DEFAULT_USER);
            this.password = props.getProperty("db.password", DEFAULT_PASSWORD);
        } catch (IOException e) {
            // Si no existe el archivo, usar valores por defecto
            this.host = DEFAULT_HOST;
            this.port = DEFAULT_PORT;
            this.database = DEFAULT_DB;
            this.username = DEFAULT_USER;
            this.password = DEFAULT_PASSWORD;
        }
    }

    public String getConnectionUrl() {
        return String.format("jdbc:mysql://%s:%s/%s?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true", 
                            host, port, database);
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getDatabase() {
        return database;
    }

    public String getHost() {
        return host;
    }

    public String getPort() {
        return port;
    }
}

