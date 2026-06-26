package app.constellationpulse

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import app.constellationpulse.backend.FirebaseFieldService
import app.constellationpulse.backend.RemoteChorusState
import app.constellationpulse.backend.RemoteFieldOrb
import app.constellationpulse.data.ChorusMemory
import app.constellationpulse.data.ChorusMemoryRepository
import app.constellationpulse.data.ChorusRelic
import app.constellationpulse.data.ChorusRelicRepository
import app.constellationpulse.data.PulseRepository
import app.constellationpulse.data.PulseSeal
import app.constellationpulse.reminder.PulseReminderScheduler
import app.constellationpulse.ui.theme.ConstellationPulseTheme
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PulseReminderScheduler.scheduleDailyReminder(applicationContext)

        setContent {
            val repository = remember { PulseRepository(applicationContext) }
            val relicRepository = remember { ChorusRelicRepository(applicationContext) }
            var currentScreen by rememberSaveable { mutableStateOf(AppScreen.Home) }
            var todaySeal by remember { mutableStateOf<PulseSeal?>(null) }
            var history by remember { mutableStateOf(emptyList<PulseSeal>()) }
            var chorusRelics by remember { mutableStateOf(emptyList<ChorusRelic>()) }
            var visiblePulse by remember { mutableStateOf<PulseSeal?>(null) }
            var pendingPulse by remember { mutableStateOf<PulseSeal?>(null) }

            fun reloadArchive() {
                todaySeal = repository.loadToday()
                history = repository.loadAll()
                chorusRelics = relicRepository.loadAll()
            }

            LaunchedEffect(repository) {
                reloadArchive()
            }

            NotificationPermissionEffect()

            ConstellationPulseTheme {
                ConstellationPulseApp(
                    currentScreen = currentScreen,
                    todaySeal = todaySeal,
                    history = history,
                    chorusRelics = chorusRelics,
                    visiblePulse = visiblePulse,
                    onNavigateHome = {
                        visiblePulse = null
                        currentScreen = AppScreen.Home
                        pendingPulse = null
                    },
                    onSealClick = {
                        if (todaySeal == null) {
                            currentScreen = AppScreen.Seal
                        } else {
                            visiblePulse = todaySeal
                            currentScreen = AppScreen.Reveal
                        }
                    },
                    onRevealToday = {
                        visiblePulse = todaySeal
                        currentScreen = AppScreen.Reveal
                    },
                    onHistory = {
                        currentScreen = AppScreen.History
                    },
                    onChorus = {
                        currentScreen = AppScreen.Chorus
                    },
                    onChorusRelic = { relic ->
                        relicRepository.save(relic)
                        reloadArchive()
                    },
                    onNearby = {
                        currentScreen = AppScreen.Nearby
                    },
                    onOpenPulse = { pulse ->
                        visiblePulse = pulse
                        currentScreen = AppScreen.Reveal
                    },
                    onSeal = { pulse ->
                        pendingPulse = pulse
                        visiblePulse = pulse
                        currentScreen = AppScreen.Ceremony
                    },
                    pendingPulse = pendingPulse,
                    onCeremonyComplete = {
                        val pulse = pendingPulse
                        if (pulse != null) {
                            repository.sealToday(pulse)
                            reloadArchive()
                            visiblePulse = repository.loadToday() ?: pulse
                        }
                        pendingPulse = null
                        currentScreen = AppScreen.Resonance
                    },
                    onResonanceComplete = {
                        currentScreen = AppScreen.Reveal
                    }
                )
            }
        }
    }
}

private enum class AppScreen {
    Home,
    Chorus,
    Seal,
    Ceremony,
    Resonance,
    Reveal,
    Nearby,
    History
}

private enum class OrbRitualState {
    Dormant,
    Listening,
    Contemplative,
    NearChorus,
    Sealed,
    Resonating
}

private enum class ChorusStage {
    PreChorus,
    Entry,
    Convergence,
    Minute,
    Afterglow,
    Sealed
}

private data class SignalSpec(
    val label: String,
    val value: Float,
    val onValueChange: (Float) -> Unit,
    val low: String,
    val high: String
)

private data class SigilNode(
    val angle: Float,
    val orbit: Float,
    val radius: Float,
    val alpha: Float,
    val drift: Float
)

private data class SigilStroke(
    val angle: Float,
    val orbit: Float,
    val length: Float,
    val alpha: Float
)

private data class PulseSigil(
    val seed: Int,
    val nodes: List<SigilNode>,
    val strokes: List<SigilStroke>,
    val ringCount: Int,
    val connectionStep: Int
)

private data class OrbRitualProfile(
    val phaseSeconds: Float,
    val deepPhaseSeconds: Float,
    val shimmerSeconds: Float,
    val breathSeconds: Float,
    val breathAmplitude: Float,
    val heartbeatMultiplier: Float,
    val density: Float,
    val glowBoost: Float,
    val toneBlend: Float,
    val pulseBoost: Float,
    val constellationAlpha: Float,
    val ringAlpha: Float
)

private data class OrbSensorState(
    val tiltX: Float = 0f,
    val tiltY: Float = 0f,
    val twist: Float = 0f,
    val motion: Float = 0f
)

private data class ResonanceEcho(
    val angle: Float,
    val orbit: Float,
    val strength: Float,
    val phaseOffset: Float
)

private data class NearbyPresence(
    val id: Int,
    val remoteOrbId: String?,
    val remoteCellId: String?,
    val pulse: PulseSeal,
    val angle: Float,
    val orbit: Float,
    val scale: Float
)

private val panelShape = RoundedCornerShape(8.dp)

@Composable
private fun NotificationPermissionEffect() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return
    }

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
private fun ConstellationPulseApp(
    currentScreen: AppScreen,
    todaySeal: PulseSeal?,
    history: List<PulseSeal>,
    chorusRelics: List<ChorusRelic>,
    visiblePulse: PulseSeal?,
    onNavigateHome: () -> Unit,
    onSealClick: () -> Unit,
    onRevealToday: () -> Unit,
    onHistory: () -> Unit,
    onChorus: () -> Unit,
    onChorusRelic: (ChorusRelic) -> Unit,
    onNearby: () -> Unit,
    onOpenPulse: (PulseSeal) -> Unit,
    onSeal: (PulseSeal) -> Unit,
    pendingPulse: PulseSeal?,
    onCeremonyComplete: () -> Unit,
    onResonanceComplete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (currentScreen) {
            AppScreen.Home -> HomeScreen(
                todaySeal = todaySeal,
                sealCount = history.size,
                onSealClick = onSealClick,
                onRevealClick = onRevealToday,
                onHistoryClick = onHistory,
                onChorusClick = onChorus,
                onNearbyClick = onNearby
            )

            AppScreen.Chorus -> ChorusScreen(
                todaySeal = todaySeal,
                onBack = onNavigateHome,
                onRelicSealed = onChorusRelic
            )

            AppScreen.Seal -> SealScreen(
                todaySeal = todaySeal,
                onBack = onNavigateHome,
                onViewToday = onRevealToday,
                onSeal = onSeal
            )

            AppScreen.Ceremony -> SealCeremonyScreen(
                pulse = pendingPulse ?: visiblePulse ?: todaySeal,
                onComplete = onCeremonyComplete
            )

            AppScreen.Resonance -> ResonanceScreen(
                pulse = visiblePulse ?: todaySeal,
                onComplete = onResonanceComplete
            )

            AppScreen.Reveal -> RevealScreen(
                pulse = visiblePulse ?: todaySeal,
                onBack = onNavigateHome,
                onHistoryClick = onHistory
            )

            AppScreen.Nearby -> NearbyFieldScreen(
                todaySeal = todaySeal,
                onBack = onNavigateHome
            )

            AppScreen.History -> HistoryScreen(
                history = history,
                chorusRelics = chorusRelics,
                onBack = onNavigateHome,
                onOpenPulse = onOpenPulse
            )
        }
    }
}

