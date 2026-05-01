package com.focusguard.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.state.EarnedItStore
import com.focusguard.state.EarnedItUiState
import com.focusguard.state.FocusSessionSummary
import com.focusguard.state.PetProfile
import com.focusguard.ui.theme.EarnedColors
import java.util.Calendar

private val HomeTitleColor = Color(0xFF1F3D24)
private val HomeSubtitleColor = HomeTitleColor.copy(alpha = 0.65f)

@Composable
fun HomeScreen(onStartSession: () -> Unit, onReplayOnboarding: () -> Unit) {
    val uiState by EarnedItStore.state.collectAsState()
    val haptics = rememberHaptics()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(EarnedColors.LightBg),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        item { Greeter(petName = uiState.pet.name, streakDays = uiState.streakDays) }
        item { PetHeroCard(pet = uiState.pet) }
        item {
            StartFocusCta(onClick = {
                haptics.confirm()
                onStartSession()
            })
        }
        item { StatRibbon(state = uiState) }
        item { TrailHeader() }
        if (uiState.sessionsToday.isEmpty()) {
            item { EmptyTrail() }
        } else {
            val visible = uiState.sessionsToday.take(3)
            itemsIndexed(visible, key = { _, s -> s.id }) { index, session ->
                TimelineRow(
                    session = session,
                    isFirst = index == 0,
                    isLast = index == visible.lastIndex
                )
            }
        }
        item { FooterChip() }
        item {
            ReplayLink(onClick = {
                haptics.tap()
                onReplayOnboarding()
            })
        }
    }
}

@Composable
private fun Greeter(petName: String, streakDays: Int) {
    val greeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good morning,"
            in 12..16 -> "Good afternoon,"
            else -> "Good evening,"
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                greeting,
                fontFamily = FontFamily.Serif,
                fontSize = 16.sp,
                color = HomeSubtitleColor
            )
            Text(
                "$petName's looking good",
                fontFamily = FontFamily.Serif,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = HomeTitleColor,
                lineHeight = 30.sp
            )
        }
        if (streakDays > 0) {
            StreakChip(days = streakDays)
        }
    }
}

@Composable
private fun StreakChip(days: Int) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = EarnedColors.Warning.copy(alpha = 0.14f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Filled.LocalFireDepartment,
                contentDescription = null,
                tint = EarnedColors.Warning,
                modifier = Modifier.size(16.dp)
            )
            Text(
                "$days-day streak",
                color = EarnedColors.Warning,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun PetHeroCard(pet: PetProfile) {
    val happy = pet.fullness > 60
    val washColor = if (happy) EarnedColors.Focus else EarnedColors.Warning
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(washColor.copy(alpha = 0.18f), Color.Transparent)
                    )
                )
                .padding(top = 28.dp, bottom = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .size(width = 130.dp, height = 18.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.10f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
                PetSprite(pet = pet, size = 168.dp, glow = true)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                pet.name,
                fontFamily = FontFamily.Serif,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = HomeTitleColor
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${pet.mood} · fullness ${pet.fullness}%",
                fontSize = 13.sp,
                color = HomeSubtitleColor
            )
            Spacer(Modifier.height(12.dp))
            FullnessBar(fullness = pet.fullness, happy = happy)
        }
    }
}

@Composable
private fun FullnessBar(fullness: Int, happy: Boolean) {
    val barColor = if (happy) EarnedColors.Focus else EarnedColors.Warning
    Box(
        modifier = Modifier
            .width(180.dp)
            .height(6.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(HomeTitleColor.copy(alpha = 0.08f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fullness.coerceIn(0, 100) / 100f)
                .height(6.dp)
                .background(barColor, RoundedCornerShape(4.dp))
        )
    }
}

@Composable
private fun StartFocusCta(onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        label = "ctaScale"
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        val released = tryAwaitRelease()
                        pressed = false
                        if (released) onClick()
                    }
                )
            },
        shape = RoundedCornerShape(22.dp),
        color = EarnedColors.Primary,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Start a focus",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Lock distractions · 25–60 min",
                    color = Color.White.copy(alpha = 0.78f),
                    fontSize = 13.sp
                )
            }
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.18f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatRibbon(state: EarnedItUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatChip(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Star,
                tint = EarnedColors.Points,
                label = "Points",
                value = "%,d".format(state.points)
            )
            StatChip(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.LocalFireDepartment,
                tint = EarnedColors.Warning,
                label = "Streak",
                value = "${state.streakDays} d"
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatChip(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Spa,
                tint = EarnedColors.Focus,
                label = "Today's focus",
                value = "${state.focusMinutesToday}m"
            )
            StatChip(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Timer,
                tint = EarnedColors.Primary,
                label = "Time bank",
                value = "${state.timeBankMinutes}m"
            )
        }
    }
}

@Composable
private fun StatChip(
    modifier: Modifier,
    icon: ImageVector,
    tint: Color,
    label: String,
    value: String
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Surface(
                shape = CircleShape,
                color = tint.copy(alpha = 0.15f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = HomeSubtitleColor,
                fontSize = 10.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = HomeTitleColor
            )
        }
    }
}

@Composable
private fun TrailHeader() {
    Text(
        "Today's trail",
        fontFamily = FontFamily.Serif,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = HomeTitleColor
    )
}

@Composable
private fun TimelineRow(
    session: FocusSessionSummary,
    isFirst: Boolean,
    isLast: Boolean
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        // Left rail with dot
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(if (isLast) 60.dp else 84.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            // vertical guideline (skip top half on first row, bottom half on last row)
            if (!isFirst) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(20.dp)
                        .background(HomeTitleColor.copy(alpha = 0.10f))
                )
            }
            Box(
                modifier = Modifier
                    .padding(top = 18.dp)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(
                        if (session.success) EarnedColors.Focus else EarnedColors.Warning
                    )
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .padding(top = 36.dp)
                        .width(2.dp)
                        .height(48.dp)
                        .background(HomeTitleColor.copy(alpha = 0.10f))
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Surface(
            modifier = Modifier
                .weight(1f)
                .padding(top = 6.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "${session.durationMinutes} min · ${session.focusScore}% focus",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = HomeTitleColor
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (session.success)
                            "${session.distractionCount} distractions handled"
                        else
                            "Ended early",
                        fontSize = 12.sp,
                        color = HomeSubtitleColor
                    )
                }
                Text(
                    if (session.success) "+${session.pointsEarned}" else "0",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (session.success) EarnedColors.Points else HomeSubtitleColor
                )
            }
        }
    }
}

@Composable
private fun EmptyTrail() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = HomeTitleColor.copy(alpha = 0.04f)
    ) {
        Text(
            "Your trail starts with your first session today.",
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            fontSize = 13.sp,
            color = HomeSubtitleColor,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FooterChip() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = EarnedColors.Focus.copy(alpha = 0.10f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Filled.Eco,
                contentDescription = null,
                tint = EarnedColors.Focus,
                modifier = Modifier.size(16.dp)
            )
            Text(
                "100% on-device · Snapdragon NPU",
                fontSize = 12.sp,
                color = EarnedColors.Focus,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ReplayLink(onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Replay onboarding",
            modifier = Modifier
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onClick() })
                }
                .padding(8.dp),
            fontSize = 12.sp,
            color = HomeSubtitleColor,
            textDecoration = TextDecoration.Underline
        )
    }
}
