package com.cucho.qhawa.data.environment

import android.content.Context
import java.io.IOException
import java.util.UUID
import javax.net.SocketFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

/** A single subscription serves the four tabs while the environmental UI is visible. */
class MqttEnvironmentRepository internal constructor(
    private val history: ReadingHistory,
    scope: CoroutineScope,
    private val uri: String,
    private val socketFactory: () -> SocketFactory?,
) : EnvironmentRepository {
    constructor(context: Context) : this(
        EnvironmentHistoryStore(context.applicationContext),
        CoroutineScope(SupervisorJob() + Dispatchers.IO),
        QhawaSensorMqtt.URI,
        { context.applicationContext.prototypeSocketFactory() },
    )

    override suspend fun forEachReading(from: Long, to: Long, action: (EnvironmentalReading) -> Unit) {
        withContext(Dispatchers.IO) {
            history.forEach(from, to) {
                ensureActive()
                action(it)
            }
        }
    }

    override val state: StateFlow<EnvironmentState> = channelFlow {
        val cached = runCatching { history.load(System.currentTimeMillis()) }.getOrDefault(emptyList())
        val live = MutableStateFlow(EnvironmentState(DeviceConnection.LOADING, cached, detail = "Conectando a Mosquitto…"))
        val forwarding = launch { live.collect { send(it) } }
        val lock = Any()

        try {
            while (isActive) {
                val client = MqttClient(uri, "qhawa-app-${UUID.randomUUID()}", MemoryPersistence())
                var lastReceivedNanos = System.nanoTime()
                var accepting = false
                client.setCallback(object : MqttCallback {
                    override fun deliveryComplete(token: IMqttDeliveryToken?) = Unit

                    override fun connectionLost(cause: Throwable?) {
                        synchronized(lock) {
                            if (!accepting) return
                            accepting = false
                            live.update {
                                it.copy(connection = DeviceConnection.DISCONNECTED, brokerConnected = false,
                                    detail = "Conexión con Mosquitto interrumpida. Reintentando…")
                            }
                        }
                    }

                    override fun messageArrived(topic: String, message: MqttMessage) {
                        if (topic != QhawaSensorMqtt.TOPIC) return
                        val now = System.currentTimeMillis()
                        val reading = parseQhawaSensorPayload(message.payload, now, message.isRetained) ?: return
                        synchronized(lock) {
                            if (!accepting) return
                            lastReceivedNanos = System.nanoTime()
                            val saved = runCatching { history.save(reading) }.isSuccess
                            live.update { previous ->
                                val samples = if (saved) {
                                    val minute = now / 60_000
                                    val cutoff = now - EnvironmentHistoryStore.RETENTION_MS
                                    previous.readings.filter { it.timestamp >= cutoff && it.timestamp / 60_000 != minute } + reading
                                } else previous.readings
                                previous.copy(
                                    connection = DeviceConnection.CONNECTED,
                                    brokerConnected = true,
                                    readings = samples,
                                    lastReceived = reading,
                                    lastPm25 = if (reading.hasValidPm25) reading else previous.lastPm25,
                                    detail = if (saved) "Recibiendo lecturas del prototipo" else "Lectura recibida · No se pudo guardar el historial",
                                )
                            }
                        }
                    }
                })

                try {
                    val factory = socketFactory()
                    live.update { it.copy(connection = DeviceConnection.LOADING, detail = "Conectando a Mosquitto…") }
                    client.connect(MqttConnectOptions().apply {
                        if (factory != null) this.socketFactory = factory
                        isCleanSession = true
                        connectionTimeout = 5
                        keepAliveInterval = 15
                        mqttVersion = MqttConnectOptions.MQTT_VERSION_3_1_1
                    })
                    synchronized(lock) {
                        accepting = client.isConnected
                        lastReceivedNanos = System.nanoTime()
                        if (accepting) live.update {
                            it.copy(connection = DeviceConnection.WAITING, brokerConnected = true, detail = "Mosquitto conectado · Esperando datos del prototipo")
                        }
                    }
                    client.subscribe(QhawaSensorMqtt.TOPIC, 0)
                    while (isActive && client.isConnected) {
                        delay(1000)
                        synchronized(lock) {
                            if (accepting && (System.nanoTime() - lastReceivedNanos) / 1_000_000 >= QhawaSensorMqtt.STALE_MS) {
                                live.update {
                                    it.copy(connection = DeviceConnection.WAITING,
                                        detail = "Sin lecturas nuevas · Comprueba que el prototipo esté encendido")
                                }
                            }
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (exception: Exception) {
                    synchronized(lock) {
                        accepting = false
                        live.update {
                            it.copy(connection = DeviceConnection.DISCONNECTED, brokerConnected = false,
                                detail = (exception as? IOException)?.message
                                    ?: "No se pudo conectar a Mosquitto. Comprueba el broker de ${QhawaSensorMqtt.WIFI}.")
                        }
                    }
                } finally {
                    synchronized(lock) { accepting = false }
                    runCatching { client.disconnectForcibly(0, 0, false) }
                    runCatching { client.close(true) }
                }
                delay(5000)
            }
        } finally {
            forwarding.cancel()
        }
    }.flowOn(Dispatchers.IO).stateIn(
        scope, SharingStarted.WhileSubscribed(5000, replayExpirationMillis = 0),
        EnvironmentState(detail = "Conéctate al Wi‑Fi ${QhawaSensorMqtt.WIFI} para recibir lecturas."),
    )
}