@Composable
private fun HomeScreen(
    todaySeal: PulseSeal?,
    sealCount: Int,
    onSealClick: () -> Unit,
    onRevealClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onChorusClick: () -> Unit,
    onNearbyClick: () -> Unit
) {
    val context = LocalContext.current
    val chorusMemory = remember(todaySeal?.dateKey) {
        ChorusMemoryRepository(context.applicationContext).load(todaySeal?.dateKey ?: PulseSeal.todayKey())
    }
    var countdown by remember { mutableStateOf(formatCountdown(millisUntilChorus())) }

    LaunchedEffect(Unit) {
        while (true) {
            countdown = formatCountdown(millisUntilChorus())
            delay(1_000)
        }
    }

    PulseScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatusPill(text = if (todaySeal == null) "Unsealed" else "Sealed")
                Text(
                    text = "Constellation Pulse",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (millisUntilChorus() < 15 * 60_000L) {
                        "The Chorus opens soon."
                    } else {
                        "One minute. No feed. Just presence."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            PulseOrb(
                pulse = todaySeal,
                modifier = Modifier.size(318.dp),
                showConstellation = true,
                echoTraceCount = chorusMemory.sentEchoes + chorusMemory.receivedEchoes,
                ritualState = homeOrbRitualState(todaySeal, chorusMemory),
                onLongPressGesture = onChorusClick
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PulseMetric(
                        label = "CHORUS",
                        value = countdown,
                        modifier = Modifier.weight(1f)
                    )
                    PulseMetric(
                        label = "ARCHIVE",
                        value = sealCount.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }

                PrimaryButton(
                    label = if (todaySeal == null) "Enter Today's Ritual" else "Reveal Today's Orb",
                    onClick = if (todaySeal == null) onSealClick else onRevealClick,
                    modifier = Modifier.fillMaxWidth()
                )

                QuietButton(label = "Open Constellation", onClick = onHistoryClick)
                QuietButton(label = "Join the Chorus", onClick = onChorusClick)
                QuietButton(label = "Nearby Field", onClick = onNearbyClick)

                AnimatedVisibility(visible = todaySeal != null) {
                    Text(
                        text = "The chorus has your signal.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ChorusScreen(
    todaySeal: PulseSeal?,
    onBack: () -> Unit,
    onRelicSealed: (ChorusRelic) -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val firebaseFieldService = remember {
        FirebaseFieldService(context.applicationContext)
    }
    val day = remember { PulseSeal.todayKey() }
    val localCellId = remember {
        nearbyCellId(context, hasCoarseLocationPermission(context))
    }
    val clientSeed = remember(day, todaySeal?.createdAtMillis) {
        pulseVisualSeed(todaySeal) xor day.hashCode() xor System.currentTimeMillis().toInt()
    }
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var hasEntered by rememberSaveable { mutableStateOf(false) }
    var isHoldingField by remember { mutableStateOf(false) }
    var chorusLiveState by remember { mutableStateOf(RemoteChorusState()) }
    var savedRelicDay by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(500)
        }
    }

    LaunchedEffect(isHoldingField) {
        if (isHoldingField) {
            delay(1_100)
            hasEntered = true
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    DisposableEffect(firebaseFieldService, day, localCellId) {
        val registration = firebaseFieldService.listenChorusState(
            day = day,
            localCellId = localCellId,
            onUpdate = { liveState ->
                chorusLiveState = liveState
            },
            onError = {
                chorusLiveState = RemoteChorusState()
            }
        )

        onDispose {
            registration?.remove()
        }
    }

    DisposableEffect(firebaseFieldService, day) {
        val registration = firebaseFieldService.listenSharedChorusRelic(
            day = day,
            onUpdate = { remoteRelic ->
                remoteRelic?.toChorusRelic()?.let { sharedRelic ->
                    onRelicSealed(sharedRelic)
                    savedRelicDay = day
                }
            },
            onError = {}
        )

        onDispose {
            registration?.remove()
        }
    }

    val timeStage = chorusTimeStage(nowMillis)
    val stage = when {
        timeStage == ChorusStage.Minute && hasEntered -> ChorusStage.Minute
        timeStage == ChorusStage.Minute -> ChorusStage.Entry
        timeStage == ChorusStage.Afterglow -> ChorusStage.Afterglow
        timeStage == ChorusStage.Sealed -> ChorusStage.Sealed
        hasEntered -> ChorusStage.Convergence
        else -> ChorusStage.PreChorus
    }

    LaunchedEffect(hasEntered, stage, isHoldingField, localCellId, clientSeed) {
        if (!hasEntered || stage == ChorusStage.Sealed || !firebaseFieldService.isAvailable()) {
            return@LaunchedEffect
        }

        while (true) {
            val isCentralMinute = stage == ChorusStage.Minute
            firebaseFieldService.publishChorusPresence(
                day = day,
                coarseCellId = localCellId,
                touchStability = when {
                    isHoldingField -> 1f
                    isCentralMinute -> 0.64f
                    else -> 0.42f
                },
                stillness = when {
                    isHoldingField -> 0.92f
                    isCentralMinute -> 0.74f
                    else -> 0.58f
                },
                turbulence = when {
                    isHoldingField -> 0.07f
                    isCentralMinute -> 0.16f
                    else -> 0.28f
                },
                clientSeed = clientSeed
            )
            delay(4_000)
        }
    }

    val millisSinceOpen = millisSinceTodayChorus(nowMillis)
    val minuteRemaining = (60 - (millisSinceOpen / 1_000).coerceIn(0, 60)).toInt()
    val countdown = formatCountdown(millisUntilChorus())
    val title = when (stage) {
        ChorusStage.PreChorus -> "The Chorus opens soon."
        ChorusStage.Entry -> "Touch the field."
        ChorusStage.Convergence -> "The field is no longer empty."
        ChorusStage.Minute -> "For one minute, you are not alone."
        ChorusStage.Afterglow -> "You were part of today's Chorus."
        ChorusStage.Sealed -> "Today's Chorus is sealed."
    }
    val support = when (stage) {
        ChorusStage.PreChorus -> countdown
        ChorusStage.Entry -> "Hold the sphere to cross the threshold."
        ChorusStage.Convergence -> if (chorusLiveState.globalPresenceCount > 1) {
            "Others are entering the field."
        } else {
            "Your signal is waiting for the field."
        }
        ChorusStage.Minute -> "${minuteRemaining}s"
        ChorusStage.Afterglow -> if (chorusLiveState.globalPresenceCount > 0) {
            "The minute has left a trace."
        } else {
            "The field is becoming a trace."
        }
        ChorusStage.Sealed -> "Return tomorrow."
    }

    LaunchedEffect(stage) {
        if (stage == ChorusStage.Minute || stage == ChorusStage.Afterglow) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    LaunchedEffect(stage, hasEntered, chorusLiveState, day, clientSeed) {
        val canSealRelic = hasEntered && (stage == ChorusStage.Afterglow || stage == ChorusStage.Sealed)
        if (!canSealRelic || savedRelicDay == day) {
            return@LaunchedEffect
        }

        val fallbackPresence = if (hasEntered) 1 else 0
        val relic = ChorusRelic(
            day = day,
            createdAtMillis = System.currentTimeMillis(),
            afterglowSeed = chorusLiveState.afterglowSeed.takeIf { it != 0 }
                ?: (clientSeed xor day.hashCode()),
            globalPresenceCount = chorusLiveState.globalPresenceCount.coerceAtLeast(fallbackPresence),
            localFieldDensity = chorusLiveState.localFieldDensity,
            synchronizationLevel = chorusLiveState.synchronizationLevel,
            coherence = chorusLiveState.coherence,
            turbulence = chorusLiveState.turbulence
        )
        onRelicSealed(relic)
        firebaseFieldService.publishSharedChorusRelic(relic)
        savedRelicDay = day
    }

    PulseScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            if (stage == ChorusStage.Minute) {
                Spacer(modifier = Modifier.height(42.dp))
            } else {
                TopBar(title = "Chorus", onBack = onBack)
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                StatusPill(
                    text = when (stage) {
                        ChorusStage.PreChorus -> "Pre-Chorus"
                        ChorusStage.Entry -> "Entry"
                        ChorusStage.Convergence -> "Convergence"
                        ChorusStage.Minute -> "The Minute"
                        ChorusStage.Afterglow -> "Afterglow"
                        ChorusStage.Sealed -> "Sealed"
                    }
                )
                ChorusEclipseField(
                    pulse = todaySeal,
                    stage = stage,
                    entered = hasEntered,
                    holding = isHoldingField,
                    liveState = chorusLiveState,
                    onHoldingChange = { isHoldingField = it },
                    onEnter = {
                        hasEntered = true
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    modifier = Modifier.size(360.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = support,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    letterSpacing = if (stage == ChorusStage.Minute) 2.sp else 0.sp
                )
            }

            if (stage == ChorusStage.Minute) {
                Spacer(modifier = Modifier.height(42.dp))
            } else {
                QuietButton(label = "Return", onClick = onBack)
            }
        }
    }
}

@Composable
private fun ChorusEclipseField(
    pulse: PulseSeal?,
    stage: ChorusStage,
    entered: Boolean,
    holding: Boolean,
    liveState: RemoteChorusState,
    onHoldingChange: (Boolean) -> Unit,
    onEnter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val time = rememberOrbTimeSeconds(active = true)
    val seed = remember(pulse?.dateKey, pulse?.createdAtMillis, liveState.afterglowSeed) {
        pulseVisualSeed(pulse) xor PulseSeal.todayKey().hashCode() xor liveState.afterglowSeed
    }
    val livePresence = (liveState.globalPresenceCount / 32f).coerceIn(0f, 1f)
    val liveCoherence = liveState.coherence.coerceIn(0f, 1f)
    val liveTurbulence = liveState.turbulence.coerceIn(0f, 1f)
    val liveNearness = liveState.localFieldDensity.coerceIn(0f, 1f)
    val activePresences = liveState.activePresences
    val baseStagePull = when (stage) {
        ChorusStage.PreChorus -> 0.10f
        ChorusStage.Entry -> if (holding) 0.38f else 0.20f
        ChorusStage.Convergence -> 0.62f
        ChorusStage.Minute -> 0.82f
        ChorusStage.Afterglow -> 0.42f
        ChorusStage.Sealed -> 0.22f
    }
    val baseCoherence = when (stage) {
        ChorusStage.PreChorus -> 0.20f
        ChorusStage.Entry -> if (entered) 0.42f else 0.26f
        ChorusStage.Convergence -> 0.64f
        ChorusStage.Minute -> if (holding) 0.92f else 0.76f
        ChorusStage.Afterglow -> 0.70f
        ChorusStage.Sealed -> 0.34f
    }
    val stagePull by animateFloatAsState(
        targetValue = (baseStagePull + liveNearness * 0.12f + livePresence * 0.08f).coerceIn(0f, 0.92f),
        animationSpec = tween(1_400, easing = FastOutSlowInEasing),
        label = "chorus-stage-pull"
    )
    val coherence by animateFloatAsState(
        targetValue = (baseCoherence * 0.62f + liveCoherence * 0.38f).coerceIn(0.10f, 0.96f),
        animationSpec = tween(1_800, easing = FastOutSlowInEasing),
        label = "chorus-coherence"
    )
    val fallbackPresenceCount = when (stage) {
        ChorusStage.PreChorus -> 7
        ChorusStage.Entry -> 10
        ChorusStage.Convergence -> 18
        ChorusStage.Minute -> 24
        ChorusStage.Afterglow -> 16
        ChorusStage.Sealed -> 9
    }
    val presenceCount = if (liveState.globalPresenceCount > 0) {
        (liveState.globalPresenceCount + fallbackPresenceCount / 2).coerceIn(8, 42)
    } else {
        fallbackPresenceCount
    }
    val orbState = when (stage) {
        ChorusStage.PreChorus -> OrbRitualState.NearChorus
        ChorusStage.Entry -> OrbRitualState.Listening
        ChorusStage.Convergence -> OrbRitualState.Resonating
        ChorusStage.Minute -> OrbRitualState.Resonating
        ChorusStage.Afterglow -> OrbRitualState.Contemplative
        ChorusStage.Sealed -> OrbRitualState.Sealed
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val min = size.minDimension
            val centerPoint = center
            val stroke = min * 0.004f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primary.copy(alpha = 0.10f + coherence * 0.10f),
                        secondary.copy(alpha = 0.05f + stagePull * 0.07f),
                        Color.Transparent
                    ),
                    center = centerPoint,
                    radius = min * (0.36f + stagePull * 0.36f)
                ),
                radius = min * (0.36f + stagePull * 0.36f),
                center = centerPoint
            )

            repeat(presenceCount) { index ->
                val livePresenceSeed = activePresences.getOrNull(index % activePresences.size.coerceAtLeast(1))
                val random = Random((livePresenceSeed?.clientSeed ?: seed) + index * 9137)
                val direction = if (index % 2 == 0) 1f else -1f
                val baseAngle = random.nextFloat() * 360f
                val angle = (baseAngle + time * (4.5f + coherence * 8f) * direction) * PI.toFloat() / 180f
                val outerOrbit = 0.46f + random.nextFloat() * 0.10f
                val innerOrbit = 0.18f + random.nextFloat() * 0.12f
                val orbit = outerOrbit + (innerOrbit - outerOrbit) * stagePull
                val presenceStillness = livePresenceSeed?.stillness ?: coherence
                val presenceTurbulence = livePresenceSeed?.turbulence ?: liveTurbulence
                val wobble = sin(time * 1.3f + index * 0.72f) *
                    min *
                    0.010f *
                    (1f - coherence + presenceTurbulence * 0.8f) *
                    (1.12f - presenceStillness * 0.34f)
                val point = Offset(
                    x = centerPoint.x + cos(angle) * min * orbit + cos(angle + PI.toFloat() / 2f) * wobble,
                    y = centerPoint.y + sin(angle) * min * orbit + sin(angle + PI.toFloat() / 2f) * wobble
                )
                val alpha = 0.08f + coherence * 0.18f + livePresence * 0.06f + (index % 5) * 0.008f

                drawLine(
                    color = if (index % 3 == 0) {
                        primary.copy(alpha = alpha * 0.42f)
                    } else {
                        secondary.copy(alpha = alpha * 0.34f)
                    },
                    start = point,
                    end = centerPoint,
                    strokeWidth = stroke * (0.28f + coherence * 0.30f),
                    cap = StrokeCap.Round
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = alpha),
                            primary.copy(alpha = alpha * 0.56f),
                            Color.Transparent
                        ),
                        center = point,
                        radius = min * (0.018f + coherence * 0.012f)
                    ),
                    radius = min * (0.018f + coherence * 0.012f),
                    center = point
                )
            }

            if (stage == ChorusStage.Afterglow || stage == ChorusStage.Sealed) {
                repeat(5) { index ->
                    val radius = min * (0.13f + index * 0.044f)
                    drawCircle(
                        color = Color.White.copy(alpha = (0.10f - index * 0.012f).coerceAtLeast(0.025f)),
                        radius = radius,
                        center = centerPoint,
                        style = Stroke(width = stroke * 0.46f, cap = StrokeCap.Round)
                    )
                }
            }
        }

        PulseOrb(
            pulse = pulse,
            modifier = Modifier.size(252.dp),
            showConstellation = true,
            echoTraceCount = if (entered) 6 else 0,
            ritualState = orbState,
            onPressChanged = onHoldingChange,
            onLongPressGesture = onEnter
        )
    }
}

