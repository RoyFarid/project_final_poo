# Guía de Usuario

## Introducción

Aplicación de mensajería con chat, transferencia de archivos y videollamadas.

## Inicio Rápido

### Primer Uso

1. **Ejecutar la aplicación**
   ```bash
   mvn javafx:run
   ```

2. **Pantalla de Login**
   
   Opciones:
   - Registrarse
   - Iniciar sesión
   - Modo (Servidor/Cliente)

##  Gestión de Cuenta

### Registro de Usuario

1. En la pantalla de login, hacer clic en **"Registrarse"**
2. Completar el formulario:
   - **Username**: Nombre único de usuario (mínimo 3 caracteres)
   - **Email**: Dirección de correo electrónico válida
   - **Password**: Contraseña (mínimo 6 caracteres)
   - **Confirmar Password**: Repetir la contraseña

3. Hacer clic en **"Registrar"**

**Requisitos del Username:**
- Único en el sistema
- Alfanumérico (letras y números)
- Sin espacios

**Requisitos de Password:**
- Mínimo 6 caracteres
- Se recomienda usar mayúsculas, minúsculas y números

### Inicio de Sesión

1. Ingresar **Username**
2. Ingresar **Password**
3. Seleccionar modo:
   - **Modo Servidor**: Para ser el host
   - **Modo Cliente**: Para conectarse a un servidor
4. Hacer clic en **"Iniciar Sesión"**

##  Modo Servidor

### Iniciar Servidor

1. Iniciar sesión seleccionando "Modo Servidor"
2. Configurar puerto (default: 5000)
3. Hacer clic en **"Iniciar Servidor"**

**Estado del servidor:**
```
Servidor activo en puerto 5000
 Clientes conectados: 3
```

### Interfaz del Servidor

La ventana del servidor muestra:

#### Panel de Clientes Conectados
- Lista de usuarios conectados en tiempo real
- IP y puerto de cada cliente
- Indicador de estado (conectado/desconectado)

#### Panel de Chat
- Ver todos los mensajes entre usuarios
- Enviar mensajes a clientes específicos
- Historial de conversaciones

#### Controles
- **Seleccionar destinatario**: Lista desplegable con clientes conectados
- **Enviar Mensaje**: Botón para enviar texto
- **Enviar Archivo**: Botón para compartir archivos
- **Iniciar Video**: Botón para videollamada
- **Desconectar Todos**: Cerrar todas las conexiones

### Gestionar Clientes

**Ver información del cliente:**
- Hacer clic en un cliente en la lista
- Ver detalles: IP, puerto, tiempo conectado

**Enviar mensaje a un cliente:**
1. Seleccionar cliente en lista desplegable
2. Escribir mensaje en campo de texto
3. Presionar Enter o clic en "Enviar"

**Desconectar cliente específico:**
1. Clic derecho en cliente
2. Seleccionar "Desconectar"

##  Modo Cliente

### Conectarse al Servidor

1. Iniciar sesión seleccionando "Modo Cliente"
2. Ingresar datos del servidor:
   - **Host**: Dirección IP del servidor
     - Red local: IP del servidor (ej: `192.168.1.100`)
     - Mismo equipo: `localhost` o `127.0.0.1`
     - Hamachi: IP virtual de Hamachi
   - **Puerto**: Puerto del servidor (default: 5000)
3. Hacer clic en **"Conectar"**

**Estados de conexión:**
-  Desconectado
- 🟡 Conectando...
- 🟢 Conectado

### Interfaz del Cliente

La ventana del cliente incluye:

#### Lista de Usuarios
- Usuarios conectados al servidor
- Estado de cada usuario (online/offline)
- Selección de destinatario para chat

#### Panel de Chat
- Mensajes enviados y recibidos
- Timestamp de cada mensaje
- Indicador de mensaje propio vs. recibido

#### Controles de Comunicación
- Campo de texto para mensajes
- Botón "Enviar"
- Botón "Archivo"
- Botón "Video"

##  Chat de Texto

### Enviar Mensajes

1. **Seleccionar destinatario** en la lista de usuarios
2. **Escribir mensaje** en el campo de texto
3. **Enviar** con:
   - Clic en botón "Enviar"
   - Presionar tecla Enter

**Ejemplo:**
```
[10:30 AM] Tú: Hola, ¿cómo estás?
[10:31 AM] Juan: ¡Bien! ¿Y tú?
```

### Mensajes Recibidos

Los mensajes entrantes aparecen automáticamente en el chat con:
- Nombre del remitente
- Hora de envío
- Contenido del mensaje

**Formato:**
```
[Hora] Remitente: Mensaje
```

