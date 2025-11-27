# Guía de Usuario

## Introducción

RoomWave es una aplicación de mensajería instantánea con arquitectura cliente-servidor. Permite chat en tiempo real, transferencia de archivos, videollamadas y gestión de salas de chat grupales (rooms).

## Inicio Rápido

### Primer Uso

1. Ejecutar la aplicación:
   ```bash
   mvn javafx:run
   ```

2. En la pantalla de login:
   - Registrarse: Crear nueva cuenta
   - Iniciar sesión: Acceder con cuenta existente
   - Seleccionar modo: Servidor o Cliente

## Gestión de Cuenta

### Registro de Usuario

1. En la pantalla de login, hacer clic en "Registrarse"
2. Completar el formulario:
   - Username: Nombre único de usuario (mínimo 3 caracteres)
   - Email: Dirección de correo electrónico válida
   - Password: Contraseña (mínimo 6 caracteres)
   - Confirmar Password: Repetir la contraseña
3. Hacer clic en "Registrar"

**Requisitos:**
- Username: Único, alfanumérico, sin espacios
- Password: Mínimo 6 caracteres (recomendado: mayúsculas, minúsculas, números)

### Inicio de Sesión

1. Ingresar Username
2. Ingresar Password
3. Seleccionar modo:
   - Modo Servidor: Para hostear la sesión
   - Modo Cliente: Para conectarse a un servidor
4. Hacer clic en "Iniciar Sesión"

## Modo Servidor

### Iniciar Servidor

1. Iniciar sesión seleccionando "Modo Servidor"
2. Configurar puerto (default: 5000)
3. Hacer clic en "Iniciar Servidor"

El servidor muestra el estado y número de clientes conectados.

### Interfaz del Servidor

**Panel de Clientes Conectados:**
- Lista de usuarios conectados en tiempo real
- IP y puerto de cada cliente
- Indicador de estado (conectado/desconectado)

**Panel de Chat:**
- Ver todos los mensajes entre usuarios
- Enviar mensajes a clientes específicos
- Historial de conversaciones

**Gestión de Rooms:**
- Ver solicitudes de rooms pendientes
- Aprobar o rechazar rooms
- Crear rooms directamente
- Gestionar miembros de rooms

**Controles Administrativos:**
- Silenciar usuarios
- Bloquear mensajes de usuarios
- Desactivar cámara de usuarios

### Gestionar Clientes

**Ver información del cliente:**
- Hacer clic en un cliente en la lista
- Ver detalles: IP, puerto, tiempo conectado

**Enviar mensaje a un cliente:**
1. Seleccionar cliente en lista desplegable
2. Escribir mensaje en campo de texto
3. Presionar Enter o clic en "Enviar"

**Desconectar cliente específico:**
- Clic derecho en cliente
- Seleccionar "Desconectar"

## Modo Cliente

### Conectarse al Servidor

1. Iniciar sesión seleccionando "Modo Cliente"
2. Ingresar datos del servidor:
   - Host: Dirección IP del servidor
     - Red local: IP del servidor (ej: `192.168.1.100`)
     - Mismo equipo: `localhost` o `127.0.0.1`
   - Puerto: Puerto del servidor (default: 5000)
3. Hacer clic en "Conectar"

**Estados de conexión:**
- Desconectado
- Conectando...
- Conectado

### Interfaz del Cliente

**Lista de Usuarios:**
- Usuarios conectados al servidor
- Estado de cada usuario (online/offline)
- Selección de destinatario para chat

**Panel de Chat:**
- Mensajes enviados y recibidos
- Timestamp de cada mensaje
- Indicador de mensaje propio vs. recibido

**Gestión de Rooms:**
- Ver rooms disponibles
- Crear solicitud de room
- Unirse a rooms activos
- Enviar mensajes y archivos a rooms

**Controles de Comunicación:**
- Campo de texto para mensajes
- Botón "Enviar"
- Botón "Archivo"
- Botón "Video"

## Chat de Texto

