# API y Servicios

## Contenidos

1. [Servicios Principales](#servicios-principales)
2. [Protocolo de Mensajería](#protocolo-de-mensajería)
3. [API de Red](#api-de-red)
4. [Servicios de Negocio](#servicios-de-negocio)
5. [Comandos](#comandos)
6. [Eventos](#eventos)
7. [Repositorios](#repositorios)

---

## Servicios Principales

### NetworkFacade

Interfaz principal para operaciones de red.

```java
public class NetworkFacade implements NetworkEventObserver
```

#### Constructor
```java
public NetworkFacade()
```
Inicializa todos los servicios de red y se suscribe a eventos.

#### Métodos de Conexión

##### startServer
```java
public void startServer(int port) throws IOException
```
Inicia el servidor en el puerto especificado.

**Parámetros:**
- `port`: Puerto en el que escuchar (típicamente 5000)

**Excepciones:**
- `IOException`: Si el puerto está ocupado o hay error de red

**Ejemplo:**
```java
NetworkFacade facade = new NetworkFacade();
facade.startServer(5000);
```

##### connectToServer
```java
public void connectToServer(String host, int port) throws IOException
```
Conecta como cliente a un servidor.

**Parámetros:**
- `host`: IP o hostname del servidor
- `port`: Puerto del servidor

**Ejemplo:**
```java
facade.connectToServer("192.168.1.100", 5000);
```

##### disconnect
```java
public void disconnect()
```
Desconecta todas las conexiones y detiene servicios.

#### Métodos de Chat

##### sendMessage
```java
public void sendMessage(String connectionId, String message, Long userId, String peerIp) throws IOException
```

**Parámetros:**
- `connectionId`: ID de la conexión destino
- `message`: Texto del mensaje
- `userId`: ID del usuario que envía
- `peerIp`: IP del destinatario

**Ejemplo:**
```java
facade.sendMessage(connId, "Hola mundo", 1L, "192.168.1.100");
```

#### Métodos de Transferencia de Archivos

##### sendFile
```java
public void sendFile(String serverConnectionId, String targetConnectionId, String filePath, Long userId) throws IOException
```

**Parámetros:**
- `serverConnectionId`: ID de conexión al servidor
- `targetConnectionId`: ID del destinatario
- `filePath`: Ruta completa del archivo
- `userId`: ID del usuario

**Ejemplo:**
```java
facade.sendFile(serverId, targetId, "/path/to/file.pdf", 1L);
```

#### Métodos de Video

##### startVideoCall
```java
public void startVideoCall(String serverConnectionId, String targetConnectionId)
```
Inicia videollamada con streaming de video y audio.

##### stopVideoCall
```java
public void stopVideoCall()
```
Detiene la videollamada activa.

##### setMicrophoneMuted
```java
public void setMicrophoneMuted(boolean muted)
```
Silencia o activa el micrófono durante llamada.

##### setSpeakerMuted
```java
public void setSpeakerMuted(boolean muted)
```
Silencia o activa el altavoz durante llamada.

#### Métodos de Información

##### getConnectionState
```java
public ConnectionState getConnectionState()
```
Retorna el estado actual de conexión.

**Estados posibles:**
- `DESCONECTADO`
- `CONECTANDO`
- `ACTIVO`
- `ERROR`

##### getConnectedClients
```java
public Set<String> getConnectedClients()
```
Retorna conjunto de IDs de clientes conectados.

---

##  Protocolo de Mensajería

### MessageHeader

Estructura de cabecera de todos los mensajes en la red.

```java
public class MessageHeader implements Serializable
```

#### Constantes
```java
public static final int HEADER_SIZE = 13; // bytes
```

#### Estructura

| Campo | Tipo | Tamaño | Descripción |
|-------|------|--------|-------------|
| tipo | byte | 1 byte | Tipo de mensaje |
| longitud | int | 4 bytes | Tamaño del payload |
| correlId | int | 4 bytes | ID de correlación |
| checksum | int | 4 bytes | Verificación CRC32 |

#### Tipos de Mensaje

```java
public static class MessageType {
    public static final byte CHAT = 0;
    public static final byte ARCHIVO = 1;
    public static final byte VIDEO = 2;
    public static final byte CONTROL = 3;
    public static final byte AUDIO = 4;
}
```

#### Métodos

##### toBytes
```java
public byte[] toBytes()
```
Convierte el header a array de bytes para transmisión.

##### fromBytes
```java
public static MessageHeader fromBytes(byte[] data)
```
Reconstruye header desde bytes recibidos.

**Ejemplo:**
```java
MessageHeader header = new MessageHeader(
    MessageType.CHAT,
    messageBytes.length,
    12345,
    calculateChecksum(messageBytes)
);
byte[] headerBytes = header.toBytes();
```

### Formato de Mensaje Completo

Header (13 bytes) + Payload (variable). El header incluye tipo, longitud, correlId y checksum.

---

##  API de Red

### ConnectionManager

Gestiona todas las conexiones de red.

```java
public class ConnectionManager // Singleton
```

#### Obtener Instancia
```java
ConnectionManager manager = ConnectionManager.getInstance();
```

#### Métodos Principales

##### send
```java
public void send(String connectionId, byte[] data) throws IOException
```
Envía datos a una conexión específica.

**Protocolo:**
```java
// Formato: [length:4 bytes][data:length bytes]
outputStream.writeInt(data.length);
outputStream.write(data);
outputStream.flush();
```

##### broadcast
```java
public void broadcast(byte[] data)
```
Envía datos a todas las conexiones activas.

**Ejemplo:**
```java
ConnectionManager manager = ConnectionManager.getInstance();
byte[] message = createMessage("Broadcast a todos");
manager.broadcast(message);
```

##### disconnectClient
```java
public void disconnectClient(String connectionId)
```
Desconecta un cliente específico.

### SocketFactory

Factory para crear sockets.

```java
public class SocketFactory
```

#### Métodos

##### createTcpSocket
```java
public static Socket createTcpSocket(String host, int port) throws IOException
```

##### createTcpServerSocket
```java
public static ServerSocket createTcpServerSocket(int port) throws IOException
```

---

##  Servicios de Negocio

### AuthService

Gestión de autenticación y seguridad.

```java
public class AuthService
```

#### Métodos

##### registrar
```java
public Usuario registrar(String username, String password, String email)
```

**Validaciones:**
- Username no vacío, único
- Password mínimo 6 caracteres
- Email válido con @

**Proceso:**
Valida entrada, verifica unicidad, genera salt BCrypt (12 rounds), hashea password, persiste usuario.

**Ejemplo:**
```java
AuthService auth = new AuthService();
Usuario user = auth.registrar("juanito", "pass123", "juan@email.com");
```

##### autenticar
```java
public Optional<Usuario> autenticar(String username, String password)
```

**Retorna:**
- `Optional.of(usuario)` si autenticación exitosa
- `Optional.empty()` si falla

**Proceso:**
Busca usuario, verifica estado ACTIVO, valida password con BCrypt, actualiza último login.

**Ejemplo:**
```java
Optional<Usuario> userOpt = auth.autenticar("juanito", "pass123");
if (userOpt.isPresent()) {
    Usuario user = userOpt.get();
    // Iniciar sesión
}
```

##### cambiarPassword
```java
public boolean cambiarPassword(Long userId, String oldPassword, String newPassword)
```

### ChatService

Gestión de mensajes de chat.

```java
public class ChatService
```

#### sendMessage
```java
public void sendMessage(String connectionId, String message, Long userId, String peerIp) throws IOException
```

**Proceso:**
Crea MessageHeader (tipo CHAT), serializa mensaje, calcula checksum, envía vía ConnectionManager, registra en DB.

#### handleReceivedMessage
```java
public void handleReceivedMessage(byte[] data, String source)
```

**Proceso:**
Parsea MessageHeader, extrae payload, verifica checksum, publica evento MESSAGE_RECEIVED, registra en DB.

### FileTransferService

Transferencia de archivos con integridad.

```java
public class FileTransferService
```

#### sendFile
```java
public void sendFile(String serverConnectionId, String targetConnectionId, String filePath, Long userId) throws IOException
```

**Proceso:**
Lee archivo, calcula checksum SHA-256, divide en chunks 64KB, envía header + chunks, registra en DB.

**Metadata enviada:**
```java
{
    "fileName": "documento.pdf",
    "fileSize": 2048576,
    "checksum": "sha256hash...",
    "totalChunks": 32
}
```

#### handleIncomingPacket
```java
public void handleIncomingPacket(byte[] data, String source)
```

**Proceso:**
Identifica tipo (header/chunk/final), almacena chunks en buffer, al completar ensambla archivo, verifica checksum, guarda en disco.

### VideoStreamService & AudioStreamService

Streaming de multimedia en tiempo real.

```java
public class VideoStreamService
public class AudioStreamService
```

#### startStreaming
```java
public void startStreaming(String serverConnectionId, String targetConnectionId)
```

**Video:**
- Captura desde webcam (30 FPS)
- Comprime frames (JPEG)
- Envía como mensajes VIDEO

**Audio:**
- Captura desde micrófono (44.1 KHz)
- Codifica en formato PCM
- Envía como mensajes AUDIO

#### stopStreaming
```java
public void stopStreaming()
```
Detiene captura y transmisión.

---

## Comandos

### Patrón Command

```java
public interface Command {
    void execute();
    void undo();
}
```

### SendMessageCommand

```java
public class SendMessageCommand implements Command
```

**Constructor:**
```java
public SendMessageCommand(
    NetworkFacade facade,
    String connectionId,
    String message,
    Long userId,
    String peerIp
)
```

**Uso:**
```java
Command cmd = new SendMessageCommand(facade, connId, "Hola", 1L, "192.168.1.1");
CommandInvoker invoker = new CommandInvoker();
invoker.executeCommand(cmd);
```

### SendFileCommand

Similar a SendMessageCommand pero para archivos.

### StartVideoCallCommand

Inicia videollamada.

### CommandInvoker

```java
public class CommandInvoker
```

#### executeCommand
```java
public void executeCommand(Command command)
```
Ejecuta comando y lo guarda en historial.

#### undoLastCommand
```java
public void undoLastCommand()
```
Deshace último comando ejecutado.

---

##  Eventos

### Sistema Observer

```java
public interface NetworkEventObserver {
    void onNetworkEvent(NetworkEvent event);
}
```

### EventAggregator

Publicador/Suscriptor de eventos.

```java
EventAggregator aggregator = EventAggregator.getInstance();
```

#### subscribe
```java
public void subscribe(NetworkEventObserver observer)
```

#### publish
```java
public void publish(NetworkEvent event)
```

### NetworkEvent

```java
public class NetworkEvent
```

**Tipos de eventos:**
```java
public enum EventType {
    CONNECTED,
    DISCONNECTED,
    MESSAGE_RECEIVED,
    ERROR,
    FILE_TRANSFER_PROGRESS,
    FILE_TRANSFER_COMPLETE,
    VIDEO_FRAME_RECEIVED
}
```

**Estructura:**
```java
NetworkEvent event = new NetworkEvent(
    EventType.MESSAGE_RECEIVED,
    messageData,
    sourceConnectionId
);
```

**Ejemplo de uso:**
```java
public class MyObserver implements NetworkEventObserver {
    @Override
    public void onNetworkEvent(NetworkEvent event) {
        switch (event.getType()) {
            case MESSAGE_RECEIVED:
                handleMessage(event.getData());
                break;
            case DISCONNECTED:
                handleDisconnect(event.getSource());
                break;
        }
    }
}

// Suscribirse
EventAggregator.getInstance().subscribe(new MyObserver());
```

---

##  Repositorios

### IRepository Interface

```java
public interface IRepository<T> {
    T save(T entity);
    Optional<T> findById(Long id);
    List<T> findAll();
    void update(T entity);
    void delete(Long id);
}
```

### UsuarioRepository

```java
public class UsuarioRepository implements IRepository<Usuario>
```

#### Métodos Específicos

##### findByUsername
```java
public Optional<Usuario> findByUsername(String username)
```

##### updateLastLogin
```java
public void updateLastLogin(Long userId)
```

**Ejemplo:**
```java
UsuarioRepository repo = new UsuarioRepository();
Optional<Usuario> user = repo.findByUsername("juanito");
```

### LogRepository

```java
public class LogRepository implements IRepository<Log>
```

#### Métodos Específicos

##### findByDateRange
```java
public List<Log> findByDateRange(LocalDateTime start, LocalDateTime end)
```

##### findByTraceId
```java
public List<Log> findByTraceId(String traceId)
```

##### findByUserId
```java
public List<Log> findByUserId(Long userId)
```

### TransferenciaRepository

```java
public class TransferenciaRepository implements IRepository<Transferencia>
```

#### Métodos Específicos

##### findByUserId
```java
public List<Transferencia> findByUserId(Long userId)
```

##### findByEstado
```java
public List<Transferencia> findByEstado(Transferencia.EstadoTransferencia estado)
```

##### updateEstado
```java
public void updateEstado(Long id, Transferencia.EstadoTransferencia nuevoEstado)
```

**Ejemplo:**
```java
TransferenciaRepository repo = new TransferenciaRepository();
List<Transferencia> transfers = repo.findByUserId(1L);
```

---

##  Seguridad

### BCrypt

La aplicación usa BCrypt para hashing de contraseñas.

```java
// Generar hash
String salt = BCrypt.gensalt(12); // 12 rounds
String hash = BCrypt.hashpw(password, salt);

// Verificar
boolean valid = BCrypt.checkpw(password, storedHash);
```

**Configuración:**
- Rounds: 12 (balance seguridad/performance)
- Salt generado automáticamente
- Hash de 60 caracteres

---

##  Códigos de Estado

### ConnectionState

```java
public enum ConnectionState {
    DESCONECTADO,
    CONECTANDO,
    ACTIVO,
    ERROR
}
```

### EstadoTransferencia

```java
public enum EstadoTransferencia {
    PENDIENTE,
    EN_PROGRESO,
    COMPLETADA,
    FALLIDA,
    CANCELADA
}
```

### EstadoUsuario

```java
public enum EstadoUsuario {
    ACTIVO,
    INACTIVO,
    BLOQUEADO
}
```

---

##  Ejemplos de Uso

### Ejemplo Completo: Enviar Mensaje

```java
// 1. Crear NetworkFacade
NetworkFacade facade = new NetworkFacade();

// 2. Conectar a servidor
facade.connectToServer("192.168.1.100", 5000);

// 3. Obtener ID de conexión
String connId = facade.getPrimaryConnectionId();

// 4. Enviar mensaje
facade.sendMessage(connId, "Hola mundo", 1L, "192.168.1.100");
```

### Ejemplo Completo: Transferir Archivo

```java
NetworkFacade facade = new NetworkFacade();
facade.connectToServer("localhost", 5000);

String serverId = facade.getPrimaryConnectionId();
String targetId = obtenerIdDestinatario();

facade.sendFile(serverId, targetId, "/ruta/archivo.pdf", 1L);
```

### Ejemplo Completo: Videollamada

```java
NetworkFacade facade = new NetworkFacade();
facade.connectToServer("localhost", 5000);

// Iniciar llamada
facade.startVideoCall(serverId, targetId);

// Durante la llamada
facade.setMicrophoneMuted(false);
facade.setSpeakerMuted(false);

// Terminar
facade.stopVideoCall();
```

---

##  Performance

### Métricas Típicas

| Operación | Tiempo Promedio |
|-----------|-----------------|
| Autenticación | 50-100ms |
| Envío de mensaje | 10-30ms |
| Transferencia archivo (1MB) | 1-3s |
| Inicio videollamada | 200-500ms |
| Frame de video | 33ms (30 FPS) |

### Límites

| Recurso | Límite |
|---------|--------|
| Tamaño máximo archivo | 100 MB |
| Conexiones simultáneas | 100 clientes |
| FPS video | 30 |
| Calidad audio | 44.1 KHz |
| Chunk size archivos | 64 KB |

---

Esta documentación cubre los aspectos principales de la API. Para detalles de implementación, consultar el código fuente en cada paquete.
