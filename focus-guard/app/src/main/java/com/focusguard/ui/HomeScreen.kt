package com.focusguard.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.state.EarnedItStore
import com.focusguard.state.FocusSessionSummary
import com.focusguard.state.PurchaseResult
import com.focusguard.ui.theme.EarnedColors
import java.time.Instant
import java.time.ZoneId

@Composable
fun HomeScreen(onStartSession: () -> Unit, onReplayOnboarding: () -> Unit) {
    val uiState by EarnedItStore.state.collectAsState()
    var showPetCollection by remember { mutableStateOf(false) }

    if (showPetCollection) {
        PetCollectionBottomSheet(
            currentPet = uiState.pet,
            unlockedPetSpecies = uiState.unlockedPetSpecies,
            bankedMinutes = uiState.timeBankMinutes,
            lifetimeFocusMinutes = uiState.lifetimeFocusMinutes,
            onDismiss = { showPetCollection = false },
            onSelectPet = { species, stage ->
                EarnedItStore.pickPetVersion(species.id, species.displayName, stage)
                showPetCollection = false
            },
            onUnlockPet = { species, costMinutes ->
                EarnedItStore.unlockPetSpecies(species.id, costMinutes) == PurchaseResult.Success
            },
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "EarnedIt",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        "Lock distractions. Earn your time back.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = EarnedColors.Primary.copy(alpha = 0.12f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Shield,
                            contentDescription = null,
                            tint = EarnedColors.Primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        item { NpuStatusCard() }

        item {
            OutlinedButton(
                onClick = onReplayOnboarding,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Replay onboarding")
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    EarnedColors.Primary.copy(alpha = 0.12f),
                                    EarnedColors.Focus.copy(alpha = 0.08f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(22.dp)
                ) {
                    Text(
                        "POINTS BALANCE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "%,d".format(uiState.points),
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = EarnedColors.Points
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            tint = EarnedColors.Points,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Filled.LocalFireDepartment,
                            label = "Streak",
                            value = "${uiState.streakDays} days",
                            tint = EarnedColors.Warning
                        )
                        StatTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Filled.Timer,
                            label = "Banked",
                            value = "${uiState.timeBankMinutes} min",
                            tint = EarnedColors.Primary
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = onStartSession,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EarnedColors.Primary,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Start session", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }

        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPetCollection = true },
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    PetSprite(pet = uiState.pet, size = 76.dp, glow = false)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(uiState.pet.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            "${uiState.pet.mood} · fullness ${uiState.pet.fullness}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(4.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(uiState.pet.fullness / 100f)
                                    .height(7.dp)
                                    .background(
                                        if (uiState.pet.fullness > 60) EarnedColors.Focus else EarnedColors.Warning,
                                        RoundedCornerShape(4.dp)
                                    )
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                "Today",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Spa,
                    label = "Focus time",
                    value = "${uiState.focusMinutesToday}m",
                    tint = EarnedColors.Focus
                )
                StatTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.CalendarMonth,
                    label = "Sessions",
                    value = "${uiState.sessionsToday.size}",
                    tint = EarnedColors.Secondary
                )
            }
        }

        item {
            if (uiState.sessionsToday.isEmpty()) {
                Text(
                    "No sessions yet today. Earn your first 100+ points by completing one.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(uiState.sessionsToday.take(3), key = { it.id }) { session ->
            SessionRow(session)
        }

        item { DailyQuestsSection(uiState.sessionsToday, uiState.focusMinutesToday) }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = EarnedColors.Focus.copy(alpha = 0.08f)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = EarnedColors.Focus)
                    Column {
                        Text("Privacy ledger", fontWeight = FontWeight.SemiBold)
                        Text(
                            "100% on-device · 0 cloud uploads",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NpuStatusCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, EarnedColors.Primary.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = EarnedColors.Primary.copy(alpha = 0.12f),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("NPU", fontSize = 10.sp, color = EarnedColors.Primary, fontWeight = FontWeight.Bold)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "SNAPDRAGON NPU",
                    style = MaterialTheme.typography.labelSmall,
                    color = EarnedColors.Primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "All inference on-device · 0 KB uploaded",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = EarnedColors.Focus, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun StatTile(
    icon: ImageVector,
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                Text(
                    label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
    }
}

@Composable
private fun DailyQuestsSection(sessionsToday: List<FocusSessionSummary>, focusMinutesToday: Int) {
    val sessionsBeforeNoon = remember(sessionsToday) {
        sessionsToday.count { session ->
            val hour = Instant.ofEpochMilli(session.startTimeMs)
                .atZone(ZoneId.systemDefault()).hour
            hour < 12
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("\u2728", fontSize = 20.sp)
                Text(
                    "Daily quests",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                "RESETS AT MIDNIGHT",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }

        QuestCard(
            title = "Early bird",
            description = "Complete 2 sessions before noon",
            current = sessionsBeforeNoon,
            target = 2,
            reward = 150
        )

        QuestCard(
            title = "Marathon mind",
            description = "Log 90 focus minutes today",
            current = focusMinutesToday.coerceAtMost(90),
            target = 90,
            reward = 300
        )

        QuestCard(
            title = "Two-hour titan",
            description = "Log 120 focus minutes today",
            current = focusMinutesToday.coerceAtMost(120),
            target = 120,
            reward = 450
        )
    }
}

@Composable
private fun QuestCard(
    title: String,
    description: String,
    current: Int,
    target: Int,
    reward: Int
) {
    val progress = (current.toFloat() / target).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(600),
        label = "quest_progress"
    )
    val completed = current >= target

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    "$current/$target",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = if (completed) EarnedColors.Focus else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (completed) EarnedColors.Focus else EarnedColors.Primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "+$reward",
                modifier = Modifier.align(Alignment.End),
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = if (completed) EarnedColors.Focus else EarnedColors.Points
            )
        }
    }
}

@Composable
private fun SessionRow(session: FocusSessionSummary) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("${session.durationMinutes} min · Focus ${session.focusScore}%", fontWeight = FontWeight.Medium)
                Text(
                    if (session.success) "Success · ${session.distractionCount} distractions" else "Ended early",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                if (session.success) "+${session.pointsEarned}" else "0",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (session.success) EarnedColors.Points else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
