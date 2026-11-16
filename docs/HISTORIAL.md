# Historial: rediseño de la pantalla

La pestaña Historial tiene una composición propia en `feature/home/HistoryScreen.kt`.

- Filtros Hoy, Semana y Mes: días de calendario, hasta el momento actual, incluyendo
  períodos incompletos. Un período vacío muestra un estado explícito.
- Fuente: el interruptor conserva la selección compartida entre ejemplos locales y
  el repositorio MQTT existente; no crea conexiones ni publica datos.
- Monitor compacto con el nivel de la última lectura del período y la mascota interactiva.
- Gráfico con área, fechas reales, selección táctil y deslizador accesible. Las tarjetas
  de temperatura y humedad cambian la variable del gráfico. Los sensores ausentes no
  se sustituyen por cero.
- En PM2.5 y PM10 se muestra una sola fecha y hora, correspondiente al punto seleccionado.
  El gráfico de PM2.5 incluye la leyenda de niveles. Los valores exactos siguen en el CSV.
- Resumen de siete días calculado sobre promedios diarios con PM válido. Se muestran
  los días registrados y los días sin datos, sin inventar una semana completa.
- El PM2.5 usa los [rangos del prototipo](MQTT_PROTOTIPO.md). PM10 se puede consultar
  en una gráfica independiente.
- El botón de descarga abre el selector de archivos Android y escribe un CSV UTF-8
  con zona horaria, origen, PM2.5, PM10, temperatura, humedad y nivel de PM2.5. Exporta
  todas las lecturas válidas del período. El gráfico toma la última muestra de cada
  minuto y dibuja como máximo 180 puntos.
  La preparación y escritura se realizan fuera del hilo principal. El archivo temporal
  permite conservar el reporte durante cambios de configuración del selector.
- El prototipo guarda sus lecturas en una base SQLite separada del historial de Wokwi.
  Los registros de simulación no se trasladan al historial del prototipo.

## Pruebas

`HistoryDataTest` comprueba límites de calendario, orden temporal, promedios y CSV.
`EnvironmentHistoryStoreTest` comprueba la retención, las muestras por minuto y la
carga en SQLite después de reabrir la base de datos. `HistoryScreenTest` comprueba
el estado vacío y la llegada de registros, los filtros y el cambio de variable.

Capturas de la versión anterior en `docs/evidence/history/`.
