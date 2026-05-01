package com.focusguard.pet

import androidx.annotation.DrawableRes
import com.focusguard.R

enum class PetSpecies(val id: String, val displayName: String) {
    KITSU("kitsu", "Kitsu"),
    OWLY("owly", "Owly"),
    LUMI("lumi", "Lumi");

    companion object {
        fun fromId(id: String): PetSpecies =
            entries.firstOrNull { it.id == id } ?: KITSU
    }
}

object PetAssets {

    private val sprites: Map<PetSpecies, List<Int>> = mapOf(
        PetSpecies.KITSU to listOf(
            R.drawable.kitsu_1,
            R.drawable.kitsu_2,
            R.drawable.kitsu_3,
            R.drawable.kitsu_4,
            R.drawable.kitsu_5,
        ),
        PetSpecies.OWLY to listOf(
            R.drawable.owly_1,
            R.drawable.owly_2,
            R.drawable.owly_3,
            R.drawable.owly_4,
            R.drawable.owly_5,
        ),
        PetSpecies.LUMI to listOf(
            R.drawable.lumi_1,
            R.drawable.lumi_2,
            R.drawable.lumi_3,
            R.drawable.lumi_4,
            R.drawable.lumi_5,
        ),
    )

    const val MIN_STAGE = 1
    const val MAX_STAGE = 5

    @DrawableRes
    fun spriteRes(species: PetSpecies, stage: Int): Int {
        val stageIndex = stage.coerceIn(MIN_STAGE, MAX_STAGE) - 1
        return sprites.getValue(species)[stageIndex]
    }

    @DrawableRes
    fun spriteRes(speciesId: String, stage: Int): Int =
        spriteRes(PetSpecies.fromId(speciesId), stage)
}
