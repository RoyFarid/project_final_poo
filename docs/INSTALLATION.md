# Instalación

## Requisitos

### Software Necesario

| Software | Versión Mínima | Versión Recomendada | Propósito |
|----------|---------------|---------------------|-----------|
| Java JDK | 17 | 17 o 21 | Ejecución de la aplicación |
| Maven | 3.6 | 3.8+ | Gestión de dependencias |
| MySQL | 8.0 | 8.2+ | Base de datos |
| JavaFX | 21 | 21 | Interfaz gráfica |

### Requisitos de Hardware

- **CPU**: Procesador dual-core o superior
- **RAM**: Mínimo 4GB (8GB recomendado)
- **Disco**: 500MB de espacio libre
- **Red**: Conexión de red para modo cliente-servidor
- **Webcam**: Opcional, solo para videollamadas

## Instalación

### 1. Instalar Java JDK

#### Windows
1. Descargar JDK 17 desde [Oracle](https://www.oracle.com/java/technologies/downloads/) o [Adoptium](https://adoptium.net/)
2. Ejecutar el instalador
3. Configurar variable de entorno `JAVA_HOME`:
   ```cmd
   setx JAVA_HOME "C:\Program Files\Java\jdk-17"
   setx PATH "%PATH%;%JAVA_HOME%\bin"
   ```
4. Verificar instalación:
   ```bash
   java -version
   ```

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
1. Descargar desde [Maven oficial](https://maven.apache.org/download.cgi)
2. Extraer a `C:\Program Files\Apache\maven`
3. Agregar a PATH:
   ```cmd
   setx MAVEN_HOME "C:\Program Files\Apache\maven"
   setx PATH "%PATH%;%MAVEN_HOME%\bin"
   ```
4. Verificar:
   ```bash
   mvn -version
   ```

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
1. Descargar [MySQL Installer](https://dev.mysql.com/downloads/installer/)
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

Ejecutar los siguientes comandos SQL:
```sql
-- Crear usuario para la aplicación
CREATE USER 'whatsapp_user'@'localhost' IDENTIFIED BY 'tu_password_seguro';

-- Otorgar permisos
GRANT ALL PRIVILEGES ON whatsapp_clone.* TO 'whatsapp_user'@'localhost';
FLUSH PRIVILEGES;

-- Crear base de datos (opcional, la app lo hace automáticamente)
CREATE DATABASE IF NOT EXISTS whatsapp_clone;

-- Salir
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

** IMPORTANTE**: 
- Nunca subir `db.properties` a control de versiones
- Usar contraseñas seguras en producción
- El archivo `.gitignore` ya incluye `db.properties`

### 5. Clonar el Proyecto

```bash
# Clonar repositorio
git clone https://github.com/RoyFarid/project_final_poo.git
cd project_final_poo

# Verificar estructura
ls -la
```

### 6. Compilar el Proyecto

```bash
# Limpiar y compilar
mvn clean install

# Si hay errores de tests, usar:
mvn clean install -DskipTests
```

Salida esperada:
```
[INFO] BUILD SUCCESS
[INFO] Total time: XX.XXX s
[INFO] Finished at: YYYY-MM-DD...
```

##  Ejecutar la Aplicación

### Método 1: Maven (Recomendado)

```bash
mvn javafx:run
```

### Método 2: JAR Ejecutable

```bash
# Compilar JAR
mvn clean package

# Ejecutar
java -jar target/whatsapp-clone-1.0.0.jar
```

### Método 3: IDE (IntelliJ IDEA / Eclipse)

#### IntelliJ IDEA
1. Abrir proyecto con `File > Open`
2. Esperar a que Maven descargue dependencias
3. Ejecutar `Main.java` con botón Run
4. Si hay error con JavaFX, agregar VM options:
   ```
   --module-path "C:\Path\To\javafx-sdk-21\lib" --add-modules javafx.controls,javafx.fxml,javafx.media
   ```

#### Eclipse
1. Importar como proyecto Maven
2. Actualizar proyecto: `Alt+F5`
3. Run As > Java Application > `Main`

##  Verificar Instalación

### 1. Verificar Base de Datos

```bash
mysql -u whatsapp_user -p whatsapp_clone
```

Ejecutar:
```sql
SHOW TABLES;
-- Debe mostrar: Usuario, Log, Transferencia

DESCRIBE Usuario;
-- Debe mostrar estructura de la tabla
```

### 2. Verificar Puertos

#### Verificar puerto MySQL (3306)
```bash
# Windows
netstat -an | findstr 3306

# Linux/macOS
netstat -an | grep 3306
```

#### Verificar puerto de aplicación (por defecto 5000)
```bash
# Windows
netstat -an | findstr 5000

# Linux/macOS
netstat -an | grep 5000
```

### 3. Ejecutar Prueba Básica

Al iniciar la aplicación, debe aparecer:
- Ventana de login
- Sin errores en consola
- Conexión exitosa a base de datos

##  Configuración de Red

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

**macOS:**
```bash
# Firewall se configura en Preferencias del Sistema > Seguridad
```

### Para Uso por Internet (Hamachi)

Ver documentación específica en `TESTEAR_CON_HAMACHI.md`

##  Solución de Problemas

### Error: "No se puede conectar a MySQL"

**Síntomas:**
```
SQLException: Communications link failure
```

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

**Síntomas:**
```
BindException: Address already in use
```

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

2. Actualizar Maven:
   ```bash
   mvn --version
   # Si es antigua, actualizar
   ```

3. Verificar Java version:
   ```bash
   mvn -version  # Debe mostrar Java 17+
   ```

### Problema: Webcam no detectada

**Soluciones:**

1. Verificar permisos de cámara (macOS/Windows)
2. Instalar drivers de webcam
3. La app funciona sin webcam (solo no habrá videollamadas)

##  Dependencias Automáticas

Maven descargará automáticamente:
- JavaFX (controls, fxml, media)
- MySQL Connector
- BCrypt
- SLF4J/Logback
- Webcam Capture
- JUnit (tests)

Total descarga: ~150MB

##  Actualización

```bash
# Obtener última versión
git pull origin main

# Recompilar
mvn clean install

# Ejecutar
mvn javafx:run
```

##  Verificación Post-Instalación

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

##  Soporte

Si persisten los problemas:

1. Revisar logs en consola
2. Verificar logs de MySQL: `/var/log/mysql/error.log` (Linux)
3. Consultar documentación adicional en `/docs`
4. Revisar issues en GitHub

---

**¡Instalación completa!** Continúa con [USER_GUIDE.md](USER_GUIDE.md) para aprender a usar la aplicación.
