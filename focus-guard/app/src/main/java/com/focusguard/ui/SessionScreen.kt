package com.focusguard.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.R
import com.focusguard.session.SessionManager
import com.focusguard.state.EarnedItStore
import com.focusguard.state.PetProfile
import com.focusguard.state.PurchaseResult
import com.focusguard.ui.theme.EarnedColors

private enum class FocusPhase {
    Focused,
    Refocus,
    Distracted
}

private data class FocusBackground(
    val id: String,
    val title: String,
    val costMinutes: Int,
    @DrawableRes val imageRes: Int,
)

private val focusBackgrounds = listOf(
    FocusBackground("cozy_desk", "Cozy Desk", 0, R.drawable.focus_bg_cozy_desk),
    FocusBackground("balcony_night", "Balcony Night", 20, R.drawable.focus_bg_balcony_night),
)

@Composable
fun SessionScreen(onSessionEnd: (endedEarly: Boolean) -> Unit) {
    val session by SessionManager.stateFlow.collectAsState()
    val appState by EarnedItStore.state.collectAsState()

    LaunchedEffect(session.isActive, session.remainingSeconds) {
        if (!session.isActive && session.remainingSeconds <= 0) {
            onSessionEnd(false)
        }
    }

    val mlScore = (session.attentionScore * 100f).coerceIn(0f, 100f)
    val mlPhase = when {
        mlScore >= 70f -> FocusPhase.Focused
        mlScore >= 40f -> FocusPhase.Refocus
        else -> FocusPhase.Distracted
    }
    var phaseOverride by remember { mutableStateOf<FocusPhase?>(null) }
    val phase = phaseOverride ?: mlPhase
    val displayScore = phaseOverride?.let {
        when (it) {
            FocusPhase.Focused -> 84
            FocusPhase.Refocus -> 56
            FocusPhase.Distracted -> 28
        }
    } ?: mlScore.toInt()
    val phaseColor by animateColorAsState(
        targetValue = when (phase) {
            FocusPhase.Focused -> EarnedColors.Focus
            FocusPhase.Refocus -> EarnedColors.Warning
            FocusPhase.Distracted -> EarnedColors.Danger
        },
        animationSpec = tween(650),
        label = "phase_color"
    )
    val sceneDim by animateFloatAsState(
        targetValue = when (phase) {
            FocusPhase.Focused -> 0.04f
            FocusPhase.Refocus -> 0.18f
            FocusPhase.Distracted -> 0.42f
        },
        animationSpec = tween(800, easing = EaseInOutCubic),
        label = "scene_dim"
    )
    val amberGlow by animateFloatAsState(
        targetValue = if (phase == FocusPhase.Refocus) 0.42f else 0.12f,
        animationSpec = tween(700, easing = EaseInOutCubic),
        label = "amber_glow"
    )
    val redGlow by animateFloatAsState(
        targetValue = if (phase == FocusPhase.Distracted) 0.32f else 0f,
        animationSpec = tween(700, easing = EaseInOutCubic),
        label = "red_glow"
    )

    val minutes = session.remainingSeconds / 60
    val seconds = session.remainingSeconds % 60
    val totalDuration = session.initialDurationSeconds.coerceAtLeast(session.remainingSeconds)
    val progress = if (totalDuration > 0) 1f - (session.remainingSeconds.toFloat() / totalDuration) else 0f
    val selectedBackground = focusBackgrounds.firstOrNull { it.id == appState.selectedFocusBackground } ?: focusBackgrounds.first()

    var showEndDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            SessionTopBar(blockedCount = session.blacklistedApps.size)

            FocusCompanionHero(
                pet = appState.pet,
                background = selectedBackground,
                phase = phase,
                phaseColor = phaseColor,
                sceneDim = sceneDim,
                amberGlow = amberGlow,
                redGlow = redGlow,
                timer = "%02d:%02d".format(minutes, seconds),
                score = displayScore,
                progress = progress,
            )

            PhaseTestControls(
                selectedPhase = phaseOverride,
                mlPhase = mlPhase,
                onSelected = { phaseOverride = it }
            )

            BackgroundPicker(
                selectedId = selectedBackground.id,
                unlockedIds = appState.unlockedFocusBackgrounds,
                bankedMinutes = appState.timeBankMinutes,
                onSelect = EarnedItStore::selectFocusBackground,
                onUnlock = { background ->
                    EarnedItStore.unlockFocusBackground(background.id, background.costMinutes)
                },
            )

            SessionControls(onEndSession = { showEndDialog = true })
        }
    }

    if (showEndDialog) {
        AlertDialog(
            onDismissRequest = { showEndDialog = false },
            title = { Text("End session early?") },
            text = { Text("You'll forfeit any points for this session.") },
            confirmButton = {
                TextButton(onClick = {
                    showEndDialog = false
                    SessionManager.stopSession()
                    onSessionEnd(true)
                }) {
                    Text("End session", color = EarnedColors.Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDialog = false }) {
                    Text("Keep going")
                }
            }
        )
    }
}

