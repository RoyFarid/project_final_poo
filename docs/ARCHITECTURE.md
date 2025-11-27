# Arquitectura del Sistema

## Visión General

RoomWave implementa una arquitectura en capas con separación clara de responsabilidades. El sistema sigue el patrón cliente-servidor donde un servidor central gestiona múltiples clientes conectados mediante TCP/IP.

## Arquitectura en Capas

### Capa de Presentación (UI)

**Paquete**: `com.whatsapp.ui`

Componentes principales:
- `LoginView`: Autenticación y registro de usuarios
- `ServerView`: Interfaz del servidor con gestión de clientes y rooms
- `ClientView`: Interfaz del cliente para conectarse al servidor
- `ChatView`: Vista de chat individual entre usuarios
- `RoomChatView`: Vista de chat grupal en rooms
- `ClientRoomChatView`: Vista de rooms desde el cliente

Responsabilidades:
- Captura de eventos del usuario
- Renderizado de datos en la interfaz
- Validación básica de entrada
- Delegación de operaciones a la capa de servicio

### Capa de Servicio

**Paquete**: `com.whatsapp.service`

#### NetworkFacade

Patrón: Facade

Orquesta todos los servicios de red proporcionando una interfaz unificada:
- `ConnectionManager`: Gestión de conexiones
- `ChatService`: Mensajería de texto
- `FileTransferService`: Transferencia de archivos
- `VideoStreamService`: Streaming de video
- `AudioStreamService`: Streaming de audio
- `ControlService`: Mensajes de control del sistema
- `RoomService`: Gestión de salas de chat

#### AuthService

Gestiona autenticación y autorización:
- Registro de nuevos usuarios con validación
- Autenticación con BCrypt (12 rounds)
- Cambio de contraseñas
- Gestión de estados de usuario (ACTIVO, INACTIVO, BLOQUEADO)

#### ChatService

Maneja mensajería de texto:
- Envío y recepción de mensajes
- Serialización mediante MessageHeader
- Verificación de checksums
- Persistencia en base de datos

#### FileTransferService

Transferencia de archivos con integridad:
- División de archivos en chunks de 64KB
- Cálculo de checksums SHA-256
- Reintentos con estrategias configurables
- Verificación de integridad al recibir

#### VideoStreamService y AudioStreamService

Streaming multimedia en tiempo real:
- Captura desde webcam (30 FPS)
- Captura de audio desde micrófono (44.1 KHz)
- Compresión de frames (JPEG para video)
- Transmisión mediante mensajes VIDEO y AUDIO

#### RoomService

Patrón: Singleton

Gestión de salas de chat grupales:
- Creación de solicitudes de rooms (pendientes de aprobación)
- Aprobación/rechazo de rooms por el servidor
- Unión y salida de usuarios a rooms
- Envío de mensajes y archivos a rooms
- Cierre de rooms

#### ControlService

Manejo de mensajes de control del sistema:
- Lista de usuarios conectados
- Notificaciones de conexión/desconexión
- Autenticación remota (AUTH_REQUEST/RESPONSE)
- Registro remoto (REGISTER_REQUEST/RESPONSE)
- Gestión de rooms (CREATE, APPROVE, REJECT, JOIN, LEAVE)
- Control administrativo (MUTE, BLOCK_MESSAGES, DISABLE_CAMERA)

#### RemoteAuthClient

Patrón: Proxy

Cliente auxiliar para autenticación remota:
- Encapsula solicitudes de autenticación al servidor
- Maneja respuestas asíncronas mediante CompletableFuture
- Timeout de 10 segundos para operaciones

#### UserAliasRegistry

Patrón: Singleton

Registro centralizado de alias de usuarios:
- Mapea connectionIds a nombres de usuario legibles
- Sincronización entre servidor y clientes
- Actualización en tiempo real

#### ServerRuntime

