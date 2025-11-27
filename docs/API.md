# API y Servicios

## NetworkFacade

Interfaz principal para operaciones de red. Orquesta todos los servicios de red.

### Conexión

```java
public void startServer(int port) throws IOException
```
Inicia el servidor en el puerto especificado.

```java
public void connectToServer(String host, int port) throws IOException
```
Conecta como cliente a un servidor.

```java
public void disconnect()
```
Desconecta todas las conexiones y detiene servicios.

```java
public ConnectionState getConnectionState()
```
Retorna el estado actual de conexión (DESCONECTADO, CONECTANDO, ACTIVO, ERROR).

```java
public Set<String> getConnectedClients()
```
Retorna conjunto de IDs de clientes conectados.

### Mensajería

```java
public void sendMessage(String connectionId, String message, Long userId, String peerIp) throws IOException
```
Envía mensaje de texto a un usuario específico.

### Transferencia de Archivos

```java
public void sendFile(String serverConnectionId, String targetConnectionId, String filePath, Long userId) throws IOException
```
Envía archivo dividido en chunks con verificación de integridad.

### Videollamadas

```java
public void startVideoCall(String serverConnectionId, String targetConnectionId)
```
Inicia videollamada con streaming de video y audio.

```java
public void stopVideoCall()
```
Detiene la videollamada activa.

```java
public void setMicrophoneMuted(boolean muted)
```
Silencia o activa el micrófono durante llamada.

```java
public void setSpeakerMuted(boolean muted)
```
Silencia o activa el altavoz durante llamada.

## ControlService

Maneja mensajes de control del sistema. Tipos de mensajes de control:

- `CONTROL_USER_LIST`: Lista de usuarios conectados
- `CONTROL_USER_CONNECTED`: Notificación de usuario conectado
- `CONTROL_USER_DISCONNECTED`: Notificación de usuario desconectado
- `CONTROL_USER_ALIAS`: Actualización de alias de usuario
- `CONTROL_AUTH_REQUEST`: Solicitud de autenticación
- `CONTROL_AUTH_RESPONSE`: Respuesta de autenticación
- `CONTROL_REGISTER_REQUEST`: Solicitud de registro
- `CONTROL_REGISTER_RESPONSE`: Respuesta de registro
- `CONTROL_ROOM_CREATE_REQUEST`: Solicitud de creación de room
- `CONTROL_ROOM_CREATE_RESPONSE`: Respuesta de creación de room
- `CONTROL_ROOM_APPROVE`: Aprobación de room
- `CONTROL_ROOM_REJECT`: Rechazo de room
- `CONTROL_ROOM_JOIN_REQUEST`: Solicitud de unión a room
- `CONTROL_ROOM_JOIN_RESPONSE`: Respuesta de unión a room
- `CONTROL_ROOM_LEAVE`: Salida de room
- `CONTROL_ROOM_CLOSE`: Cierre de room
- `CONTROL_ROOM_LIST`: Lista de rooms disponibles
- `CONTROL_ROOM_MESSAGE`: Mensaje en room
- `CONTROL_ROOM_FILE`: Archivo en room
- `CONTROL_ADMIN_MUTE`: Silenciar usuario (admin)
- `CONTROL_ADMIN_UNMUTE`: Activar audio usuario (admin)
- `CONTROL_ADMIN_DISABLE_CAMERA`: Desactivar cámara (admin)
- `CONTROL_ADMIN_ENABLE_CAMERA`: Activar cámara (admin)
- `CONTROL_ADMIN_BLOCK_MESSAGES`: Bloquear mensajes (admin)
- `CONTROL_ADMIN_UNBLOCK_MESSAGES`: Desbloquear mensajes (admin)

### Métodos Principales

```java
public void sendUserList(String connectionId) throws IOException
```
Envía lista de usuarios conectados a un cliente específico.

```java
public void broadcastUserList()
```
Difunde lista de usuarios a todos los clientes.

```java
public void sendControlMessage(String connectionId, byte controlType, String data) throws IOException
```
Envía mensaje de control a un cliente específico.

```java
public void sendAuthRequest(String serverConnectionId, String username, String password) throws IOException
```
Envía solicitud de autenticación al servidor.

```java
public void sendRegisterRequest(String serverConnectionId, String username, String password, String email) throws IOException
```
Envía solicitud de registro al servidor.

```java
public void handleControlMessage(byte[] data, String source)
```
Procesa mensaje de control recibido.

## RoomService

Gestión de salas de chat grupales. Patrón Singleton.

### Métodos Principales

```java
public Room createRoomRequest(String roomName, String creatorConnectionId, String creatorUsername, Set<String> memberConnectionIds, String requestMessage)
```
Crea solicitud de room pendiente de aprobación.

```java
public Room createRoomRequest(String roomName, String creatorConnectionId, String creatorUsername, Set<String> memberConnectionIds, String requestMessage, boolean includeServer)
```
Crea solicitud de room con opción de incluir servidor.

```java
public Room createRoomByServer(String roomName, Set<String> memberConnectionIds, boolean includeServer)
```
Crea room activo directamente desde el servidor.

```java
public boolean approveRoom(Long roomId)
```
Aprueba un room pendiente.

```java
public boolean rejectRoom(Long roomId)
```
Rechaza un room pendiente.

```java
public boolean joinRoom(Long roomId, String connectionId, String username)
```
Une un usuario a un room activo.

