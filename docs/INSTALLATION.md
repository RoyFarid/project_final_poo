# Instalación

## Requisitos

### Software Necesario

| Software | Versión Mínima | Versión Recomendada |
|----------|---------------|---------------------|
| Java JDK | 17 | 17 o 21 |
| Maven | 3.6 | 3.8+ |
| MySQL | 8.0 | 8.2+ |

### Requisitos de Hardware

- CPU: Procesador dual-core o superior
- RAM: Mínimo 4GB (8GB recomendado)
- Disco: 500MB de espacio libre
- Red: Conexión de red para modo cliente-servidor
- Webcam: Opcional, solo para videollamadas

## Instalación Paso a Paso

### 1. Instalar Java JDK

#### Windows
1. Descargar JDK 17 desde Oracle o Adoptium
2. Ejecutar el instalador
3. Configurar variable de entorno:
   ```cmd
   setx JAVA_HOME "C:\Program Files\Java\jdk-17"
   setx PATH "%PATH%;%JAVA_HOME%\bin"
   ```
4. Verificar: `java -version`

#### Linux (Ubuntu/Debian)
```bash
sudo apt update
sudo apt install openjdk-17-jdk
java -version
```

#### macOS
```bash
brew install openjdk@17
echo 'export PATH="/usr/local/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
java -version
```

### 2. Instalar Maven

#### Windows
1. Descargar desde Maven oficial
2. Extraer a `C:\Program Files\Apache\maven`
3. Agregar a PATH:
   ```cmd
   setx MAVEN_HOME "C:\Program Files\Apache\maven"
   setx PATH "%PATH%;%MAVEN_HOME%\bin"
   ```
4. Verificar: `mvn -version`

#### Linux
```bash
sudo apt install maven
mvn -version
```

#### macOS
```bash
brew install maven
mvn -version
```

### 3. Instalar MySQL

#### Windows
1. Descargar MySQL Installer
2. Ejecutar instalador
3. Seleccionar "Developer Default"
4. Configurar root password durante instalación
5. Iniciar MySQL Server

#### Linux
```bash
sudo apt update
sudo apt install mysql-server
sudo systemctl start mysql
sudo mysql_secure_installation
```

#### macOS
```bash
brew install mysql
brew services start mysql
mysql_secure_installation
```

### 4. Configurar Base de Datos

#### Paso 4.1: Crear Usuario MySQL

Conectar a MySQL como root:
```bash
mysql -u root -p
```

Ejecutar:
```sql
CREATE USER 'whatsapp_user'@'localhost' IDENTIFIED BY 'tu_password_seguro';
GRANT ALL PRIVILEGES ON whatsapp_clone.* TO 'whatsapp_user'@'localhost';
FLUSH PRIVILEGES;
CREATE DATABASE IF NOT EXISTS whatsapp_clone;
EXIT;
```

#### Paso 4.2: Configurar Archivo de Propiedades

1. Navegar al directorio del proyecto
2. Copiar archivo de ejemplo:
   ```bash
   cp db.properties.example db.properties
   ```
3. Editar `db.properties`:
   ```properties
   db.host=localhost
   db.port=3306
   db.database=whatsapp_clone
   db.username=whatsapp_user
   db.password=tu_password_seguro
   ```

**IMPORTANTE**: Nunca subir `db.properties` a control de versiones. El archivo `.gitignore` ya lo incluye.

### 5. Clonar el Proyecto

```bash
git clone https://github.com/RoyFarid/project_final_poo.git
cd project_final_poo
```

### 6. Compilar el Proyecto

```bash
mvn clean install
```

Si hay errores de tests:
```bash
mvn clean install -DskipTests
```

## Ejecutar la Aplicación

### Método 1: Maven (Recomendado)

```bash
mvn javafx:run
```

### Método 3: JAR Ejecutable

```bash
mvn clean package
java -jar target/whatsapp-clone-1.0.0.jar
```

### Método 4: IDE

#### IntelliJ IDEA
1. Abrir proyecto con File > Open
2. Esperar a que Maven descargue dependencias
3. Ejecutar `Main.java` con botón Run
4. Si hay error con JavaFX, agregar VM options:
   ```
   --module-path "C:\Path\To\javafx-sdk-21\lib" --add-modules javafx.controls,javafx.fxml,javafx.media
   ```

#### Eclipse
1. Importar como proyecto Maven
2. Actualizar proyecto: Alt+F5
3. Run As > Java Application > `Main`