@Composable
private fun SealScreen(
    todaySeal: PulseSeal?,
    onBack: () -> Unit,
    onViewToday: () -> Unit,
    onSeal: (PulseSeal) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    var message by rememberSaveable { mutableStateOf("") }
    var light by rememberSaveable { mutableStateOf(50f) }
    var motion by rememberSaveable { mutableStateOf(50f) }
    var charge by rememberSaveable { mutableStateOf(50f) }
    var clarity by rememberSaveable { mutableStateOf(50f) }
    var connection by rememberSaveable { mutableStateOf(50f) }

    val previewPulse = PulseSeal.today(
        message = message,
        valence = light.roundToInt(),
        arousal = motion.roundToInt(),
        energy = charge.roundToInt(),
        focus = clarity.roundToInt(),
        social = connection.roundToInt()
    )

    PulseScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            TopBar(title = "Ritual", onBack = onBack)

            if (todaySeal != null) {
                LockedTodayPanel(onViewToday = onViewToday)
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    PulseOrb(
                        pulse = previewPulse,
                        modifier = Modifier.size(154.dp),
                        showConstellation = false
                    )
                }

                OutlinedTextField(
                    value = message,
                    onValueChange = { next ->
                        if (next.length <= 140) {
                            message = next
                        }
                    },
                    label = { Text("Leave one line, or leave silence") },
                    supportingText = { Text("${message.length}/140") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = panelShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.62f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.58f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                    )
                )

                val specs = listOf(
                    SignalSpec("Light", light, { light = it }, "dim", "bright"),
                    SignalSpec("Motion", motion, { motion = it }, "still", "alive"),
                    SignalSpec("Charge", charge, { charge = it }, "low", "full"),
                    SignalSpec("Clarity", clarity, { clarity = it }, "mist", "clear"),
                    SignalSpec("Connection", connection, { connection = it }, "alone", "near")
                )

                specs.forEach { spec ->
                    PulseSlider(spec)
                }

                PrimaryButton(
                    label = "Join the Chorus",
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSeal(previewPulse.copy(message = message.trim()))
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SealCeremonyScreen(
    pulse: PulseSeal?,
    onComplete: () -> Unit
) {
    var line by remember { mutableStateOf("Breathe in slowly.") }

    LaunchedEffect(pulse?.dateKey, pulse?.createdAtMillis) {
        line = "Breathe in slowly."
        delay(1_900)
        line = "Let the field find you."
        delay(2_100)
        line = "Now join the chorus."
        delay(2_000)
        onComplete()
    }

    PulseScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            StatusPill(text = "Sealing")

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(26.dp)
            ) {
                PulseOrb(
                    pulse = pulse,
                    modifier = Modifier.size(338.dp),
                    showConstellation = true,
                    ritualState = OrbRitualState.Listening
                )
                Text(
                    text = line,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
            }

            Text(
                text = "No feed. No score. Presence only.",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ResonanceScreen(
    pulse: PulseSeal?,
    onComplete: () -> Unit
) {
    val echoes = remember(pulse?.dateKey, pulse?.createdAtMillis, pulse?.message) {
        buildResonanceEchoes(pulse)
    }
    var activeEchoes by remember { mutableStateOf(0) }
    var line by remember { mutableStateOf("The field is listening.") }

    LaunchedEffect(pulse?.dateKey, pulse?.createdAtMillis) {
        activeEchoes = 0
        line = "The field is listening."
        delay(1_600)
        echoes.forEachIndexed { index, _ ->
            activeEchoes = index + 1
            line = when {
                activeEchoes == 1 -> "A distant signal answers."
                activeEchoes < 4 -> "Another presence enters the field."
                activeEchoes < 7 -> "The chorus begins to breathe."
                else -> "Your orb is held in resonance."
            }
            delay(1_350)
        }
        line = "The field remembers you."
        delay(2_200)
        onComplete()
    }

    PulseScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            StatusPill(text = "Resonance")

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                ResonanceField(
                    pulse = pulse,
                    echoes = echoes.take(activeEchoes),
                    modifier = Modifier.size(354.dp)
                )
                Text(
                    text = line,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
            }

            Text(
                text = if (activeEchoes == 0) {
                    "waiting for distant presences"
                } else {
                    "$activeEchoes anonymous presences felt"
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ResonanceField(
    pulse: PulseSeal?,
    echoes: List<ResonanceEcho>,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
            animationSpec = infiniteRepeatable(
            animation = tween(14_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val core = pulseCoreColor(pulse)
    val halo = pulseHaloColor(pulse)
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val min = size.minDimension
            val stroke = min * 0.004f

            echoes.forEachIndexed { index, echo ->
                val angle = echo.angle * PI.toFloat() / 180f
                val point = Offset(
                    x = center.x + cos(angle) * min * echo.orbit,
                    y = center.y + sin(angle) * min * echo.orbit
                )
                val wave = (phase + echo.phaseOffset) % 1f
                val waveRadius = min * (0.025f + wave * 0.22f * echo.strength)
                val alpha = (1f - wave) * (0.18f + echo.strength * 0.14f)

                drawCircle(
                    color = halo.copy(alpha = alpha),
                    radius = waveRadius,
                    center = point,
                    style = Stroke(width = stroke * (0.8f + echo.strength))
                )
                drawCircle(
                    color = if (index % 2 == 0) primary.copy(alpha = 0.72f) else secondary.copy(alpha = 0.62f),
                    radius = min * (0.007f + echo.strength * 0.006f),
                    center = point
                )
                drawLine(
                    color = core.copy(alpha = 0.07f + echo.strength * 0.07f),
                    start = point,
                    end = center,
                    strokeWidth = stroke * 0.5f
                )
            }

            drawCircle(
                color = halo.copy(alpha = 0.08f + echoes.size * 0.008f),
                radius = min * (0.34f + echoes.size.coerceAtMost(10) * 0.006f),
                center = center,
                style = Stroke(width = stroke * 2f)
            )
        }

        PulseOrb(
            pulse = pulse,
            modifier = Modifier.size(246.dp),
            showConstellation = true,
            ritualState = OrbRitualState.Resonating
        )
    }
}

@Composable
private fun RevealScreen(
    pulse: PulseSeal?,
    onBack: () -> Unit,
    onHistoryClick: () -> Unit
) {
    val context = LocalContext.current
    val chorusMemory = remember(pulse?.dateKey) {
        ChorusMemoryRepository(context.applicationContext).load(pulse?.dateKey ?: PulseSeal.todayKey())
    }

    PulseScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            TopBar(title = "Daily Orb", onBack = onBack)

            if (pulse == null) {
                EmptyState(
                    title = "The chorus is waiting.",
                    actionLabel = "Open Constellation",
                    onAction = onHistoryClick
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    PulseOrb(
                        pulse = pulse,
                        modifier = Modifier.size(306.dp),
                        showConstellation = true,
                        echoTraceCount = chorusMemory.sentEchoes + chorusMemory.receivedEchoes,
                        ritualState = revealOrbRitualState(pulse, chorusMemory)
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = formatDateKey(pulse.dateKey),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = fieldSignature(pulse),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = pulse.message.ifBlank { "A quiet seal." },
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )
                    }

                    PulseStats(pulse)
                }
            }

            QuietButton(label = "Open Constellation", onClick = onHistoryClick)
        }
    }
}

@Composable
private fun NearbyFieldScreen(
    todaySeal: PulseSeal?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val firebaseFieldService = remember {
        FirebaseFieldService(context.applicationContext)
    }
    val chorusMemoryRepository = remember {
        ChorusMemoryRepository(context.applicationContext)
    }
    val todayKey = remember { PulseSeal.todayKey() }
    var hasLocationPermission by remember {
        mutableStateOf(hasCoarseLocationPermission(context))
    }
    var remoteOrbsByCell by remember { mutableStateOf(emptyMap<String, List<RemoteFieldOrb>>()) }
    var receivedEchoesByCell by remember { mutableStateOf(emptyMap<String, Int>()) }
    var chorusMemory by remember {
        mutableStateOf(chorusMemoryRepository.load(todayKey))
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission = granted
    }
    val cellId = remember(hasLocationPermission) {
        nearbyCellId(context, hasLocationPermission)
    }
    val listeningCellIds = remember(cellId) {
        nearbyCellIds(cellId)
    }
    val fieldSeed = remember(hasLocationPermission) {
        nearbyFieldSeed(context, hasLocationPermission)
    }
    val remoteOrbs = remember(remoteOrbsByCell) {
        mergeRemoteOrbs(remoteOrbsByCell.values.flatten())
    }
    val presences = remember(fieldSeed, remoteOrbs) {
        buildNearbyPresences(fieldSeed, remoteOrbs)
    }
    var selectedPresence by remember { mutableStateOf<NearbyPresence?>(null) }
    var echoLine by remember { mutableStateOf<String?>(null) }
    var fieldState by remember {
        mutableStateOf(if (firebaseFieldService.isAvailable()) "Connecting field" else "Local ritual field")
    }
    var seenRemoteOrbIds by remember { mutableStateOf(emptySet<String>()) }
    var fieldReaction by remember { mutableStateOf(false) }

    DisposableEffect(hasLocationPermission, cellId, todaySeal?.dateKey, todaySeal?.createdAtMillis) {
        if (!hasLocationPermission || !firebaseFieldService.isAvailable()) {
            fieldState = if (hasLocationPermission) "Local ritual field" else "Private field"
            onDispose { }
        } else {
            val day = todayKey
            var disposed = false
            val registrations = mutableListOf<ListenerRegistration>()
            val echoRegistrations = mutableListOf<ListenerRegistration>()

            if (todaySeal != null) {
                fieldState = "Opening live field"
                firebaseFieldService.publishOrb(cellId, todaySeal) { success ->
                    fieldState = if (success) "Live field open" else "Auth is still waking"
                }
            } else {
                fieldState = "Live field listening"
            }
            listeningCellIds.forEach { listenedCellId ->
                firebaseFieldService.listenNearbyField(
                    day = day,
                    cellId = listenedCellId,
                    onUpdate = { orbs ->
                        val nextByCell = remoteOrbsByCell + (listenedCellId to orbs)
                        val mergedOrbs = mergeRemoteOrbs(nextByCell.values.flatten())
                        val nextIds = mergedOrbs.map { it.orbId }.toSet()
                        val hasNewSignal = seenRemoteOrbIds.isNotEmpty() && nextIds.any { it !in seenRemoteOrbIds }
                        remoteOrbsByCell = nextByCell
                        seenRemoteOrbIds = nextIds
                        fieldState = if (mergedOrbs.isNotEmpty()) "Live field open" else "Listening across nearby cells"
                        if (mergedOrbs.isNotEmpty()) {
                            chorusMemory = chorusMemoryRepository.recordPresences(
                                day = day,
                                count = mergedOrbs.size,
                                mood = chorusFieldMood(mergedOrbs)
                            )
                        }
                        if (hasNewSignal) {
                            fieldReaction = true
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    },
                    onError = {
                        fieldState = "Live field paused"
                    }
                )?.let { registrations += it }
            }
            firebaseFieldService.resolveDailyOrbId(day) { orbId ->
                if (disposed || orbId == null) {
                    return@resolveDailyOrbId
                }
                listeningCellIds.forEach { listenedCellId ->
                    firebaseFieldService.listenReceivedEchoes(
                        day = day,
                        cellId = listenedCellId,
                        targetOrbId = orbId,
                        onUpdate = { echoes ->
                            val nextByCell = receivedEchoesByCell + (listenedCellId to echoes.size)
                            val totalEchoes = nextByCell.values.sum()
                            if (totalEchoes > chorusMemoryRepository.load(day).receivedEchoes) {
                                fieldReaction = true
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            receivedEchoesByCell = nextByCell
                            chorusMemory = chorusMemoryRepository.recordReceivedEchoes(day, totalEchoes)
                        },
                        onError = {
                            fieldState = "Echoes are quiet"
                        }
                    )?.let { echoRegistrations += it }
                }
            }
            onDispose {
                disposed = true
                registrations.forEach { it.remove() }
                echoRegistrations.forEach { it.remove() }
            }
        }
    }

    LaunchedEffect(selectedPresence?.id) {
        echoLine = null
    }

    LaunchedEffect(echoLine) {
        if (echoLine == "Echo released.") {
            delay(1_600)
            echoLine = "Resonance returned."
        }
    }

    LaunchedEffect(fieldReaction) {
        if (fieldReaction) {
            delay(1_200)
            fieldReaction = false
        }
    }

    PulseScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            TopBar(title = "Nearby Field", onBack = onBack)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusPill(text = fieldState)
                Text(
                    text = if (hasLocationPermission) {
                        if (remoteOrbs.isNotEmpty()) {
                            "${remoteOrbs.size} live ${if (remoteOrbs.size == 1) "presence" else "presences"} in the chorus"
                        } else {
                            "${presences.size} local echoes holding space"
                        }
                    } else {
                        "Let the app feel the nearby field"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
            }

            if (!hasLocationPermission) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    PulseOrb(
                        pulse = null,
                        modifier = Modifier.size(230.dp),
                        showConstellation = true,
                        ritualState = OrbRitualState.Listening
                    )
                    Text(
                        text = "No map. No names. Only anonymous resonance.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    PrimaryButton(
                        label = "Open Nearby Field",
                        onClick = {
                            permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                NearbyPresenceField(
                    selfPulse = todaySeal,
                    presences = presences,
                    selectedPresence = selectedPresence,
                    fieldReaction = fieldReaction,
                    echoTraceCount = chorusMemory.sentEchoes + chorusMemory.receivedEchoes,
                    onSelectPresence = { presence ->
                        selectedPresence = presence
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp)
                )
            }

            if (hasLocationPermission) {
                NearbyPresencePanel(
                    presence = selectedPresence,
                    echoLine = echoLine,
                    memory = chorusMemory,
                    onSendEcho = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        fieldReaction = true
                        if (firebaseFieldService.isAvailable()) {
                            selectedPresence?.remoteOrbId?.let { targetOrbId ->
                                firebaseFieldService.sendEcho(
                                    day = todayKey,
                                    cellId = selectedPresence?.remoteCellId ?: cellId,
                                    targetOrbId = targetOrbId
                                ) { success ->
                                    echoLine = if (success) "Echo released." else "Echo kept locally."
                                    if (success) {
                                        chorusMemory = chorusMemoryRepository.recordSentEcho(todayKey)
                                    }
                                }
                            }
                        }
                        if (selectedPresence?.remoteOrbId == null) {
                            echoLine = "Echo kept locally."
                            chorusMemory = chorusMemoryRepository.recordSentEcho(todayKey)
                        }
                    }
                )
            } else {
                Text(
                    text = "Approximate only. Nothing precise is shown.",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun NearbyPresenceField(
    selfPulse: PulseSeal?,
    presences: List<NearbyPresence>,
    selectedPresence: NearbyPresence?,
    fieldReaction: Boolean,
    echoTraceCount: Int,
    onSelectPresence: (NearbyPresence) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val phase = rememberOrbTimeSeconds(active = true) / 16f
    var attractionPoint by remember { mutableStateOf<Offset?>(null) }
    var isAttracting by remember { mutableStateOf(false) }
    val reactionGlow by animateFloatAsState(
        targetValue = if (fieldReaction) 1f else 0f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "field-reaction"
    )
    val attractionStrength by animateFloatAsState(
        targetValue = if (isAttracting) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (isAttracting) 720 else 980,
            easing = FastOutSlowInEasing
        ),
        label = "field-attraction"
    )
    val selfScale by animateFloatAsState(
        targetValue = 1f + reactionGlow * 0.12f + attractionStrength * 0.06f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "self-orb-reaction"
    )
    LaunchedEffect(isAttracting) {
        if (!isAttracting) {
            delay(1_050)
            attractionPoint = null
        }
    }

    BoxWithConstraints(
        modifier = modifier.pointerInput(presences.size) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                attractionPoint = down.position
                isAttracting = true
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)

                do {
                    val event = awaitPointerEvent()
                    attractionPoint = event.changes.firstOrNull { it.pressed }?.position ?: attractionPoint
                } while (event.changes.any { it.pressed })

                isAttracting = false
            }
        },
        contentAlignment = Alignment.Center
    ) {
        val attractionNorm = remember(attractionPoint, maxWidth, maxHeight, density) {
            attractionPoint?.let { point ->
                val x = with(density) { point.x.toDp() / maxWidth }
                val y = with(density) { point.y.toDp() / maxHeight }
                Offset(x.coerceIn(0.08f, 0.92f), y.coerceIn(0.08f, 0.92f))
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val min = size.minDimension
            val centerPoint = center
            val attractPoint = attractionPoint

            fun presencePoint(presence: NearbyPresence, index: Int): Offset {
                val angle = (presence.angle + phase * 360f * 0.18f) * PI.toFloat() / 180f
                val base = Offset(
                    x = centerPoint.x + cos(angle) * min * presence.orbit,
                    y = centerPoint.y + sin(angle) * min * presence.orbit
                )
                if (attractPoint == null || attractionStrength <= 0.01f) {
                    return base
                }
                val orbitOffset = (presence.angle + phase * 128f + index * 41f) * PI.toFloat() / 180f
                val pull = attractionStrength * (0.50f + presence.scale * 0.10f)
                val gathered = Offset(
                    x = base.x + (attractPoint.x - base.x) * pull,
                    y = base.y + (attractPoint.y - base.y) * pull
                )
                val halo = min * (0.030f + (index % 4) * 0.006f) * attractionStrength
                return Offset(
                    x = gathered.x + cos(orbitOffset) * halo,
                    y = gathered.y + sin(orbitOffset) * halo
                )
            }

            if (reactionGlow > 0.01f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primary.copy(alpha = 0.18f * reactionGlow),
                            secondary.copy(alpha = 0.08f * reactionGlow),
                            Color.Transparent
                        ),
                        center = centerPoint,
                        radius = min * (0.22f + reactionGlow * 0.32f)
                    ),
                    radius = min * (0.22f + reactionGlow * 0.32f),
                    center = centerPoint
                )
            }

            if (attractPoint != null && attractionStrength > 0.01f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primary.copy(alpha = 0.18f * attractionStrength),
                            secondary.copy(alpha = 0.10f * attractionStrength),
                            Color.Transparent
                        ),
                        center = attractPoint,
                        radius = min * (0.20f + attractionStrength * 0.16f)
                    ),
                    radius = min * (0.20f + attractionStrength * 0.16f),
                    center = attractPoint
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.18f * attractionStrength),
                    radius = min * (0.030f + attractionStrength * 0.022f),
                    center = attractPoint,
                    style = Stroke(width = min * 0.0028f)
                )
            }

            drawCircle(
                color = secondary.copy(alpha = 0.055f),
                radius = min * 0.46f,
                center = centerPoint,
                style = Stroke(width = min * 0.004f)
            )
            drawCircle(
                color = primary.copy(alpha = 0.045f),
                radius = min * 0.31f,
                center = centerPoint,
                style = Stroke(width = min * 0.003f)
            )

            selectedPresence?.let { presence ->
                val selectedIndex = presences.indexOfFirst { it.id == presence.id }.coerceAtLeast(0)
                val selectedPoint = presencePoint(presence, selectedIndex)
                drawLine(
                    color = pulseHaloColor(presence.pulse).copy(alpha = 0.34f + reactionGlow * 0.16f + attractionStrength * 0.10f),
                    start = centerPoint,
                    end = selectedPoint,
                    strokeWidth = min * (0.0032f + reactionGlow * 0.0016f + attractionStrength * 0.0012f),
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = pulseHaloColor(presence.pulse).copy(alpha = 0.11f + reactionGlow * 0.08f),
                    radius = min * (0.086f + reactionGlow * 0.026f + attractionStrength * 0.018f),
                    center = selectedPoint,
                    style = Stroke(width = min * 0.003f)
                )
            }

            presences.forEachIndexed { index, presence ->
                val point = presencePoint(presence, index)
                drawLine(
                    color = pulseHaloColor(presence.pulse).copy(alpha = 0.055f + attractionStrength * 0.052f),
                    start = centerPoint,
                    end = point,
                    strokeWidth = min * (0.0015f + attractionStrength * 0.0010f)
                )
                if (attractPoint != null && attractionStrength > 0.01f) {
                    drawLine(
                        color = pulseHaloColor(presence.pulse).copy(alpha = 0.040f * attractionStrength),
                        start = point,
                        end = attractPoint,
                        strokeWidth = min * 0.0012f,
                        cap = StrokeCap.Round
                    )
                }
                if (presence.remoteOrbId != null) {
                    drawCircle(
                        color = pulseHaloColor(presence.pulse).copy(alpha = 0.06f + reactionGlow * 0.05f + attractionStrength * 0.05f),
                        radius = min * (0.026f + presence.scale * 0.014f + reactionGlow * 0.008f + attractionStrength * 0.007f),
                        center = point,
                        style = Stroke(width = min * 0.002f)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .size((76f * selfScale).dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.52f))
                .border(
                    BorderStroke(
                        width = 1.2.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f + reactionGlow * 0.2f)
                    ),
                    RoundedCornerShape(999.dp)
                )
                .padding(5.dp),
            contentAlignment = Alignment.Center
        ) {
            PulseOrb(
                pulse = selfPulse,
                modifier = Modifier.fillMaxSize(),
                showConstellation = false,
                echoTraceCount = echoTraceCount,
                ritualState = if (fieldReaction) OrbRitualState.Resonating else OrbRitualState.Listening
            )
        }

        presences.forEach { presence ->
            val index = presences.indexOf(presence)
            val angle = (presence.angle + phase * 360f * 0.18f) * PI.toFloat() / 180f
            val baseXNorm = 0.5f + cos(angle) * presence.orbit
            val baseYNorm = 0.5f + sin(angle) * presence.orbit
            val gatherAngle = (presence.angle + phase * 128f + index * 41f) * PI.toFloat() / 180f
            val pull = attractionStrength * (0.50f + presence.scale * 0.10f)
            val targetXNorm = attractionNorm?.x ?: baseXNorm
            val targetYNorm = attractionNorm?.y ?: baseYNorm
            val xNorm = (
                baseXNorm + (targetXNorm - baseXNorm) * pull +
                    cos(gatherAngle) * 0.030f * attractionStrength
                ).coerceIn(0.06f, 0.94f)
            val yNorm = (
                baseYNorm + (targetYNorm - baseYNorm) * pull +
                    sin(gatherAngle) * 0.030f * attractionStrength
                ).coerceIn(0.06f, 0.94f)
            val orbSize = (42f + presence.scale * 30f).dp
            val isSelected = selectedPresence?.id == presence.id

            Box(
                modifier = Modifier
                    .offset(
                        x = maxWidth * xNorm - orbSize * 0.5f,
                        y = maxHeight * yNorm - orbSize * 0.5f
                    )
                    .size(orbSize)
                    .clip(RoundedCornerShape(999.dp))
                    .border(
                        BorderStroke(
                            width = if (isSelected) 1.5.dp else 0.6.dp,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.38f)
                            }
                        ),
                        RoundedCornerShape(999.dp)
                    )
                    .clickable { onSelectPresence(presence) },
                contentAlignment = Alignment.Center
            ) {
                PulseOrb(
                    pulse = presence.pulse,
                    modifier = Modifier.fillMaxSize(),
                    showConstellation = false
                )
            }
        }
    }
}

