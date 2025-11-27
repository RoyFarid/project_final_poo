# Guía de Desarrollo

## Para Desarrolladores

Guía para contribuir o extender el proyecto RoomWave.

## Estructura del Proyecto

```
src/main/java/com/whatsapp/
├── command/          # Patrón Command
├── database/         # Gestión de MySQL
├── model/            # Entidades del dominio
├── network/          # Comunicación TCP/IP
├── protocol/         # Protocolo de mensajería
├── repository/       # Patrón Repository
├── service/          # Lógica de negocio
└── ui/               # Interfaz JavaFX
```

## Setup del Entorno

### Prerrequisitos

Java JDK 17+, Maven 3.8+, MySQL 8.0+, Git. Ver [INSTALLATION.md](INSTALLATION.md).

### Clonar el Repositorio

```bash
git clone https://github.com/RoyFarid/project_final_poo.git
cd project_final_poo
```

### Configurar Base de Datos

```bash
cp db.properties.example db.properties
nano db.properties  # Editar credenciales
```

### Instalar Dependencias

```bash
mvn clean install
```

### Ejecutar en Modo Desarrollo

```bash
mvn javafx:run
```

## Principios de Diseño

El proyecto sigue estos principios:

1. **SOLID Principles**
   - Single Responsibility: Cada clase tiene una responsabilidad única
   - Open/Closed: Abierto a extensión, cerrado a modificación
   - Liskov Substitution: Subclases intercambiables
   - Interface Segregation: Interfaces específicas
   - Dependency Inversion: Depender de abstracciones

