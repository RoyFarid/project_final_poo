# Arquitectura del Sistema

## Visión General

El proyecto usa una arquitectura en capas con separación de responsabilidades e implementación de patrones de diseño.

## Capas de la Arquitectura

### 1. Capa de Presentación (UI)
`com.whatsapp.ui`

Maneja la interfaz gráfica y eventos del usuario.

**Componentes**:
- `LoginView`: Pantalla de autenticación
- `ServerView`: Interfaz del modo servidor
- `ClientView`: Interfaz del modo cliente
- `ChatView`: Vista de chat individual

**Responsabilidades**:
- Capturar eventos del usuario
- Renderizar datos
- Validación básica de entrada
- Delegación a la capa de servicio

### 2. Capa de Servicio
`com.whatsapp.service`

Lógica de negocio de la aplicación.

**Componentes principales**:

#### NetworkFacade
**Patrón**: Facade
```java
public class NetworkFacade {
    // Orquesta todos los servicios de red
    - ConnectionManager
    - ChatService
    - FileTransferService
    - VideoStreamService
    - AudioStreamService
}
```
Simplifica operaciones de red complejas.

#### AuthService
```java
public class AuthService {
    + registrar(username, password, email): Usuario
    + autenticar(username, password): Optional<Usuario>
    + cambiarPassword(userId, oldPassword, newPassword): boolean
}
```
Autenticación y autorización con BCrypt.

#### ChatService
Maneja mensajes de texto entre usuarios.

#### FileTransferService
Transferencia de archivos con checksums.

#### VideoStreamService & AudioStreamService
Streaming de video y audio.

#### LogService
**Patrón**: Singleton
Logging de operaciones y errores.

### 3. Capa de Red (Network)
**Ubicación**: `com.whatsapp.network`

Gestiona todas las comunicaciones de red.

#### ConnectionManager
**Patrón**: Singleton

```java
public class ConnectionManager {
    - Map<String, Socket> connections
    - Map<String, DataOutputStream> outputStreams
    - ExecutorService executorService
    
    + startServer(port): void
    + connectToServer(host, port): Socket
    + send(connectionId, data): void
    + broadcast(data): void
}
```

**Características**:
- Gestión de múltiples conexiones concurrentes
- Pool de hilos para manejo asíncrono
- Estados de conexión bien definidos

#### Factory Pattern
**Ubicación**: `com.whatsapp.network.factory`

```java
public class SocketFactory {
    + createTcpSocket(host, port): Socket
    + createTcpServerSocket(port): ServerSocket
}
```

**Beneficio**: Centraliza la creación de sockets y permite fácil extensión.

#### Observer Pattern
**Ubicación**: `com.whatsapp.network.observer`

```java
public class EventAggregator {
    - List<NetworkEventObserver> observers
    
    + subscribe(observer): void
    + unsubscribe(observer): void
    + publish(event): void
}
```

**Eventos soportados**:
- `CONNECTED`: Nueva conexión establecida
- `DISCONNECTED`: Conexión terminada
- `MESSAGE_RECEIVED`: Mensaje recibido
- `ERROR`: Error de red

#### Strategy Pattern
**Ubicación**: `com.whatsapp.network.strategy`

```java
public interface RetryStrategy {
    long getDelay(int attempt);
    boolean shouldRetry(int attempt, Exception error);
}
```

**Implementaciones**:
- `FixedDelayStrategy`: Reintento con delay fijo
- `ExponentialBackoffStrategy`: Reintento con backoff exponencial

### 4. Capa de Comando (Command)
**Ubicación**: `com.whatsapp.command`

**Patrón**: Command

```java
public interface Command {
    void execute();
    void undo();
}

public class CommandInvoker {
    - Stack<Command> history
    
    + executeCommand(command): void
    + undoLastCommand(): void
}
```

**Comandos implementados**:
- `SendMessageCommand`: Envío de mensajes
- `SendFileCommand`: Envío de archivos
- `StartVideoCallCommand`: Inicio de videollamada

**Beneficios**:
- Desacoplamiento entre UI y lógica
- Historial de acciones
- Posibilidad de deshacer operaciones

### 5. Capa de Protocolo
**Ubicación**: `com.whatsapp.protocol`

Define el formato de los mensajes en la red.

```java
public class MessageHeader {
    - byte version
    - byte tipo (CHAT, ARCHIVO, VIDEO, AUDIO, CONTROL)
    - long timestamp
    - int contentLength
    - String senderId
    
    + toBytes(): byte[]
    + fromBytes(data): MessageHeader
}
```

**Estructura del mensaje**:
```
[Header: 256 bytes] + [Content: variable]
```

### 6. Capa de Datos (Repository)
**Ubicación**: `com.whatsapp.repository`

**Patrón**: Repository

Abstrae el acceso a datos de la base de datos.

