# WhatsApp Clone - Proyecto Final POO

Aplicación de mensajería instantánea similar a WhatsApp desarrollada en Java con JavaFX, implementando los principales patrones de diseño y principios SOLID.

## Características Principales

- ✅ **Autenticación y Registro**: Sistema de usuarios con hashing de contraseñas (bcrypt)
- ✅ **Mensajería Instantánea**: Envío y recepción de mensajes de texto sobre TCP
- ✅ **Transferencia de Archivos**: Envío de archivos grandes con fragmentación, reanudación y selector de carpeta al recibir
- ✅ **Videollamadas**: Streaming de video en tiempo real sobre UDP
- ✅ **Base de Datos**: MySQL para persistencia de usuarios, logs y transferencias
- ✅ **Logging**: Sistema de logs con correlación de trazas (traceId)

## Arquitectura

### Topología
- **Cliente/Servidor**: El servidor acepta múltiples conexiones de clientes
- **Protocolos**: TCP para control y archivos; UDP para video

### Módulos Principales

1. **AuthService**: Autenticación y registro de usuarios
2. **ChatService**: Mensajería instantánea
3. **FileTransferService**: Transferencia de archivos con fragmentación
4. **VideoStreamService**: Streaming de video en tiempo real
5. **ConnectionManager**: Gestión de conexiones TCP/UDP
6. **LogService**: Sistema de logging con correlación
7. **NetworkFacade**: Orquestación de servicios de red

### Patrones de Diseño Implementados

- ✅ **Factory Method**: `SocketFactory` para crear sockets TCP/UDP
- ✅ **Strategy**: `RetryStrategy` (ExponentialBackoff, FixedDelay)
- ✅ **Observer**: `EventAggregator` para notificaciones de eventos de red
- ✅ **Command**: `Command`, `SendMessageCommand`, `SendFileCommand`, `StartVideoCallCommand`
- ✅ **Singleton**: `ConnectionManager`, `LogService`, `EventAggregator`, `DatabaseManager`
- ✅ **Facade**: `NetworkFacade` para orquestar servicios
- ✅ **State**: `ConnectionState` para estados de conexión
- ✅ **Repository**: `UsuarioRepository`, `LogRepository`, `TransferenciaRepository`

### Principios SOLID

- **SRP**: Cada servicio tiene una responsabilidad única
- **DIP**: Uso de interfaces (`INetworkClient`, `IRepository`)
- **ISP**: Interfaces específicas para cada contrato
- **OCP**: Extensible mediante Strategy y Factory
- **LSP**: Implementaciones respetan contratos de interfaces

## Modelo de Datos

### Entidades

- **Usuario**: Id, Username, PasswordHash, Salt, Email, FechaCreación, ÚltimoLogin, Estado
- **Log**: Id, Nivel, Mensaje, Módulo, Fecha, TraceId, UserId
- **Transferencia**: Id, Tipo, Nombre, Tamaño, Checksum, Estado, Inicio, Fin, UserId, PeerIp

### Protocolo de Mensajes

```
struct Header {
  uint8 Tipo;        // 0=Chat, 1=Archivo, 2=Video, 3=Ctrl
  uint32 Longitud;   // tamaño del payload
  uint32 CorrelId;   // ID de correlación
  uint32 Checksum;   // CRC32 truncado
}
```

## Requisitos

- Java 17 o superior
- Maven 3.6+
- JavaFX 21
- MySQL Server 8.0+ (o MariaDB 10.3+)

## Instalación y Ejecución

1. **Clonar el repositorio**:
```bash
git clone <repository-url>
cd proyecto_final_poo
```

2. **Compilar el proyecto**:
```bash
mvn clean compile
```

3. **Ejecutar la aplicación**:
```bash
mvn javafx:run
```

O usando Java directamente:
```bash
mvn clean package
java --module-path <javafx-path> --add-modules javafx.controls,javafx.fxml,javafx.media -cp target/whatsapp-clone-1.0.0.jar com.whatsapp.Main
```

## Uso de la Aplicación

