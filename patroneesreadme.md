## Patrones de Diseño Utilizados

### Singleton
- **Dónde:** [`ConnectionManager`](src/main/java/com/whatsapp/network/ConnectionManager.java#L24-L57), [`EventAggregator`](src/main/java/com/whatsapp/network/observer/EventAggregator.java#L7-L38), [`LogService`](src/main/java/com/whatsapp/service/LogService.java#L11-L42), [`UserAliasRegistry`](src/main/java/com/whatsapp/service/UserAliasRegistry.java#L1-L35), [`ServerRuntime`](src/main/java/com/whatsapp/service/ServerRuntime.java#L1-L23).
- **Cómo se usa:** cada clase expone `getInstance()` y mantiene constructor privado (ver, por ejemplo, `ConnectionManager` líneas 24-57). Todas las capas llaman a `Class.getInstance()` en vez de `new`.
- **Por qué es el patrón indicado:** necesitamos un único estado global (conexiones, suscriptores, logs) accesible desde cualquier hilo. Un Factory o Prototype no garantizaría la unicidad ni el control de concurrencia.
- **Para qué se usa en el proyecto:** compartir recursos: `ConnectionManager` controla sockets, `EventAggregator` distribuye eventos, `LogService` centraliza logs, `UserAliasRegistry` mapea alias, `ServerRuntime` define si el proceso es servidor o cliente.

### Observer
- **Dónde:** [`EventAggregator`](src/main/java/com/whatsapp/network/observer/EventAggregator.java), [`NetworkEvent`](src/main/java/com/whatsapp/network/observer/NetworkEvent.java), [`NetworkEventObserver`](src/main/java/com/whatsapp/network/observer/NetworkEventObserver.java); observadores concretos en `ChatView`, `ClientView`, `ServerView`, `FileTransferService`, etc.
- **Cómo se usa:** los productores (`ConnectionManager`, `NetworkFacade`, `ControlService`) llaman `EventAggregator.publish(...)`; las vistas y servicios se suscriben con `EventAggregator.subscribe(observer)` y reciben callbacks `onNetworkEvent`.
- **Por qué es el patrón indicado:** permite notificar a múltiples consumidores sin acoplarlos al emisor y soporta asincronía. Otros patrones (Mediator, Command) no cubrirían la difusión one-to-many sin dependencias cruzadas.
- **Para qué se usa en el proyecto:** propagar mensajes recibidos, progreso de archivos, frames de video, eventos de conexión, etc., tanto a la UI como a servicios como `FileTransferService`.

### Facade
- **Dónde:** [`NetworkFacade`](src/main/java/com/whatsapp/service/NetworkFacade.java#L1-L163).
- **Cómo se usa:** la UI invoca métodos del facade (`connectToServer`, `startVideoCall`, `sendFile`) y este coordina `ConnectionManager`, `ChatService`, `FileTransferService`, `VideoStreamService`, `AudioStreamService`, `ControlService`.
- **Por qué es el patrón indicado:** encapsula la complejidad de los subsistemas y expone una API simple a la UI. Adapter no encaja porque no sólo estamos convirtiendo interfaces, sino coordinando múltiples servicios.
- **Para qué se usa en el proyecto:** servir de puerta de entrada a todas las capacidades de red, manteniendo la UI desacoplada de detalles de sockets, reintentos, codecs, etc.

### Command
- **Dónde:** [`SendMessageCommand`](src/main/java/com/whatsapp/command/SendMessageCommand.java), [`SendFileCommand`](src/main/java/com/whatsapp/command/SendFileCommand.java), [`StartVideoCallCommand`](src/main/java/com/whatsapp/command/StartVideoCallCommand.java), [`CommandInvoker`](src/main/java/com/whatsapp/command/CommandInvoker.java).
- **Cómo se usa:** `ChatView` crea comandos concretos y los ejecuta vía `CommandInvoker.executeCommand()`, lo que delega en `NetworkFacade`. `StartVideoCallCommand` implementa `undo` para detener la llamada.
- **Por qué es el patrón indicado:** encapsula cada acción como objeto, permitiendo colas, deshacer y logging uniforme. Strategy no aplica porque no elegimos algoritmos, sino que necesitamos encapsular invocaciones.
- **Para qué se usa en el proyecto:** estructurar las acciones de UI (enviar mensaje, archivo, iniciar videollamada) y permitir deshacer videollamadas.

### Strategy
- **Dónde:** [`RetryStrategy`](src/main/java/com/whatsapp/network/strategy/RetryStrategy.java), [`ExponentialBackoffStrategy`](src/main/java/com/whatsapp/network/strategy/ExponentialBackoffStrategy.java), [`FixedDelayStrategy`](src/main/java/com/whatsapp/network/strategy/FixedDelayStrategy.java).
- **Cómo se usa:** `FileTransferService` instancia `ExponentialBackoffStrategy` (ver constructor en líneas 50-59) y lo aplica en `sendFileChunk` para decidir tiempos de reintento.
- **Por qué es el patrón indicado:** permite intercambiar políticas completas sin tocar `FileTransferService`. Template Method no aplica porque no hay pasos comunes, solo comportamientos alternativos.
- **Para qué se usa en el proyecto:** modularizar la lógica de reintentos en transferencias de archivos (backoff exponencial vs. otros posibles esquemas).

### Factory Method
- **Dónde:** [`SocketFactory`](src/main/java/com/whatsapp/network/factory/SocketFactory.java#L1-L33).
- **Cómo se usa:** `ConnectionManager.startServer` y `connectToServer` llaman `SocketFactory.createTcpServerSocket` / `createTcpSocket` para instanciar sockets.
- **Por qué es el patrón indicado:** encapsula la creación de objetos complejos (sockets configurados) en un único lugar, facilitando cambios futuros (TLS, timeouts). Abstract Factory sería excesivo porque no manejamos familias completas.
- **Para qué se usa en el proyecto:** centralizar la configuración de sockets TCP usados por servidor y clientes.

### Repository
- **Dónde:** [`UsuarioRepository`](src/main/java/com/whatsapp/repository/UsuarioRepository.java), [`LogRepository`](src/main/java/com/whatsapp/repository/LogRepository.java), [`TransferenciaRepository`](src/main/java/com/whatsapp/repository/TransferenciaRepository.java).
- **Cómo se usa:** servicios como `AuthService`, `LogService`, `ChatService`, `FileTransferService` invocan métodos `findBy…`, `save`, `update` encapsulados dentro de cada repositorio, que a su vez usan `DatabaseManager`.
- **Por qué es el patrón indicado:** separa la lógica de negocio del acceso a datos, facilitando cambios de almacenamiento. Active Record no aplica porque las entidades (`Usuario`, `Transferencia`, `Log`) no deben conocer SQL; DAO puro no enfatiza tanto el modelo de dominio.
- **Para qué se usa en el proyecto:** persistir usuarios, logs y transferencias sin duplicar queries ni acoplar los servicios al driver JDBC.

### State (Enum)
- **Dónde:** [`ConnectionState`](src/main/java/com/whatsapp/network/ConnectionState.java).
- **Cómo se usa:** `ConnectionManager` cambia su atributo `state` y notifica a listeners; `NetworkFacade.getConnectionState()` lo expone para la UI (`ClientView`, `ServerView`).
- **Por qué es el patrón indicado:** los comportamientos dependen de un conjunto finito de estados (activo, desconectado, reconectando). Strategy no aplicaría porque no se intercambian algoritmos completos, solo se consulta un estado.
- **Para qué se usa en el proyecto:** reflejar en la UI y en la lógica las transiciones de conexión/desconexión.

### Proxy/Facade Remoto
- **Dónde:** [`RemoteAuthClient`](src/main/java/com/whatsapp/service/RemoteAuthClient.java#L1-L74).
- **Cómo se usa:** `LoginView` crea un `RemoteAuthClient` para enviar `ControlService.sendAuthRequest/SendRegisterRequest` y espera los eventos `AUTH_RESULT`/`REGISTER_RESULT` que recibe vía Observer.
- **Por qué es el patrón indicado:** representa localmente un servicio remoto asincrónico, encapsulando detalles de control messages y futuros reintentos. Decorator o Adapter no encajan porque no extendemos ni adaptamos clases existentes, sino que interponemos un proxy.
- **Para qué se usa en el proyecto:** permitir que los clientes autenticados hablen con el servidor central sin tocar directamente la base de datos ni la lógica interna.

