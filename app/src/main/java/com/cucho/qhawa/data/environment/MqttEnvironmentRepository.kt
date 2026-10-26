package com.cucho.qhawa.data.environment

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.UUID

/** A single shared subscription while the environmental UI is visible; no Android service required. */
class MqttEnvironmentRepository internal constructor(
    private val history: ReadingHistory,
    private val scope: CoroutineScope,
) : EnvironmentRepository {
    constructor(context: Context) : this(EnvironmentHistoryStore(context), CoroutineScope(SupervisorJob() + Dispatchers.IO))

    override val state: StateFlow<EnvironmentState> = channelFlow {
        val cached = runCatching { history.load(System.currentTimeMillis()) }.getOrDefault(emptyList())
        val live = MutableStateFlow(EnvironmentState(DeviceConnection.LOADING, cached,
            detail = "Conectando al broker MQTT…", source = "Wokwi · Simulación"))
        val forwarding = launch { live.collect { send(it) } }
        val gate = WokwiSequenceGate()
        val lock = Any()
        var lastReceivedElapsed: Long? = null
        val client = MqttClient(WokwiMqtt.URI, "qhawa-app-${UUID.randomUUID()}", MemoryPersistence())
        client.setCallback(object : MqttCallback {
            override fun deliveryComplete(token: IMqttDeliveryToken?) = Unit
            override fun connectionLost(cause: Throwable?) {
                live.update { it.copy(connection = DeviceConnection.DISCONNECTED, brokerConnected = false,
                    detail = "Conexión MQTT interrumpida. Reintentando…") }
            }
            override fun messageArrived(topic: String, message: MqttMessage) {
                if (topic != WokwiMqtt.TOPIC) return
                val now = System.currentTimeMillis()
                val reading = parseWokwiPayload(message.payload.toString(Charsets.UTF_8), now, message.isRetained) ?: return
                synchronized(lock) {
                    if (!gate.accept(reading)) return
                    lastReceivedElapsed = (System.nanoTime() / 1_000_000)
                    val saved = runCatching { history.save(reading) }.isSuccess
                    live.update { previous ->
                        val samples = previous.readings.filter { it.timestamp >= now - EnvironmentHistoryStore.RETENTION_MS && it.timestamp / 60_000 != now / 60_000 } + reading
                        previous.copy(connection = DeviceConnection.CONNECTED, brokerConnected = true, readings = samples,
                            detail = if (saved) "Recibiendo Wokwi cada 5 s · secuencia ${reading.sequence}" else "Lectura recibida · No se pudo guardar el historial")
                    }
                }
            }
        })
        try {
            while (isActive) {
                try {
                    live.update { it.copy(connection = DeviceConnection.LOADING, brokerConnected = false, detail = "Conectando al broker MQTT…") }
                    client.connect(MqttConnectOptions().apply {
                        isCleanSession = true
                        isAutomaticReconnect = false // The coroutine also retries initial connection failures.
                        connectionTimeout = 10
                        keepAliveInterval = 15
                        mqttVersion = MqttConnectOptions.MQTT_VERSION_3_1_1
                    })
                    synchronized(lock) { lastReceivedElapsed = null }
                    live.update { it.copy(connection = DeviceConnection.WAITING, brokerConnected = true,
                        detail = "MQTT conectado · Esperando que Wokwi publique") }
                    client.subscribe(WokwiMqtt.TOPIC, 0)
                    while (isActive && client.isConnected) {
                        delay(1000)
                        synchronized(lock) {
                            val last = lastReceivedElapsed
                            if (client.isConnected && last != null && (System.nanoTime() / 1_000_000) - last > WokwiMqtt.STALE_MS) {
                                live.update { it.copy(connection = DeviceConnection.WAITING,
                                    detail = "Sin lecturas nuevas hace más de 20 s · Verifica que Wokwi esté ejecutándose") }
                            }
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    live.update { it.copy(connection = DeviceConnection.DISCONNECTED, brokerConnected = false,
                        detail = "No se pudo conectar a MQTT. Revisa internet; reintento en 5 s") }
                    runCatching { client.disconnectForcibly(0, 0, false) }
                }
                delay(5000)
            }
        } finally {
            forwarding.cancel()
            runCatching { client.disconnectForcibly(0, 0, false) }
            runCatching { client.close(true) }
        }
    }.flowOn(Dispatchers.IO).stateIn(scope, SharingStarted.WhileSubscribed(5000, replayExpirationMillis = 0),
        EnvironmentState(detail = "Abre Inicio para conectar con Wokwi", source = "Wokwi · Simulación"))
}
