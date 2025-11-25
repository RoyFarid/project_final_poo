# Instrucciones de Ejecución

## Requisitos Previos

1. **Java 17 o superior** instalado
2. **Maven 3.6+** instalado
3. **JavaFX 21** (se descarga automáticamente con Maven)

## Verificar Instalación

```bash
java -version    # Debe mostrar Java 17 o superior
mvn -version     # Debe mostrar Maven 3.6 o superior
```

## Compilar el Proyecto

```bash
mvn clean compile
```

## Ejecutar la Aplicación

### Opción 1: Usando Maven (Recomendado)

```bash
mvn javafx:run
```

### Opción 2: Compilar JAR y ejecutar manualmente

```bash
# Compilar
mvn clean package

# Ejecutar (ajustar ruta de JavaFX según tu instalación)
java --module-path <ruta-javafx>/lib --add-modules javafx.controls,javafx.fxml,javafx.media -cp target/whatsapp-clone-1.0.0.jar com.whatsapp.Main
```

### Opción 3: Con JavaFX incluido (si está instalado globalmente)

```bash
mvn clean package
java -cp target/whatsapp-clone-1.0.0.jar com.whatsapp.Main
```

## Uso de la Aplicación

### Paso 1: Login/Registro
1. Al iniciar, verá la pantalla de login
2. Si es la primera vez, haga clic en "Registrarse"
3. Complete el formulario de registro
4. Luego inicie sesión con sus credenciales

### Paso 2: Seleccionar Modo
- **Servidor**: Para aceptar conexiones de otros usuarios
- **Cliente**: Para conectarse a un servidor

### Paso 3a: Modo Servidor
1. Ingrese el puerto (por defecto 8080)
2. Haga clic en "Iniciar Servidor"
3. Verá la lista de usuarios conectados
4. Verá las actividades en tiempo real

### Paso 3b: Modo Cliente
1. Ingrese la dirección del servidor (localhost si está en la misma máquina)
2. Ingrese el puerto (8080 por defecto)
3. Haga clic en "Conectar"
4. Verá la lista de usuarios conectados
5. Haga clic en un usuario para abrir el chat

### Paso 4: Chatear
- Escriba mensajes en el campo de texto
- Presione Enter o haga clic en "Enviar"
- Use "Enviar Archivo" para compartir archivos
- Use "Videollamada" para iniciar una videollamada

## Solución de Problemas

### Error: "No se puede encontrar el módulo javafx"
- Asegúrese de tener JavaFX instalado o use Maven que lo descarga automáticamente
- Verifique que la versión de JavaFX coincida con su versión de Java

### Error: "Puerto ya en uso"
- Cambie el puerto en la configuración del servidor
- O cierre la aplicación que está usando ese puerto

### Error: "No se puede conectar al servidor"
- Verifique que el servidor esté ejecutándose
- Verifique la dirección IP y puerto
- Verifique el firewall

### La base de datos no se crea
- Verifique los permisos de escritura en el directorio
- La base de datos se crea automáticamente en `whatsapp_clone.db`

## Notas

- La base de datos SQLite se crea automáticamente
- Los archivos se guardan en el directorio actual
- Los logs se guardan en la base de datos y también se muestran en consola

