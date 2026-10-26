# Verificación de Qhawa y alertas · 26/10/2026

## Problemas encontrados y cambios

- El banner se dibujaba antes del contenido y podía quedar cubierto. Ahora está
  por encima de las cuatro pantallas, con animación de entrada/salida y cierre.
- El temporizador dependía del color. Dos avisos del mismo color podían cerrarse
  antes de tiempo. Ahora cada evento tiene identidad y reinicia sus seis segundos.
- La primera lectura peligrosa se ignoraba. Ahora una primera lectura roja o
  fuera de referencia produce un aviso; una primera lectura normal permanece silenciosa.
- Una lectura ausente se consideraba buena. Ahora se distingue de una recuperación.
- Los niveles PM divergían del firmware. Se respeta el LED publicado por Wokwi:
  VERDE <25, AMARILLO 25–<75, ROJO ≥75; el valor transmitido se redondea y el LED
  puede diferir exactamente en un límite redondeado.
- El tono se reproducía directamente con RingtoneManager, sin una notificación
  real. Ahora se publica con NotificationManager, canal de importancia alta,
  tono de notificación, icono, color, texto y acceso a Consejos.
- Se declara POST_NOTIFICATIONS y se solicita desde la campana en Android 13+.
  Rechazar ese permiso no impide ver el banner y no provoca un tono por otra vía.
- La prueba de sonido retiraba el aviso demasiado pronto. Ahora lo mantiene
  tres segundos antes de limpiar sus propios avisos de prueba.

## Cuándo se avisa

Se procesan exclusivamente lecturas frescas recibidas por MQTT, con conexión
activa. Los ejemplos locales, registros almacenados, desconexiones y mensajes
sin cambios de nivel no generan falsas alertas ambientales.

El color refleja el nivel actual más alto de las variables disponibles: verde,
ámbar o rojo. Temperatura 18–25 °C y humedad 30–60 % son referencias de confort
configuradas en la app, no criterios sanitarios. PM y los colores de Wokwi son
una simulación. No se declara una mejora real del aire del hogar.

Un cambio de nivel produce banner; los tonos tienen una pausa mínima de treinta
segundos, salvo una subida de gravedad. El aviso de Android se actualiza en una
misma entrada para evitar acumular notificaciones idénticas.

## Activar y probar manualmente

1. Instala el APK actualizado y entra a Inicio.
2. Pulsa la campana → **Permisos de notificación** → permite los avisos de Qhawa.
3. Pulsa **Probar notificación y sonido**. Es un aviso explícito de prueba.
4. Para Wokwi, desactiva los ejemplos locales y ejecuta el sketch actualizado.
5. Cambia el potenciómetro de 0 a 750 (PM2.5 alrededor de 110 µg/m³, ROJO).
6. Debe aparecer el banner y una notificación del sistema. Tocar el banner o
   la notificación abre Consejos (si hace falta, inicia sesión primero).
7. Mantener el valor estable no repite el aviso en cada publicación de cinco segundos.

El sonido depende del permiso, del canal, del tono elegido, del volumen y de No
molestar. La app respeta esos controles. En la lectura de diagnóstico del teléfono
conectado se observó volumen 12/15, sin silencio y sin No molestar.

La supervisión actual funciona mientras la interfaz de Qhawa observa MQTT. No
hay un servicio permanente ni avisos garantizados con la app cerrada.

## Wokwi y broker

El broker anterior no respondió en el puerto 1883 y Wokwi reportaba `MQTT error: -2`.
La versión actual usa `broker.hivemq.com:1883` en ambos extremos, conservando el
mismo tópico y JSON. Los archivos actualizados están en `docs/wokwi/`.

La pestaña abierta se modificó para esta prueba. Si Wokwi pide iniciar sesión
para guardar, el usuario debe guardar el sketch en su propio proyecto. Recargar
la URL original antes de guardarlo recuperará el broker anterior.

## Pruebas

- `EnvironmentalAlertEngineTest`: primera lectura, límites del circuito, valores
  ausentes, duplicados, datos antiguos, ejemplos locales, cambios consecutivos
  del mismo color y pausa entre sonidos.
- `SystemNotificationsTest`: permiso, publicación en Android, canal, importancia,
  sonido configurado y acción de la notificación; mantiene el aviso tres segundos.
- `EnvironmentalNotificationsTest`: banner visible, nuevo temporizador para dos
  eventos del mismo color y navegación a Consejos.
- `WokwiAlertsLiveTest`: recorrido real Wokwi → MQTT → banner rojo → notificación;
  guarda captura y configuración de sonido en `notification-evidence` de la copia de prueba.

Se usa `-Pqhawa.notificationCheck=true` para instalar una copia aislada llamada
**Qhawa · Verificación**, sin sustituir la identidad de la app habitual.

## Resultado concreto de esta sesión

- 52 pruebas JVM ejecutadas: 51 aprobadas, 1 omitida (pausa manual optativa),
  0 errores. Incluyen dos verificaciones MQTT en vivo con el broker actualizado.
- Lecturas reales del simulador recibidas: PM2.5 110.0 µg/m³, temperatura 24.0 °C,
  humedad 56.0 %, LED ROJO, secuencias 159, 160 y 161.
- La prueba independiente `SystemNotificationsTest` pasó en el teléfono Android 15:
  publicación, permiso, canal de importancia alta, URI sonora y acceso a la app.
- El usuario no oyó esa primera prueba: se encontró que su limpieza cancelaba
  la notificación inmediatamente. Se corrigió la prueba para mantenerla tres segundos.
- Las ejecuciones posteriores fueron bloqueadas por `INSTALL_FAILED_USER_RESTRICTED`.
  El usuario eligió instalar el APK manualmente. No se realizaron más intentos.
- No se da por confirmada la audición física del tono ni la prueba visual del
  banner en el teléfono. Los tests de interfaz correspondientes quedan preparados.

La evidencia MQTT de esta sesión se conserva en `docs/evidence/2026-10-26/`.