### Enviar Mensajes

1. Seleccionar destinatario en la lista de usuarios
2. Escribir mensaje en el campo de texto
3. Enviar con:
   - Clic en botón "Enviar"
   - Presionar tecla Enter

**Formato de mensajes:**
```
[10:30 AM] Tú: Hola, ¿cómo estás?
[10:31 AM] Juan: ¡Bien! ¿Y tú?
```

### Mensajes Recibidos

Los mensajes entrantes aparecen automáticamente con:
- Nombre del remitente
- Hora de envío
- Contenido del mensaje

### Caracteres Especiales

La aplicación soporta:
- Emojis estándar
- Caracteres especiales (ñ, á, é, etc.)
- Múltiples líneas (Shift + Enter)

## Sistema de Rooms

### Crear Room

**Desde Cliente:**
1. Hacer clic en "Crear Room" o "Nuevo Room"
2. Ingresar nombre del room
3. Seleccionar miembros a incluir
4. Opcional: Mensaje de solicitud
5. Opcional: Incluir servidor en el room
6. Enviar solicitud

**Desde Servidor:**
1. Hacer clic en "Crear Room"
2. Ingresar nombre del room
3. Seleccionar miembros
4. Opcional: Incluir servidor
5. Crear room directamente (sin aprobación)

### Aprobar/Rechazar Rooms

**Solo Servidor:**
1. Ver solicitudes pendientes en panel de rooms
2. Revisar detalles: nombre, creador, miembros, mensaje
3. Aprobar o rechazar la solicitud

### Unirse a Room

1. Ver lista de rooms activos disponibles
2. Seleccionar room
3. Hacer clic en "Unirse"
4. El servidor procesa la solicitud

### Chat en Room

1. Seleccionar room de la lista
2. Ver mensajes del room
3. Escribir mensaje en campo de texto
4. Enviar mensaje (se distribuye a todos los miembros)

### Salir de Room

1. Seleccionar room
2. Hacer clic en "Salir de Room"
3. Confirmar acción

### Cerrar Room

**Solo Creador o Servidor:**
1. Seleccionar room
2. Hacer clic en "Cerrar Room"
3. Confirmar acción
4. Todos los miembros son notificados

## Transferencia de Archivos

### Enviar Archivo

1. Seleccionar destinatario o room
2. Hacer clic en botón "Archivo" o "Enviar Archivo"
3. Navegar al archivo deseado
4. Seleccionar el archivo
5. Hacer clic en "Abrir"

**Tipos soportados:**
- Documentos (PDF, DOC, TXT, etc.)
- Imágenes (JPG, PNG, GIF, etc.)
- Videos (MP4, AVI, etc.)
- Audio (MP3, WAV, etc.)
- Archivos comprimidos (ZIP, RAR, etc.)

**Límite:** 100 MB por archivo

### Proceso de Transferencia

Durante la transferencia se muestra:
- Nombre del archivo
- Progreso en porcentaje
- Tiempo restante estimado

**Estados:**
- Enviando...
- Enviado correctamente
- Error en transferencia

### Recibir Archivo

1. Notificación aparece en chat:
   ```
   [10:45 AM] Juan envió: documento.pdf (2.5 MB)
   ```
2. Diálogo de confirmación:
   - Aceptar descarga
   - Rechazar archivo
3. Guardar archivo:
   - Seleccionar ubicación
   - Hacer clic en "Guardar"
4. Verificación automática de integridad

### Historial de Transferencias

Ver todas las transferencias:
- Panel "Transferencias"
- Filtrar por: Enviadas / Recibidas
- Ver detalles: Nombre, tamaño, fecha, estado

## Videollamadas

### Iniciar Videollamada

1. Seleccionar destinatario
2. Hacer clic en botón "Video" o "Iniciar Videollamada"
3. Confirmar acceso a cámara y micrófono
4. Esperar que el destinatario acepte la llamada

### Durante la Videollamada

