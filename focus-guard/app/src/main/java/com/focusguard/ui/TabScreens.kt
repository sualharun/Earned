package com.focusguard.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.state.EarnedItStore
import com.focusguard.ui.theme.EarnedColors

@Composable
fun InsightsScreen(onOpenDetail: (String) -> Unit) {
    val state by EarnedItStore.state.collectAsState()
    val avgFocus = state.sessionsToday.takeIf { it.isNotEmpty() }?.map { it.focusScore }?.average()?.toInt() ?: 0

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ScreenTitle("Insights", "Your focus trends and distraction profile.") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KpiCard("Avg focus", "$avgFocus%", Modifier.weight(1f))
                KpiCard("This week", "${state.focusMinutesToday}m", Modifier.weight(1f))
                KpiCard("Sessions", "${state.sessionsToday.size}", Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CtaCard(Icons.Filled.Fingerprint, "Focus DNA", "Reveal your archetype", Modifier.weight(1f)) {
                    onOpenDetail("Focus DNA")
                }
                CtaCard(Icons.Filled.Diamond, "Your Wrapped", "Share-ready recap", Modifier.weight(1f)) {
                    onOpenDetail("Wrapped")
                }
            }
        }
        item { BarCard("Focus minutes - last 14 days", listOf(0.28f, 0.42f, 0.22f, 0.68f, 0.50f, 0.80f, 0.62f)) }
        item { RadarCard() }
        item { BarCard("Best time of day", listOf(0.12f, 0.20f, 0.88f, 0.58f, 0.35f, 0.16f), labels = listOf("12a", "4a", "8a", "12p", "4p", "8p")) }
    }
}

@Composable
fun SocialScreen() {
    var code by remember { mutableStateOf("") }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ScreenTitle("Study Rooms", "Focus with friends in real time.") }
        item {
            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EarnedColors.Primary, contentColor = Color.White)
            ) {
                Icon(Icons.Filled.GroupAdd, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Create a room", fontWeight = FontWeight.Bold)
            }
        }
        item {
            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Join with code", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it.uppercase().take(6) },
                            singleLine = true,
                            placeholder = { Text("ABC123") },
                            modifier = Modifier.weight(1f)
                        )
                        Button(onClick = {}, shape = RoundedCornerShape(12.dp)) { Text("Join") }
                    }
                }
            }
        }
        item { EmptyPanel(Icons.Filled.QrCode, "No active rooms", "Create a room when you are ready to wire live sync.") }
    }
}

@Composable
fun CoachScreen() {
    var input by remember { mutableStateOf("") }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ScreenTitle("AI Coach", "Local study guidance with no cloud sync.") }
        item {
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    ChatBubble("assistant", "You are on a 7 day streak. A 25 minute physical study session is the best next step.")
                    ChatBubble("user", "What should I improve today?")
                    ChatBubble("assistant", "Protect your 8-12 window and keep reward apps locked until after your first session. This coach runs locally in the app.")
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("Ask your coach...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = { input = "" }, shape = CircleShape, contentPadding = PaddingValues(14.dp)) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                }
            }
        }
    }
}