@Composable
private fun SessionTopBar(blockedCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Icon(Icons.Filled.Shield, contentDescription = null, tint = EarnedColors.Primary, modifier = Modifier.size(16.dp))
                Text("Session · $blockedCount blocked", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        ) {
            IconButton(onClick = { }) {
                Icon(Icons.Filled.MoreHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun FocusCompanionHero(
    pet: PetProfile,
    background: FocusBackground,
    phase: FocusPhase,
    phaseColor: Color,
    sceneDim: Float,
    amberGlow: Float,
    redGlow: Float,
    timer: String,
    score: Int,
    progress: Float,
) {
    val infinite = rememberInfiniteTransition(label = "companion_motion")
    val lampPulse by infinite.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.34f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "lamp_pulse"
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(545.dp),
        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
        color = Color.Black
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Crossfade(
                targetState = heroSceneRes(pet.species, pet.stage, background.id, phase),
                animationSpec = tween(850, easing = EaseInOutCubic),
                label = "focus_scene_phase"
            ) { sceneRes ->
                Image(
                    painter = painterResource(sceneRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = sceneDim))
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                EarnedColors.Warning.copy(alpha = lampPulse + amberGlow),
                                Color.Transparent
                            ),
                            center = Offset(95f, 355f),
                            radius = 420f
                        )
                    )
            )
            if (redGlow > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color.Transparent, EarnedColors.Danger.copy(alpha = redGlow)),
                                radius = 760f
                            )
                        )
                )
            }

            TimerRing(
                progress = progress,
                color = phaseColor,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 20.dp)
                    .size(245.dp)
            )

            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 86.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    timer,
                    color = Color.White,
                    fontSize = 58.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.sp
                )
                Surface(shape = RoundedCornerShape(18.dp), color = phaseColor) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Filled.Spa, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                        Text("${phaseLabel(phase)} · $score%", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(7.dp))
                Text("ML tracking active · on-device", color = Color.White.copy(alpha = 0.82f), fontSize = 11.sp)
                if (phase != FocusPhase.Focused) {
                    Spacer(Modifier.height(9.dp))
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.Black.copy(alpha = 0.42f),
                        border = BorderStroke(1.dp, phaseColor.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = phaseMessage(phase),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            PhaseStrip(
                phase = phase,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 13.dp, vertical = 18.dp)
            )
        }
    }
}

