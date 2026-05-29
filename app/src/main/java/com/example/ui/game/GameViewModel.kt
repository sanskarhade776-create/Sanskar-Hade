package com.example.ui.game

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.HighScoreEntity
import com.example.data.repository.HighScoreRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.absoluteValue
import kotlin.math.sqrt
import kotlin.random.Random

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = HighScoreRepository(db.highScoreDao())

    // High Score Leaderboard List Flow
    val topScores: StateFlow<List<HighScoreEntity>> = repository.topScores
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Primary Game States
    private val _gameState = MutableStateFlow(GameState.MENU)
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _selectedCharacter = MutableStateFlow(CharacterType.SUBMARINE)
    val selectedCharacter: StateFlow<CharacterType> = _selectedCharacter.asStateFlow()

    // Physics & Game Metrics State Flows
    private val _playerY = MutableStateFlow(500f)
    val playerY: StateFlow<Float> = _playerY.asStateFlow()

    private val _playerVy = MutableStateFlow(0f)
    val playerVy: StateFlow<Float> = _playerVy.asStateFlow()

    private val _playerRadius = MutableStateFlow(38f)
    val playerRadius: StateFlow<Float> = _playerRadius.asStateFlow()

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()

    private val _bubbleScore = MutableStateFlow(0) // Extra collectibles
    val bubbleScore: StateFlow<Int> = _bubbleScore.asStateFlow()

    private val _highScore = MutableStateFlow(0)
    val highScore: StateFlow<Int> = _highScore.asStateFlow()

    // Invincibility flicker state (flashing after shield breaks)
    private val _isInvincible = MutableStateFlow(false)
    val isInvincible: StateFlow<Boolean> = _isInvincible.asStateFlow()

    // Custom Coral obstacles
    private val _corals = MutableStateFlow<List<Coral>>(emptyList())
    val corals: StateFlow<List<Coral>> = _corals.asStateFlow()

    // Active power-up floating elements on-screen
    private val _powerUps = MutableStateFlow<List<PowerUpItem>>(emptyList())
    val powerUps: StateFlow<List<PowerUpItem>> = _powerUps.asStateFlow()

    // Collectible Pearls on-screen
    private val _collectibles = MutableStateFlow<List<CollectibleBubble>>(emptyList())
    val collectibles: StateFlow<List<CollectibleBubble>> = _collectibles.asStateFlow()

    // Background underwater ambient bubbles
    private val _backgroundBubbles = MutableStateFlow<List<BubbleParticle>>(emptyList())
    val backgroundBubbles: StateFlow<List<BubbleParticle>> = _backgroundBubbles.asStateFlow()

    // Active power-up states
    private val _activePowerUp = MutableStateFlow<PowerUpType?>(null)
    val activePowerUp: StateFlow<PowerUpType?> = _activePowerUp.asStateFlow()

    private val _powerUpTimeLeft = MutableStateFlow(0L) // Remaining time in ms
    val powerUpTimeLeft: StateFlow<Long> = _powerUpTimeLeft.asStateFlow()

    // UI and Anim flags
    private val _lastScoreIncreaseTime = MutableStateFlow(0L)
    val lastScoreIncreaseTime: StateFlow<Long> = _lastScoreIncreaseTime.asStateFlow()

    private val _isInvincibleFlickerOn = MutableStateFlow(false)
    val isInvincibleFlickerOn: StateFlow<Boolean> = _isInvincibleFlickerOn.asStateFlow()

    private var gameLoopJob: Job? = null
    private var lastTickTime = 0L
    private var shieldInvincibleJob: Job? = null

    // Difficulty tracking
    private var currentScrollSpeed = GameConfig.BASE_SPEED
    private var coralIdCounter = 0L
    private var powerUpIdCounter = 0L
    private var collectibleIdCounter = 0L

    init {
        // Load global high score
        viewModelScope.launch {
            _highScore.value = repository.getHighScore()
        }
        // Initialize scenic background bubble positions
        generateBackgroundBubbles(initial = true)
    }

    fun selectCharacter(character: CharacterType) {
        _selectedCharacter.value = character
        _playerRadius.value = character.baseRadius
    }

    fun changeState(newState: GameState) {
        _gameState.value = newState
        if (newState == GameState.PLAYING) {
            startGame()
        } else {
            stopGameLoop()
        }
    }

    private fun startGame() {
        stopGameLoop()
        
        // Reset metrics
        _playerY.value = 500f
        _playerVy.value = 0f
        _score.value = 0
        _bubbleScore.value = 0
        currentScrollSpeed = GameConfig.BASE_SPEED
        _activePowerUp.value = null
        _powerUpTimeLeft.value = 0L
        _isInvincible.value = false
        _playerRadius.value = _selectedCharacter.value.baseRadius

        // Pre-populate obstacles
        coralIdCounter = 0L
        powerUpIdCounter = 0L
        collectibleIdCounter = 0L
        _corals.value = listOf(
            generateCoral(900f),
            generateCoral(900f + GameConfig.CORAL_SPACING),
            generateCoral(900f + GameConfig.CORAL_SPACING * 2)
        )
        _powerUps.value = emptyList()
        _collectibles.value = emptyList()

        lastTickTime = System.currentTimeMillis()

        // Refresh global high scores reference as well
        viewModelScope.launch {
            _highScore.value = repository.getHighScore()
        }

        // Start 60 FPS Game Loop
        gameLoopJob = viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                val delta = (now - lastTickTime).coerceIn(10L, 50L) // limit delta jump spikes
                lastTickTime = now
                
                try {
                    updatePhysics(delta)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(16) // ~60fps standard target
            }
        }
    }

    private fun stopGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = null
    }

    fun tapBuoyancy() {
        if (_gameState.value != GameState.PLAYING) return
        
        val multiplier = _selectedCharacter.value.tapImpulseMultiplier
        val impulse = GameConfig.TAP_IMPULSE_DEFAULT * multiplier
        
        // Tap gives upwards velocity directly
        _playerVy.value = impulse
    }

    private fun updatePhysics(deltaMs: Long) {
        if (_gameState.value != GameState.PLAYING) return

        // 1. Player Physics (Buoyancy counter-gravity)
        val gravityMultiplier = _selectedCharacter.value.gravityMultiplier
        val gravityAcceleration = GameConfig.GRAVITY_DEFAULT * gravityMultiplier
        
        _playerVy.value += gravityAcceleration
        
        // Dampen maximum falling/rising speed for perfect controlability
        _playerVy.value = _playerVy.value.coerceIn(-18f, 18f)
        _playerY.value += _playerVy.value

        // Top limits: Bounce off surface elegantly
        if (_playerY.value < 0f) {
            _playerY.value = 0f
            _playerVy.value = 3.0f // send bouncing downward slightly
        }
        
        // Bottom limits: Crashing on the sandy marine floor is instantly game over!
        if (_playerY.value > GameConfig.GRID_HEIGHT) {
            triggerGameOver()
            return
        }

        // 2. Active Power-up Timer ticks
        val currentPowerUp = _activePowerUp.value
        if (currentPowerUp != null) {
            _powerUpTimeLeft.value = (_powerUpTimeLeft.value - deltaMs).coerceAtLeast(0L)
            
            // Handle power-up expiration
            if (_powerUpTimeLeft.value == 0L) {
                _activePowerUp.value = null
                // Return size if shrink was active
                if (currentPowerUp == PowerUpType.SHRINK) {
                    _playerRadius.value = _selectedCharacter.value.baseRadius
                }
            }
        }

        // Apply visual flicker for brief post-shield invincibility
        if (_isInvincible.value) {
            _isInvincibleFlickerOn.value = System.currentTimeMillis() % 200 < 100
        } else {
            _isInvincibleFlickerOn.value = false
        }

        // Scroll Speeds (affected by Slow Motion jellyfish powerup!)
        val isSlowed = _activePowerUp.value == PowerUpType.SLOW_MO
        val scrollSpeed = (currentScrollSpeed * (if (isSlowed) 0.60f else 1.0f))

        // 3. Move Corals
        val currentCorals = _corals.value.map { coral ->
            // If coral sways, update its current swayOffset offset
            var newOffset = coral.swayOffset
            var newDir = coral.swayDirection
            if (coral.isSwaying) {
                newOffset += newDir * 1.5f
                if (newOffset.absoluteValue > 85f) {
                    newDir = -newDir
                }
            }
            coral.copy(
                x = coral.x - scrollSpeed,
                swayOffset = newOffset,
                swayDirection = newDir
            )
        }

        // Filter out off-screen corals
        val visibleCorals = currentCorals.filter { it.x > -250f }

        // Spawn new corals on the right if needed to ensure continuity
        val finalCorals = visibleCorals.toMutableList()
        val rightmostCoral = finalCorals.maxByOrNull { it.x }
        if (rightmostCoral == null || rightmostCoral.x < GameConfig.CORAL_SPAWN_X) {
            val startX = (rightmostCoral?.x ?: 900f) + GameConfig.CORAL_SPACING
            finalCorals.add(generateCoral(startX))
        }

        // 4. Pass Obstacles Scoring Check
        var earnedPoints = 0
        val checkedCorals = finalCorals.map { coral ->
            if (!coral.passed && coral.x < GameConfig.BIRD_X) {
                earnedPoints++
                coral.copy(passed = true)
            } else {
                coral
            }
        }
        _corals.value = checkedCorals

        if (earnedPoints > 0) {
            _score.value += earnedPoints
            _lastScoreIncreaseTime.value = System.currentTimeMillis()
            
            // Play mock visual bump. Also escalate speed every 5 points!
            if (_score.value > 0 && _score.value % 5 == 0) {
                currentScrollSpeed = (GameConfig.BASE_SPEED + (_score.value / 5) * GameConfig.SPEED_STEP)
                    .coerceAtMost(GameConfig.MAX_SPEED)
            }

            // Spawn power-ups on score thresholds (or random chance) to make game interesting
            if (_score.value % 4 == 0) {
                _powerUps.value = _powerUps.value + generatePowerUpItem()
            }
        }

        // 5. Ambient Background bubble drifts (pure visual)
        generateBackgroundBubbles(initial = false)

        // 6. Move Power-Ups floating objects
        val updatedPowerUps = _powerUps.value.map { pu ->
            pu.copy(x = pu.x - scrollSpeed)
        }.filter { it.x > -100f && !it.isCollected }
        _powerUps.value = updatedPowerUps

        // Check collection of power-ups
        val currentRadius = _playerRadius.value
        _powerUps.value.forEach { pu ->
            if (!pu.isCollected) {
                val dx = pu.x - GameConfig.BIRD_X
                val dy = pu.y - _playerY.value
                val dist = sqrt(dx * dx + dy * dy)
                if (dist < (currentRadius + 25f)) {
                    // Collect!
                    _powerUps.value = _powerUps.value.map { if (it.id == pu.id) it.copy(isCollected = true) else it }
                    applyPowerUp(pu.type)
                }
            }
        }

        // 7. Move & Attract Collectibles (Pearls)
        val isMagnetActive = _activePowerUp.value == PowerUpType.MAGNET
        val updatedCollectibles = _collectibles.value.map { coll ->
            var targetX = coll.x - scrollSpeed
            var targetY = coll.y

            if (isMagnetActive && !coll.isCollected) {
                // Attract pearls toward player
                val dx = GameConfig.BIRD_X - coll.x
                val dy = _playerY.value - coll.y
                val dist = sqrt(dx * dx + dy * dy)
                if (dist < 400f && dist > 0f) {
                    val attractionF = 20f * (1.0f - dist / 400f) // stronger attraction closer
                    targetX += (dx / dist) * attractionF
                    targetY += (dy / dist) * attractionF
                }
            }

            val scaleWave = 1.0f + 0.15f * kotlin.math.sin((System.currentTimeMillis() + coll.id * 100) / 150.0).toFloat()
            coll.copy(x = targetX, y = targetY, pulseScale = scaleWave)
        }.filter { it.x > -100f && !it.isCollected }
        _collectibles.value = updatedCollectibles

        // Check collection of Collectible Bubbles
        _collectibles.value.forEach { coll ->
            if (!coll.isCollected) {
                val dx = coll.x - GameConfig.BIRD_X
                val dy = coll.y - _playerY.value
                val dist = sqrt(dx * dx + dy * dy)
                if (dist < (currentRadius + 20f)) {
                    // Collected!
                    _collectibles.value = _collectibles.value.map { if (it.id == coll.id) it.copy(isCollected = true) else it }
                    _bubbleScore.value += 1
                    _score.value += 1 // pearl counts as extra score point!
                    
                    if (_score.value > _highScore.value) {
                        _highScore.value = _score.value
                    }
                }
            }
        }

        // Ensure high score stays dynamically up to date
        if (_score.value > _highScore.value) {
            _highScore.value = _score.value
        }

        // 8. Collision Detection with pipe coral pillars!
        if (!_isInvincible.value) {
            val collided = checkCoralCollisions()
            if (collided) {
                if (_activePowerUp.value == PowerUpType.SHIELD) {
                    // Break Shield instead of dying!
                    _activePowerUp.value = null
                    triggerTemporaryInvincibility()
                    // Burst nearby corals out of the screen so player gets a breather!
                    _corals.value = _corals.value.map {
                        if (it.x in (GameConfig.BIRD_X - 150f)..(GameConfig.BIRD_X + 250f)) {
                            it.copy(x = -300f) // force remove
                        } else {
                            it
                        }
                    }
                } else {
                    triggerGameOver()
                }
            }
        }
    }

    private fun generateCoral(startX: Float): Coral {
        val rand = Random(System.nanoTime())
        val id = ++coralIdCounter
        
        // Define random gap center and random gap size
        val gapY = rand.nextInt(280, 720).toFloat()
        
        // Gaps shrink slightly with higher score to raise the challenge!
        val scoreFactor = (_score.value / 10f).coerceIn(0f, 1f)
        val gapSize = (GameConfig.CORAL_MAX_GAP - scoreFactor * (GameConfig.CORAL_MAX_GAP - GameConfig.CORAL_MIN_GAP))
        
        // Assign beautiful tropical neon colors: coral orange, pink, lavender, lime, sky
        val neonColors = listOf(
            Color(0xFFFF5722), // Deep Orange
            Color(0xFFE91E63), // Pink Hot Kelp
            Color(0xFF9C27B0), // Royal Lavender
            Color(0xFF00E676), // Lime Glow
            Color(0xFF00E5FF)  // Deep Cyan
        )
        val coralColor = neonColors[rand.nextInt(neonColors.size)]

        // Corals start in swaying mode past score level 10
        val activateSway = _score.value >= 10 && rand.nextFloat() > 0.35f
        val swayDir = if (rand.nextBoolean()) 1f else -1f

        // Spawn a collection bubble pearl inside the gap of newly spawned coral!
        generateCollectibleBubble(startX, gapY)

        return Coral(
            id = id,
            x = startX,
            gapY = gapY,
            gapSize = gapSize,
            color = coralColor,
            isSwaying = activateSway,
            swayDirection = swayDir
        )
    }

    private fun generateCollectibleBubble(x: Float, y: Float) {
        val id = ++collectibleIdCounter
        val bubble = CollectibleBubble(
            id = id,
            x = x,
            y = y
        )
        // Add to active collectibles list safely
        _collectibles.value = _collectibles.value + bubble
    }

    private fun generatePowerUpItem(): PowerUpItem {
        val rand = Random(System.nanoTime())
        val id = ++powerUpIdCounter
        val types = PowerUpType.values()
        val randomType = types[rand.nextInt(types.size)]
        
        return PowerUpItem(
            id = id,
            x = 1250f,
            y = rand.nextInt(250, 750).toFloat(),
            type = randomType
        )
    }

    private fun applyPowerUp(type: PowerUpType) {
        _activePowerUp.value = type
        _powerUpTimeLeft.value = GameConfig.POWER_UP_DURATION

        if (type == PowerUpType.SHRINK) {
            // shrink bird size to 60%!
            _playerRadius.value = _selectedCharacter.value.baseRadius * 0.60f
        }
    }

    private fun triggerTemporaryInvincibility() {
        _isInvincible.value = true
        shieldInvincibleJob?.cancel()
        shieldInvincibleJob = viewModelScope.launch {
            delay(1500) // 1.5 seconds invincibility flicker
            _isInvincible.value = false
        }
    }

    private fun checkCoralCollisions(): Boolean {
        val playerYVal = _playerY.value
        val pr = _playerRadius.value
        val px = GameConfig.BIRD_X
        val cw = GameConfig.CORAL_WIDTH

        for (coral in _corals.value) {
            val cx = coral.x
            // Only check bounding pipes adjacent to flappy user
            if ((cx - cw / 2f - pr) < px && px < (cx + cw / 2f + pr)) {
                val actualGapY = coral.gapY + coral.swayOffset
                val topBotY = actualGapY - coral.gapSize / 2f
                val botTopY = actualGapY + coral.gapSize / 2f

                // Rectangle 1: Top Coral Pillar
                val closestX1 = px.coerceIn(cx - cw/2f, cx + cw/2f)
                val closestY1 = playerYVal.coerceIn(0f.coerceAtMost(topBotY), topBotY.coerceAtLeast(0f))
                val distSq1 = (px - closestX1) * (px - closestX1) + (playerYVal - closestY1) * (playerYVal - closestY1)

                // Rectangle 2: Bottom Coral Pillar
                val closestX2 = px.coerceIn(cx - cw/2f, cx + cw/2f)
                val closestY2 = playerYVal.coerceIn(botTopY.coerceAtMost(GameConfig.GRID_HEIGHT), botTopY.coerceAtLeast(GameConfig.GRID_HEIGHT))
                val distSq2 = (px - closestX2) * (px - closestX2) + (playerYVal - closestY2) * (playerYVal - closestY2)

                if (distSq1 < pr * pr || distSq2 < pr * pr) {
                    return true // Collided!
                }
            }
        }
        return false
    }

    private fun generateBackgroundBubbles(initial: Boolean) {
        val rand = Random(System.nanoTime())
        val currentList = _backgroundBubbles.value.toMutableList()

        if (initial) {
            // Fill initial bubbles throughout the viewport
            for (i in 0..16) {
                currentList.add(
                    BubbleParticle(
                        x = rand.nextInt(20, 980).toFloat(),
                        y = rand.nextInt(50, 950).toFloat(),
                        speed = rand.nextFloat() * 1.5f + 0.8f,
                        radius = rand.nextInt(6, 18).toFloat(),
                        opacity = rand.nextFloat() * 0.4f + 0.1f
                    )
                )
            }
            _backgroundBubbles.value = currentList
        } else {
            // Move and update bubbles. If bubble escapes, respawn at the bottom floor
            val updated = currentList.map { bubble ->
                val nextY = bubble.y - bubble.speed
                if (nextY < -50f) {
                    BubbleParticle(
                        x = rand.nextInt(20, 980).toFloat(),
                        y = GameConfig.GRID_HEIGHT + rand.nextInt(10, 80).toFloat(),
                        speed = rand.nextFloat() * 1.5f + 0.8f,
                        radius = rand.nextInt(6, 18).toFloat(),
                        opacity = rand.nextFloat() * 0.4f + 0.1f
                    )
                } else {
                    bubble.copy(y = nextY)
                }
            }
            _backgroundBubbles.value = updated
        }
    }

    private fun triggerGameOver() {
        stopGameLoop()
        _gameState.value = GameState.GAME_OVER
    }

    fun submitPlayerScore(playerName: String) {
        val finalName = playerName.trim().ifEmpty { "Aqua Cadet" }
        viewModelScope.launch {
            repository.insertScore(
                HighScoreEntity(
                    playerName = finalName,
                    score = _score.value
                )
            )
            // Reload high scores of our database
            _highScore.value = repository.getHighScore()
            changeState(GameState.HIGH_SCORES)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopGameLoop()
        shieldInvincibleJob?.cancel()
    }
}