```java
public boolean leaveRoom(Long roomId, String connectionId)
```
Saca un usuario de un room.

```java
public boolean closeRoom(Long roomId)
```
Cierra un room.

```java
public List<Room> getActiveRooms()
```
Retorna lista de rooms activos.

```java
public Optional<Room> getRoomById(Long roomId)
```
Obtiene room por ID.

## AuthService

Gestión de autenticación y autorización.

```java
public Usuario registrar(String username, String password, String email)
```
Registra nuevo usuario con validación y hashing BCrypt.

```java
public Optional<Usuario> autenticar(String username, String password)
```
Autentica usuario. Retorna Optional vacío si falla.

```java
public boolean cambiarPassword(Long userId, String oldPassword, String newPassword)
```
Cambia contraseña de usuario.

## ChatService

Manejo de mensajería de texto.

```java
public void sendMessage(String connectionId, String message, Long userId, String peerIp) throws IOException
```
Envía mensaje con serialización y checksum.

```java
public void handleReceivedMessage(byte[] data, String source)
```
Procesa mensaje recibido, verifica checksum y publica evento.

## FileTransferService

Transferencia de archivos con integridad.

```java
public void sendFile(String serverConnectionId, String targetConnectionId, String filePath, Long userId) throws IOException
```
Envía archivo dividido en chunks de 64KB con checksum SHA-256.

```java
public void handleIncomingPacket(byte[] data, String source)
```
Procesa chunks recibidos, ensambla archivo y verifica integridad.

## VideoStreamService y AudioStreamService

Streaming multimedia en tiempo real.

```java
public void startStreaming(String serverConnectionId, String targetConnectionId)
```
Inicia captura y transmisión de video/audio.

```java
public void stopStreaming()
```
Detiene captura y transmisión.

## RemoteAuthClient

Cliente auxiliar para autenticación remota. Patrón Proxy.

```java
public OperationResultPayload authenticate(String serverConnectionId, String username, String password) throws IOException, TimeoutException, InterruptedException
```
Autentica usuario remoto con timeout de 10 segundos.

```java
public OperationResultPayload register(String serverConnectionId, String username, String password, String email) throws IOException, TimeoutException, InterruptedException
```
Registra usuario remoto con timeout de 10 segundos.

## UserAliasRegistry

Registro centralizado de alias de usuarios. Patrón Singleton.

```java
public void registerAlias(String connectionId, String alias)
```
Registra alias para un connectionId.

```java
public void removeAlias(String connectionId)
```
Elimina alias de un connectionId.

```java
public String getAliasOrDefault(String connectionId)
```
Obtiene alias o retorna connectionId si no existe.

```java
public Map<String, String> snapshot()
```
Retorna snapshot del registro de alias.

## ConnectionManager

Gestión centralizada de conexiones TCP/IP. Patrón Singleton.

```java
public static ConnectionManager getInstance()
```
Obtiene instancia única.

```java
public void startServer(int port) throws IOException
```
Inicia servidor en el puerto especificado.

```java
public void connectToServer(String host, int port) throws IOException
```
Conecta como cliente al servidor.

```java
public void send(String connectionId, byte[] data) throws IOException
```
Envía datos a una conexión específica.

```java
public void broadcast(byte[] data)
```
Envía datos a todas las conexiones activas.

```java
public void disconnectClient(String connectionId)
```
Desconecta un cliente específico.

```java
public Set<String> getConnectedClients()
```
Retorna conjunto de IDs de clientes conectados.

```java
public boolean isServerMode()
```
Indica si está en modo servidor.

## MessageHeader

Estructura de cabecera de mensajes.

### Constantes

```java
public static final int HEADER_SIZE = 13; // bytes
```

### Tipos de Mensaje

```java
public static class MessageType {
    public static final byte CHAT = 0;
    public static final byte ARCHIVO = 1;
    public static final byte VIDEO = 2;
    public static final byte CONTROL = 3;
    public static final byte AUDIO = 4;
}
```

### Métodos

```java
public byte[] toBytes()
```
Convierte header a array de bytes.

```java
public static MessageHeader fromBytes(byte[] data)
```
Reconstruye header desde bytes.

## Repositorios

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
public Optional<Usuario> findByUsername(String username)
```
Busca usuario por nombre de usuario.

```java
public void updateLastLogin(Long userId)
```
Actualiza último login del usuario.

### LogRepository

```java
public List<Log> findByDateRange(LocalDateTime start, LocalDateTime end)
```
Busca logs por rango de fechas.

```java
public List<Log> findByTraceId(String traceId)
```
Busca logs por traceId.

```java
public List<Log> findByUserId(Long userId)
```
Busca logs por usuario.

### TransferenciaRepository

```java
public List<Transferencia> findByUserId(Long userId)
```
Busca transferencias por usuario.

```java
public List<Transferencia> findByEstado(Transferencia.EstadoTransferencia estado)
```
Busca transferencias por estado.

```java
public void updateEstado(Long id, Transferencia.EstadoTransferencia nuevoEstado)
```
Actualiza estado de transferencia.

### RoomRepository

```java
public List<Room> findByServerUsername(String serverUsername)
```
Busca rooms por servidor.

```java
public List<Room> findByEstado(Room.EstadoRoom estado)
```
Busca rooms por estado.

```java
public void deleteAllByServerUsername(String serverUsername)
```
Elimina todos los rooms de un servidor.