2. **DRY (Don't Repeat Yourself)**
   - Código reutilizable en servicios y utilidades

3. **KISS (Keep It Simple, Stupid)**
   - Soluciones simples y directas

4. **Separation of Concerns**
   - Capas bien definidas

## Patrones de Diseño Implementados

Ver [ARCHITECTURE.md](ARCHITECTURE.md) para detalles completos.

### Singleton
- `ConnectionManager`, `EventAggregator`, `LogService`, `UserAliasRegistry`, `RoomService`, `DatabaseManager`

### Factory Method
- `SocketFactory`: Creación de sockets TCP

### Observer
- `EventAggregator` / `NetworkEventObserver`: Sistema pub/sub

### Strategy
- `RetryStrategy`: Estrategias de reintento intercambiables

### Command
- `Command` / `CommandInvoker`: Encapsulación de acciones

### Facade
- `NetworkFacade`: Interfaz unificada para servicios de red

### Repository
- `IRepository<T>`: Abstracción de acceso a datos

### Proxy
- `RemoteAuthClient`: Proxy para autenticación remota

## Guías de Desarrollo

### Agregar Nuevo Tipo de Mensaje

**Pasos:**

1. **Agregar constante en MessageHeader**
```java
// En MessageHeader.MessageType
public static final byte NUEVO_TIPO = 5;
```

2. **Crear servicio correspondiente**
```java
public class NuevoTipoService {
    public void handleIncomingPacket(byte[] data, String source) {
        // Lógica de procesamiento
    }
}
```

3. **Registrar en NetworkFacade**
```java
// En NetworkFacade constructor
case MessageHeader.MessageType.NUEVO_TIPO:
    nuevoTipoService.handleIncomingPacket(data, event.getSource());
    break;
```

4. **Agregar método público en NetworkFacade**
```java
public void sendNuevoTipo(String connId, Object data) {
    nuevoTipoService.send(connId, data);
}
```

### Agregar Nueva Entidad de Base de Datos

**Pasos:**

1. **Crear modelo**
```java
public class NuevaEntidad {
    private Long id;
    private String campo1;
    private LocalDateTime fecha;
    // Constructor, getters, setters
}
```

2. **Crear tabla en DatabaseManager**
```java
String createNuevaEntidadTable = """
    CREATE TABLE IF NOT EXISTS NuevaEntidad (
        Id INT AUTO_INCREMENT PRIMARY KEY,
        Campo1 VARCHAR(100) NOT NULL,
        Fecha DATETIME NOT NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
""";
stmt.execute(createNuevaEntidadTable);
```

3. **Crear repository**
```java
public class NuevaEntidadRepository implements IRepository<NuevaEntidad> {
    @Override
    public NuevaEntidad save(NuevaEntidad entity) {
        String sql = "INSERT INTO NuevaEntidad (Campo1, Fecha) VALUES (?, ?)";
        // Implementación
    }
    // Otros métodos CRUD
}
```

### Agregar Nuevo Comando

**Pasos:**

1. **Crear clase de comando**
```java
public class NuevoCommand implements Command {
    private NetworkFacade facade;
    private Object parametro;
    
    public NuevoCommand(NetworkFacade facade, Object parametro) {
        this.facade = facade;
        this.parametro = parametro;
    }
    
    @Override
    public void execute() {
        // Lógica de ejecución
    }
    
    @Override
    public void undo() {
        // Lógica de deshacer
    }
}
```

2. **Usar en UI**
```java
Command cmd = new NuevoCommand(facade, parametro);
CommandInvoker invoker = new CommandInvoker();
invoker.executeCommand(cmd);
```

### Agregar Nueva Vista

**Pasos:**

1. **Crear clase de vista**
```java
public class NuevaView extends VBox {
    public NuevaView() {
        initUI();
    }
    
    private void initUI() {
        // Construir interfaz
    }
}
```

2. **Registrar en Main**
```java
private void showNuevaView() {
    NuevaView view = new NuevaView();
    Scene scene = new Scene(view, 800, 600);
    primaryStage.setScene(scene);
    primaryStage.show();
}
```

### Agregar Nuevo Tipo de Control Message

**Pasos:**

1. **Agregar constante en ControlService**
```java
public static final byte CONTROL_NUEVO_TIPO = 26;
```

2. **Agregar handler en handleControlMessage**
```java
case CONTROL_NUEVO_TIPO:
    handleNuevoTipo(controlData, source);
    break;
```

3. **Implementar método handler**
```java
private void handleNuevoTipo(String data, String source) {
    // Lógica de procesamiento
}
```

## Testing

### Estructura de Tests

```
src/test/java/com/whatsapp/
├── service/
│   ├── AuthServiceTest.java
│   ├── ChatServiceTest.java
│   └── RoomServiceTest.java
├── repository/
│   └── UsuarioRepositoryTest.java
└── network/
    └── ConnectionManagerTest.java
```

### Ejemplo de Test Unitario

```java
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AuthServiceTest {
    
    @Test
    public void testRegistrarUsuario() {
        AuthService auth = new AuthService();
        Usuario user = auth.registrar("testuser", "pass123", "test@email.com");
        
        assertNotNull(user);
        assertEquals("testuser", user.getUsername());
        assertNotNull(user.getPasswordHash());
    }
    
    @Test
    public void testAutenticarUsuarioExitoso() {
        AuthService auth = new AuthService();
        auth.registrar("testuser", "pass123", "test@email.com");
        
        Optional<Usuario> userOpt = auth.autenticar("testuser", "pass123");
        
        assertTrue(userOpt.isPresent());
    }
}
```

### Ejecutar Tests

```bash
# Todos los tests
mvn test

# Test específico
mvn test -Dtest=AuthServiceTest

# Con coverage
mvn test jacoco:report
```

## Estándares de Código

### Convenciones de Nombres

- **Clases**: PascalCase (`UsuarioRepository`)
- **Métodos**: camelCase (`sendMessage`)
- **Constantes**: UPPER_SNAKE_CASE (`HEADER_SIZE`)
- **Variables**: camelCase (`connectionId`)
- **Paquetes**: lowercase (`com.whatsapp.service`)

### Comentarios y Documentación

```java
/**
 * Servicio para gestionar autenticación de usuarios.
 * Utiliza BCrypt para hashear contraseñas con 12 rounds.
 */
public class AuthService {
    
    /**
     * Registra un nuevo usuario en el sistema.
     * 
     * @param username Nombre único del usuario (3-50 caracteres)
     * @param password Contraseña (mínimo 6 caracteres)
     * @param email Correo electrónico válido
     * @return Usuario recién creado con ID asignado
     * @throws IllegalArgumentException si las validaciones fallan
     */
    public Usuario registrar(String username, String password, String email) {
        // Implementación
    }
}
```

### Manejo de Excepciones

```java
// Correcto
try {
    // código
} catch (IOException e) {
    logger.error("Error de I/O al procesar archivo", e);
    throw new ApplicationException("No se pudo procesar el archivo", e);
}

// Incorrecto
try {
    // código
} catch (Exception e) {
    e.printStackTrace();
}
```

### Logging

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyClass {
    private static final Logger logger = LoggerFactory.getLogger(MyClass.class);
    
    public void myMethod() {
        logger.debug("Iniciando proceso");
        logger.info("Proceso completado exitosamente");
        logger.warn("Advertencia: recurso limitado");
        logger.error("Error crítico", exception);
    }
}
```

## Debugging

### Logs de Aplicación

Los logs se guardan en:
- Consola: Salida estándar
- Base de datos: Tabla `Log`

### Habilitar Debug Logging

En `logback.xml`:
```xml
<configuration>
    <logger name="com.whatsapp" level="DEBUG"/>
    <root level="INFO">
        <appender-ref ref="STDOUT" />
    </root>
</configuration>
```

### Debugging en IntelliJ IDEA

1. Establecer breakpoints (clic en margen izquierdo)
2. Run > Debug 'Main'
3. Usar controles:
   - F8: Step Over
   - F7: Step Into
   - Shift+F8: Step Out
   - F9: Resume

### Debugging de Red

```bash
# Monitorear tráfico TCP
tcpdump -i any port 5000

# Ver conexiones activas
netstat -an | grep 5000
```

## Build y Deploy

### Compilar para Producción

```bash
mvn clean package
```

Genera: `target/whatsapp-clone-1.0.0.jar`

### Crear Ejecutable Nativo

```bash
jpackage --input target/ \
         --name WhatsAppClone \
         --main-jar whatsapp-clone-1.0.0.jar \
         --main-class com.whatsapp.Main \
         --type exe
```

### Configuración de Producción

`db.properties` para producción:
```properties
db.host=production-server.com
db.port=3306
db.database=whatsapp_clone_prod
db.username=prod_user
db.password=SECURE_PASSWORD_HERE
```

## Contribuir al Proyecto

### Workflow de Contribución

1. Fork del repositorio
2. Crear rama feature:
   ```bash
   git checkout -b feature/nueva-funcionalidad
   ```
3. Hacer cambios y commits:
   ```bash
   git add .
   git commit -m "feat: agregar nueva funcionalidad"
   ```
4. Push a tu fork:
   ```bash
   git push origin feature/nueva-funcionalidad
   ```
5. Crear Pull Request

### Convención de Commits

Seguir Conventional Commits:
```
feat: agregar autenticación OAuth
fix: corregir error en transferencia de archivos
docs: actualizar README con ejemplos
style: formatear código según estándares
refactor: reorganizar estructura de servicios
test: agregar tests para ChatService
chore: actualizar dependencias
```

### Code Review

Antes de merge, verificar:
- [ ] Código sigue estándares del proyecto
- [ ] Tests pasan exitosamente
- [ ] Documentación actualizada
- [ ] Sin conflictos con main
- [ ] Cambios revisados por al menos un desarrollador

## Recursos Adicionales

### Documentación Relacionada

- [README.md](../README.md) - Visión general
- [ARCHITECTURE.md](ARCHITECTURE.md) - Arquitectura detallada
- [API.md](API.md) - Documentación de API
- [DATABASE.md](DATABASE.md) - Esquema de base de datos
- [INSTALLATION.md](INSTALLATION.md) - Guía de instalación
- [USER_GUIDE.md](USER_GUIDE.md) - Manual de usuario

### Librerías y Frameworks

- [JavaFX Documentation](https://openjfx.io/)
- [MySQL Connector/J](https://dev.mysql.com/doc/connector-j/en/)
- [BCrypt Java](https://github.com/patrickfav/bcrypt)
- [SLF4J](http://www.slf4j.org/)
- [Webcam Capture](https://github.com/sarxos/webcam-capture)

### Herramientas Recomendadas

- IDE: IntelliJ IDEA, Eclipse, VS Code
- DB Client: MySQL Workbench, DBeaver
- Git GUI: GitKraken, SourceTree
- Diff Tool: Beyond Compare, Meld
- Profiler: JProfiler, VisualVM

## Reportar Bugs

### Información a Incluir

1. Descripción del bug
2. Pasos para reproducir
3. Comportamiento esperado vs actual
4. Logs de error
5. Entorno: OS, Java version, MySQL version

### Template de Issue

```markdown
## Descripción
[Descripción clara del bug]

## Pasos para Reproducir
1. Paso 1
2. Paso 2
3. ...

## Comportamiento Esperado
[Qué debería suceder]

## Comportamiento Actual
[Qué sucede realmente]

## Logs
[Pegar logs relevantes]

## Entorno
- OS: Windows 10
- Java: 17.0.5
- MySQL: 8.0.31
```

## Mejores Prácticas

### Performance

1. Usar pools de hilos para operaciones concurrentes
2. Cachear resultados de queries frecuentes
3. Cerrar recursos con try-with-resources
4. Lazy loading para datos grandes

### Seguridad

1. Nunca hardcodear credenciales
2. Validar toda entrada de usuario
3. Sanitizar queries SQL (usar PreparedStatement)
4. Hashear contraseñas con BCrypt
5. Logging sin información sensible

### Mantenibilidad

1. Código autoexplicativo > Comentarios excesivos
2. Métodos pequeños (< 50 líneas)
3. Clases cohesivas (< 500 líneas)
4. Tests para lógica crítica
5. Documentar decisiones de diseño