@Composable
private fun NearbyPresencePanel(
    presence: NearbyPresence?,
    echoLine: String?,
    memory: ChorusMemory,
    onSendEcho: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(panelShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.48f)),
                panelShape
            )
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (presence == null) {
            Text(
                text = "Choose the light that calls back.",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = "No names. No distance. Only a nearby state of feeling.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        } else {
            Text(
                text = nearbyMood(presence.pulse),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = nearbySignature(presence.pulse),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                letterSpacing = 1.sp
            )
            PrimaryButton(
                label = echoLine ?: "Release Echo",
                onClick = onSendEcho,
                modifier = Modifier.fillMaxWidth()
            )
        }

        ChorusMemoryStrip(memory)
    }
}

@Composable
private fun ChorusMemoryStrip(memory: ChorusMemory) {
    val line = when {
        memory.peakPresences == 0 && memory.sentEchoes == 0 && memory.receivedEchoes == 0 ->
            "The field has not left a trace yet."
        memory.receivedEchoes > 0 ->
            "${memory.receivedEchoes} ${if (memory.receivedEchoes == 1) "echo has" else "echoes have"} returned to you."
        memory.sentEchoes > 0 ->
            "${memory.sentEchoes} ${if (memory.sentEchoes == 1) "echo" else "echoes"} released into the chorus."
        else ->
            "${memory.peakPresences} ${if (memory.peakPresences == 1) "presence" else "presences"} crossed today's field."
    }
    val mood = memory.lastFieldMood.ifBlank { "The chorus is still forming." }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(panelShape)
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.28f))
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)),
                panelShape
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = line,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            text = mood,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun HistoryScreen(
    history: List<PulseSeal>,
    chorusRelics: List<ChorusRelic>,
    onBack: () -> Unit,
    onOpenPulse: (PulseSeal) -> Unit
) {
    PulseScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TopBar(title = "Constellation", onBack = onBack)

            if (history.isEmpty() && chorusRelics.isEmpty()) {
                EmptyState(
                    title = "No orbs yet.",
                    actionLabel = "Back",
                    onAction = onBack
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (chorusRelics.isNotEmpty()) {
                        val latestRelic = chorusRelics.first()
                        val olderRelics = chorusRelics.drop(1)
                        item(key = "latest-chorus-relic-label") {
                            Text(
                                text = "Latest Chorus",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                        }
                        item(key = "latest-relic-${latestRelic.day}") {
                            ChorusRelicHero(relic = latestRelic)
                        }
                        if (olderRelics.isNotEmpty()) {
                            item(key = "older-chorus-relics-label") {
                                Text(
                                    text = "Earlier relics",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 1.sp
                                )
                            }
                            items(olderRelics, key = { relic -> "relic-${relic.day}" }) { relic ->
                                ChorusRelicRow(relic = relic)
                            }
                        }
                    }
                    if (history.isNotEmpty()) {
                        item(key = "daily-orbs-label") {
                            Text(
                                text = "Daily orbs",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                    items(history, key = { it.dateKey }) { pulse ->
                        HistoryRow(
                            pulse = pulse,
                            onClick = { onOpenPulse(pulse) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PulseScaffold(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        Color(0xFF0E1210),
                        Color(0xFF030405)
                    )
                )
            )
    ) {
        AmbientField(modifier = Modifier.fillMaxSize())
        content()
    }
}

@Composable
private fun AmbientField(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val gold = Color(0xFFEEDB9A)
        val green = Color(0xFF9CE0C0)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(gold.copy(alpha = 0.08f), Color.Transparent),
                center = Offset(size.width * 0.18f, size.height * 0.12f),
                radius = size.minDimension * 0.58f
            ),
            radius = size.minDimension * 0.58f,
            center = Offset(size.width * 0.18f, size.height * 0.12f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(green.copy(alpha = 0.07f), Color.Transparent),
                center = Offset(size.width * 0.82f, size.height * 0.78f),
                radius = size.minDimension * 0.62f
            ),
            radius = size.minDimension * 0.62f,
            center = Offset(size.width * 0.82f, size.height * 0.78f)
        )

        repeat(34) { index ->
            val x = ((index * 47) % 100) / 100f * size.width
            val y = ((index * 83) % 100) / 100f * size.height
            drawCircle(
                color = Color.White.copy(alpha = 0.035f + (index % 3) * 0.018f),
                radius = 0.8f + (index % 4) * 0.32f,
                center = Offset(x, y)
            )
        }
    }
}

@Composable
private fun TopBar(
    title: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        QuietButton(label = "Back", onClick = onBack)
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun LockedTodayPanel(onViewToday: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(panelShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f))
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.72f)),
                panelShape
            )
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Today is sealed.",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Button(
            onClick = onViewToday,
            shape = panelShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("Reveal Today's Orb")
        }
    }
}

@Composable
private fun EmptyState(
    title: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        PulseOrb(
            pulse = null,
            modifier = Modifier.size(206.dp),
            showConstellation = true,
            ritualState = OrbRitualState.Dormant
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        PrimaryButton(label = actionLabel, onClick = onAction)
    }
}

@Composable
private fun StatusPill(text: String) {
    Row(
        modifier = Modifier
            .clip(panelShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.54f))
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.58f)),
                panelShape
            )
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.secondary)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = panelShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun QuietButton(
    label: String,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        shape = panelShape,
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun PulseMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(panelShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.68f))
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.46f)),
                panelShape
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun PulseSlider(spec: SignalSpec) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(panelShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.52f)),
                panelShape
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(spec.label, color = MaterialTheme.colorScheme.onSurface)
            Text(
                spec.value.roundToInt().toString(),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            value = spec.value,
            onValueChange = spec.onValueChange,
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(spec.low, style = MaterialTheme.typography.labelSmall)
            Text(spec.high, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun PulseStats(pulse: PulseSeal) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(panelShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.52f)),
                panelShape
            )
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        PulseStatRow("Pulse index", pulse.pulseIndex)
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.22f))
        SignalBar("Light", pulse.valence)
        SignalBar("Motion", pulse.arousal)
        SignalBar("Charge", pulse.energy)
        SignalBar("Clarity", pulse.focus)
        SignalBar("Connection", pulse.social)
    }
}

@Composable
private fun PulseStatRow(label: String, value: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurface)
        Text(value.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SignalBar(label: String, value: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        PulseStatRow(label, value)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.34f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(value.coerceIn(0, 100) / 100f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.secondary,
                                MaterialTheme.colorScheme.primary
                            )
                        )
                    )
            )
        }
    }
}

