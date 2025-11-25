# Guía para Testear la Aplicación con Hamachi

Esta guía te ayudará a configurar y testear tu aplicación WhatsApp Clone con tus compañeros usando Hamachi, una VPN que permite conectar múltiples computadoras como si estuvieran en la misma red local.

## 📋 Requisitos Previos

- Todos los participantes deben tener **Hamachi instalado** (gratis)
- Todos deben estar en la **misma red de Hamachi**
- El servidor debe tener el **puerto 8080 abierto** en su firewall (o el puerto que uses)
- Todos deben tener la aplicación compilada y lista para ejecutar

---

## 🔧 Paso 1: Instalar y Configurar Hamachi

### Para el Servidor (quien alojará la sesión):

1. **Descargar e instalar Hamachi**
   - Ve a: https://www.vpn.net/
   - Descarga e instala Hamachi (versión gratuita es suficiente)

2. **Crear una red en Hamachi**
   - Abre Hamachi
   - Haz clic en el botón **"Encender"** (Power On)
   - Haz clic en **"Crear una nueva red"** (Create a new network)
   - Elige un **ID de red** (ejemplo: `whatsapp-test-2024`)
   - Crea una **contraseña** y compártela con tus compañeros
   - Anota tu **IP de Hamachi** (aparece en la interfaz, algo como `25.x.x.x`)

3. **Obtener tu IP de Hamachi**
   - En la interfaz de Hamachi, verás tu IP (ejemplo: `25.123.45.67`)
   - **Esta es la IP que compartirás con tus compañeros**

### Para los Clientes (quienes se conectarán):

1. **Descargar e instalar Hamachi** (mismo proceso que arriba)

2. **Unirse a la red de Hamachi**
   - Abre Hamachi
   - Haz clic en el botón **"Encender"** (Power On)
   - Haz clic en **"Unirse a una red existente"** (Join an existing network)
   - Ingresa el **ID de red** que te compartió el servidor
   - Ingresa la **contraseña**
   - Espera a que aparezcas en la lista de miembros de la red

---

## 🖥️ Paso 2: Configurar el Servidor

1. **Ejecutar la aplicación en modo Servidor**
   ```bash
   mvn javafx:run
   ```

2. **Iniciar sesión o registrarse**

3. **Seleccionar modo "Servidor"**

4. **Configurar el puerto**
   - Por defecto es `8080`
   - Puedes cambiarlo si es necesario
   - Haz clic en **"Iniciar Servidor"**

5. **Verificar que el servidor está activo**
   - Deberías ver: "Estado: Activo - Puerto 8080"
   - El servidor está listo para aceptar conexiones

6. **Compartir tu IP de Hamachi con los clientes**
   - Comparte tu IP de Hamachi (ejemplo: `25.123.45.67`)
   - Comparte el puerto (por defecto: `8080`)

---

## 👥 Paso 3: Configurar los Clientes

1. **Ejecutar la aplicación en modo Cliente**
   ```bash
   mvn javafx:run
   ```

2. **Iniciar sesión o registrarse**

3. **Seleccionar modo "Cliente"**

4. **Configurar la conexión**
   - En el campo **"Servidor"**, ingresa la **IP de Hamachi del servidor**
     - ❌ **NO uses** `localhost` o `127.0.0.1`
     - ✅ **USA** la IP de Hamachi (ejemplo: `25.123.45.67`)
   - En el campo **"Puerto"**, ingresa el puerto (por defecto: `8080`)

5. **Conectar**
   - Haz clic en **"Conectar"**
   - Si todo está bien, deberías ver: "Estado: Conectado a 25.123.45.67:8080"
   - Deberías ver la lista de usuarios conectados

---

## 🔥 Paso 4: Configurar el Firewall (IMPORTANTE)

### Windows Firewall:

**Para el Servidor:**

1. Abre **"Firewall de Windows Defender"**
2. Haz clic en **"Configuración avanzada"**
3. Selecciona **"Reglas de entrada"** → **"Nueva regla"**
4. Elige **"Puerto"** → **Siguiente**
5. Selecciona **TCP** y especifica el puerto (ejemplo: `8080`)
6. Elige **"Permitir la conexión"**
7. Aplica a todos los perfiles
8. Dale un nombre (ejemplo: "WhatsApp Clone Server")

**Para los Clientes:**
- Generalmente no necesitas abrir puertos, pero si tienes problemas, permite conexiones salientes en el puerto 8080

### Alternativa rápida (solo para pruebas):

Si estás en un entorno de prueba y quieres desactivar temporalmente el firewall:
- ⚠️ **Solo hazlo en redes seguras y para pruebas**
- Ve a Configuración → Red e Internet → Firewall de Windows Defender
- Desactiva temporalmente el firewall (no recomendado para producción)

---

## ✅ Paso 5: Verificar la Conexión

### Verificación Básica:

1. **Desde el servidor:**
   - Verifica que apareces en "Usuarios Conectados"
   - Deberías ver actividades cuando alguien se conecta

