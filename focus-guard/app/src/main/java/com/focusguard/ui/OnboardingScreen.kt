package com.focusguard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.state.EarnedItStore
import com.focusguard.state.PetProfile
import com.focusguard.ui.theme.EarnedColors

private data class WelcomeSlide(
    val title: String,
    val subtitle: String,
    val body: String,
    val icon: ImageVector
)

private val welcomeSlides = listOf(
    WelcomeSlide(
        "EarnedIt",
        "Focus, verified.\nRewards, earned.",
        "Lock distracting apps during study sessions and earn points for verified focus.",
        Icons.Filled.Star
    ),
    WelcomeSlide(
        "On-device verification",
        "We watch the work, not you.",
        "Camera and microphone signals stay on your phone while EarnedIt checks presence, posture, and environment.",
        Icons.Filled.CameraAlt
    ),
    WelcomeSlide(
        "Real consequences",
        "Apps stay locked until you earn them.",
        "Pick the apps that pull you away. They stay blocked during focus sessions.",
        Icons.Filled.Shield
    )
)

private val permissionSteps = listOf(
    Triple("Camera", "Verify you are present and focused at your desk.", Icons.Filled.CameraAlt),
    Triple("Microphone", "Detect distracting audio like conversation or TV.", Icons.Filled.Mic),
    Triple("Usage Stats", "See which app is in the foreground during a session.", Icons.Filled.PhoneAndroid),
    Triple("Draw Over Apps", "Show the EarnedIt block screen over locked apps.", Icons.Filled.Security),
    Triple("Background Run", "Keep your session alive while you study.", Icons.Filled.Star)
)

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    var selectedSpecies by remember { mutableStateOf("kitsu") }
    var petName by remember { mutableStateOf("Kitsu") }
    val totalSteps = welcomeSlides.size + 1 + permissionSteps.size
    val petStep = welcomeSlides.size
    val permissionIndex = step - petStep - 1

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(
                        EarnedColors.Primary.copy(alpha = 0.12f),
                        MaterialTheme.colorScheme.background,
                        EarnedColors.Focus.copy(alpha = 0.10f)
                    )
                )
            )
            .padding(horizontal = 24.dp, vertical = 36.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
            repeat(totalSteps) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(
                            if (index <= step) EarnedColors.Primary else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(4.dp)
                        )
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            when {
                step < welcomeSlides.size -> WelcomeStep(welcomeSlides[step])
                step == petStep -> PetOnboardingStep(
                    species = selectedSpecies,
                    name = petName,
                    onSpecies = {
                        selectedSpecies = it
                        petName = it.replaceFirstChar { c -> c.uppercase() }
                    },
                    onName = { petName = it.take(20) }
                )
                else -> PermissionStep(permissionSteps[permissionIndex])
            }
        }

        Button(
            onClick = {
                if (step + 1 >= totalSteps) {
                    EarnedItStore.pickPet(selectedSpecies, petName)
                    EarnedItStore.finalizeOnboarding()
                    onComplete()
                } else {
                    step += 1
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EarnedColors.Primary, contentColor = Color.White)
        ) {
            Text(
                when {
                    step + 1 >= totalSteps -> "Start using EarnedIt"
                    step == petStep -> "Hatch my pet"
                    step > petStep -> "Continue"
                    else -> "Continue"
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(6.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
private fun WelcomeStep(slide: WelcomeSlide) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = RoundedCornerShape(30.dp),
            color = EarnedColors.Primary,
            shadowElevation = 10.dp,
            modifier = Modifier.size(if (slide.title == "EarnedIt") 112.dp else 96.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(slide.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(52.dp))
            }
        }
        Spacer(Modifier.height(28.dp))
        Text(slide.title, fontSize = 38.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(
            slide.subtitle,
            modifier = Modifier.padding(top = 12.dp),
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            color = EarnedColors.Primary,
            textAlign = TextAlign.Center
        )
        Text(
            slide.body,
            modifier = Modifier.padding(top = 22.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}

@Composable
private fun PetOnboardingStep(
    species: String,
    name: String,
    onSpecies: (String) -> Unit,
    onName: (String) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PetSprite(PetProfile(name = name, species = species, stage = 1), size = 138.dp)
        Spacer(Modifier.height(18.dp))
        Text("Pick your focus pet", fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text(
            "Your pet evolves with every focused minute.",
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("kitsu", "lumi", "owly").forEach { id ->
                val selected = id == species
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSpecies(id) }
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) EarnedColors.Primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    color = if (selected) EarnedColors.Primary.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 12.dp)) {
                        PetSprite(PetProfile(species = id, name = id, stage = 1), size = 54.dp, glow = false)
                        Text(id.replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = name,
            onValueChange = onName,
            label = { Text("Name your pet") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PermissionStep(step: Triple<String, String, ImageVector>) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.size(96.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(step.third, contentDescription = null, tint = EarnedColors.Primary, modifier = Modifier.size(48.dp))
            }
        }
        Spacer(Modifier.height(28.dp))
        Text(step.first, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text(
            step.second,
            modifier = Modifier.padding(top = 14.dp),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp
        )
        Spacer(Modifier.height(28.dp))
        Surface(shape = CircleShape, color = EarnedColors.Focus.copy(alpha = 0.10f)) {
            Text(
                "Will request real Android permission wiring later",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                color = EarnedColors.Focus,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
