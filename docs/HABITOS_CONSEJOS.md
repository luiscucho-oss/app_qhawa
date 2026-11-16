# Hábitos y Consejos

La composición `WellnessRoute` se utiliza únicamente en las pestañas Hábitos y Consejos.
La fuente elegida en Inicio o Historial también se aplica al contexto de Consejos.

## Persistencia por usuario

`WellnessRepository` usa Preferences DataStore 1.1.7, con una única instancia por proceso
y claves separadas por cuenta/demo. Antes de exponer el estado, importa de forma atómica
e idempotente los registros de `qhawa_habits` de ese usuario:

- Conserva todos los días completados y las acciones originales por fecha.
- Mantiene el hábito de revisión o limpieza asociado a los registros antiguos.
- No borra las SharedPreferences originales.
- Conserva las recomendaciones registradas antes de la integración del prototipo.

Las nuevas acciones se confirman visualmente después de guardarse. Una escritura iniciada
termina aunque se abandone su pantalla. Si falla el almacenamiento se muestra un error;
no se reemplazan registros por un estado vacío.

## Calendario

El mes, los días y la posición de cada fecha se calculan con el calendario del dispositivo,
incluidos años bisiestos. Hay 31 hábitos con identificadores y orden estables: cada fecha
del mes recibe un hábito diferente, calculado de forma determinista. Al completarlo se
guarda también la asignación. No se deben cambiar el tamaño ni el orden del catálogo v1
en futuras versiones; un catálogo nuevo debe tener su propia versión de asignación.

Se pueden consultar y completar fechas pasadas; las futuras se bloquean tanto en la UI
como en el repositorio. El progreso muestra hoy, la semana de lunes a domingo y el mes
completo; los registros futuros no se contabilizan. El calendario une los días completados
con bandas naranjas y adapta la altura de sus celdas al tamaño de fuente.

## Consejos

La lectura actual de PM2.5 selecciona cuatro recomendaciones según los
[rangos del prototipo](MQTT_PROTOTIPO.md). Las tarjetas muestran el título y la
acción correspondiente al rango. Cada tarjeta abre un panel con instrucciones
y un botón para registrar el consejo. Puedes registrar más de un consejo seguido.

Los ejemplos locales y la ausencia de una lectura actual de PM2.5 muestran
orientación general. Los registros guardados y los hábitos conservan sus datos
para cada cuenta.
