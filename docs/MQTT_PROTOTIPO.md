# Qhawa y el prototipo físico

El ESP8266 publica una muestra aproximadamente cada tres segundos. Mosquitto la recibe en
`192.168.4.2:1883` y Qhawa se suscribe al tópico `qhawa/sensor` por el Wi‑Fi
`QHAWA_SENSOR`. La app usa la conexión Wi‑Fi para el socket MQTT aunque el teléfono
mantenga datos móviles para acceder a internet.

## Instalar y comprobar

1. Enciende el prototipo. En el equipo con Mosquitto, comprueba que el broker escucha
   en la interfaz de `192.168.4.2`. Desde esa red puedes observar publicaciones con
   `mosquitto_sub -h 192.168.4.2 -p 1883 -t qhawa/sensor -v -R`.
2. Instala el APK de depuración, conecta el teléfono a `QHAWA_SENSOR` y, si Android
   indica que esa red carece de acceso a internet, elige permanecer conectado. En
   Android 17 o superior, concede **Permitir acceso a la red local** en Inicio.
3. En Inicio, mantén desactivado **Explorar datos de ejemplo**. Comprueba que aparecen
   temperatura, humedad, PM2.5 y PM10 y que los valores coinciden con el tópico.
4. Abre Historial → **Hoy**. Comprueba PM2.5 y pulsa **Ver PM10** para ver la otra gráfica.
   La app guarda cada lectura válida en SQLite por hasta 30 días. La gráfica carga
   la última muestra por minuto, mientras que el CSV exporta todas las lecturas
   del período seleccionado.
5. Abre Consejos y compara el encabezado con el PM2.5 de Inicio. Abre una tarjeta
   y comprueba que el panel muestra las instrucciones. Puedes abrir otro consejo
   o registrar otra acción inmediatamente.

| Nivel del prototipo | PM2.5 reportado | Acción |
| --- | --- | --- |
| Verde | 0 a 12 µg/m³, inclusive | Prevenir partículas |
| Amarillo | Mayor que 12 y hasta 35 µg/m³ | Reducir partículas |
| Rojo | Mayor que 35 µg/m³ | Actuar ahora |

El firmware ya aplica el factor de escala antes de publicar. El valor `-1` se
trata como dato ausente, y cuando falta PM2.5 o se pierden las lecturas en vivo,
Consejos muestra orientación general. Estos rangos replican el indicador del
circuito.

## Pruebas

Para compilar ambos APK, ejecuta `bash gradlew :app:assembleDebug :app:assembleDebugAndroidTest`.
Para la prueba JVM contra un Mosquitto independiente, inicia `mosquitto -p 28883`
en modo local y ejecuta `bash gradlew :app:testDebugUnitTest -Pqhawa.testMqttPort=28883`.
Esta prueba usa mensajes de prueba y comprueba los límites de los tres rangos.

Ejecuta este comando para la prueba instrumentada con el hardware conectado.

```bash
bash gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.cucho.qhawa.PrototypeMqttInstrumentedTest \
  -Pandroid.testInstrumentationRunnerArguments.prototypeMqtt=true
```

En Android 17, concede el permiso a Qhawa antes de lanzar la prueba. Esta
verificación espera una publicación posterior al arranque de la prueba y
comprueba Inicio, SQLite, Historial y el panel de consejos. El resultado
corresponde al rango que el prototipo emita durante la prueba.

Para verificar el globo, el fondo y la tarjeta de PM2.5 en los tres rangos,
ejecuta la misma clase con `-Pandroid.testInstrumentationRunnerArguments.prototypeRanges=true`.
La prueba espera lecturas reales de cada rango y comprueba el texto y el color
mostrados en Inicio.