@Composable
private fun TimerRing(progress: Float, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = 5.dp.toPx()
        val padding = stroke / 2
        drawArc(
            color = Color.White.copy(alpha = 0.22f),
            startAngle = -210f,
            sweepAngle = 300f,
            useCenter = false,
            topLeft = Offset(padding, padding),
            size = Size(size.width - padding * 2, size.height - padding * 2),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        drawArc(
            color = color,
            startAngle = -210f,
            sweepAngle = 300f * progress.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = Offset(padding, padding),
            size = Size(size.width - padding * 2, size.height - padding * 2),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun PhaseStrip(phase: FocusPhase, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color.Black.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PhaseHint(
                icon = Icons.Filled.Spa,
                title = "Focused",
                subtitle = "You're in the zone!",
                color = EarnedColors.Focus,
                active = phase == FocusPhase.Focused,
                modifier = Modifier.weight(1f)
            )
            PhaseHint(
                icon = Icons.Filled.Headphones,
                title = "Refocus",
                subtitle = "Stay with it.",
                color = EarnedColors.Warning,
                active = phase == FocusPhase.Refocus,
                modifier = Modifier.weight(1f)
            )
            PhaseHint(
                icon = Icons.Filled.Close,
                title = "Distracted",
                subtitle = "Let's get back.",
                color = EarnedColors.Danger,
                active = phase == FocusPhase.Distracted,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PhaseHint(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.alpha(if (active) 1f else 0.72f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Column {
            Text(title, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color.White.copy(alpha = 0.78f), fontSize = 8.sp, maxLines = 1)
        }
    }
}

@Composable
private fun PhaseTestControls(
    selectedPhase: FocusPhase?,
    mlPhase: FocusPhase,
    onSelected: (FocusPhase?) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ML state preview", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Auto: ${phaseLabel(mlPhase)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PhasePreviewChip(
                    label = "Auto",
                    selected = selectedPhase == null,
                    color = MaterialTheme.colorScheme.onSurface,
                    onClick = { onSelected(null) },
                    modifier = Modifier.weight(1f)
                )
                PhasePreviewChip(
                    label = "Focused",
                    selected = selectedPhase == FocusPhase.Focused,
                    color = EarnedColors.Focus,
                    onClick = { onSelected(FocusPhase.Focused) },
                    modifier = Modifier.weight(1f)
                )
                PhasePreviewChip(
                    label = "Refocus",
                    selected = selectedPhase == FocusPhase.Refocus,
                    color = EarnedColors.Warning,
                    onClick = { onSelected(FocusPhase.Refocus) },
                    modifier = Modifier.weight(1f)
                )
                PhasePreviewChip(
                    label = "Distracted",
                    selected = selectedPhase == FocusPhase.Distracted,
                    color = EarnedColors.Danger,
                    onClick = { onSelected(FocusPhase.Distracted) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PhasePreviewChip(
    label: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .height(36.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) color.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) color else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun BackgroundPicker(
    selectedId: String,
    unlockedIds: List<String>,
    bankedMinutes: Int,
    onSelect: (String) -> Unit,
    onUnlock: (FocusBackground) -> PurchaseResult,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 14.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Background", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Choose your focus space", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                }
                Surface(shape = RoundedCornerShape(14.dp), color = EarnedColors.Focus.copy(alpha = 0.1f)) {
                    Text(
                        "$bankedMinutes",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        color = EarnedColors.Focus,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(Modifier.height(13.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                focusBackgrounds.forEach { background ->
                    val unlocked = background.id in unlockedIds
                    val selected = background.id == selectedId
                    FocusBackgroundCard(
                        background = background,
                        selected = selected,
                        unlocked = unlocked,
                        bankedMinutes = bankedMinutes,
                        onClick = {
                            if (unlocked) onSelect(background.id) else onUnlock(background)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun FocusBackgroundCard(
    background: FocusBackground,
    selected: Boolean,
    unlocked: Boolean,
    bankedMinutes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(enabled = unlocked || bankedMinutes >= background.costMinutes, onClick = onClick),
        shape = RoundedCornerShape(13.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) EarnedColors.Focus else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            Box {
                Image(
                    painter = painterResource(background.imageRes),
                    contentDescription = background.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(78.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .alpha(if (unlocked) 1f else 0.55f),
                    contentScale = ContentScale.Crop
                )
                if (selected) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(5.dp)
                            .size(28.dp),
                        shape = CircleShape,
                        color = EarnedColors.Focus
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(background.title, modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                if (!unlocked) {
                    Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(3.dp))
                    Text("${background.costMinutes} min", fontSize = 10.sp, color = EarnedColors.Focus, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SessionControls(onEndSession: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
            IconButton(onClick = { SessionManager.pauseSession() }) {
                Icon(Icons.Filled.Pause, contentDescription = null)
            }
        }
        Button(
            onClick = onEndSession,
            modifier = Modifier
                .weight(1f)
                .height(58.dp),
            shape = RoundedCornerShape(22.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EarnedColors.Primary, contentColor = Color.White)
        ) {
            Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(8.dp))
            Text("End Session", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
            IconButton(onClick = { }) {
                Icon(Icons.Filled.GraphicEq, contentDescription = null)
            }
        }
    }
}

@DrawableRes
private fun heroSceneRes(species: String, stage: Int, backgroundId: String, phase: FocusPhase): Int {
    val focusStage = focusSceneStage(species, stage)
    return when (species) {
        "owly" -> if (backgroundId == "balcony_night") {
            R.drawable.focus_scene_owly_balcony_night
        } else {
            when (focusStage) {
                1 -> when (phase) {
                    FocusPhase.Focused -> R.drawable.focus_scene_owly_1_cozy_desk
                    FocusPhase.Refocus -> R.drawable.focus_scene_owly_1_cozy_desk_refocus
                    FocusPhase.Distracted -> R.drawable.focus_scene_owly_1_cozy_desk_distracted
                }
                5 -> when (phase) {
                    FocusPhase.Focused -> R.drawable.focus_scene_owly_5_cozy_desk
                    FocusPhase.Refocus -> R.drawable.focus_scene_owly_5_cozy_desk_refocus
                    FocusPhase.Distracted -> R.drawable.focus_scene_owly_5_cozy_desk_distracted
                }
                else -> when (phase) {
                    FocusPhase.Focused -> R.drawable.focus_scene_owly_cozy_desk
                    FocusPhase.Refocus -> R.drawable.focus_scene_owly_cozy_desk_refocus
                    FocusPhase.Distracted -> R.drawable.focus_scene_owly_cozy_desk_distracted
                }
            }
        }
        "lumi" -> if (backgroundId == "balcony_night") {
            R.drawable.focus_scene_lumi_balcony_night
        } else {
            when (focusStage) {
                1 -> when (phase) {
                    FocusPhase.Focused -> R.drawable.focus_scene_lumi_1_cozy_desk
                    FocusPhase.Refocus -> R.drawable.focus_scene_lumi_1_cozy_desk_refocus
                    FocusPhase.Distracted -> R.drawable.focus_scene_lumi_1_cozy_desk_distracted
                }
                4 -> when (phase) {
                    FocusPhase.Focused -> R.drawable.focus_scene_lumi_4_cozy_desk
                    FocusPhase.Refocus -> R.drawable.focus_scene_lumi_4_cozy_desk_refocus
                    FocusPhase.Distracted -> R.drawable.focus_scene_lumi_4_cozy_desk_distracted
                }
                else -> when (phase) {
                    FocusPhase.Focused -> R.drawable.focus_scene_lumi_cozy_desk
                    FocusPhase.Refocus -> R.drawable.focus_scene_lumi_cozy_desk_refocus
                    FocusPhase.Distracted -> R.drawable.focus_scene_lumi_cozy_desk_distracted
                }
            }
        }
        else -> if (backgroundId == "balcony_night") {
            R.drawable.focus_scene_kitsu_balcony_night
        } else {
            when (focusStage) {
                1 -> when (phase) {
                    FocusPhase.Focused -> R.drawable.focus_scene_kitsu_1_cozy_desk
                    FocusPhase.Refocus -> R.drawable.focus_scene_kitsu_1_cozy_desk_refocus
                    FocusPhase.Distracted -> R.drawable.focus_scene_kitsu_1_cozy_desk_distracted
                }
                5 -> when (phase) {
                    FocusPhase.Focused -> R.drawable.focus_scene_kitsu_5_cozy_desk
                    FocusPhase.Refocus -> R.drawable.focus_scene_kitsu_5_cozy_desk_refocus
                    FocusPhase.Distracted -> R.drawable.focus_scene_kitsu_5_cozy_desk_distracted
                }
                else -> when (phase) {
                    FocusPhase.Focused -> R.drawable.focus_scene_kitsu_cozy_desk
                    FocusPhase.Refocus -> R.drawable.focus_scene_kitsu_cozy_desk_refocus
                    FocusPhase.Distracted -> R.drawable.focus_scene_kitsu_cozy_desk_distracted
                }
            }
        }
    }
}

private fun focusSceneStage(species: String, stage: Int): Int =
    when (species) {
        "lumi" -> when {
            stage <= 1 -> 1
            stage >= 4 -> 4
            else -> 3
        }
        else -> when {
            stage <= 1 -> 1
            stage >= 5 -> 5
            else -> 3
        }
    }

private fun phaseLabel(phase: FocusPhase): String =
    when (phase) {
        FocusPhase.Focused -> "Focused"
        FocusPhase.Refocus -> "Refocus"
        FocusPhase.Distracted -> "Distracted"
    }

private fun phaseMessage(phase: FocusPhase): String =
    when (phase) {
        FocusPhase.Focused -> "Progress earning normally"
        FocusPhase.Refocus -> "Refocus now to keep progress smooth"
        FocusPhase.Distracted -> "+2s penalty while distracted"
    }