@Composable
private fun HistoryRow(
    pulse: PulseSeal,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(panelShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.48f)),
                panelShape
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        PulseOrb(
            pulse = pulse,
            modifier = Modifier.size(58.dp),
            showConstellation = false
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = formatDateKey(pulse.dateKey),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = pulse.message.ifBlank { "A quiet seal." },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Text(
            text = pulse.pulseIndex.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ChorusRelicHero(relic: ChorusRelic) {
    val relicPulse = remember(relic.day, relic.afterglowSeed) {
        relic.toPulseSeal()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(panelShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.62f))
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)),
                panelShape
            )
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        PulseOrb(
            pulse = relicPulse,
            modifier = Modifier.size(132.dp),
            showConstellation = true,
            echoTraceCount = relic.globalPresenceCount.coerceIn(1, 12),
            ritualState = OrbRitualState.Contemplative,
            interactive = false
        )
        Text(
            text = "You were part of today's Chorus.",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            text = formatDateKey(relic.day),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ChorusRelicRow(relic: ChorusRelic) {
    val relicPulse = remember(relic.day, relic.afterglowSeed) {
        relic.toPulseSeal()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(panelShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.66f))
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.34f)),
                panelShape
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        PulseOrb(
            pulse = relicPulse,
            modifier = Modifier.size(58.dp),
            showConstellation = false,
            echoTraceCount = relic.globalPresenceCount.coerceIn(1, 12),
            ritualState = OrbRitualState.Contemplative
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = formatDateKey(relic.day),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "You were part of today's Chorus.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Text(
            text = "RELIC",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun PulseOrb(
    pulse: PulseSeal?,
    modifier: Modifier = Modifier,
    showConstellation: Boolean,
    echoTraceCount: Int = 0,
    ritualState: OrbRitualState? = null,
    onPressChanged: (Boolean) -> Unit = {},
    onLongPressGesture: (() -> Unit)? = null,
    interactive: Boolean = showConstellation
) {
    val haptics = LocalHapticFeedback.current
    val sigil = remember(pulse?.dateKey, pulse?.createdAtMillis, pulse?.message) {
        buildPulseSigil(pulse, dense = showConstellation)
    }
    var tapToken by remember { mutableStateOf(0) }
    var tapPoint by remember { mutableStateOf<Offset?>(null) }
    var constellationTargetPoint by remember { mutableStateOf<Offset?>(null) }
    var constellationAttractionPoint by remember { mutableStateOf<Offset?>(null) }
    var isConstellationAttracting by remember { mutableStateOf(false) }
    var isPressedOpen by remember { mutableStateOf(false) }
    var isContemplating by remember(pulse?.dateKey, pulse?.createdAtMillis, showConstellation) {
        mutableStateOf(false)
    }
    LaunchedEffect(tapToken) {
        if (tapToken > 0) {
            delay(820)
            tapPoint = null
        }
    }
    LaunchedEffect(isConstellationAttracting) {
        if (!isConstellationAttracting) {
            delay(1_100)
            constellationTargetPoint = null
            constellationAttractionPoint = null
        }
    }
    LaunchedEffect(showConstellation) {
        while (showConstellation) {
            withFrameNanos { }
            val target = constellationTargetPoint
            val current = constellationAttractionPoint
            if (target != null && current != null) {
                val dx = target.x - current.x
                val dy = target.y - current.y
                val distance = sqrt(dx * dx + dy * dy)
                val speed = (0.026f + ln(1f + distance * 0.012f) * 0.032f).coerceIn(0.026f, 0.115f)
                constellationAttractionPoint = Offset(
                    x = current.x + dx * speed,
                    y = current.y + dy * speed
                )
            }
        }
    }
    LaunchedEffect(isPressedOpen) {
        if (isPressedOpen) {
            delay(1_650)
            isPressedOpen = false
        }
    }
    LaunchedEffect(pulse?.dateKey, pulse?.createdAtMillis, showConstellation) {
        isContemplating = false
        if (showConstellation) {
            delay(4_800)
            isContemplating = true
        }
    }
    val orbTimeSeconds = rememberOrbTimeSeconds(active = true)
    val contemplation by animateFloatAsState(
        targetValue = if (isContemplating) 1f else 0f,
        animationSpec = tween(2_400, easing = FastOutSlowInEasing),
        label = "orb-contemplation"
    )
    val tapRipple by animateFloatAsState(
        targetValue = if (tapPoint != null) 1f else 0f,
        animationSpec = tween(760, easing = FastOutSlowInEasing),
        label = "orb-tap-ripple"
    )
    val gestureOpen by animateFloatAsState(
        targetValue = if (isPressedOpen) 1f else 0f,
        animationSpec = tween(1_200, easing = FastOutSlowInEasing),
        label = "orb-gesture-open"
    )
    val constellationAttraction by animateFloatAsState(
        targetValue = if (isConstellationAttracting && showConstellation) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (isConstellationAttracting) 760 else 1_050,
            easing = FastOutSlowInEasing
        ),
        label = "orb-constellation-attraction"
    )
    val chorusIntensity = if (showConstellation) chorusApproachIntensity() else 0f
    val effectiveRitualState = remember(
        ritualState,
        pulse?.dateKey,
        pulse?.createdAtMillis,
        isContemplating,
        showConstellation,
        chorusIntensity
    ) {
        resolveOrbRitualState(
            requested = ritualState,
            pulse = pulse,
            isContemplating = isContemplating && showConstellation,
            chorusIntensity = chorusIntensity
        )
    }
    val ritualProfile = orbRitualProfile(effectiveRitualState)
    LaunchedEffect(effectiveRitualState, showConstellation, interactive) {
        if (showConstellation && interactive && effectiveRitualState == OrbRitualState.Contemplative) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    val scale = 1f + sin((orbTimeSeconds / ritualProfile.breathSeconds) * 2f * PI.toFloat()) * ritualProfile.breathAmplitude
    val phase = orbTimeSeconds / ritualProfile.phaseSeconds
    val deepPhase = orbTimeSeconds / ritualProfile.deepPhaseSeconds
    val shimmerPhase = orbTimeSeconds / ritualProfile.shimmerSeconds
    val tonePhase = orbTimeSeconds / 96f
    val heartbeatDuration = remember(pulse?.arousal, pulse?.energy) {
        (8_400 - (pulse?.arousal ?: 50) * 16 - (pulse?.energy ?: 50) * 8)
            .coerceIn(5_200, 8_600)
    } * ritualProfile.heartbeatMultiplier
    val heartbeatPhase = fractional(orbTimeSeconds / (heartbeatDuration / 1000f))
    val sensorState = rememberOrbSensorState(enabled = showConstellation)
    val ritualTone = blendColor(
        ritualToneColor(pulse, tonePhase),
        ritualStateTone(effectiveRitualState, pulse, tonePhase),
        ritualProfile.toneBlend
    )
    val core = blendColor(
        pulseCoreColor(pulse),
        ritualTone,
        if (showConstellation) 0.08f + chorusIntensity * 0.16f + ritualProfile.toneBlend * 0.12f else 0f
    )
    val halo = blendColor(
        pulseHaloColor(pulse),
        ritualTone,
        if (showConstellation) 0.12f + chorusIntensity * 0.20f + ritualProfile.toneBlend * 0.18f else 0f
    )
    val sparkle = if (pulse?.isBright == true) Color(0xFFFFD88A) else Color(0xFFB8F3D6)
    val quiet = Color(0xFFF5F2E9)

    val interactionModifier = if (interactive) {
        val attractionModifier = if (showConstellation) {
            Modifier.pointerInput(pulse?.dateKey, pulse?.createdAtMillis) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    constellationTargetPoint = down.position
                    constellationAttractionPoint = constellationAttractionPoint ?: down.position
                    isConstellationAttracting = true

                    do {
                        val event = awaitPointerEvent()
                        constellationTargetPoint = event.changes.firstOrNull { it.pressed }?.position
                            ?: constellationTargetPoint
                    } while (event.changes.any { it.pressed })

                    isConstellationAttracting = false
                }
            }
        } else {
            Modifier
        }
        val tapModifier = Modifier.pointerInput(pulse?.dateKey, pulse?.createdAtMillis) {
            detectTapGestures(
                onPress = {
                    onPressChanged(true)
                    try {
                        tryAwaitRelease()
                    } finally {
                        onPressChanged(false)
                    }
                },
                onTap = { offset ->
                    tapPoint = offset
                    tapToken += 1
                },
                onLongPress = { offset ->
                    tapPoint = offset
                    tapToken += 1
                    isPressedOpen = true
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongPressGesture?.invoke()
                }
            )
        }
        attractionModifier.then(tapModifier)
    } else {
        Modifier
    }

    Canvas(modifier = modifier.then(interactionModifier)) {
        val min = size.minDimension
        val strokeBase = min * 0.004f
        val energy = (pulse?.energy ?: 52) / 100f
        val arousal = (pulse?.arousal ?: 50) / 100f
        val focus = (pulse?.focus ?: 48) / 100f
        val social = (pulse?.social ?: 44) / 100f
        val shimmer = 0.5f + 0.5f * sin(shimmerPhase * 2f * PI.toFloat())
        val motionPhase = phase * (1f - contemplation * 0.58f) + deepPhase * contemplation * 0.58f
        val contemplativeBreath = 0.5f + 0.5f * sin((deepPhase * 2f * PI.toFloat()) + sigil.seed * 0.013f)
        val sensorWeight = if (showConstellation) 1f else 0f
        val tiltX = sensorState.tiltX * sensorWeight
        val tiltY = sensorState.tiltY * sensorWeight
        val twist = sensorState.twist * sensorWeight
        val motion = sensorState.motion * sensorWeight
        val parallax = Offset(
            x = tiltX * min * (0.052f + contemplation * 0.018f),
            y = tiltY * min * (0.052f + contemplation * 0.018f)
        )
        val motionGlow = (motion * 0.16f).coerceIn(0f, 0.16f)
        val pulseBeat = (
            pulseBeatEnvelope(heartbeatPhase, energy, arousal) *
                (0.76f + chorusIntensity * 0.42f + contemplation * 0.22f + ritualProfile.pulseBoost) +
                motion * 0.12f
            ).coerceIn(0f, 1f)
        val pulseTone = blendColor(
            ritualTone,
            if (pulse?.isBright == true) Color(0xFFFFE3A6) else Color(0xFFBDFBE2),
            0.38f + energy * 0.28f
        )
        val pulseCore = blendColor(core, pulseTone, 0.12f + pulseBeat * 0.34f)
        val pulseHalo = blendColor(halo, pulseTone, 0.16f + pulseBeat * 0.44f)
        val traceCount = echoTraceCount.coerceIn(0, 12)
        val constellationPull = if (showConstellation) constellationAttraction else 0f
        val constellationFocus = constellationAttractionPoint

        if (pulseBeat > 0.01f) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        pulseHalo.copy(alpha = (0.19f + ritualProfile.glowBoost * 0.06f) * pulseBeat),
                        pulseCore.copy(alpha = (0.08f + ritualProfile.glowBoost * 0.04f) * pulseBeat),
                        Color.Transparent
                    ),
                    center = center + parallax * 0.42f,
                    radius = min * (0.42f + pulseBeat * 0.34f)
                ),
                radius = min * (0.42f + pulseBeat * 0.34f),
                center = center
            )
        }

        if (tapRipple > 0.01f) {
            val origin = tapPoint ?: center
            val ripple = 1f - tapRipple
            drawCircle(
                color = pulseTone.copy(alpha = tapRipple * 0.24f),
                radius = min * (0.055f + ripple * 0.44f),
                center = origin,
                style = Stroke(width = strokeBase * (0.9f + tapRipple * 1.4f))
            )
            drawLine(
                color = pulseTone.copy(alpha = tapRipple * 0.16f),
                start = origin,
                end = center + parallax * 0.36f,
                strokeWidth = strokeBase * 0.6f,
                cap = StrokeCap.Round
            )
        }

        if (constellationFocus != null && constellationPull > 0.01f) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        pulseTone.copy(alpha = 0.20f * constellationPull),
                        pulseHalo.copy(alpha = 0.09f * constellationPull),
                        Color.Transparent
                    ),
                    center = constellationFocus,
                    radius = min * (0.16f + constellationPull * 0.24f)
                ),
                radius = min * (0.16f + constellationPull * 0.24f),
                center = constellationFocus
            )
            drawCircle(
                color = quiet.copy(alpha = 0.18f * constellationPull),
                radius = min * (0.024f + constellationPull * 0.016f),
                center = constellationFocus,
                style = Stroke(width = strokeBase * 0.72f)
            )
        }

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    pulseHalo.copy(alpha = 0.20f + energy * 0.05f + contemplation * 0.08f + motionGlow + pulseBeat * 0.06f + gestureOpen * 0.10f + constellationPull * 0.07f + ritualProfile.glowBoost * 0.08f),
                    pulseCore.copy(alpha = 0.08f + social * 0.03f + contemplation * 0.04f + motionGlow * 0.5f + pulseBeat * 0.035f + gestureOpen * 0.06f + constellationPull * 0.04f + ritualProfile.glowBoost * 0.045f),
                    Color.Transparent
                ),
                center = center + parallax * 0.55f,
                radius = min * (0.62f + contemplation * 0.08f + pulseBeat * 0.055f + gestureOpen * 0.075f + constellationPull * 0.035f + ritualProfile.glowBoost * 0.035f)
            ),
            radius = min * (0.62f + contemplation * 0.08f + pulseBeat * 0.055f + gestureOpen * 0.075f + constellationPull * 0.035f + ritualProfile.glowBoost * 0.035f),
            center = center
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    pulseCore.copy(alpha = 0.13f + pulseBeat * 0.08f),
                    pulseHalo.copy(alpha = 0.08f + pulseBeat * 0.06f),
                    Color.Transparent
                ),
                center = Offset(
                    x = center.x + parallax.x + cos(deepPhase * 2f * PI.toFloat()) * min * 0.045f,
                    y = center.y + parallax.y + sin(deepPhase * 2f * PI.toFloat()) * min * 0.045f
                ),
                radius = min * (0.47f + pulseBeat * 0.035f)
            ),
            radius = min * (0.48f + pulseBeat * 0.035f),
            center = center
        )
        drawCircle(
            color = pulseHalo.copy(alpha = 0.075f + shimmer * 0.035f + pulseBeat * 0.10f),
            radius = min * (0.485f + shimmer * 0.012f + pulseBeat * 0.03f),
            center = center,
            style = Stroke(width = strokeBase * (1.5f + pulseBeat * 1.15f))
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.045f),
            radius = min * 0.39f,
            center = center,
            style = Stroke(width = strokeBase)
        )

        val ritualRingCount = (sigil.ringCount * ritualProfile.density).roundToInt().coerceIn(2, 9)
        repeat(ritualRingCount) { index ->
            val radius = min * (0.18f + index * 0.075f + pulseBeat * (0.006f + index * 0.002f) + gestureOpen * 0.006f)
            val alpha = (0.045f + index * 0.014f + pulseBeat * 0.018f + gestureOpen * 0.012f) * ritualProfile.ringAlpha
            drawCircle(
                color = if (index % 2 == 0) pulseHalo.copy(alpha = alpha) else pulseCore.copy(alpha = alpha),
                radius = radius,
                center = center,
                style = Stroke(width = strokeBase * (0.62f + index * 0.08f))
            )
        }

        if (traceCount > 0) {
            repeat(traceCount) { index ->
                val random = Random(sigil.seed + index * 7349)
                val angle = (random.nextFloat() * 360f + deepPhase * 18f * if (index % 2 == 0) 1f else -1f) * PI.toFloat() / 180f
                val radius = min * (0.18f + random.nextFloat() * 0.26f)
                val length = min * (0.026f + random.nextFloat() * 0.055f)
                val markCenter = Offset(
                    x = center.x + cos(angle) * radius + parallax.x * 0.12f,
                    y = center.y + sin(angle) * radius + parallax.y * 0.12f
                )
                val tangent = angle + PI.toFloat() / 2f
                val start = Offset(
                    x = markCenter.x - cos(tangent) * length,
                    y = markCenter.y - sin(tangent) * length
                )
                val end = Offset(
                    x = markCenter.x + cos(tangent) * length,
                    y = markCenter.y + sin(tangent) * length
                )
                val traceAlpha = 0.13f + (index % 4) * 0.025f + pulseBeat * 0.06f
                drawLine(
                    color = pulseTone.copy(alpha = traceAlpha),
                    start = start,
                    end = end,
                    strokeWidth = strokeBase * (0.58f + (index % 3) * 0.22f),
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = quiet.copy(alpha = traceAlpha * 0.55f),
                    radius = strokeBase * (0.85f + (index % 2) * 0.55f),
                    center = markCenter
                )
            }
        }

        val arcCount = (7f * ritualProfile.density).roundToInt().coerceIn(4, 11)
        repeat(arcCount) { index ->
            val ring = index / (arcCount - 1f)
            val radius = min * (0.14f + ring * 0.31f)
            val drift = if (index % 2 == 0) 1f else -1f
            val start = sigil.seed % 83 + index * 47f + deepPhase * 360f * drift + twist * 18f
            val sweep = 48f + focus * 44f + sin((motionPhase * 2f * PI.toFloat()) + index) * 10f + contemplation * 16f
            drawArc(
                color = if (index % 2 == 0) {
                    sparkle.copy(alpha = (0.055f + shimmer * 0.035f + pulseBeat * 0.04f) * ritualProfile.constellationAlpha)
                } else {
                    pulseHalo.copy(alpha = (0.05f + energy * 0.035f + pulseBeat * 0.045f) * ritualProfile.constellationAlpha)
                },
                startAngle = start,
                sweepAngle = sweep + pulseBeat * 18f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(
                    width = strokeBase * (0.42f + ring * 0.9f),
                    cap = StrokeCap.Round
                )
            )
        }

        sigil.strokes.forEachIndexed { index, stroke ->
            val direction = if (index % 2 == 0) 1f else -1f
            val turns = 1f + (index % 2)
            val angle = (stroke.angle + motionPhase * 360f * turns * direction + deepPhase * 42f * direction + twist * 14f) * PI.toFloat() / 180f
            val orbit = min * (stroke.orbit + sin(shimmerPhase * 2f * PI.toFloat() + index) * 0.006f + motion * 0.004f + pulseBeat * 0.006f)
            val strokeCenter = Offset(
                x = center.x + parallax.x * 0.34f + cos(angle) * orbit,
                y = center.y + parallax.y * 0.34f + sin(angle) * orbit
            )
            val tangent = angle + PI.toFloat() / 2f
            val half = min * stroke.length * 0.5f
            val start = Offset(
                x = strokeCenter.x - cos(tangent) * half,
                y = strokeCenter.y - sin(tangent) * half
            )
            val end = Offset(
                x = strokeCenter.x + cos(tangent) * half,
                y = strokeCenter.y + sin(tangent) * half
            )

            drawLine(
                color = if (index % 3 == 0) {
                    sparkle.copy(alpha = stroke.alpha * (0.72f + shimmer * 0.34f + pulseBeat * 0.52f) * ritualProfile.constellationAlpha)
                } else {
                    quiet.copy(alpha = stroke.alpha * (0.48f + shimmer * 0.28f + pulseBeat * 0.34f) * ritualProfile.constellationAlpha)
                },
                start = start,
                end = end,
                strokeWidth = strokeBase * (1f + index % 4),
                cap = StrokeCap.Round
            )
        }

        val points = sigil.nodes.mapIndexed { index, node ->
            val angle = (node.angle + motionPhase * 360f * node.drift + deepPhase * 28f + twist * 10f) * PI.toFloat() / 180f
            val breathingOrbit = node.orbit +
                sin((motionPhase * 360f + index * 27f) * PI.toFloat() / 180f) * 0.012f +
                sin((shimmerPhase * 360f + index * 19f) * PI.toFloat() / 180f) * 0.004f +
                motion * 0.0035f +
                pulseBeat * 0.004f
            val basePoint = Offset(
                x = center.x + parallax.x * 0.18f + cos(angle) * min * breathingOrbit,
                y = center.y + parallax.y * 0.18f + sin(angle) * min * breathingOrbit
            )
            if (constellationFocus == null || constellationPull <= 0.01f) {
                basePoint
            } else {
                val dx = constellationFocus.x - basePoint.x
                val dy = constellationFocus.y - basePoint.y
                val distanceNorm = sqrt(dx * dx + dy * dy) / min
                val nodePull = (
                    constellationPull *
                        constellationCompliance(sigil.seed, index) *
                        logarithmicGravity(distanceNorm) *
                        (0.48f + node.alpha.coerceIn(0.18f, 0.72f) * 0.24f)
                    ).coerceIn(0f, 0.46f)
                val eddyAngle = (node.angle + deepPhase * 220f + index * 57f) * PI.toFloat() / 180f
                val eddyRadius = min * (0.016f + (index % 4) * 0.004f) * constellationPull
                Offset(
                    x = basePoint.x + dx * nodePull + cos(eddyAngle) * eddyRadius,
                    y = basePoint.y + dy * nodePull + sin(eddyAngle) * eddyRadius
                )
            }
        }

        if (showConstellation && points.size > 3) {
            if (constellationFocus != null && constellationPull > 0.01f) {
                points.forEachIndexed { index, point ->
                    val tether = constellationCompliance(sigil.seed, index)
                    if (tether < 0.18f) {
                        return@forEachIndexed
                    }
                    drawLine(
                        color = pulseTone.copy(alpha = (0.020f + (index % 5) * 0.005f) * constellationPull * tether),
                        start = point,
                        end = constellationFocus,
                        strokeWidth = strokeBase * (0.30f + constellationPull * 0.22f),
                        cap = StrokeCap.Round
                    )
                }
            }
            points.forEachIndexed { index, point ->
                val next = points[(index + sigil.connectionStep) % points.size]
                val skip = points[(index + sigil.connectionStep + 2) % points.size]
                val alpha = 0.075f + (index % 4) * 0.018f + constellationPull * 0.035f
                drawLine(
                    color = halo.copy(alpha = alpha * ritualProfile.constellationAlpha),
                    start = point,
                    end = next,
                    strokeWidth = strokeBase * 0.52f
                )
                if (index % 3 == 0) {
                    drawLine(
                        color = core.copy(alpha = alpha * 0.78f * ritualProfile.constellationAlpha),
                        start = point,
                        end = skip,
                        strokeWidth = strokeBase * 0.38f
                    )
                }
            }
        }

        points.forEachIndexed { index, point ->
            val node = sigil.nodes[index]
            val nodePulse = 0.72f + 0.28f * sin(shimmerPhase * 2f * PI.toFloat() + index * 0.61f) + pulseBeat * 0.28f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        quiet.copy(alpha = node.alpha * nodePulse),
                        pulseTone.copy(alpha = node.alpha * 0.58f * nodePulse * ritualProfile.constellationAlpha),
                        Color.Transparent
                    ),
                    center = point,
                    radius = min * node.radius * (3.2f + shimmer * 0.8f)
                ),
                radius = min * node.radius * (3.2f + shimmer * 0.8f),
                center = point
            )
            drawCircle(
                color = quiet.copy(alpha = node.alpha * nodePulse * ritualProfile.constellationAlpha),
                radius = min * node.radius * (0.88f + shimmer * 0.18f),
                center = point
            )
        }

        repeat(5) { index ->
            val direction = if (index % 2 == 0) 1f else -1f
            val angle = (index * 72f + sigil.seed % 31 + motionPhase * 360f * direction + twist * 12f) * PI.toFloat() / 180f
            val inner = min * (0.085f + index * 0.018f)
            val outer = min * (0.19f + (index % 2) * 0.035f)
            drawLine(
                color = pulseCore.copy(alpha = 0.08f + index * 0.018f + pulseBeat * 0.035f),
                start = Offset(center.x + cos(angle) * inner, center.y + sin(angle) * inner),
                end = Offset(center.x + cos(angle) * outer, center.y + sin(angle) * outer),
                strokeWidth = strokeBase * 0.9f,
                cap = StrokeCap.Round
            )
        }

        scale(scale, scale, pivot = center) {
            if (contemplation > 0.01f) {
                repeat(9) { index ->
                    val radius = min * (0.055f + index * 0.024f + contemplativeBreath * 0.006f)
                    val direction = if (index % 2 == 0) 1f else -1f
                    drawArc(
                        color = if (index % 2 == 0) {
                            quiet.copy(alpha = contemplation * (0.10f + index * 0.008f + pulseBeat * 0.04f))
                        } else {
                            pulseTone.copy(alpha = contemplation * (0.08f + focus * 0.05f + pulseBeat * 0.04f))
                        },
                        startAngle = sigil.seed % 67 + index * 41f + deepPhase * 360f * direction + twist * 12f,
                        sweepAngle = 22f + focus * 34f + contemplativeBreath * 12f,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2f, radius * 2f),
                        style = Stroke(
                            width = strokeBase * (0.35f + index * 0.08f),
                            cap = StrokeCap.Round
                        )
                    )
                }
                repeat(6) { index ->
                    val angle = (sigil.seed % 360 + index * 60f + deepPhase * 120f + twist * 18f) * PI.toFloat() / 180f
                    val inner = min * (0.035f + contemplation * 0.012f)
                    val outer = min * (0.16f + focus * 0.04f + index * 0.004f)
                    drawLine(
                        color = pulseCore.copy(alpha = contemplation * (0.06f + contemplativeBreath * 0.04f + pulseBeat * 0.04f)),
                        start = Offset(center.x + cos(angle) * inner, center.y + sin(angle) * inner),
                        end = Offset(center.x + cos(angle) * outer, center.y + sin(angle) * outer),
                        strokeWidth = strokeBase * 0.38f,
                        cap = StrokeCap.Round
                    )
                }
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        quiet.copy(alpha = 0.94f),
                        pulseCore.copy(alpha = 0.74f + contemplation * 0.06f + pulseBeat * 0.10f),
                        pulseHalo.copy(alpha = 0.24f + contemplation * 0.08f + pulseBeat * 0.08f),
                        Color.Transparent
                    ),
                    center = Offset(
                        x = center.x + parallax.x + cos(deepPhase * 2f * PI.toFloat()) * min * 0.035f,
                        y = center.y + parallax.y - min * 0.055f + sin(deepPhase * 2f * PI.toFloat()) * min * 0.025f
                    ),
                    radius = min * (0.28f + shimmer * 0.018f + contemplation * 0.024f + pulseBeat * 0.018f)
                ),
                radius = min * (0.30f + contemplation * 0.018f + pulseBeat * 0.014f),
                center = center
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.22f + shimmer * 0.08f + pulseBeat * 0.10f),
                        pulseTone.copy(alpha = 0.12f + pulseBeat * 0.06f),
                        Color.Transparent
                    ),
                    center = Offset(center.x - min * 0.09f + parallax.x * 1.35f, center.y - min * 0.12f + parallax.y * 1.35f),
                    radius = min * 0.15f
                ),
                radius = min * 0.15f,
                center = Offset(center.x - min * 0.09f + parallax.x * 1.35f, center.y - min * 0.12f + parallax.y * 1.35f)
            )
            repeat(4) { index ->
                val radius = min * (0.18f + index * 0.042f)
                val direction = if (index % 2 == 0) 1f else -1f
                drawArc(
                    color = pulseCore.copy(alpha = 0.075f + shimmer * 0.032f + pulseBeat * 0.045f),
                    startAngle = sigil.seed % 41 + index * 73f + motionPhase * 360f * direction + twist * 10f,
                    sweepAngle = 18f + arousal * 52f + contemplation * 12f + pulseBeat * 18f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(
                        width = strokeBase * (0.8f + index * 0.25f),
                        cap = StrokeCap.Round
                    )
                )
            }
            drawCircle(
                color = Color.White.copy(alpha = 0.095f + shimmer * 0.045f + pulseBeat * 0.06f),
                radius = min * (0.36f + shimmer * 0.01f + pulseBeat * 0.024f),
                center = center,
                style = Stroke(width = strokeBase * 1.8f)
            )
            drawArc(
                color = quiet.copy(alpha = 0.34f + contemplation * 0.12f + motionGlow * 0.45f + pulseBeat * 0.12f),
                startAngle = -30f + motionPhase * 360f + twist * 16f,
                sweepAngle = 38f + (pulse?.arousal ?: 40) * 0.18f + contemplation * 18f + motion * 14f + pulseBeat * 20f,
                useCenter = false,
                topLeft = Offset(size.width * 0.21f, size.height * 0.21f),
                size = Size(size.width * 0.58f, size.height * 0.58f),
                style = Stroke(
                    width = min * (0.012f + pulseBeat * 0.004f),
                    cap = StrokeCap.Round
                )
            )
        }
    }
}

