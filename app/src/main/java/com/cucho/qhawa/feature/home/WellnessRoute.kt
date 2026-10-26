package com.cucho.qhawa.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.cucho.qhawa.core.designsystem.component.QhawaMascotComponent
import com.cucho.qhawa.core.designsystem.component.QhawaMascotState
import com.cucho.qhawa.data.environment.EnvironmentalReading
import com.cucho.qhawa.data.habits.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private sealed interface WellnessLoad {
    data object Loading : WellnessLoad
    data class Ready(val progress: WellnessSnapshot) : WellnessLoad
    data object Error : WellnessLoad
}

/** Ruta exclusiva de Hábitos/Consejos. El reloj vuelve a leerse al regresar a primer plano. */
@Composable
internal fun WellnessRoute(
    selectedTab: Int,
    userKey: String,
    reading: EnvironmentalReading?,
    demo: Boolean,
    alert: String?,
    onHistory: () -> Unit,
    clock: () -> Long = System::currentTimeMillis,
) {
    val context = LocalContext.current
    val repository = remember(userKey) { WellnessRepository(context, userKey) }
    var retry by remember { mutableIntStateOf(0) }
    val flow = remember(repository, retry) {
        repository.data.map<WellnessSnapshot, WellnessLoad> { WellnessLoad.Ready(it) }.catch { emit(WellnessLoad.Error) }
    }
    val load by flow.collectAsStateWithLifecycle(initialValue = WellnessLoad.Loading)
    var now by remember { mutableLongStateOf(clock()) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentClock by rememberUpdatedState(clock)
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) { now = currentClock(); delay(1000) }
        }
    }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var saving by remember(userKey) { mutableStateOf(false) }
    var celebration by remember(userKey) { mutableIntStateOf(0) }
    var celebratingHabit by remember(userKey) { mutableStateOf(false) }
    LaunchedEffect(celebration) {
        if (celebration > 0) { celebratingHabit = true; delay(2200); celebratingHabit = false }
    }

    val base = MaterialTheme.typography
    // Sólo estas dos pantallas reciben la escala de lectura ampliada.
    MaterialTheme(typography = base.copy(
        bodyMedium = base.bodyMedium.copy(fontSize = 16.sp, lineHeight = 24.sp),
        bodySmall = base.bodySmall.copy(fontSize = 14.sp, lineHeight = 20.sp),
        labelSmall = base.labelSmall.copy(fontSize = 12.sp, lineHeight = 18.sp),
    )) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            when (val state = load) {
                WellnessLoad.Loading -> Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text("Cargando tu progreso…", Modifier.padding(top = 12.dp))
                }
                WellnessLoad.Error -> Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("No se pudo leer tu progreso. Tus registros no se han reemplazado.")
                    Button(onClick = { retry++ }) { Text("Reintentar") }
                }
                is WellnessLoad.Ready -> {
                    val progress = state.progress
                    val waiting = !progress.adviceReady(now)
                    val habitDone = dateKey(now) in progress.completedDays
                    val welcome = when {
                        alert != null -> "Cambio en las lecturas: $alert. Revisa tus registros."
                        selectedTab == 2 && (habitDone || celebratingHabit) -> "¡Qhawa está feliz! Excelente trabajo."
                        selectedTab == 2 -> "¿Listos para completar el hábito de hoy?"
                        waiting -> "¡Genial! Cada acción cuenta."
                        else -> "Aquí tienes algunas ideas para hoy."
                    }
                    var tip by rememberSaveable(selectedTab, welcome) { mutableIntStateOf(0) }
                    val tips = if (selectedTab == 2) listOf("Cada día tiene un hábito distinto. Toca una fecha para consultarlo.", "Tus días completados seguirán aquí cuando vuelvas.")
                        else listOf("Después de actuar, consulta el historial para comparar registros.", "El siguiente consejo llegará cuando termine la espera; no necesitas dejar la app abierta.")
                    val mascot: @Composable () -> Unit = {
                        QhawaMascotComponent(
                            state = when {
                                alert != null -> QhawaMascotState.ALERT
                                selectedTab == 2 && (habitDone || celebratingHabit) || selectedTab == 3 && waiting -> QhawaMascotState.HAPPY
                                else -> QhawaMascotState.GREETING
                            },
                            message = if (tip == 0) welcome else tips[(tip - 1) % tips.size],
                            modifier = Modifier.fillMaxWidth(), mascotSize = 100.dp,
                            reactionKey = "$selectedTab:$celebration:${progress.adviceCompletions.size}",
                            onMascotClick = { tip = tip % tips.size + 1 },
                        )
                    }
                    Column(Modifier.widthIn(max = 620.dp).fillMaxWidth().verticalScroll(rememberScrollState(selectedTab))
                        .padding(horizontal = 16.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (selectedTab == 2) {
                            HabitsContent(progress, now, saving, mascot) { date ->
                                if (!saving) scope.launch {
                                    saving = true
                                    val result = runCatching { repository.completeHabit(date, currentClock()) }
                                    saving = false
                                    if (result.getOrDefault(false)) celebration++
                                    if (result.isFailure) snackbar.showSnackbar("No se pudo guardar el hábito. Inténtalo de nuevo.")
                                }
                            }
                        } else {
                            AdviceContent(progress, now, saving, reading, demo, mascot, onHistory) {
                                if (!saving) scope.launch {
                                    saving = true
                                    val result = runCatching { repository.completeAdvice(currentClock()) }
                                    saving = false
                                    if (result.isFailure) snackbar.showSnackbar("No se pudo guardar la recomendación. Inténtalo de nuevo.")
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
        }
    }
}