2. **Desde los clientes:**
   - Verifica que puedes ver otros usuarios en la lista
   - Haz clic en un usuario para abrir el chat

### Prueba de Conectividad (Opcional):

**Desde un cliente, prueba con `ping` o `telnet`:**

```bash
# En Windows CMD o PowerShell
ping 25.123.45.67  # Reemplaza con la IP de Hamachi del servidor

# O prueba el puerto específico
telnet 25.123.45.67 8080
```

Si el ping funciona pero la conexión no, probablemente es un problema de firewall.

---

## 🐛 Solución de Problemas Comunes

### Problema 1: "No se pudo conectar al servidor"

**Soluciones:**
- ✅ Verifica que el servidor esté ejecutándose y en modo "Servidor"
- ✅ Verifica que estás usando la **IP de Hamachi**, no `localhost`
- ✅ Verifica que el puerto es correcto (8080 por defecto)
- ✅ Verifica que ambos están en la misma red de Hamachi
- ✅ Verifica que el firewall del servidor permite conexiones en el puerto 8080
- ✅ Verifica que Hamachi está "Encendido" en ambas máquinas

### Problema 2: "El servidor no acepta conexiones"

**Soluciones:**
- ✅ Verifica que el servidor está en modo "Servidor" y dice "Estado: Activo"
- ✅ Verifica que el puerto no está siendo usado por otra aplicación
- ✅ Verifica que el firewall del servidor permite conexiones entrantes en el puerto 8080
- ✅ Prueba con `netstat -an | findstr 8080` para ver si el puerto está escuchando

### Problema 3: "Los usuarios no aparecen en la lista"

**Soluciones:**
- ✅ Verifica que todos están conectados correctamente
- ✅ Espera unos segundos, puede haber un pequeño retraso
- ✅ Verifica que todos están usando la misma versión de la aplicación

### Problema 4: "Hamachi muestra 'Relayed' en lugar de 'Direct'"

**Soluciones:**
- Esto significa que la conexión pasa por servidores de Hamachi (más lento pero funciona)
- Para conexión directa, verifica que:
  - Ambos tienen UPnP habilitado en sus routers
  - No hay firewalls bloqueando la conexión
  - Ambos están en la misma región geográfica

---

## 📝 Checklist de Configuración

Antes de empezar a testear, verifica:

- [ ] Hamachi instalado en todas las máquinas
- [ ] Todos están en la misma red de Hamachi
- [ ] El servidor tiene Hamachi "Encendido"
- [ ] El servidor tiene la IP de Hamachi anotada
- [ ] El servidor tiene el firewall configurado (puerto 8080 abierto)
- [ ] El servidor está ejecutando la app en modo "Servidor"
- [ ] El servidor muestra "Estado: Activo"
- [ ] Los clientes tienen la IP de Hamachi del servidor
- [ ] Los clientes están ejecutando la app en modo "Cliente"
- [ ] Todos pueden conectarse y ver la lista de usuarios

---

## 🎮 Pruebas Recomendadas

Una vez que todos estén conectados, prueba:

1. **Mensajería:**
   - Envía mensajes de texto entre usuarios
   - Verifica que los mensajes llegan correctamente

2. **Transferencia de Archivos:**
   - Envía archivos pequeños primero (texto, imágenes)
   - Luego prueba con archivos más grandes
   - Verifica el progreso de transferencia

3. **Videollamadas:**
   - Inicia una videollamada entre dos usuarios
   - Verifica que el video se transmite correctamente

4. **Múltiples Usuarios:**
   - Conecta 3-4 usuarios simultáneamente
   - Verifica que todos pueden comunicarse entre sí

5. **Reconexión:**
   - Desconecta y reconecta un cliente
   - Verifica que puede volver a conectarse sin problemas

---

## 💡 Consejos Adicionales

1. **Rendimiento:**
   - Hamachi puede ser más lento que una red local real
   - Las videollamadas pueden tener más latencia
   - Las transferencias de archivos grandes pueden tardar más

2. **Seguridad:**
   - Usa contraseñas fuertes para tu red de Hamachi
   - No compartas la red públicamente
   - Considera cambiar la contraseña después de las pruebas

3. **Alternativas a Hamachi:**
   - Si Hamachi no funciona bien, puedes probar:
     - **Radmin VPN** (gratis, similar a Hamachi)
     - **ZeroTier** (gratis, más moderno)
     - **Tailscale** (gratis para uso personal)

4. **Puertos Alternativos:**
   - Si el puerto 8080 está ocupado, puedes usar otro (ej: 8081, 9090)
   - Solo asegúrate de que todos usen el mismo puerto

---

## 📞 Soporte

Si tienes problemas que no se resuelven con esta guía:

1. Verifica los logs de la aplicación
2. Verifica los logs de Hamachi
3. Prueba con `ping` y `telnet` para diagnosticar problemas de red
4. Verifica que todos tienen las mismas versiones de Java y la aplicación

¡Buena suerte con las pruebas! 🚀