### 1. Login/Registro
- Al iniciar, aparecerá la pantalla de login
- Puede registrarse creando una nueva cuenta
- O iniciar sesión con credenciales existentes

### 2. Modo Servidor
- Seleccione "Servidor" después del login
- Configure el puerto (por defecto 8080)
- Haga clic en "Iniciar Servidor"
- Verá:
  - Lista de usuarios conectados
  - Actividades en tiempo real (mensajes, archivos, videollamadas)

### 3. Modo Cliente
- Seleccione "Cliente" después del login
- Ingrese la dirección del servidor (localhost por defecto)
- Ingrese el puerto (8080 por defecto)
- Haga clic en "Conectar"
- Verá la lista de usuarios conectados
- Haga clic en un usuario para abrir una ventana de chat

### 4. Chat
- Escriba mensajes en el campo de texto
- Envíe archivos con el botón "Enviar Archivo"; al completar en el receptor se abrirá un selector de carpeta para guardar el archivo recibido
- Inicie videollamadas con el botón "Videollamada"

## Estructura del Proyecto

```
src/main/java/com/whatsapp/
├── Main.java                    # Punto de entrada
├── model/                       # Modelos de datos
│   ├── Usuario.java
│   ├── Log.java
│   └── Transferencia.java
├── database/                    # Gestión de BD
│   └── DatabaseManager.java
├── repository/                  # Repositorios
│   ├── IRepository.java
│   ├── UsuarioRepository.java
│   ├── LogRepository.java
│   └── TransferenciaRepository.java
├── service/                     # Servicios
│   ├── AuthService.java
│   ├── ChatService.java
│   ├── FileTransferService.java
│   ├── VideoStreamService.java
│   ├── LogService.java
│   └── NetworkFacade.java
├── network/                     # Red y conexiones
│   ├── ConnectionManager.java
│   ├── ConnectionState.java
│   ├── INetworkClient.java
│   ├── factory/
│   │   └── SocketFactory.java
│   ├── strategy/
│   │   ├── RetryStrategy.java
│   │   ├── ExponentialBackoffStrategy.java
│   │   └── FixedDelayStrategy.java
│   └── observer/
│       ├── NetworkEvent.java
│       ├── NetworkEventObserver.java
│       └── EventAggregator.java
├── protocol/                    # Protocolos
│   └── MessageHeader.java
├── command/                     # Patrón Command
│   ├── Command.java
│   ├── SendMessageCommand.java
│   ├── SendFileCommand.java
│   ├── StartVideoCallCommand.java
│   └── CommandInvoker.java
└── ui/                          # Interfaz gráfica
    ├── LoginView.java
    ├── ServerView.java
    ├── ClientView.java
    └── ChatView.java
```

## Seguridad

- **Hashing de contraseñas**: bcrypt con 12 rounds
- **Validación de entrada**: Sanitización y validación de datos
- **Checksums**: Verificación de integridad de mensajes y archivos
- **Límites**: Validación de tamaños de archivo y mensajes

## Concurrencia

- **Thread Pool**: Ejecutor de servicios para manejo de múltiples clientes
- **Thread-Safe**: Uso de `ConcurrentHashMap` y `CopyOnWriteArrayList`
- **Sincronización**: Locks finos y `synchronized` donde es necesario
- **Backpressure**: Control de tasa de envío y buffers limitados

## Logging

El sistema de logging registra:
- Eventos de conexión/desconexión
- Mensajes enviados/recibidos
- Transferencias de archivos
- Errores y excepciones
- Todo con correlación mediante `traceId`

## Notas

- La base de datos MySQL se configura mediante el archivo `db.properties` (ver `CONFIGURACION_MYSQL.md`)
- La base de datos y las tablas se crean automáticamente si no existen
- El streaming de video requiere implementación adicional de captura de cámara
- Los archivos se fragmentan en chunks de 64 KB
- El protocolo UDP para video usa el puerto 8888 por defecto

## Autor

Proyecto desarrollado como trabajo final de Programación Orientada a Objetos.

## Licencia

Este proyecto es de uso educativo.