Mantiene el estado del proceso:
- Identifica si el proceso actual es servidor o cliente
- Controla acceso a base de datos (solo servidor accede directamente)

#### LogService

Patrón: Singleton

Sistema de logging centralizado:
- Logging a consola y base de datos
- Generación de TraceIds para seguimiento
- Niveles: INFO, WARN, ERROR, DEBUG

### Capa de Red (Network)

**Paquete**: `com.whatsapp.network`

#### ConnectionManager

Patrón: Singleton

Gestión centralizada de conexiones TCP/IP:
- Pool de conexiones concurrentes
- Gestión de estados (DESCONECTADO, CONECTANDO, ACTIVO, ERROR)
- Envío y recepción de datos
- Broadcast a múltiples clientes
- Thread-safe mediante ConcurrentHashMap

#### SocketFactory

Patrón: Factory Method

Creación de sockets TCP:
- `createTcpSocket`: Socket cliente
- `createTcpServerSocket`: ServerSocket para servidor
- Centraliza configuración de sockets

#### EventAggregator

Patrón: Observer (Singleton)

Sistema de eventos pub/sub:
- Suscripción de observadores
- Publicación de eventos de red
- Notificación asíncrona a suscriptores
- Thread-safe con CopyOnWriteArrayList

#### RetryStrategy

Patrón: Strategy

Estrategias de reintento:
- `FixedDelayStrategy`: Reintento con delay fijo
- `ExponentialBackoffStrategy`: Backoff exponencial
- Intercambiables sin modificar código cliente

### Capa de Protocolo

**Paquete**: `com.whatsapp.protocol`

#### MessageHeader

Estructura de cabecera de mensajes:
- Tipo de mensaje (CHAT, ARCHIVO, VIDEO, AUDIO, CONTROL)
- Longitud del payload
- ID de correlación
- Checksum CRC32
- Serialización a bytes

Formato: `[Header: 13 bytes][Payload: variable]`

### Capa de Comando

**Paquete**: `com.whatsapp.command`

Patrón: Command

Comandos implementados:
- `SendMessageCommand`: Envío de mensajes
- `SendFileCommand`: Envío de archivos
- `StartVideoCallCommand`: Inicio de videollamada

`CommandInvoker` gestiona ejecución y historial para deshacer operaciones.

### Capa de Datos (Repository)

**Paquete**: `com.whatsapp.repository`

Patrón: Repository

Repositorios implementados:
- `UsuarioRepository`: CRUD de usuarios
- `LogRepository`: Gestión de logs con consultas por fecha, traceId, userId
- `TransferenciaRepository`: Historial de transferencias
- `RoomRepository`: Gestión de rooms y miembros

Todos implementan `IRepository<T>` con operaciones CRUD estándar.

### Capa de Base de Datos

**Paquete**: `com.whatsapp.database`

#### DatabaseManager

Patrón: Singleton

Gestión de conexión a MySQL:
- Pool de conexiones
- Inicialización automática de tablas
- Configuración desde `db.properties`
- Manejo de transacciones

#### DatabaseConfig

Carga configuración desde archivo de propiedades:
- Host, puerto, nombre de base de datos
- Credenciales de usuario
- Validación de configuración

## Flujos de Datos

### Flujo de Autenticación

```
LoginView → RemoteAuthClient → ControlService.sendAuthRequest
                                    ↓
                            [Red TCP/IP]
                                    ↓
                        ControlService.handleAuthRequest
                                    ↓
                            AuthService.autenticar
                                    ↓
                        UsuarioRepository.findByUsername
                                    ↓
                            DatabaseManager
                                    ↓
                                MySQL
                                    ↓
                        ControlService.sendAuthResponse
                                    ↓
                        RemoteAuthClient.onNetworkEvent
                                    ↓
                            LoginResult → ClientView/ServerView
```

### Flujo de Mensajería

