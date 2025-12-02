package com.example.stressbustersimulator

import kotlin.random.Random

data class Splash(
    var x: Float,
    var y: Float,
    var radius: Float,
    var color: Int,
    var alpha: Int = 255,

    // Gravity fall speed (1f..4f)
    var driftY: Float = Random.nextFloat() * 3f + 1f,

    // Horizontal drift (-2f..2f)
    var driftX: Float = Random.nextFloat() * 4f - 2f,

    // Shrink rate (0.15f..0.35f)
    var shrinkRate: Float = Random.nextFloat() * 0.20f + 0.15f,

    // Fade-out speed (6..12)
    var fadeRate: Int = Random.nextInt(6, 13)
)
