# RoomWave

Aplicación de mensajería instantánea en Java con arquitectura cliente-servidor. Soporta chat en tiempo real, transferencia de archivos, videollamadas y gestión de salas de chat (rooms).

## Características Principales

- **Chat en tiempo real**: Mensajería instantánea entre usuarios conectados
- **Transferencia de archivos**: Envío de archivos con verificación de integridad mediante checksums
- **Videollamadas**: Streaming de video y audio en tiempo real
- **Sistema de Rooms**: Creación y gestión de salas de chat grupales
- **Autenticación segura**: Sistema de registro y login con BCrypt
- **Modo servidor/cliente**: Arquitectura centralizada con servidor y múltiples clientes
- **Base de datos MySQL**: Persistencia de usuarios, logs, transferencias y rooms

## Requisitos

- Java JDK 17 o superior
- Maven 3.6 o superior
- MySQL 8.0 o superior
- JavaFX 21 (incluido como dependencia)

## Instalación Rápida

```bash
# Configurar base de datos
cp db.properties.example db.properties
# Editar db.properties con tus credenciales de MySQL

# Compilar y ejecutar
mvn clean install
mvn javafx:run
```

Para una guía detallada de instalación, ver [INSTALLATION.md](docs/INSTALLATION.md).

## Uso Básico

1. **Registro**: Crear una cuenta nueva desde la pantalla de login
2. **Modo Servidor**: Iniciar sesión como servidor para hostear la sesión
3. **Modo Cliente**: Iniciar sesión como cliente y conectarse a un servidor
4. **Chat**: Enviar mensajes a usuarios individuales o crear rooms grupales
5. **Archivos**: Compartir archivos con otros usuarios
6. **Video**: Iniciar videollamadas con otros usuarios

## Estructura del Proyecto

```
src/main/java/com/whatsapp/
├── command/          # Patrón Command para acciones
├── database/         # Gestión de conexión a MySQL
├── model/            # Entidades: Usuario, Room, Transferencia, Log
├── network/          # Gestión de conexiones TCP/IP
├── protocol/         # Protocolo de mensajería
├── repository/       # Patrón Repository para acceso a datos
├── service/          # Lógica de negocio
└── ui/               # Interfaz gráfica JavaFX
```

## Documentación

- [INSTALLATION.md](docs/INSTALLATION.md) - Guía detallada de instalación
- [ARCHITECTURE.md](docs/ARCHITECTURE.md) - Arquitectura del sistema y patrones de diseño
- [API.md](docs/API.md) - Documentación de servicios y APIs
- [DATABASE.md](docs/DATABASE.md) - Esquema de base de datos
- [USER_GUIDE.md](docs/USER_GUIDE.md) - Manual de usuario
- [DEVELOPMENT.md](docs/DEVELOPMENT.md) - Guía para desarrolladores

## Stack Tecnológico

- **Java 17**: Lenguaje de programación
- **JavaFX 21**: Framework de interfaz gráfica
- **MySQL 8.2**: Base de datos relacional
- **Maven**: Gestión de dependencias y build
- **BCrypt**: Hashing de contraseñas
- **SLF4J/Logback**: Sistema de logging
- **Webcam Capture**: Captura de video para videollamadas

## Solución de Problemas

Para problemas comunes de instalación y configuración, consultar la sección [Solución de Problemas](docs/INSTALLATION.md#solución-de-problemas) en INSTALLATION.md.

Para problemas durante el uso de la aplicación, consultar la sección [Solución de Problemas](docs/USER_GUIDE.md#solución-de-problemas) en USER_GUIDE.md.

## Licencia

Este proyecto es de uso educativo.
