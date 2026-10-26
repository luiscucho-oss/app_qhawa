# Historial: rediseño de la pantalla

La pestaña Historial tiene una composición propia en `feature/home/HistoryScreen.kt`.
Inicio, Hábitos y Consejos conservan su código y diseño. El contenedor principal sólo
elige esta nueva composición cuando la pestaña seleccionada es Historial.

- Filtros Hoy, Semana y Mes: días de calendario, hasta el momento actual, incluyendo
  períodos incompletos. Un período vacío muestra un estado explícito.
- Fuente: el interruptor conserva la selección compartida entre ejemplos locales y
  el repositorio MQTT existente; no crea conexiones ni publica datos.
- Monitor compacto con la última lectura del período y la mascota interactiva.
- Gráfico con área, fechas reales, selección táctil y deslizador accesible. Las tarjetas
  de temperatura y humedad cambian la variable del gráfico. Los sensores ausentes no
  se sustituyen por cero.
- Resumen de siete días calculado sobre promedios diarios con PM válido. Se muestran
  los días registrados y los días sin datos, sin inventar una semana completa.
- Wokwi conserva sus rangos de demostración (25/75) y el estado LED recibido. Estos
  datos se identifican como simulación, sin atribuirles condiciones del hogar.
- El botón de descarga abre el selector de archivos Android y escribe un CSV UTF-8
  con zona horaria, origen, variable PM, PM, temperatura, humedad y estado LED. Exporta
  todas las lecturas filtradas, aunque el gráfico dibuje como máximo 180 puntos.
  La preparación y escritura se realizan fuera del hilo principal. El archivo temporal
  permite conservar el reporte durante cambios de configuración del selector.

## Validación

`HistoryDataTest`: límites de calendario y orden temporal, registros futuros, días sin
datos, promedios diarios, rangos de Wokwi y formato CSV con valores ausentes y escape.

`HistoryScreenTest`: vacío MQTT, llegada de registros, sensores DHT22 ausentes, cambio
de variable, cambio de fuente, los tres períodos y disponibilidad del reporte.
Se mantienen las pruebas de navegación, mascota y acciones de las otras pestañas.

La suite local registra 37 pruebas unitarias aprobadas y 3 optativas omitidas.
Las 5 pruebas instrumentadas aprobaron en Medium_Phone (Android 14), y la prueba de
Historial también aprobó con un ancho de 360 dp. Se guardó un CSV desde el selector
nativo de Android y se verificaron sus 4 filas del período Hoy, unidades y origen
`Ejemplo local` en `docs/evidence/history/reporte-ejemplo.csv`.
La comprobación separada contra el broker público de Wokwi agotó el tiempo de conexión;
no se pudo confirmar recepción en vivo en esta sesión. El protocolo y repositorio MQTT
no se modificaron. Las pruebas de interfaz verifican el consumo de actualizaciones
del mismo `EnvironmentRepository` que utiliza la aplicación.

Capturas: `docs/evidence/history/`.
