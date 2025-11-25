# WhatsApp Clone

Aplicación de mensajería instantánea en Java con chat, transferencia de archivos y videollamadas.

## Inicio Rápido

### Primera vez

**Requisitos:** Java 17+, Maven 3.6+, MySQL 8.0+

```bash
# Windows
setup.bat

# Linux/macOS  
./setup.sh
```

El script configura todo automáticamente. Solo edita `db.properties` cuando se abra.

### Ya configurado

```bash
mvn javafx:run
```

### Manual

```bash
cp db.properties.example db.properties  # Editar credenciales
mvn clean install && mvn javafx:run
```

## Uso

Registrarse → Elegir modo (Servidor o Cliente) → Chatear/Archivos/Video. Ver [USER_GUIDE.md](docs/USER_GUIDE.md).

## Características

- Chat en tiempo real
- Transferencia de archivos con verificación de integridad
- Videollamadas con audio
- Autenticación segura (BCrypt)
- Base de datos MySQL

## Arquitectura

Capas: UI, Servicios, Red, Datos. Patrones: Singleton, Factory, Observer, Command, Strategy, Facade, Repository. Ver [ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Problemas Comunes

**No conecta a MySQL**
- Verificar que MySQL esté ejecutándose
- Revisar credenciales en `db.properties`

**Puerto 5000 ocupado**
```bash
# Windows
netstat -ano | findstr :5000
taskkill /PID <PID> /F

# Linux/macOS
lsof -ti:5000 | xargs kill -9
```

**Error de compilación**
```bash
mvn clean
mvn install
```

## Documentación

- [docs/USER_GUIDE.md](docs/USER_GUIDE.md) - Manual de usuario
- [docs/INSTALLATION.md](docs/INSTALLATION.md) - Instalación detallada
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) - Arquitectura y patrones
- [docs/API.md](docs/API.md) - API y servicios
- [docs/DATABASE.md](docs/DATABASE.md) - Esquema de base de datos
- [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) - Guía de desarrollo

## Stack

Java 17, JavaFX 21, MySQL 8.2, Maven, BCrypt, SLF4J/Logback, Webcam Capture

