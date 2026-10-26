# Hábitos y Consejos

La composición `WellnessRoute` se utiliza únicamente en las pestañas Hábitos y Consejos.
Inicio, Historial, el repositorio MQTT, los datos de ejemplo y el componente compartido
de Qhawa conservan su implementación. El bloque «Explorar datos de ejemplo», el panel
técnico MQTT y el rótulo de gamificación se omiten en estas dos vistas. La fuente elegida
en Inicio o Historial sigue disponible para contextualizar los consejos.

## Persistencia por usuario

`WellnessRepository` usa Preferences DataStore 1.1.7, con una única instancia por proceso
y claves separadas por cuenta/demo. Antes de exponer el estado, importa de forma atómica
e idempotente los registros de `qhawa_habits` de ese usuario:

- Conserva todos los días completados y las acciones originales por fecha.
- Mantiene el hábito de revisión o limpieza asociado a los registros antiguos.
- No borra las SharedPreferences originales.
- Conserva las recomendaciones históricas y continúa con la siguiente del catálogo.
  El formato anterior no tenía hora de finalización, por lo que no inventa una espera
  para las acciones antiguas.

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

Completar una recomendación guarda, en una misma transacción, su registro, el índice
siguiente y `availableAt = instanteDeFinalización + 300000`. La tarjeta sale con animación
y aparece un estado de espera. La siguiente recomendación sólo se muestra cuando la hora
actual alcanza esa fecha. La cuenta atrás se recalcula al volver a primer plano y no se
reinicia al cambiar de pestaña, cerrar la app o crear de nuevo el repositorio.

Se usa la hora del dispositivo para que el plazo sobreviva al cierre del proceso. Cambiar
manualmente esa hora afecta su disponibilidad. No hay temporizadores de servidor ni
permisos nuevos. Los diálogos, toques y poses de celebración de Qhawa se mantienen.

## Comprobaciones

- 43 pruebas unitarias aprobadas; 3 pruebas optativas externas omitidas. Las nuevas
  pruebas cubren migración, aislamiento, asignaciones, calendario bisiesto, progreso,
  concurrencia y reinicio físico de DataStore durante la espera.
- 7 pruebas de interfaz aprobadas en Medium_Phone (Android 14), incluidas las regresiones
  de Inicio, Historial y Qhawa.
- Las 2 pruebas de Hábitos/Consejos aprobaron también a 360 dp y con fuente al 130 %.
- El límite de 5 minutos se comprueba con un reloj controlado: bloqueo a 299999 ms,
  disponibilidad a 300000 ms, sin reducir el plazo de producción.
- Se completaron un hábito y un consejo en una cuenta ficticia de la app, se cerró el
  proceso con `am force-stop` y se volvió a iniciar sesión. Se comprobaron el mismo
  hábito completado y la misma hora de disponibilidad, con la recomendación siguiente
  todavía bloqueada. La app quedó abierta en Hábitos.

Capturas: `docs/evidence/wellness/` y `docs/evidence/wellness-accessibility/`.

Referencia de la API: [DataStore de Android](https://developer.android.com/topic/libraries/architecture/datastore).
