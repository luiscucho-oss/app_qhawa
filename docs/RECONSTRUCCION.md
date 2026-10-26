> Actualización MQTT: la integración Wokwi ya está implementada. Consulta [MQTT_WOKWI.md](MQTT_WOKWI.md) para configuración, uso y validación. Las observaciones de integración pendiente de esta revisión inicial se refieren al circuito físico de producción.

# Qhawa · Reconstrucción según el Prompt Maestro

## Revisión rápida

La base es Android nativo con Kotlin, Jetpack Compose, Navigation Compose,
Material 3, Inter y repositorios de autenticación Firebase/demostración.
Se conserva esa tecnología, la paleta y los formularios con validación.

Se corrigieron las funciones de bienestar animal, las lecturas fijas que
aparentaban ser reales, la conexión ficticia, las categorías de calidad sin
criterios y los botones sin comportamiento de las secciones principales.
El gato blanco con detalles marrones queda como identidad visual.

## Experiencia

- Bienvenida: hogar y aire, propósito y botón Comenzar.
- Acceso: login, registro y recuperación existentes. Sin Firebase se advierte
  que es una sesión de demostración, sin cuenta remota. No se simula el envío de correo.
- Inicio: conexión, última lectura, PM en µg/m³, temperatura en °C y humedad en %.
- Historial: gráficos independientes con selección por toque o deslizador;
  Hoy, 7 y 30 días según disponibilidad. Datos sintéticos explícitos y optativos.
- Hábitos: una llama por fecha completada, racha, acción pendiente/completada,
  persistencia local por identidad y fecha. La recomendación y el hábito tienen
  registros independientes; varias acciones en una fecha cuentan como un día.
- Consejos: orientación general, cautela sobre condiciones exteriores y acceso
  al historial. Registrar una acción no confirma una mejora ambiental.

## Integración física pendiente

`EnvironmentRepository` entrega un StateFlow de estado y registros temporales.
`UnconfiguredEnvironmentRepository` parte desconectado y sin mediciones.
El adaptador real debe sustituirse en `DefaultAppContainer`, después de confirmar
el circuito y su protocolo. No se ha supuesto Bluetooth, Wi-Fi ni almacenamiento remoto.

Confirmar antes de integrar: modelo de sensores, fracción de PM, unidades de
origen, cadencia de envío, marcas temporales y criterios de clasificación con
su período de agregación. No se atribuyen partículas al tráfico ni se detectan
aerosoles específicos. El aviso de antigüedad de 15 minutos es un criterio de
interfaz provisional; se debe ajustar a la cadencia real del circuito.

Firebase requiere la configuración propia del proyecto. Los hábitos se guardan
localmente (no se sincronizan entre dispositivos); las identidades Firebase
usan UID y las sesiones demo se aíslan de las cuentas remotas.

## Verificación

Ejecutar `bash gradlew testDebugUnitTest assembleDebug lintDebug --offline`.
Las pruebas cubren validaciones y flujos de autenticación existentes, el estado
inicial sin lecturas, la separación de ejemplos y las rachas con días pendientes,
cambio de año y fechas futuras.

APK: `app/build/outputs/apk/debug/app-debug.apk`.
La revisión visual en dispositivo requiere un emulador o teléfono conectado.

Resultado de esta revisión: 29 pruebas unitarias aprobadas; APK de depuración y
APK de pruebas instrumentadas compilados. Lint finaliza sin errores, con
advertencias no bloqueantes (entre ellas recursos sin uso y sugerencias de
actualización). La prueba de interacción está preparada en
`EnvironmentalFlowTest`, pero no se ejecutó: no hay dispositivo conectado y el
emulador disponible informó un bloqueo de otra instancia. La revisión visual y
la validación con el circuito real permanecen pendientes.
