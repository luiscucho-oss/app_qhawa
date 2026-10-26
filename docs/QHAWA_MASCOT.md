# Mascota nativa de Qhawa

`QhawaMascotComponent` vive en `core/designsystem/component` y sólo recibe estado,
texto y eventos. No requiere Rive, red ni permisos Android adicionales.

```kotlin
QhawaMascotComponent(
    state = QhawaMascotState.GREETING,
    message = "¡Hola! Exploremos tus registros.",
    modifier = Modifier.fillMaxWidth(),
    onMascotClick = { /* Cambiar el mensaje o procesar un evento de la pantalla. */ },
)
```

## Estados y recursos

| Estado | Imagen original | Recurso Android |
| --- | --- | --- |
| IDLE | idle.jpg | R.drawable.qhawa_mascot_idle |
| GREETING | guiño.jpg | R.drawable.qhawa_mascot_greeting |
| HAPPY | feliz.jpg | R.drawable.qhawa_mascot_happy |
| ALERT | alert_triste.jpg | R.drawable.qhawa_mascot_alert |

Los originales con fondo sólido se conservan en `img_qhawa`. Sus versiones PNG RGBA
se generan con rembg y se integran en `app/src/main/res/drawable-nodpi`, sustituyendo
los JPG anteriores con cuadrícula. Este directorio evita que Android multiplique el tamaño
de decodificación de las imágenes de 2048 × 2048 según la densidad de pantalla.
Los identificadores siguen siendo `R.drawable.*`, sin cambios en el componente Kotlin.
Los cuatro resultados se verificaron como RGBA con alfa de 0 a 255 y esquinas totalmente
transparentes. Se revisaron visualmente sobre fondo claro y oscuro en
[`evidence/qhawa-transparent-preview.png`](evidence/qhawa-transparent-preview.png).

### Preparar los originales con rembg

`scripts/prepare_qhawa_mascot.py` procesa las versiones **con fondo blanco/crema sólido**
llamadas `guiño.jpg`, `feliz.jpg`, `idle.jpg` y `alert_triste.jpg`. Comprueba que existan
las cuatro antes de cargar el modelo, reutiliza una sesión CPU y genera PNG RGBA en
`drawable_output/`. No modifica los originales ni sustituye los recursos de la app.

```bash
python3.12 -m venv /tmp/qhawa-rembg-venv
/tmp/qhawa-rembg-venv/bin/python -m pip install -r scripts/requirements-mascot.txt
/tmp/qhawa-rembg-venv/bin/python scripts/prepare_qhawa_mascot.py /ruta/a/originales
```

La primera ejecución de `new_session` descarga el modelo U2Net en la caché de rembg.
La instalación incluye `cpu` y `cli`, según la [documentación de rembg](https://github.com/danielgatis/rembg).
Después de revisar visualmente los bordes de los cuatro resultados, se pueden colocar
en `res/drawable-nodpi` sustituyendo los JPG con los mismos nombres base.

## Integración con las cuatro vistas

`HomeScreen` mantiene una instancia estable antes del contenido de cada pestaña.
`QhawaHomeCompanion` adapta el estado de las pantallas al componente:

| Pantalla | Pose y diálogo |
| --- | --- |
| Inicio | GREETING al recibir datos; IDLE esperando; ALERT sin conexión o con datos antiguos. |
| Historial | IDLE y orientación para explorar registros; mensaje de espera si no hay datos. |
| Hábitos | GREETING al entrar; HAPPY cuando se completa la revisión diaria. |
| Consejos | ALERT como pose de atención; HAPPY al registrar la acción. |

Una alerta ambiental temporal muestra su mensaje contextual en cualquiera de las vistas.
Los toques recorren dos consejos de cada pantalla mediante `onMascotClick`. Una nueva
pantalla o una novedad contextual recuperan el mensaje principal. Durante una alerta,
los consejos conservan el aviso en su texto.

## Animaciones y ciclo de vida

- `AnimatedContent`: fundido y escala entre poses, con transformaciones sin recorte.
- `rememberInfiniteTransition`: flotación de 4 dp en IDLE/GREETING.
- `Animatable`: salto al cambiar estado/mensaje y sacudida para ALERT.
- Toque: compresión elástica y salto, HAPPY temporal y retorno al estado del padre.
- Globo Material 3: visibilidad animada, texto animado y región accesible de anuncio cortés.
- `reactionKey`: cambia su valor para repetir la reacción con el mismo texto/estado.

Una sola `LaunchedEffect(state, message, reactionKey, taps)` coordina las reacciones.
Al llegar otro evento cancela la anterior, restaura las transformaciones y ejecuta
la nueva secuencia. La corrutina de toque está ligada a la composición y el callback se
ejecuta sin esperar a que termine la animación. El área táctil no tiene ripple.

Se siguieron las APIs de la [guía oficial de animación de Compose](https://developer.android.com/develop/ui/compose/animation/quick-guide).

## Validación

```bash
bash gradlew :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest --offline
bash gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.cucho.qhawa.QhawaMascotComponentTest --offline
```

`QhawaMascotComponentTest` comprueba callback y cambio de mensaje, retorno de HAPPY a la
pose del padre, interrupción con toques rápidos/alertas, ocultación del diálogo vacío y
presencia de mensajes interactivos en las cuatro pestañas. La segunda orden necesita un
dispositivo o emulador activo.

Validación actual en Medium_Phone (Android 14): las 4 pruebas instrumentadas de
`QhawaMascotComponentTest` y `EnvironmentalFlowTest` aprobaron, sin fallos ni omisiones.
Cubren toques y cambio de diálogo, retorno a la pose original, interrupciones con alertas,
navegación por las cuatro pestañas, historial con datos de ejemplo y celebración al completar
hábitos/consejos. También se compilaron los APK de aplicación y pruebas. Las pruebas
unitarias de la implementación inicial registraron 33 aprobadas y 3 omitidas.

Las capturas del emulador se guardan en `docs/evidence/mascot-emulator/`. El test crea
las imágenes en el directorio externo de la app; para conservarlas después de ejecutar,
instalar ambos APK con adb y ejecutar `am instrument`, ya que la tarea de Gradle
puede desinstalar la app y limpiar ese directorio al terminar.