```
ChatView → SendMessageCommand → NetworkFacade.sendMessage
                                    ↓
                            ChatService.sendMessage
                                    ↓
                        MessageHeader + serialización
                                    ↓
                        ConnectionManager.send
                                    ↓
                            [Red TCP/IP]
                                    ↓
                        ConnectionManager.receive
                                    ↓
                        EventAggregator.publish
                                    ↓
                        ChatService.handleReceivedMessage
                                    ↓
                        ChatView.displayMessage
```

### Flujo de Creación de Room

```
ClientView → ControlService.sendRoomCreateRequest
                                    ↓
                            [Red TCP/IP]
                                    ↓
                        ControlService.handleRoomCreateRequest
                                    ↓
                        RoomService.createRoomRequest
                                    ↓
                        RoomRepository.save
                                    ↓
                        ControlService.sendRoomCreateResponse
                                    ↓
                        ServerView muestra solicitud pendiente
                                    ↓
                        Servidor aprueba → RoomService.approveRoom
                                    ↓
                        Notificación a todos los miembros
```

## Patrones de Diseño Implementados

### Singleton
- `ConnectionManager`: Una única instancia gestiona todas las conexiones
- `EventAggregator`: Un único agregador de eventos
- `LogService`: Logging centralizado
- `UserAliasRegistry`: Registro único de alias
- `RoomService`: Gestión única de rooms
- `DatabaseManager`: Conexión única a base de datos

### Factory Method
- `SocketFactory`: Creación de sockets TCP

### Observer
- `EventAggregator` / `NetworkEventObserver`: Sistema pub/sub para eventos de red

### Strategy
- `RetryStrategy`: Estrategias intercambiables de reintento

### Command
- `Command` / `CommandInvoker`: Encapsulación de acciones con historial

### Facade
- `NetworkFacade`: Interfaz unificada para servicios de red

### Repository
- `IRepository<T>`: Abstracción de acceso a datos

### Proxy
- `RemoteAuthClient`: Proxy para autenticación remota

## Principios SOLID

### Single Responsibility
Cada clase tiene una única responsabilidad:
- `AuthService`: Solo autenticación
- `ChatService`: Solo mensajería
- `ConnectionManager`: Solo gestión de conexiones

### Open/Closed
- `RetryStrategy`: Abierto a extensión (nuevas estrategias), cerrado a modificación
- `Command`: Nuevos comandos sin modificar el invoker

### Liskov Substitution
- Implementaciones de `RetryStrategy` son intercambiables
- Repositorios pueden usarse polimórficamente

### Interface Segregation
- `IRepository`: Interfaz específica para repositorios
- `NetworkEventObserver`: Interfaz específica para observadores

### Dependency Inversion
- Capas superiores dependen de abstracciones (`IRepository`, `RetryStrategy`)
- No hay dependencias directas entre capas concretas

## Gestión de Concurrencia

### ExecutorService
Servicios usan pools de hilos para operaciones asíncronas:
```java
ExecutorService executorService = Executors.newCachedThreadPool();
```

### Thread-Safe Collections
```java
Map<String, Socket> connections = new ConcurrentHashMap<>();
List<NetworkEventObserver> observers = new CopyOnWriteArrayList<>();
```

### Sincronización
Operaciones críticas sincronizadas:
```java
synchronized (outputStream) {
    outputStream.writeInt(data.length);
    outputStream.write(data);
    outputStream.flush();
}
```

## Extensibilidad

### Agregar Nuevo Tipo de Mensaje
1. Agregar constante en `MessageHeader.MessageType`
2. Crear servicio correspondiente
3. Registrar handler en `NetworkFacade`

### Agregar Nueva Estrategia de Reintento
1. Implementar interfaz `RetryStrategy`
2. Instanciar en el servicio necesario

### Agregar Nueva Entidad
1. Crear modelo en `com.whatsapp.model`
2. Crear repository implementando `IRepository`
3. Agregar tabla en `DatabaseManager.initializeDatabase()`

