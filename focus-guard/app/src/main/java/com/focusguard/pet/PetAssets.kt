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

    private val sprites: Map<PetSpecies, Map<Int, Int>> = mapOf(
        PetSpecies.KITSU to mapOf(
            1 to R.drawable.kitsu_1,
            2 to R.drawable.kitsu_2,
            3 to R.drawable.kitsu_5,
        ),
        PetSpecies.OWLY to mapOf(
            1 to R.drawable.owly_1,
            2 to R.drawable.owly_2,
            3 to R.drawable.owly_5,
        ),
        PetSpecies.LUMI to mapOf(
            1 to R.drawable.lumi_1,
            2 to R.drawable.lumi_2,
            3 to R.drawable.lumi_4,
        ),
    )

    const val MIN_STAGE = 1
    const val MAX_STAGE = 3

    @DrawableRes
    fun spriteRes(species: PetSpecies, stage: Int): Int {
        val normalizedStage = stage.coerceIn(MIN_STAGE, MAX_STAGE)
        return sprites.getValue(species).getValue(normalizedStage)
    }

    @DrawableRes
    fun spriteRes(speciesId: String, stage: Int): Int =
        spriteRes(PetSpecies.fromId(speciesId), stage)
}
