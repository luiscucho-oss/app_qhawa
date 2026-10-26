# Qhawa ↔ Wokwi por MQTT

## Configuración implementada

Proyecto: https://wokwi.com/projects/477950143546040321

- Broker: `tcp://test.mosquitto.org:1883`.
- Tópico exacto del sketch: `qhawa/demo/477386567410462721/lecturas`.
  Su identificador difiere del ID de la URL; no se debe sustituir por el ID de la URL.
- Dispositivo esperado: `macegato-demo-01`.
- MQTT 3.1.1, QoS 0, sin usuario ni contraseña, cliente único por suscripción.
- Publicación cada 5 segundos; reconexión de la app cada 5 segundos.
- Más de 20 segundos sin mensaje válido: «Esperando lecturas», aunque el broker siga conectado.

El proyecto Wokwi se usa tal como está: no necesita modificaciones ni mensajes
inventados desde la app. Qhawa solo se suscribe. El broker público no permite
autenticar el origen de los mensajes; este enlace es exclusivamente para la simulación.

## Uso

1. Abre el proyecto Wokwi y pulsa **Start the simulation**.
2. Espera `MQTT conectado` y `MQTT TX enviado` en su consola.
3. Instala/ejecuta la nueva versión de Qhawa y entra a Inicio.
4. Mantén **Explorar datos de ejemplo** desactivado. Ese interruptor muestra
   ejemplos locales anteriores, separados del flujo MQTT.
5. Inicio mostrará **Wokwi en vivo · Simulación**, PM2.5, temperatura y humedad.
6. Mueve el potenciómetro y cambia temperatura/humedad pulsando el DHT22.
   El próximo mensaje, aproximadamente en 5 segundos de simulación, actualizará la app.
7. Abre Historial para consultar los registros recibidos.
8. Detén Wokwi. Después de 20 segundos reales sin publicaciones válidas,
   la app avisa que espera lecturas y conserva el historial.

El teléfono y Wokwi necesitan acceso a internet, no necesitan estar en la misma
red Wi-Fi. La app no necesita un cable USB para MQTT; el cable sirve para instalar
la compilación desde Android Studio. La simulación debe permanecer ejecutándose:
Wokwi no publica cuando está detenida, y no hay mensajes retenidos en el sketch.

## Contrato de datos

```json
{"device_id":"macegato-demo-01","simulated":true,"pollutant":"PM2.5","value":42.3,"unit":"ug/m3","state":"AMARILLO","threshold_profile":"demo_not_health_standard","temperature_c":24.0,"humidity_pct":56.0,"dht_ok":true,"uptime_ms":5007,"sequence":1}
```

PM2.5 se calcula en Wokwi desde el potenciómetro, entre 0 y 150 µg/m³: no existe
un sensor físico PM en este circuito. Temperatura y humedad proceden del DHT22
simulado. Si `dht_ok=false`, ambas variables aparecen como ausentes (—).

La app muestra el estado LED publicado por el circuito: VERDE (<25), AMARILLO
(25–<75), ROJO (≥75). Son umbrales demostrativos, nunca categorías sanitarias.
El estado LED se respeta incluso en los límites de redondeo del valor a un decimal.

`uptime_ms` es el tiempo desde que arrancó el ESP32, no una fecha Unix.
Se usa la fecha de recepción del teléfono y se etiqueta como tal. Los mensajes
retenidos se rechazan porque el sketch no envía una fecha absoluta verificable.
Se validan identidad, simulación, unidades, variables, rangos y secuencias duplicadas.
El reinicio de la secuencia/uptime permite retomar la simulación al reiniciar el ESP32.

## Historial y ciclo de vida

SQLite local, separado de los ejemplos estáticos, sin subir datos de cuentas.
Se conserva la última lectura recibida de cada minuto durante 30 días. Inicio
se actualiza en cada publicación de 5 segundos. Los filtros de 7/30 días solo
se habilitan cuando existen registros recibidos de ese período.

La conexión es compartida entre las cuatro secciones y se suspende cinco segundos
después de que la interfaz deje de observarla. Al volver se conecta y se suscribe
de nuevo. No hay un servicio de captura en segundo plano: mientras la app esté
cerrada no se recopilan muestras, ni se reconstruye un historial ficticio.

## Validación

Pruebas regulares y compilación:

```sh
bash gradlew testDebugUnitTest assembleDebug lintDebug --no-configuration-cache
```

Prueba optativa contra la simulación real, sin publicar datos artificiales:

```sh
bash gradlew testDebugUnitTest -Pqhawa.liveMqtt=true --no-configuration-cache
```

Para reproducir la comprobación de cambios, configura el potenciómetro en 750
(aprox. 110 µg/m³), DHT22 en 29 °C y 68 %, y ejecuta:

```sh
bash gradlew testDebugUnitTest -Pqhawa.liveMqtt=true -Pqhawa.expectedPmMin=100 -Pqhawa.expectedTemperature=29 -Pqhawa.expectedHumidity=68 --no-configuration-cache
```

La evidencia de tres mensajes consecutivos se guarda en
`app/build/reports/mqtt-live-readings.jsonl`. La prueba se omite por defecto para
que una compilación habitual no dependa de Wokwi o del broker público.

La recepción se comprobó tanto con un cliente Paho independiente como con el
mismo `MqttEnvironmentRepository` que observa Compose. También se pausó y reanudó
Wokwi durante una prueba optativa: el repositorio pasó de CONNECTED a WAITING y
volvió a CONNECTED sin reiniciar la app, conservando las muestras.
La evidencia de esa prueba queda en `app/build/reports/mqtt-pause-phase.txt`.

La comprobación de pantalla en Android se puede ejecutar con un teléfono
conectado y Wokwi en marcha:

```sh
bash gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.liveMqtt=true --no-configuration-cache
```

`WokwiMqttInstrumentedTest` espera una lectura, comprueba PM2.5 y el LED demo en
Inicio y guarda una captura en los archivos privados de la app (`mqtt-home-proof.png`).

## APK para instalación manual

Se ofrece **Qhawa MQTT** (`com.cucho.qhawa.mqtt`) como copia de depuración
independiente. Android rechazó actualizar la versión anterior por una firma
diferente (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`). La copia evita el conflicto
de identidad; no es necesario desinstalar una versión existente para instalarla.

```sh
bash gradlew assembleDebug -Pqhawa.mqttPreview=true --no-configuration-cache
```

La opción solo cambia la identidad/nombre de la compilación debug; el ID de
producción sigue siendo `com.cucho.qhawa`. Sin la opción se genera el paquete
original. El APK entregable se copia a
`app/build/outputs/apk/mqtt-preview/Qhawa-MQTT-debug.apk`.

### Resultado de esta sesión

- Compilación Android, pruebas unitarias y Lint sin errores.
- Recepción real desde Wokwi con PM2.5 = 110.0 µg/m³, temperatura = 29.0 °C,
  humedad = 68.0 %, LED demo ROJO; secuencias consecutivas verificadas.
- Prueba del repositorio usado por la app y de su escritura en el almacén de historial.
- Pausa: WAITING después de más de 20 s sin lecturas. Reanudación: CONNECTED sin
  reiniciar la app, verificado en el mismo repositorio.
- Prueba de interfaz Android compilada, pero no ejecutada: el teléfono canceló
  la instalación (`INSTALL_FAILED_USER_RESTRICTED`). El usuario eligió instalar
  el APK manualmente; no se realizó otro intento.