@Composable
fun MoreScreen(onOpen: (String) -> Unit) {
    val sections = listOf(
        "Progress" to listOf(
            MoreItem("Trophies", "Badges & milestones", Icons.Filled.EmojiEvents),
            MoreItem("Focus DNA", "Your focus archetype", Icons.Filled.Fingerprint),
            MoreItem("Wrapped", "Your week, recapped", Icons.Filled.Diamond)
        ),
        "Tools" to listOf(
            MoreItem("Focus Pet", "Feed your focus companion", Icons.Filled.Pets),
            MoreItem("Store", "Spend points and unlock rewards", Icons.Filled.Store),
            MoreItem("Desk audit", "AI rates your workspace", Icons.Filled.CameraAlt),
            MoreItem("Calendar", "Plan focus around events", Icons.Filled.CalendarMonth),
            MoreItem("Time bank", "Earned time for reward apps", Icons.Filled.Timer)
        ),
        "Account" to listOf(
            MoreItem("Privacy ledger", "100% on-device · 0 uploads", Icons.Filled.Shield),
            MoreItem("Settings", "Theme, notifications, account", Icons.Filled.Settings)
        )
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item { ScreenTitle("More", "Progress, tools, and account.") }
        sections.forEach { section ->
            item {
                Text(section.first.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) {
                    Column {
                        section.second.forEach { item ->
                            MoreRow(item = item, onClick = { onOpen(item.label) })
                        }
                    }
                }
            }
        }
        item {
            Text(
                "EarnedIt · Powered by Snapdragon NPU",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun PetDetailScreen() {
    val state by EarnedItStore.state.collectAsState()
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ScreenTitle("Focus Pet", "Your companion grows with focused minutes.") }
        item {
            Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(22.dp)) {
                    PetSprite(state.pet, size = 210.dp)
                    Text(state.pet.name, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Text("${state.pet.mood} · Stage ${state.pet.stage}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(14.dp))
                    ProgressBar(state.pet.fullness / 100f, EarnedColors.Focus)
                    Text("Fullness ${state.pet.fullness}/100", modifier = Modifier.padding(top = 8.dp), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun FeatureScreen(title: String) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ScreenTitle(title, "Native workflow shell ported from the React app.") }
        item {
            EmptyPanel(Icons.Filled.Lock, "Ready for the next implementation pass", "The screen is present in Android navigation and styled to match EarnedIt.")
        }
    }
}

private data class MoreItem(val label: String, val hint: String, val icon: ImageVector)

@Composable
private fun ScreenTitle(title: String, subtitle: String) {
    Column {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun KpiCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.padding(13.dp)) {
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CtaCard(icon: ImageVector, title: String, body: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(18.dp), color = EarnedColors.Primary.copy(alpha = 0.10f)) {
        Column(modifier = Modifier.padding(15.dp)) {
            Icon(icon, contentDescription = null, tint = EarnedColors.Primary)
            Spacer(Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
    }
}

@Composable
private fun BarCard(title: String, values: List<Float>, labels: List<String> = values.indices.map { "" }) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.Bottom, modifier = Modifier.height(130.dp).fillMaxWidth()) {
                values.forEachIndexed { index, value ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Spacer(Modifier.weight(1f - value.coerceIn(0.05f, 1f)))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(value.coerceIn(0.05f, 1f))
                                .background(if (value > 0.75f) EarnedColors.Points else EarnedColors.Primary, RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp))
                        )
                        if (labels[index].isNotEmpty()) Text(labels[index], fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun RadarCard() {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Distraction profile", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                val stroke = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                val start = -90f
                val topLeft = Offset(size.width / 2 - 55.dp.toPx(), size.height / 2 - 55.dp.toPx())
                val arcSize = Size(110.dp.toPx(), 110.dp.toPx())
                drawArc(EarnedColors.Primary.copy(alpha = 0.85f), start, 120f, false, topLeft, arcSize, style = stroke)
                drawArc(EarnedColors.Warning.copy(alpha = 0.85f), start + 126f, 80f, false, topLeft, arcSize, style = stroke)
                drawArc(EarnedColors.Focus.copy(alpha = 0.85f), start + 212f, 92f, false, topLeft, arcSize, style = stroke)
            }
        }
    }
}

@Composable
private fun MoreRow(item: MoreItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = RoundedCornerShape(10.dp), color = EarnedColors.Primary.copy(alpha = 0.12f), modifier = Modifier.size(38.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(item.icon, contentDescription = null, tint = EarnedColors.Primary, modifier = Modifier.size(19.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.label, fontWeight = FontWeight.Medium)
            Text(item.hint, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptyPanel(icon: ImageVector, title: String, body: String) {
    Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(28.dp)) {
            Icon(icon, contentDescription = null, tint = EarnedColors.Primary, modifier = Modifier.size(42.dp))
            Spacer(Modifier.height(12.dp))
            Text(title, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun ChatBubble(role: String, text: String) {
    Row(horizontalArrangement = if (role == "user") Arrangement.End else Arrangement.Start, modifier = Modifier.fillMaxWidth()) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (role == "user") EarnedColors.Primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth(0.82f)
        ) {
            Text(
                text,
                modifier = Modifier.padding(12.dp),
                color = if (role == "user") Color.White else MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun ProgressBar(progress: Float, color: Color) {
    Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))) {
        Box(modifier = Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(8.dp).background(color, RoundedCornerShape(8.dp)))
    }
}