## Verificar Instalación

### 1. Verificar Base de Datos

```bash
mysql -u whatsapp_user -p whatsapp_clone
```

```sql
SHOW TABLES;
-- Debe mostrar: Usuario, Log, Transferencia, Room, RoomMember

DESCRIBE Usuario;
-- Debe mostrar estructura de la tabla
```

### 2. Verificar Puertos

```bash
# Windows
netstat -an | findstr 3306  # MySQL
netstat -an | findstr 5000  # Aplicación

# Linux/macOS
netstat -an | grep 3306
netstat -an | grep 5000
```

### 3. Ejecutar Prueba Básica

Al iniciar la aplicación debe aparecer:
- Ventana de login
- Sin errores en consola
- Conexión exitosa a base de datos

## Configuración de Red

### Para Uso Local (Mismo Equipo)

No requiere configuración adicional. Usar:
- Host: `localhost` o `127.0.0.1`
- Puerto: `5000` (default)

### Para Uso en Red Local (LAN)

#### Paso 1: Obtener IP Local

**Windows:**
```cmd
ipconfig
# Buscar "IPv4 Address" en adaptador de red activo
```

**Linux/macOS:**
```bash
ifconfig
# o
ip addr show
```

#### Paso 2: Configurar Firewall

**Windows Firewall:**
1. Panel de Control > Sistema y Seguridad > Firewall de Windows
2. Configuración avanzada
3. Reglas de entrada > Nueva regla
4. Tipo: Puerto
5. TCP, Puerto: 5000
6. Permitir la conexión

**Linux (UFW):**
```bash
sudo ufw allow 5000/tcp
sudo ufw reload
```

## Solución de Problemas

### Error: "No se puede conectar a MySQL"

**Soluciones:**
1. Verificar que MySQL esté ejecutándose:
   ```bash
   # Windows
   services.msc  # Buscar MySQL
   
   # Linux
   sudo systemctl status mysql
   ```
2. Verificar credenciales en `db.properties`
3. Verificar puerto MySQL:
   ```sql
   SHOW VARIABLES LIKE 'port';
   ```
4. Probar conexión manual:
   ```bash
   mysql -h localhost -P 3306 -u whatsapp_user -p
   ```

### Error: "JavaFX runtime components are missing"

**Soluciones:**
1. Verificar que JavaFX esté en Maven:
   ```bash
   mvn dependency:tree | grep javafx
   ```
2. Agregar VM options al ejecutar:
   ```
   --module-path /path/to/javafx-sdk/lib --add-modules javafx.controls,javafx.fxml,javafx.media
   ```
3. Usar Java con JavaFX incluido (Liberica, Zulu FX)

### Error: "Port already in use"

**Soluciones:**
1. Verificar qué proceso usa el puerto:
   ```bash
   # Windows
   netstat -ano | findstr :5000
   taskkill /PID <PID> /F
   
   # Linux/macOS
   lsof -i :5000
   kill -9 <PID>
   ```
2. Usar otro puerto en la aplicación

### Error: Compilación falla con Maven

**Soluciones:**
1. Limpiar caché de Maven:
   ```bash
   mvn clean
   rm -rf ~/.m2/repository/com/whatsapp
   mvn install
   ```
2. Actualizar Maven
3. Verificar Java version: `mvn -version` (debe mostrar Java 17+)

### Problema: Webcam no detectada

**Soluciones:**
1. Verificar permisos de cámara (macOS/Windows)
2. Instalar drivers de webcam
3. La app funciona sin webcam (solo no habrá videollamadas)

## Dependencias Automáticas

Maven descargará automáticamente:
- JavaFX (controls, fxml, media)
- MySQL Connector
- BCrypt
- SLF4J/Logback
- Webcam Capture
- JUnit (tests)

Total descarga: ~150MB

## Actualización

```bash
git pull origin main
mvn clean install
mvn javafx:run
```

## Verificación Post-Instalación

Lista de verificación:
- [ ] Java 17+ instalado y verificado
- [ ] Maven instalado y verificado
- [ ] MySQL instalado y ejecutándose
- [ ] Base de datos `whatsapp_clone` creada
- [ ] Usuario MySQL con permisos
- [ ] Archivo `db.properties` configurado
- [ ] Proyecto compilado sin errores
- [ ] Aplicación inicia correctamente
- [ ] Puertos de red disponibles

