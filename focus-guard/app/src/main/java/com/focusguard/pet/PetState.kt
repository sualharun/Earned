package com.focusguard.pet

data class PetEvolutionState(
    val species: PetSpecies = PetSpecies.KITSU,
    val petName: String = "Kitsu",
    val currentStage: Int = 1,
    val lifetimeFocusMinutes: Int = 0,
    val cutscene: EvolutionCutsceneData? = null,
)

data class EvolutionCutsceneData(
    val fromStage: Int,
    val toStage: Int,
    val species: PetSpecies,
    val petName: String,
)

object EvolutionThresholds {

    fun stageForMinutes(totalMinutes: Int): Int = when {
        totalMinutes >= 240 -> 3
        totalMinutes >= 60 -> 2
        else -> 1
    }.coerceIn(PetAssets.MIN_STAGE, PetAssets.MAX_STAGE)

    val STAGE_NAMES: Map<Int, String> = mapOf(
        1 to "Egg",
        2 to "Kid",
        3 to "Adult",
    )

    fun stageName(stage: Int): String =
        STAGE_NAMES[stage.coerceIn(PetAssets.MIN_STAGE, PetAssets.MAX_STAGE)] ?: "Unknown"
}