@Composable
private fun rememberOrbSensorState(enabled: Boolean): OrbSensorState {
    val context = LocalContext.current
    var sensorState by remember(enabled) { mutableStateOf(OrbSensorState()) }

    DisposableEffect(context, enabled) {
        if (!enabled) {
            sensorState = OrbSensorState()
            return@DisposableEffect onDispose { }
        }

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            ?: return@DisposableEffect onDispose { }
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val rotationMatrix = FloatArray(9)
        val orientation = FloatArray(3)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                        SensorManager.getOrientation(rotationMatrix, orientation)

                        val targetX = (orientation[2] / (PI.toFloat() / 4f)).coerceIn(-1f, 1f)
                        val targetY = (-orientation[1] / (PI.toFloat() / 4f)).coerceIn(-1f, 1f)
                        val targetTwist = (orientation[0] / PI.toFloat()).coerceIn(-1f, 1f)
                        sensorState = sensorState.copy(
                            tiltX = smooth(sensorState.tiltX, targetX, 0.12f),
                            tiltY = smooth(sensorState.tiltY, targetY, 0.12f),
                            twist = smooth(sensorState.twist, targetTwist, 0.08f)
                        )
                    }

                    Sensor.TYPE_ACCELEROMETER -> {
                        val magnitude = sqrt(
                            event.values[0] * event.values[0] +
                                event.values[1] * event.values[1] +
                                event.values[2] * event.values[2]
                        )
                        val targetMotion = (abs(magnitude - SensorManager.GRAVITY_EARTH) / 7f).coerceIn(0f, 1f)
                        sensorState = sensorState.copy(
                            motion = smooth(sensorState.motion, targetMotion, 0.16f)
                        )
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        rotationSensor?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }
        accelerometer?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    return sensorState
}