### Emojis y Caracteres Especiales

La aplicación soporta:
- Emojis estándar
- Caracteres especiales (ñ, á, é, etc.)
- Múltiples líneas (Shift + Enter)

##  Transferencia de Archivos

### Enviar Archivo

1. **Seleccionar destinatario** en la lista
2. Hacer clic en botón **"Archivo"** o **"Enviar Archivo"**
3. **Navegar** al archivo deseado
4. **Seleccionar** el archivo
5. Hacer clic en **"Abrir"**

**Tipos de archivo soportados:**
- Documentos (PDF, DOC, TXT, etc.)
- Imágenes (JPG, PNG, GIF, etc.)
- Videos (MP4, AVI, etc.)
- Audio (MP3, WAV, etc.)
- Archivos comprimidos (ZIP, RAR, etc.)

**Límite de tamaño:** 100 MB por archivo

### Proceso de Transferencia

Durante la transferencia verás:

```
 Enviando: documento.pdf
 Progreso: [] 80%
⏱ Tiempo restante: 5 segundos
```

**Estados:**
-  Enviando...
- Enviado correctamente
- Error en transferencia

### Recibir Archivo

Cuando recibes un archivo:

1. **Notificación** aparece en chat:
   ```
   [10:45 AM] Juan envió: documento.pdf (2.5 MB)
   ```

2. **Diálogo de confirmación**:
   - Aceptar descarga
   - Rechazar archivo

3. **Guardar archivo**:
   - Seleccionar ubicación
   - Hacer clic en "Guardar"

4. **Verificación automática**:
   - La aplicación verifica integridad con checksum
   - Confirmación si el archivo está completo

### Historial de Transferencias

Ver todas las transferencias:
- Panel "Transferencias"
- Filtrar por: Enviadas / Recibidas
- Ver detalles: Nombre, tamaño, fecha, estado

##  Videollamadas

### Iniciar Videollamada

1. **Seleccionar destinatario**
2. Hacer clic en botón **"Video"** o **"Iniciar Videollamada"**
3. **Confirmar** acceso a cámara y micrófono
4. Esperar que el destinatario **acepte** la llamada

### Durante la Videollamada

**Interfaz de video:**
```

   Video del Destinatario    
                              
         (pantalla grande)    
                              


 Tu video   (esquina)


[] [] [] []
```

**Controles disponibles:**

- ** Micrófono**:
  - Verde: Activado
  - Rojo: Silenciado
  - Clic para alternar

- ** Altavoz**:
  - Verde: Activado
  - Rojo: Silenciado
  - Clic para alternar

- ** Cámara**:
  - Verde: Activada
  - Rojo: Desactivada
  - Clic para alternar

- **Finalizar**:
  - Terminar videollamada

### Recibir Videollamada

Cuando alguien te llama:

1. **Notificación emergente**:
   ```
    Juan te está llamando
   [Aceptar] [Rechazar]
   ```

2. **Opciones**:
   - **Aceptar**: Inicia la videollamada
   - **Rechazar**: Declina la llamada

3. **Preparación**:
   - Se activa tu cámara
   - Se activa tu micrófono
   - Comienza el streaming

### Finalizar Videollamada

Para terminar una videollamada:
1. Hacer clic en botón **"Finalizar"** ()
2. Confirmación: "¿Deseas terminar la llamada?"
3. Clic en "Sí"

**Ambos usuarios son desconectados automáticamente**

### Solución de Problemas de Video

**Cámara no detectada:**
- Verificar que la cámara esté conectada
- Revisar permisos de la aplicación
- Reiniciar la aplicación

**Audio no funciona:**
- Verificar que micrófono esté seleccionado
- Comprobar volumen del sistema
- Verificar que altavoz/audífonos estén conectados

**Video entrecortado:**
- Mejorar conexión a internet
- Cerrar otras aplicaciones que usen red
- Reducir calidad de video en configuración

## Configuración

### Ajustes de la Aplicación

Acceder a configuración: **Menú > Configuración**

#### Perfil
- Cambiar username (si está disponible)
- Actualizar email
- Cambiar contraseña
- Ver estadísticas de uso

#### Notificaciones
- Sonido de mensaje
- Notificaciones de escritorio
- Alerta de archivo recibido
- Notificación de llamada

#### Red
- Puerto predeterminado
- Tiempo de espera de conexión
- Número de reintentos
- Modo de reconexión automática

#### Video
- Calidad de video (Baja/Media/Alta)
- FPS (15/30/60)
- Resolución de cámara
- Dispositivo de cámara predeterminado

