package com.focusguard.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.pet.PetAssets
import com.focusguard.pet.PetSpecies
import com.focusguard.state.PetProfile
import com.focusguard.ui.theme.EarnedColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetCollectionBottomSheet(
    currentPet: PetProfile,
    unlockedPetSpecies: List<String>,
    bankedMinutes: Int,
    lifetimeFocusMinutes: Int,
    onDismiss: () -> Unit,
    onSelectPet: (PetSpecies, Int) -> Unit,
    onUnlockPet: (PetSpecies, Int) -> Boolean,
) {
    val unlockedStage = unlockedStageFor(lifetimeFocusMinutes)
    val ownedSpecies = remember(unlockedPetSpecies, currentPet.species) {
        (unlockedPetSpecies + currentPet.species).distinct()
    }
    var selectedSpecies by remember(currentPet.species) {
        mutableStateOf(PetSpecies.fromId(currentPet.species))
    }
    var selectedStage by remember(currentPet.species, currentPet.stage, lifetimeFocusMinutes) {
        mutableIntStateOf(currentPet.stage.coerceIn(PetAssets.MIN_STAGE, unlockedStage))
    }
    val activeSelectedStage = selectedStage.coerceIn(PetAssets.MIN_STAGE, unlockedStage)

    val selectedUnlocked = isSpeciesUnlocked(selectedSpecies, ownedSpecies)
    val selectedUnlockCost = speciesUnlockCost(selectedSpecies)
    val selectedTone = speciesTone(selectedSpecies)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 8.dp)
                    .size(width = 50.dp, height = 6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.42f))
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SelectedPetCard(
                    species = selectedSpecies,
                    stage = activeSelectedStage,
                    currentPet = currentPet,
                    bankedMinutes = bankedMinutes,
                    lifetimeFocusMinutes = lifetimeFocusMinutes,
                    unlocked = selectedUnlocked,
                    tone = selectedTone,
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PetSpecies.entries.forEach { species ->
                        SpeciesChip(
                            species = species,
                            selected = species == selectedSpecies,
                            unlocked = isSpeciesUnlocked(species, ownedSpecies),
                            tone = speciesTone(species),
                            onClick = {
                                selectedSpecies = species
                                selectedStage = selectedStage.coerceIn(PetAssets.MIN_STAGE, unlockedStage)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 1.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.34f))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp)) {
                        PetSpecies.entries.forEachIndexed { index, species ->
                            SpeciesStageRow(
                                species = species,
                                currentPet = currentPet,
                                ownedSpecies = ownedSpecies,
                                bankedMinutes = bankedMinutes,
                                lifetimeFocusMinutes = lifetimeFocusMinutes,
                                selectedStage = if (species == selectedSpecies) activeSelectedStage else null,
                                expanded = species == selectedSpecies,
                                onClick = {
                                    selectedSpecies = species
                                    selectedStage = selectedStage.coerceIn(PetAssets.MIN_STAGE, unlockedStage)
                                },
                                onStageClick = { stage ->
                                    selectedSpecies = species
                                    selectedStage = stage
                                },
                            )
                            if (index < PetSpecies.entries.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 15.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        if (selectedUnlocked) {
                            onSelectPet(selectedSpecies, activeSelectedStage)
                        } else {
                            onUnlockPet(selectedSpecies, selectedUnlockCost)
                        }
                    },
                    enabled = selectedUnlocked || bankedMinutes >= selectedUnlockCost,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EarnedColors.Primary,
                        contentColor = Color.White,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(
                        text = when {
                            !selectedUnlocked && bankedMinutes < selectedUnlockCost ->
                                "Need ${selectedUnlockCost - bankedMinutes} more min"
                            !selectedUnlocked -> "Unlock ${selectedSpecies.displayName} · ${selectedUnlockCost} min"
                            selectedSpecies.id == currentPet.species && activeSelectedStage == currentPet.stage -> "Current Pet"
                            selectedSpecies.id == currentPet.species -> "Equip Stage $activeSelectedStage"
                            else -> "Select ${selectedSpecies.displayName}"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedPetCard(
    species: PetSpecies,
    stage: Int,
    currentPet: PetProfile,
    bankedMinutes: Int,
    lifetimeFocusMinutes: Int,
    unlocked: Boolean,
    tone: Color,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = painterResource(PetAssets.spriteRes(species, stage)),
                contentDescription = species.displayName,
                modifier = Modifier
                    .size(92.dp)
                    .alpha(if (unlocked) 1f else 0.38f),
                contentScale = ContentScale.Fit
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    species.displayName,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    selectedPetStatus(species, currentPet, bankedMinutes, lifetimeFocusMinutes, unlocked),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(14.dp))
                ProgressTrack(
                    progress = if (species.id == currentPet.species) {
                        currentPet.fullness.coerceIn(0, 100)
                    } else {
                        speciesUnlockProgress(species, unlocked, bankedMinutes)
                    },
                    color = tone,
                    height = 8
                )
            }
        }
    }
}