private fun smooth(current: Float, target: Float, amount: Float): Float {
    return current + (target - current) * amount.coerceIn(0f, 1f)
}

@Composable
private fun rememberOrbTimeSeconds(active: Boolean): Float {
    var seconds by remember { mutableStateOf(0f) }

    LaunchedEffect(active) {
        if (!active) {
            seconds = 0f
            return@LaunchedEffect
        }

        val start = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            seconds = (now - start) / 1_000_000_000f
        }
    }

    return seconds
}

private fun fractional(value: Float): Float {
    return value - kotlin.math.floor(value)
}

private fun pulseBeatEnvelope(phase: Float, energy: Float, arousal: Float): Float {
    fun impulse(center: Float, width: Float): Float {
        val distance = minOf(
            abs(phase - center),
            abs(phase - center + 1f),
            abs(phase - center - 1f)
        )
        val raw = (1f - distance / width).coerceIn(0f, 1f)
        return raw * raw * (3f - 2f * raw)
    }

    val primary = impulse(0.10f, 0.095f)
    val secondary = impulse(0.31f, 0.082f) * (0.36f + arousal * 0.22f)
    val afterglow = impulse(0.58f, 0.30f) * 0.16f
    val strength = 0.55f + energy * 0.42f + arousal * 0.18f

    return ((primary + secondary) * strength + afterglow).coerceIn(0f, 1f)
}

private fun homeOrbRitualState(pulse: PulseSeal?, memory: ChorusMemory): OrbRitualState {
    if (pulse == null) {
        return OrbRitualState.Dormant
    }
    if (memory.sentEchoes + memory.receivedEchoes > 0) {
        return OrbRitualState.Resonating
    }
    if (chorusApproachIntensity() > 0.78f) {
        return OrbRitualState.NearChorus
    }
    return OrbRitualState.Sealed
}

private fun revealOrbRitualState(pulse: PulseSeal?, memory: ChorusMemory): OrbRitualState {
    if (pulse == null) {
        return OrbRitualState.Dormant
    }
    if (memory.receivedEchoes > 0 || memory.sentEchoes > 1) {
        return OrbRitualState.Resonating
    }
    if (chorusApproachIntensity() > 0.86f) {
        return OrbRitualState.NearChorus
    }
    return OrbRitualState.Sealed
}

private fun resolveOrbRitualState(
    requested: OrbRitualState?,
    pulse: PulseSeal?,
    isContemplating: Boolean,
    chorusIntensity: Float
): OrbRitualState {
    if (requested == OrbRitualState.Resonating) {
        return OrbRitualState.Resonating
    }
    if (requested == OrbRitualState.NearChorus) {
        return OrbRitualState.NearChorus
    }
    if (isContemplating) {
        return OrbRitualState.Contemplative
    }
    if (requested != null) {
        return requested
    }
    if (pulse == null) {
        return OrbRitualState.Dormant
    }
    if (chorusIntensity > 0.82f) {
        return OrbRitualState.NearChorus
    }
    return OrbRitualState.Sealed
}

private fun orbRitualProfile(state: OrbRitualState): OrbRitualProfile {
    return when (state) {
        OrbRitualState.Dormant -> OrbRitualProfile(
            phaseSeconds = 28f,
            deepPhaseSeconds = 58f,
            shimmerSeconds = 11.5f,
            breathSeconds = 10.8f,
            breathAmplitude = 0.024f,
            heartbeatMultiplier = 1.18f,
            density = 0.72f,
            glowBoost = -0.18f,
            toneBlend = 0.10f,
            pulseBoost = -0.08f,
            constellationAlpha = 0.66f,
            ringAlpha = 0.72f
        )
        OrbRitualState.Listening -> OrbRitualProfile(
            phaseSeconds = 20f,
            deepPhaseSeconds = 48f,
            shimmerSeconds = 8.8f,
            breathSeconds = 8.6f,
            breathAmplitude = 0.038f,
            heartbeatMultiplier = 1.02f,
            density = 0.94f,
            glowBoost = 0.02f,
            toneBlend = 0.18f,
            pulseBoost = 0.02f,
            constellationAlpha = 0.92f,
            ringAlpha = 0.94f
        )
        OrbRitualState.Contemplative -> OrbRitualProfile(
            phaseSeconds = 32f,
            deepPhaseSeconds = 72f,
            shimmerSeconds = 12.8f,
            breathSeconds = 12.4f,
            breathAmplitude = 0.032f,
            heartbeatMultiplier = 1.28f,
            density = 1.06f,
            glowBoost = 0.12f,
            toneBlend = 0.26f,
            pulseBoost = 0.04f,
            constellationAlpha = 1.08f,
            ringAlpha = 1.10f
        )
        OrbRitualState.NearChorus -> OrbRitualProfile(
            phaseSeconds = 36f,
            deepPhaseSeconds = 82f,
            shimmerSeconds = 14f,
            breathSeconds = 13.2f,
            breathAmplitude = 0.040f,
            heartbeatMultiplier = 1.38f,
            density = 1.16f,
            glowBoost = 0.24f,
            toneBlend = 0.42f,
            pulseBoost = 0.10f,
            constellationAlpha = 1.18f,
            ringAlpha = 1.20f
        )
        OrbRitualState.Sealed -> OrbRitualProfile(
            phaseSeconds = 18f,
            deepPhaseSeconds = 42f,
            shimmerSeconds = 7.2f,
            breathSeconds = 7.6f,
            breathAmplitude = 0.045f,
            heartbeatMultiplier = 1.0f,
            density = 1.0f,
            glowBoost = 0.04f,
            toneBlend = 0.14f,
            pulseBoost = 0.02f,
            constellationAlpha = 1.0f,
            ringAlpha = 1.0f
        )
        OrbRitualState.Resonating -> OrbRitualProfile(
            phaseSeconds = 15.5f,
            deepPhaseSeconds = 38f,
            shimmerSeconds = 6.4f,
            breathSeconds = 6.8f,
            breathAmplitude = 0.052f,
            heartbeatMultiplier = 0.92f,
            density = 1.28f,
            glowBoost = 0.30f,
            toneBlend = 0.38f,
            pulseBoost = 0.16f,
            constellationAlpha = 1.24f,
            ringAlpha = 1.26f
        )
    }
}

private fun ritualStateTone(state: OrbRitualState, pulse: PulseSeal?, phase: Float): Color {
    val drift = 0.5f + 0.5f * sin(phase * 2f * PI.toFloat() + pulseVisualSeed(pulse) * 0.021f)
    return when (state) {
        OrbRitualState.Dormant -> blendColor(Color(0xFFBBC3CA), Color(0xFFE9E2F3), drift * 0.35f)
        OrbRitualState.Listening -> blendColor(Color(0xFF9DDDC7), Color(0xFFE7DCA2), drift * 0.46f)
        OrbRitualState.Contemplative -> blendColor(Color(0xFFE8E2C5), Color(0xFFC7F0E3), drift * 0.38f)
        OrbRitualState.NearChorus -> blendColor(Color(0xFFFFE0A6), Color(0xFFC7F5E5), drift * 0.34f)
        OrbRitualState.Sealed -> blendColor(pulseHaloColor(pulse), Color(0xFFF3E7B8), drift * 0.28f)
        OrbRitualState.Resonating -> blendColor(Color(0xFFB8F3D6), Color(0xFFFFC7A8), drift * 0.52f)
    }
}

private fun constellationCompliance(seed: Int, index: Int): Float {
    val wave = 0.5f + 0.5f * sin(seed * 0.013f + index * 1.91f)
    val base = 0.18f + wave * 0.62f
    val anchored = abs(seed + index * 37) % 6 == 0
    return if (anchored) base * 0.18f else base
}

private fun logarithmicGravity(distanceNorm: Float): Float {
    return (1f / (1f + ln(1f + distanceNorm.coerceAtLeast(0f) * 5.5f)))
        .coerceIn(0.16f, 0.88f)
}

private fun ritualToneColor(pulse: PulseSeal?, phase: Float): Color {
    val seed = pulseVisualSeed(pulse)
    val valence = (pulse?.valence ?: 54) / 100f
    val energy = (pulse?.energy ?: 52) / 100f
    val social = (pulse?.social ?: 44) / 100f
    val waveA = 0.5f + 0.5f * sin(phase * 2f * PI.toFloat() + seed * 0.017f)
    val waveB = 0.5f + 0.5f * sin(phase * 2f * PI.toFloat() + seed * 0.031f + 2.1f)
    val waveC = 0.5f + 0.5f * sin(phase * 2f * PI.toFloat() + seed * 0.047f + 4.2f)

    return Color(
        red = (0.38f + waveA * 0.34f + valence * 0.16f).coerceIn(0f, 1f),
        green = (0.34f + waveB * 0.28f + social * 0.14f).coerceIn(0f, 1f),
        blue = (0.42f + waveC * 0.32f + (1f - energy) * 0.12f).coerceIn(0f, 1f)
    )
}

private fun chorusApproachIntensity(): Float {
    val now = Calendar.getInstance()
    val minutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    val chorusMinutes = 20 * 60

    if (minutes > chorusMinutes) {
        return 0.06f
    }

    val minutesUntil = chorusMinutes - minutes
    return (1f - minutesUntil / (12f * 60f)).coerceIn(0.08f, 1f)
}

