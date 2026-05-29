package com.example.ui.game

import androidx.compose.ui.graphics.Color

// Game Boundary Values matching our 1000x1000 normalized grid
object GameConfig {
    const val GRID_WIDTH = 1000f
    const val GRID_HEIGHT = 1000f
    
    const val BIRD_X = 250f
    const val GRAVITY_DEFAULT = 0.38f
    const val TAP_IMPULSE_DEFAULT = -9.5f
    
    const val BASE_SPEED = 4.5f
    const val SPEED_STEP = 0.25f // speed increase per 5 points
    const val MAX_SPEED = 9.0f
    
    const val CORAL_SPAWN_X = 1100f
    const val CORAL_MIN_GAP = 220f
    const val CORAL_MAX_GAP = 320f
    const val CORAL_WIDTH = 130f
    const val CORAL_SPACING = 480f // distance between pipe centers
    
    const val POWER_UP_DURATION = 8000L // 8 seconds of aquatic power!
}

enum class GameState {
    MENU,
    CHARACTER_SELECT,
    PLAYING,
    GAME_OVER,
    HIGH_SCORES,
    HELP
}

enum class CharacterType(
    val displayName: String,
    val description: String,
    val gravityMultiplier: Float,
    val tapImpulseMultiplier: Float,
    val baseRadius: Float,
    val primaryColor: Color,
    val secondaryColor: Color
) {
    SUBMARINE(
        displayName = "Yellow Submarine",
        description = "Balanced diving vehicle. Bounces off the top cleanly.",
        gravityMultiplier = 1.0f,
        tapImpulseMultiplier = 1.0f,
        baseRadius = 38f,
        primaryColor = Color(0xFFFFD54F), // Amber Yellow
        secondaryColor = Color(0xFFF57C00) // Deep Orange
    ),
    PUFFERFISH(
        displayName = "Pip the Pufferfish",
        description = "Inflates when you tap! Heavy weight, rapid dive.",
        gravityMultiplier = 1.25f,
        tapImpulseMultiplier = 1.3f,
        baseRadius = 35f,
        primaryColor = Color(0xFFFF8A65), // Coral Red
        secondaryColor = Color(0xFFD84315) // Deep Rust Red
    ),
    TURTLE(
        displayName = "Tilly the Turtle",
        description = "Slow, steady glide. Resistant to steep falls.",
        gravityMultiplier = 0.75f,
        tapImpulseMultiplier = 0.8f,
        baseRadius = 40f,
        primaryColor = Color(0xFF81C784), // Sea Green
        secondaryColor = Color(0xFF2E7D32) // Forest Green
    ),
    MANTA_RAY(
        displayName = "Manny the Manta",
        description = "Extremely narrow hitbox. Drifts aerodynamically.",
        gravityMultiplier = 0.85f,
        tapImpulseMultiplier = 0.95f,
        baseRadius = 30f,
        primaryColor = Color(0xFF4FC3F7), // Water Cyan Blue
        secondaryColor = Color(0xFF0288D1) // Bright Ocean Blue
    )
}

enum class PowerUpType(
    val displayName: String,
    val description: String,
    val bubbleColor: Color,
    val iconSymbol: String
) {
    SHIELD(
        displayName = "Turtle Shield",
        description = "Absorbs one collision with a coral obstacle",
        bubbleColor = Color(0xFF2ecc71), // Vibrant Green
        iconSymbol = "🛡️"
    ),
    SHRINK(
        displayName = "Seahorse Mini",
        description = "Shrinks player to 60% size, fitting narrow gaps easily",
        bubbleColor = Color(0xFFe84393), // Rosy Pink
        iconSymbol = "✨"
    ),
    SLOW_MO(
        displayName = "Jellyfish Drift",
        description = "Slows down time, making coral gaps simple to cross",
        bubbleColor = Color(0xFF9b59b6), // Amethyst Purple
        iconSymbol = "🌀"
    ),
    MAGNET(
        displayName = "Starfish Magnet",
        description = "Pulls all floating score bubbles toward you instantly",
        bubbleColor = Color(0xFFf1c40f), // Neon Gold/Yellow
        iconSymbol = "🧲"
    )
}

data class Coral(
    val id: Long,
    val x: Float,
    val gapY: Float,
    val gapSize: Float,
    val color: Color,
    val passed: Boolean = false,
    val isSwaying: Boolean = false,
    val swayOffset: Float = 0f,
    val swayDirection: Float = 1f // 1f or -1f
)

data class PowerUpItem(
    val id: Long,
    val x: Float,
    val y: Float,
    val type: PowerUpType,
    val isCollected: Boolean = false
)

data class CollectibleBubble(
    val id: Long,
    val x: Float,
    val y: Float,
    val value: Int = 1,
    val isCollected: Boolean = false,
    val pulseScale: Float = 1.0f
)

data class BubbleParticle(
    val x: Float,
    val y: Float,
    val speed: Float,
    val radius: Float,
    val opacity: Float
)
