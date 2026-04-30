package com.focusguard.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.session.SessionManager
import com.focusguard.ui.theme.EarnedColors

@Composable
fun SessionScreen(onSessionEnd: (endedEarly: Boolean) -> Unit) {
    val state by SessionManager.stateFlow.collectAsState()

    LaunchedEffect(state.isActive) {
        if (!state.isActive && state.remainingSeconds <= 0) {
            onSessionEnd(false)
        }
    }

    val score = (state.attentionScore * 100f).coerceIn(0f, 100f)
    val minutes = state.remainingSeconds / 60
    val seconds = state.remainingSeconds % 60
    val totalDuration = state.initialDurationSeconds.coerceAtLeast(state.remainingSeconds)
    val progress = if (totalDuration > 0) 1f - (state.remainingSeconds.toFloat() / totalDuration) else 0f
    val confidence = score / 100f

    // Phase determination
    val phase = when {
        score >= 70f -> "FOCUSED"
        score >= 40f -> "GRACE"
        else -> "DISTRACTED"
    }

    val phaseColor by animateColorAsState(
        targetValue = when (phase) {
            "FOCUSED" -> EarnedColors.Focus
            "GRACE" -> EarnedColors.Warning
            else -> EarnedColors.Danger
        },
        animationSpec = tween(500),
        label = "phaseColor"
    )

    val phaseLabel = when (phase) {
        "FOCUSED" -> "Focused"
        "GRACE" -> "Grace period"
        else -> "Distracted"
    }

    val phaseEmoji = when (phase) {
        "FOCUSED" -> "\uD83D\uDFE2"
        "GRACE" -> "\uD83D\uDFE1"
        else -> "\uD83D\uDD34"
    }

    // Pulse animation for distracted state
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (phase == "DISTRACTED") 1.03f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    var showEndDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Session",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Text(
                    "\u26A0 ${state.blacklistedApps.size} blocked",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Timer ring with score
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(280.dp)
                .scale(pulseScale)
        ) {
            // Draw dual rings: outer = timer progress, inner = confidence
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeOuter = 14.dp.toPx()
                val strokeInner = 6.dp.toPx()
                val padding = strokeOuter / 2 + 4.dp.toPx()

                // Outer track
                drawArc(
                    color = Color(0xFF2A2F3C),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(padding, padding),
                    size = Size(size.width - padding * 2, size.height - padding * 2),
                    style = Stroke(width = strokeOuter)
                )
                // Outer progress (primary color)
                drawArc(
                    color = EarnedColors.Primary,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = Offset(padding, padding),
                    size = Size(size.width - padding * 2, size.height - padding * 2),
                    style = Stroke(width = strokeOuter, cap = StrokeCap.Round)
                )

                // Inner track
                val innerPadding = padding + strokeOuter / 2 + 14.dp.toPx()
                drawArc(
                    color = Color(0xFF2A2F3C),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(innerPadding, innerPadding),
                    size = Size(size.width - innerPadding * 2, size.height - innerPadding * 2),
                    style = Stroke(width = strokeInner)
                )
                // Inner confidence ring (phase-colored)
                drawArc(
                    color = phaseColor,
                    startAngle = -90f,
                    sweepAngle = 360f * confidence,
                    useCenter = false,
                    topLeft = Offset(innerPadding, innerPadding),
                    size = Size(size.width - innerPadding * 2, size.height - innerPadding * 2),
                    style = Stroke(width = strokeInner, cap = StrokeCap.Round)
                )
            }

            // Center text
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "%02d:%02d".format(minutes, seconds),
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = 0.sp
                )
                Text(
                    "$phaseEmoji $phaseLabel",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = phaseColor
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Confidence ${score.toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Time penalty warning
        if (phase == "DISTRACTED") {
            Spacer(Modifier.height(8.dp))
            Text(
                "+2s per distracted second",
                style = MaterialTheme.typography.labelSmall,
                color = EarnedColors.Danger.copy(alpha = 0.8f)
            )
        }

        Spacer(Modifier.height(24.dp))

        // Sensor fusion panel
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "ON-DEVICE SENSOR FUSION",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))

                SensorRow(
                    emoji = "\uD83D\uDC64",
                    label = "Face presence",
                    value = if (score > 30) "Detected" else "Absent",
                    ok = score > 30,
                    confidence = if (score > 30) 0.92f else 0.15f
                )
                Spacer(Modifier.height(10.dp))
                SensorRow(
                    emoji = "\uD83E\uDE91",
                    label = "Posture",
                    value = if (score > 50) "Centered" else "Tilted",
                    ok = score > 50,
                    confidence = if (score > 50) 0.85f else 0.35f
                )
                Spacer(Modifier.height(10.dp))
                SensorRow(
                    emoji = "\uD83C\uDFA4",
                    label = "Ambient audio (YamNet)",
                    value = if (score > 60) "Quiet" else "Conversation",
                    ok = score > 60,
                    confidence = if (score > 60) 0.9f else 0.4f
                )
                Spacer(Modifier.height(10.dp))
                SensorRow(
                    emoji = "\uD83D\uDCF1",
                    label = "Phone in frame (YOLO)",
                    value = "Clear",
                    ok = true,
                    confidence = 0.95f
                )
                Spacer(Modifier.height(10.dp))
                SensorRow(
                    emoji = "\uD83E\uDDB6",
                    label = "Eye openness (EAR)",
                    value = if (score > 40) "Open" else "Closed",
                    ok = score > 40,
                    confidence = if (score > 40) 0.88f else 0.2f
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // NPU badge
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Filled.Shield,
                    contentDescription = null,
                    tint = EarnedColors.Focus,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    "NPU ON \u00B7 All inference on-device",
                    style = MaterialTheme.typography.labelSmall,
                    color = EarnedColors.Focus.copy(alpha = 0.9f),
                    fontSize = 11.sp
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // End session button
        TextButton(onClick = { showEndDialog = true }) {
            Icon(
                Icons.Filled.Close,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(4.dp))
            Text(
                "End session early",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(32.dp))
    }

    // End session confirmation dialog
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
private fun SensorRow(
    emoji: String,
    label: String,
    value: String,
    ok: Boolean,
    confidence: Float
) {
    val barColor = if (ok) EarnedColors.Focus else EarnedColors.Danger

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(emoji, fontSize = 18.sp)
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.sp
                )
                Text(
                    value,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    color = barColor
                )
            }
            Spacer(Modifier.height(4.dp))
            // Confidence bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(confidence)
                        .clip(RoundedCornerShape(3.dp))
                        .background(barColor)
                )
            }
        }
    }
}