@Composable
private fun SpeciesChip(
    species: PetSpecies,
    selected: Boolean,
    unlocked: Boolean,
    tone: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .height(58.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) tone else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(PetAssets.spriteRes(species, 2)),
                contentDescription = null,
                modifier = Modifier
                    .size(34.dp)
                    .alpha(if (unlocked) 1f else 0.36f),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.width(7.dp))
            Text(
                species.displayName,
                color = when {
                    !unlocked -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                    selected -> tone
                    else -> MaterialTheme.colorScheme.onSurface
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            if (!unlocked) {
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f),
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

@Composable
private fun SpeciesStageRow(
    species: PetSpecies,
    currentPet: PetProfile,
    ownedSpecies: List<String>,
    bankedMinutes: Int,
    lifetimeFocusMinutes: Int,
    selectedStage: Int?,
    expanded: Boolean,
    onClick: () -> Unit,
    onStageClick: (Int) -> Unit,
) {
    val tone = speciesTone(species)
    val unlockedStage = unlockedStageFor(lifetimeFocusMinutes)
    val speciesUnlocked = isSpeciesUnlocked(species, ownedSpecies)
    val activeStage = selectedStage ?: if (species.id == currentPet.species) {
        currentPet.stage.coerceIn(PetAssets.MIN_STAGE, unlockedStage)
    } else {
        unlockedStage
    }
    val progress = if (species.id == currentPet.species) {
        currentPet.fullness.coerceIn(0, 100)
    } else {
        speciesUnlockProgress(species, speciesUnlocked, bankedMinutes)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                species.displayName,
                modifier = Modifier.weight(1f),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                if (speciesUnlocked) "Stage $activeStage" else "Locked",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (speciesUnlocked) tone else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            (PetAssets.MIN_STAGE..PetAssets.MAX_STAGE).forEach { stage ->
                val stageUnlocked = speciesUnlocked && stage <= unlockedStage
                StageTile(
                    species = species,
                    stage = stage,
                    selected = speciesUnlocked && stage == activeStage,
                    unlocked = stageUnlocked,
                    tone = tone,
                    onClick = { if (stageUnlocked) onStageClick(stage) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressTrack(
                progress = progress,
                color = tone,
                height = 6,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "$progress%",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (speciesUnlocked) tone else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (expanded) {
            Spacer(Modifier.height(6.dp))
            Text(
                if (speciesUnlocked) {
                    "$lifetimeFocusMinutes lifetime focus minutes · stages 1-$unlockedStage unlocked"
                } else {
                    "${speciesUnlockCost(species)} banked minutes to unlock · $bankedMinutes available"
                },
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StageTile(
    species: PetSpecies,
    stage: Int,
    selected: Boolean,
    unlocked: Boolean,
    tone: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.BottomCenter) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .clickable(enabled = unlocked, onClick = onClick),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (unlocked) 0.34f else 0.18f),
                border = if (selected) BorderStroke(1.5.dp, tone) else null
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(PetAssets.spriteRes(species, stage)),
                        contentDescription = "${species.displayName} stage $stage",
                        modifier = Modifier
                            .padding(6.dp)
                            .size(58.dp)
                            .alpha(if (unlocked) 1f else 0.24f),
                        contentScale = ContentScale.Fit
                    )
                    if (!unlocked) {
                        Surface(
                            modifier = Modifier.size(24.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (selected) {
                Surface(
                    modifier = Modifier.size(26.dp),
                    shape = CircleShape,
                    color = tone
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            stage.toString(),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(5.dp))

        if (!selected) {
            Text(
                stage.toString(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
            )
        } else {
            Spacer(Modifier.height(17.dp))
        }
    }
}

@Composable
private fun ProgressTrack(
    progress: Int,
    color: Color,
    height: Int,
    modifier: Modifier = Modifier,
) {
    LinearProgressIndicator(
        progress = { progress / 100f },
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape((height / 2).dp)),
        color = color,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        strokeCap = StrokeCap.Round
    )
}

private fun unlockedStageFor(lifetimeFocusMinutes: Int): Int =
    (lifetimeFocusMinutes / 120 + 1).coerceIn(PetAssets.MIN_STAGE, PetAssets.MAX_STAGE)

private fun speciesUnlockCost(species: PetSpecies): Int =
    when (species) {
        PetSpecies.KITSU -> 0
        PetSpecies.OWLY -> 12
        PetSpecies.LUMI -> 24
    }

private fun isSpeciesUnlocked(
    species: PetSpecies,
    ownedSpecies: List<String>,
): Boolean =
    ownedSpecies.contains(species.id)

private fun speciesUnlockProgress(
    species: PetSpecies,
    unlocked: Boolean,
    bankedMinutes: Int,
): Int {
    if (unlocked) return 100
    val required = speciesUnlockCost(species).coerceAtLeast(1)
    return ((bankedMinutes.toFloat() / required) * 100).toInt().coerceIn(0, 99)
}

private fun selectedPetStatus(
    species: PetSpecies,
    currentPet: PetProfile,
    bankedMinutes: Int,
    lifetimeFocusMinutes: Int,
    unlocked: Boolean,
): String {
    if (species.id == currentPet.species) {
        return "${currentPet.mood} · fullness ${currentPet.fullness}%"
    }
    if (unlocked) {
        return "Unlocked · $lifetimeFocusMinutes lifetime focus minutes"
    }
    val remaining = (speciesUnlockCost(species) - bankedMinutes).coerceAtLeast(0)
    return "Locked · $remaining banked minutes to unlock"
}

private fun speciesTone(species: PetSpecies): Color =
    when (species) {
        PetSpecies.KITSU -> EarnedColors.Primary
        PetSpecies.OWLY -> EarnedColors.Secondary
        PetSpecies.LUMI -> EarnedColors.Focus
    }