#### Audio
- Dispositivo de micrófono
- Dispositivo de altavoz
- Volumen de entrada
- Volumen de salida
- Cancelación de eco

##  Seguridad y Privacidad

### Contraseña Segura

**Recomendaciones:**
- Mínimo 8 caracteres
- Combinación de mayúsculas y minúsculas
- Incluir números
- Incluir símbolos especiales
- No usar información personal

### Cambiar Contraseña

1. Ir a **Configuración > Perfil**
2. Clic en **"Cambiar Contraseña"**
3. Ingresar contraseña actual
4. Ingresar nueva contraseña
5. Confirmar nueva contraseña
6. Clic en **"Actualizar"**

### Cerrar Sesión Segura

Para cerrar sesión correctamente:
1. **Finalizar** todas las comunicaciones activas
2. Ir a **Menú > Cerrar Sesión**
3. Confirmar acción

**La aplicación:**
- Desconecta del servidor
- Cierra conexiones activas
- Limpia datos de sesión

##  Información y Estadísticas

### Ver Estadísticas

Menú > Estadísticas

**Información disponible:**
- Total de mensajes enviados
- Total de mensajes recibidos
- Archivos transferidos
- Tiempo total en videollamadas
- Usuarios contactados
- Última conexión

### Logs de Actividad

Ver historial de actividades:
1. Menú > Logs
2. Filtrar por:
   - Fecha
   - Tipo de actividad
   - Usuario

**Tipos de eventos registrados:**
- Inicio de sesión
- Mensajes enviados/recibidos
- Transferencias de archivos
- Videollamadas
- Errores de conexión

##  Solución de Problemas

### No puedo conectarme al servidor

**Verificar:**
Servidor activo, IP/puerto correctos, firewall permite conexión, misma red (LAN).

**Intentar:**
`ping <IP_servidor>`, verificar firewall, reiniciar app.

### Mensajes no se envían

**Verificar:** Conexión activa (verde), destinatario seleccionado, mensaje no vacío.

**Solución:** Reseleccionar destinatario, verificar red, reintentar.

### Video no se visualiza

**Verificar:** Cámara conectada, permisos otorgados, no usada por otra app.

**Solución:** Cerrar otras apps de video, reiniciar app, verificar drivers.

### Audio no se escucha

**Verificar:** Micrófono/altavoz no silenciados, volumen del sistema, dispositivos correctos.

**Solución:** Probar audio en otra app, verificar configuración, reiniciar dispositivos.

##  Consejos y Trucos

### Atajos de Teclado

| Atajo | Acción |
|-------|--------|
| `Enter` | Enviar mensaje |
| `Shift+Enter` | Nueva línea |
| `Ctrl+F` | Buscar en chat |
| `Ctrl+L` | Ver logs |
| `Ctrl+Q` | Cerrar sesión |
| `Esc` | Cerrar diálogo actual |

### Optimización de Rendimiento

**Para mejor experiencia:**
1. Cerrar aplicaciones innecesarias
2. Usar conexión por cable (Ethernet) en vez de WiFi
3. Configurar calidad de video según tu conexión
4. Mantener actualizada la aplicación

### Mejores Prácticas

1. **Mantener sesión activa:**
   - No cerrar la ventana abruptamente
   - Usar "Cerrar Sesión" apropiadamente

2. **Transferencia de archivos:**
   - Comprimir archivos grandes antes de enviar
   - Verificar tipo de archivo antes de enviar

3. **Videollamadas:**
   - Buena iluminación para mejor video
   - Usar audífonos para mejor audio
   - Estabilizar la cámara

##  Uso Móvil / Remoto

### Acceso desde Red Externa

Ver `TESTEAR_CON_HAMACHI.md` para configuración de VPN.

### Hamachi (Conexión por Internet)

1. Instalar Hamachi en ambos equipos
2. Crear/unirse a red Hamachi
3. Usar IP de Hamachi para conectar
4. Puerto: 5000 (default)

##  Soporte y Ayuda

### Obtener Ayuda

**Dentro de la aplicación:**
- Menú > Ayuda > Documentación
- Menú > Ayuda > Acerca de

**Reportar problemas:**
1. Menú > Ayuda > Reportar Problema
2. Describir el error
3. Adjuntar logs si es posible

### Información del Sistema

Para soporte técnico, proporcionar:
- Versión de la aplicación
- Sistema operativo
- Logs de error
- Descripción del problema

**Obtener versión:**
Menú > Acerca de

---

**¡Disfruta usando WhatsApp Clone!** Para más detalles técnicos, consulta [ARCHITECTURE.md](ARCHITECTURE.md) y [API.md](API.md).