private fun blendColor(start: Color, end: Color, amount: Float): Color {
    val t = amount.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * t,
        green = start.green + (end.green - start.green) * t,
        blue = start.blue + (end.blue - start.blue) * t,
        alpha = start.alpha + (end.alpha - start.alpha) * t
    )
}

private fun pulseCoreColor(pulse: PulseSeal?): Color {
    if (pulse == null) {
        return Color(0xFFECE7FF)
    }

    val valence = pulse.valence / 100f
    val focus = pulse.focus / 100f
    val energy = pulse.energy / 100f

    return Color(
        red = (0.28f + 0.62f * valence + 0.08f * energy).coerceIn(0f, 1f),
        green = (0.32f + 0.36f * focus + 0.14f * energy).coerceIn(0f, 1f),
        blue = (0.36f + 0.38f * (1f - valence) + 0.10f * focus).coerceIn(0f, 1f)
    )
}

private fun pulseHaloColor(pulse: PulseSeal?): Color {
    if (pulse == null) {
        return Color(0xFF8EA0FF)
    }

    val arousal = pulse.arousal / 100f
    val social = pulse.social / 100f

    return Color(
        red = (0.18f + 0.56f * social).coerceIn(0f, 1f),
        green = (0.22f + 0.46f * arousal + 0.16f * social).coerceIn(0f, 1f),
        blue = (0.46f + 0.32f * (1f - social)).coerceIn(0f, 1f)
    )
}

private fun buildPulseSigil(pulse: PulseSeal?, dense: Boolean): PulseSigil {
    val seed = pulseVisualSeed(pulse)
    val random = Random(seed)
    val baseCount = if (dense) 18 else 9
    val energy = (pulse?.energy ?: 52) / 100f
    val focus = (pulse?.focus ?: 48) / 100f
    val social = (pulse?.social ?: 44) / 100f
    val arousal = (pulse?.arousal ?: 50) / 100f

    val nodeCount = baseCount + (energy * 8f).roundToInt()
    val nodes = List(nodeCount) { index ->
        val band = index % 4
        SigilNode(
            angle = random.nextFloat() * 360f + band * 11f,
            orbit = 0.16f + band * 0.075f + random.nextFloat() * (0.035f + social * 0.025f),
            radius = 0.0048f + random.nextFloat() * (0.004f + energy * 0.004f),
            alpha = 0.42f + random.nextFloat() * 0.46f,
            drift = (1f + (index % 3)) * if (index % 2 == 0) 1f else -1f
        )
    }

    val strokeCount = 5 + (arousal * 9f).roundToInt()
    val strokes = List(strokeCount) { index ->
        SigilStroke(
            angle = random.nextFloat() * 360f + index * 9f,
            orbit = 0.18f + random.nextFloat() * 0.29f,
            length = 0.018f + random.nextFloat() * (0.035f + focus * 0.03f),
            alpha = 0.12f + random.nextFloat() * 0.24f
        )
    }

    return PulseSigil(
        seed = seed,
        nodes = nodes,
        strokes = strokes,
        ringCount = 3 + (focus * 4f).roundToInt(),
        connectionStep = 2 + random.nextInt(1, 5)
    )
}

private fun buildResonanceEchoes(pulse: PulseSeal?): List<ResonanceEcho> {
    val random = Random(pulseVisualSeed(pulse) xor 0x5EED)
    val social = (pulse?.social ?: 48) / 100f
    val arousal = (pulse?.arousal ?: 50) / 100f
    val echoCount = 4 + (social * 4f).roundToInt()

    return List(echoCount) { index ->
        ResonanceEcho(
            angle = random.nextFloat() * 360f + index * 17f,
            orbit = 0.34f + random.nextFloat() * (0.16f + social * 0.08f),
            strength = 0.42f + random.nextFloat() * (0.35f + arousal * 0.22f),
            phaseOffset = random.nextFloat()
        )
    }
}

private fun hasCoarseLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

private fun nearbyFieldSeed(context: Context, hasPermission: Boolean): Int {
    val today = PulseSeal.todayKey().hashCode()
    if (!hasPermission) {
        return today xor 0x41F1E1D
    }

    val location = lastKnownApproximateLocation(context)
    if (location == null) {
        return today xor 0x7A11F13D
    }

    val latBucket = (location.latitude * 10).roundToInt()
    val lonBucket = (location.longitude * 10).roundToInt()
    return today xor (latBucket * 73_856_093) xor (lonBucket * 19_349_663)
}

private fun nearbyCellId(context: Context, hasPermission: Boolean): String {
    if (!hasPermission) {
        return "private-field"
    }

    val location = lastKnownApproximateLocation(context)
        ?: return "unknown-field"
    val latBucket = (location.latitude * 10).roundToInt()
    val lonBucket = (location.longitude * 10).roundToInt()

    return "cell_${latBucket}_${lonBucket}"
}

private fun lastKnownApproximateLocation(context: Context): Location? {
    return try {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        listOf(
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
            .mapNotNull { provider ->
                runCatching {
                    if (manager.isProviderEnabled(provider)) {
                        manager.getLastKnownLocation(provider)
                    } else {
                        null
                    }
                }.getOrNull()
            }
            .maxByOrNull { it.time }
    } catch (_: SecurityException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}

private fun buildNearbyPresences(
    seed: Int,
    remoteOrbs: List<RemoteFieldOrb> = emptyList()
): List<NearbyPresence> {
    if (remoteOrbs.isNotEmpty()) {
        return remoteOrbs.take(24).mapIndexed { index, remoteOrb ->
            val random = Random(seed xor remoteOrb.orbId.hashCode())
            NearbyPresence(
                id = remoteOrb.orbId.hashCode(),
                remoteOrbId = remoteOrb.orbId,
                remoteCellId = remoteOrb.cellId,
                pulse = remoteOrb.toPulseSeal(),
                angle = random.nextFloat() * 360f + index * 21f,
                orbit = 0.16f + random.nextFloat() * 0.31f,
                scale = 0.55f + random.nextFloat() * 0.72f
            )
        }
    }

    val random = Random(seed)
    val count = 6 + random.nextInt(5)

    return List(count) { index ->
        val pulse = PulseSeal(
            dateKey = PulseSeal.todayKey(),
            createdAtMillis = seed.toLong() * 31L + index,
            message = "",
            valence = 18 + random.nextInt(76),
            arousal = 16 + random.nextInt(78),
            energy = 20 + random.nextInt(74),
            focus = 14 + random.nextInt(82),
            social = 12 + random.nextInt(84)
        )

        NearbyPresence(
            id = index,
            remoteOrbId = null,
            remoteCellId = null,
            pulse = pulse,
            angle = random.nextFloat() * 360f + index * 21f,
            orbit = 0.16f + random.nextFloat() * 0.31f,
            scale = 0.55f + random.nextFloat() * 0.72f
        )
    }
}

private fun nearbyCellIds(cellId: String): List<String> {
    val match = Regex("""cell_(-?\d+)_(-?\d+)""").matchEntire(cellId)
        ?: return listOf(cellId)
    val lat = match.groupValues[1].toIntOrNull() ?: return listOf(cellId)
    val lon = match.groupValues[2].toIntOrNull() ?: return listOf(cellId)

    return buildList {
        add(cellId)
        for (latOffset in -1..1) {
            for (lonOffset in -1..1) {
                val nearby = "cell_${lat + latOffset}_${lon + lonOffset}"
                if (nearby != cellId) {
                    add(nearby)
                }
            }
        }
    }
}

private fun mergeRemoteOrbs(orbs: List<RemoteFieldOrb>): List<RemoteFieldOrb> {
    return orbs
        .groupBy { it.orbId }
        .mapNotNull { (_, grouped) -> grouped.maxByOrNull { it.createdAtMillis } }
        .sortedByDescending { it.createdAtMillis }
}

private fun nearbyMood(pulse: PulseSeal): String {
    return when {
        pulse.valence <= 30 && pulse.energy <= 44 -> "a low, tender field"
        pulse.valence >= 72 && pulse.social >= 58 -> "a bright, open field"
        pulse.arousal >= 72 -> "a restless signal"
        pulse.focus >= 74 -> "a clear, narrow light"
        pulse.social <= 28 -> "a distant quiet"
        pulse.energy >= 70 -> "a charged presence"
        else -> "a quiet nearby orb"
    }
}

private fun chorusFieldMood(orbs: List<RemoteFieldOrb>): String {
    if (orbs.isEmpty()) {
        return "The chorus is still forming."
    }

    val valence = orbs.map { it.valence }.average()
    val arousal = orbs.map { it.arousal }.average()
    val energy = orbs.map { it.energy }.average()
    val social = orbs.map { it.social }.average()

    return when {
        valence < 34.0 && energy < 46.0 -> "The chorus felt low and tender."
        valence > 68.0 && social > 56.0 -> "The chorus opened bright and close."
        arousal > 70.0 -> "The chorus moved like a restless current."
        energy > 70.0 -> "The chorus gathered a charged glow."
        social < 30.0 -> "The chorus stayed distant but present."
        else -> "The chorus held a soft shared weather."
    }
}

private fun nearbySignature(pulse: PulseSeal): String {
    val light = when {
        pulse.valence >= 72 -> "bright"
        pulse.valence <= 34 -> "lowlight"
        else -> "soft"
    }
    val motion = when {
        pulse.arousal >= 70 -> "restless"
        pulse.arousal <= 32 -> "still"
        else -> "drifting"
    }
    val connection = when {
        pulse.social >= 68 -> "open"
        pulse.social <= 30 -> "distant"
        else -> "near"
    }

    return "$light / $motion / $connection"
}

private fun pulseVisualSeed(pulse: PulseSeal?): Int {
    if (pulse == null) {
        return 4099
    }

    var hash = 17
    hash = hash * 31 + pulse.dateKey.hashCode()
    hash = hash * 31 + pulse.message.hashCode()
    hash = hash * 31 + pulse.valence
    hash = hash * 31 + pulse.arousal * 3
    hash = hash * 31 + pulse.energy * 5
    hash = hash * 31 + pulse.focus * 7
    hash = hash * 31 + pulse.social * 11
    return hash
}

private fun fieldSignature(pulse: PulseSeal): String {
    val light = when {
        pulse.valence >= 72 -> "LUCENT"
        pulse.valence <= 34 -> "UMBRA"
        else -> "LOWLIGHT"
    }
    val motion = when {
        pulse.arousal >= 70 -> "TIDE"
        pulse.arousal <= 32 -> "STILL"
        else -> "DRIFT"
    }
    val connection = when {
        pulse.social >= 68 -> "CHORUS"
        pulse.social <= 30 -> "SOLUS"
        else -> "FIELD"
    }

    return "$light / $motion / $connection"
}

private fun millisUntilChorus(): Long {
    val now = Calendar.getInstance()
    val target = now.clone() as Calendar
    target.set(Calendar.HOUR_OF_DAY, 20)
    target.set(Calendar.MINUTE, 0)
    target.set(Calendar.SECOND, 0)
    target.set(Calendar.MILLISECOND, 0)

    if (!target.after(now)) {
        target.add(Calendar.DAY_OF_YEAR, 1)
    }

    return target.timeInMillis - now.timeInMillis
}

private fun millisSinceTodayChorus(nowMillis: Long = System.currentTimeMillis()): Long {
    val now = Calendar.getInstance().apply {
        timeInMillis = nowMillis
    }
    val target = now.clone() as Calendar
    target.set(Calendar.HOUR_OF_DAY, 20)
    target.set(Calendar.MINUTE, 0)
    target.set(Calendar.SECOND, 0)
    target.set(Calendar.MILLISECOND, 0)

    return now.timeInMillis - target.timeInMillis
}

private fun chorusTimeStage(nowMillis: Long = System.currentTimeMillis()): ChorusStage {
    val since = millisSinceTodayChorus(nowMillis)
    return when {
        since < 0L -> ChorusStage.PreChorus
        since <= 60_000L -> ChorusStage.Minute
        since <= 5 * 60_000L -> ChorusStage.Afterglow
        else -> ChorusStage.Sealed
    }
}

private fun formatCountdown(millis: Long): String {
    val totalSeconds = millis.coerceAtLeast(0L) / 1_000
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60

    return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
}

private fun formatDateKey(dateKey: String): String {
    return runCatching {
        val parser = SimpleDateFormat("yyyyMMdd", Locale.US)
        val formatter = SimpleDateFormat("MMM d, yyyy", Locale.US)
        formatter.format(parser.parse(dateKey)!!)
    }.getOrDefault(dateKey)
}

@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun HomeScreenPreview() {
    ConstellationPulseTheme {
        HomeScreen(
            todaySeal = null,
            sealCount = 7,
            onSealClick = {},
            onRevealClick = {},
            onHistoryClick = {},
            onChorusClick = {},
            onNearbyClick = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun RevealScreenPreview() {
    ConstellationPulseTheme {
        RevealScreen(
            pulse = PulseSeal.today(
                message = "oggi tremo ma resto lucido.",
                valence = 65,
                arousal = 50,
                energy = 70,
                focus = 40,
                social = 55
            ),
            onBack = {},
            onHistoryClick = {}
        )
    }
}