**Interfaz de video:**
- Video del destinatario (pantalla grande)
- Tu video (esquina)

**Controles:**
- Micrófono: Verde (activado) / Rojo (silenciado)
- Altavoz: Verde (activado) / Rojo (silenciado)
- Cámara: Verde (activada) / Rojo (desactivada)
- Finalizar: Terminar videollamada

### Recibir Videollamada

1. Notificación emergente:
   ```
   Juan te está llamando
   [Aceptar] [Rechazar]
   ```
2. Opciones:
   - Aceptar: Inicia la videollamada
   - Rechazar: Declina la llamada
3. Preparación automática:
   - Se activa tu cámara
   - Se activa tu micrófono
   - Comienza el streaming

### Finalizar Videollamada

1. Hacer clic en botón "Finalizar"
2. Confirmación: "¿Deseas terminar la llamada?"
3. Clic en "Sí"
4. Ambos usuarios son desconectados automáticamente

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

Acceder a configuración: Menú > Configuración

**Perfil:**
- Cambiar username (si está disponible)
- Actualizar email
- Cambiar contraseña
- Ver estadísticas de uso

**Notificaciones:**
- Sonido de mensaje
- Notificaciones de escritorio
- Alerta de archivo recibido
- Notificación de llamada

**Red:**
- Puerto predeterminado
- Tiempo de espera de conexión
- Número de reintentos
- Modo de reconexión automática

**Video:**
- Calidad de video (Baja/Media/Alta)
- FPS (15/30/60)
- Resolución de cámara
- Dispositivo de cámara predeterminado

**Audio:**
- Dispositivo de micrófono
- Dispositivo de altavoz
- Volumen de entrada
- Volumen de salida
- Cancelación de eco

## Seguridad y Privacidad

### Contraseña Segura

**Recomendaciones:**
- Mínimo 8 caracteres
- Combinación de mayúsculas y minúsculas
- Incluir números
- Incluir símbolos especiales
- No usar información personal

### Cambiar Contraseña

1. Ir a Configuración > Perfil
2. Clic en "Cambiar Contraseña"
3. Ingresar contraseña actual
4. Ingresar nueva contraseña
5. Confirmar nueva contraseña
6. Clic en "Actualizar"

### Cerrar Sesión Segura

1. Finalizar todas las comunicaciones activas
2. Ir a Menú > Cerrar Sesión
3. Confirmar acción

La aplicación:
- Desconecta del servidor
- Cierra conexiones activas
- Limpia datos de sesión

## Información y Estadísticas

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

## Solución de Problemas

### No puedo conectarme al servidor

**Verificar:**
- Servidor está activo
- IP y puerto correctos
- Firewall permite conexión
- Misma red (LAN)

**Intentar:**
- `ping <IP_servidor>`
- Verificar firewall
- Reiniciar aplicación

### Mensajes no se envían

**Verificar:**
- Conexión activa (verde)
- Destinatario seleccionado
- Mensaje no vacío

**Solución:**
- Reseleccionar destinatario
- Verificar red
- Reintentar

### Video no se visualiza

**Verificar:**
- Cámara conectada
- Permisos otorgados
- No usada por otra app

**Solución:**
- Cerrar otras apps de video
- Reiniciar app
- Verificar drivers

### Audio no se escucha

**Verificar:**
- Micrófono/altavoz no silenciados
- Volumen del sistema
- Dispositivos correctos

**Solución:**
- Probar audio en otra app
- Verificar configuración
- Reiniciar dispositivos

## Consejos y Trucos

### Atajos de Teclado

| Atajo | Acción |
|-------|--------|
| Enter | Enviar mensaje |
| Shift+Enter | Nueva línea |
| Ctrl+F | Buscar en chat |
| Ctrl+L | Ver logs |
| Ctrl+Q | Cerrar sesión |
| Esc | Cerrar diálogo actual |

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

4. **Rooms:**
   - Usar nombres descriptivos para rooms
   - Incluir servidor solo si es necesario
   - Gestionar miembros activamente