```java
public interface IRepository<T> {
    T save(T entity);
    Optional<T> findById(Long id);
    List<T> findAll();
    void update(T entity);
    void delete(Long id);
}
```

**Implementaciones**:
- `UsuarioRepository`: CRUD de usuarios
- `LogRepository`: Gestión de logs
- `TransferenciaRepository`: Historial de transferencias

### 7. Capa de Base de Datos
**Ubicación**: `com.whatsapp.database`

#### DatabaseManager
**Patrón**: Singleton

```java
public class DatabaseManager {
    - DatabaseConfig config
    - Connection connection
    
    + getInstance(): DatabaseManager
    + getConnection(): Connection
    - initializeDatabase(): void
}
```

**Responsabilidades**:
- Gestión de pool de conexiones
- Inicialización automática de tablas
- Manejo de transacciones

#### DatabaseConfig
Carga configuración desde `db.properties`:
```properties
db.host=localhost
db.port=3306
db.database=whatsapp_clone
db.username=root
db.password=****
```

##  Flujo de Datos

### Flujo de Autenticación
```
LoginView → AuthService → UsuarioRepository → DatabaseManager → MySQL
                ↓
         LoginResult (Usuario + NetworkFacade)
                ↓
         ClientView/ServerView
```

### Flujo de Mensajería
```
ChatView → Command → NetworkFacade → ChatService → ConnectionManager
                                                          ↓
                                                    Socket.send()
                                                          ↓
                                           [Red TCP/IP]
                                                          ↓
                                             ConnectionManager.receive()
                                                          ↓
                                             EventAggregator.publish()
                                                          ↓
                                             NetworkFacade.onEvent()
                                                          ↓
                                             ChatService.handleMessage()
                                                          ↓
                                             ChatView.displayMessage()
```

### Flujo de Transferencia de Archivos
```
ClientView → SendFileCommand → FileTransferService
                                      ↓
                            [Divide archivo en chunks]
                                      ↓
                            ConnectionManager.send()
                                      ↓
                            [Envía header + chunks]
                                      ↓
                     FileTransferService.handleIncoming()
                                      ↓
                            [Reconstruye archivo]
                                      ↓
                            [Verifica checksum]
                                      ↓
                         TransferenciaRepository.save()
```

##  Principios SOLID Aplicados

### Single Responsibility Principle (SRP)
Cada clase tiene una única responsabilidad:
- `AuthService`: Solo autenticación
- `ChatService`: Solo mensajería
- `ConnectionManager`: Solo gestión de conexiones

### Open/Closed Principle (OCP)
- `RetryStrategy`: Abierto a extensión (nuevas estrategias), cerrado a modificación
- `Command`: Nuevos comandos sin modificar el invoker

### Liskov Substitution Principle (LSP)
- Todas las implementaciones de `RetryStrategy` son intercambiables
- Todos los `Repository` pueden usarse polimórficamente

### Interface Segregation Principle (ISP)
- `INetworkClient`: Interfaz específica para clientes
- `IRepository`: Interfaz específica para repositorios

### Dependency Inversion Principle (DIP)
- Las capas superiores dependen de abstracciones (`IRepository`, `RetryStrategy`)
- No hay dependencias directas entre capas concretas

##  Gestión de Concurrencia

### ExecutorService
Todos los servicios usan pools de hilos:
```java
ExecutorService executorService = Executors.newCachedThreadPool();
```

### Thread-Safe Collections
```java
Map<String, Socket> connections = new ConcurrentHashMap<>();
List<NetworkEventObserver> observers = new CopyOnWriteArrayList<>();
```

### Sincronización
```java
synchronized (outputStream) {
    outputStream.writeInt(data.length);
    outputStream.write(data);
    outputStream.flush();
}
```

##  Diagramas

### Diagrama de Clases Principal
```

   Main      

       
       
       ↓          ↓          ↓
  
LoginView ServerView ClientView
  
                             
     
                  ↓
         
          NetworkFacade  
         
                  
     
     ↓            ↓            ↓
  
ChatService FileTransfer VideoStream
  
                             
      
                  ↓
       
       ConnectionManager 
       
```

##  Extensibilidad

### Agregar Nuevo Tipo de Mensaje
1. Agregar constante en `MessageHeader.MessageType`
2. Crear servicio correspondiente
3. Registrar handler en `NetworkFacade`

### Agregar Nueva Estrategia de Reintento
1. Implementar interfaz `RetryStrategy`
2. Instanciar en el cliente necesario

### Agregar Nueva Entidad
1. Crear modelo en `com.whatsapp.model`
2. Crear repository implementando `IRepository`
3. Agregar tabla en `DatabaseManager.initializeDatabase()`

---

Esta arquitectura proporciona una base sólida, mantenible y escalable para el sistema de mensajería.
