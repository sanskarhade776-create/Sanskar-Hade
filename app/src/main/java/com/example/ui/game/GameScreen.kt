package com.example.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.data.database.HighScoreEntity

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val gameState by viewModel.gameState.collectAsStateWithLifecycle()
    val score by viewModel.score.collectAsStateWithLifecycle()
    val highScore by viewModel.highScore.collectAsStateWithLifecycle()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF071B2B)) // Deep oceanic background backup
    ) {
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        
        // Responsive box helper: restricts game area aspect ratio on wide tablet devices for ideal ergonomics
        val isTablet = maxWidth > 600.dp
        val gameAreaModifier = if (isTablet) {
            Modifier
                .widthIn(max = 480.dp)
                .fillMaxHeight()
                .align(Alignment.Center)
                .border(2.dp, Color(0xFF00E5FF).copy(alpha = 0.5f))
                .shadow(16.dp)
        } else {
            Modifier.fillMaxSize()
        }

        Box(
            modifier = gameAreaModifier.background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0E4C75), // Bright Top Reef Sun rays
                        Color(0xFF0A2B45), // Depth Transition
                        Color(0xFF051421)  // Dark Abyssal ocean floor
                    )
                )
            )
        ) {
            // Render different game screens based on State Machine
            when (gameState) {
                GameState.MENU -> {
                    MainMenuScreen(
                        highScore = highScore,
                        onNavigate = { viewModel.changeState(it) }
                    )
                }
                GameState.CHARACTER_SELECT -> {
                    CharacterSelectScreen(
                        selected = viewModel.selectedCharacter.collectAsStateWithLifecycle().value,
                        onSelect = { viewModel.selectCharacter(it) },
                        onStartGame = { viewModel.changeState(GameState.PLAYING) },
                        onBack = { viewModel.changeState(GameState.MENU) }
                    )
                }
                GameState.PLAYING -> {
                    ActiveGamePlayScreen(viewModel = viewModel)
                }
                GameState.GAME_OVER -> {
                    GameOverScreen(
                        score = score,
                        highScore = highScore,
                        onSubmit = { name -> viewModel.submitPlayerScore(name) },
                        onRestart = { viewModel.changeState(GameState.PLAYING) },
                        onQuit = { viewModel.changeState(GameState.MENU) }
                    )
                }
                GameState.HIGH_SCORES -> {
                    LeaderboardScreen(
                        topScores = viewModel.topScores.collectAsStateWithLifecycle().value,
                        onBack = { viewModel.changeState(GameState.MENU) }
                    )
                }
                GameState.HELP -> {
                    HelpInstructionScreen(
                        onBack = { viewModel.changeState(GameState.MENU) }
                    )
                }
            }
        }
    }
}

@Composable
fun MainMenuScreen(
    highScore: Int,
    onNavigate: (GameState) -> Unit
) {
    // Glowing wave pulse
    val infiniteTransition = rememberInfiniteTransition(label = "menuWave")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Floating Bubbles Backdrop
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = size.width / 2f
            drawCircle(
                color = Color(0xFF00E5FF).copy(alpha = 0.05f),
                radius = 300f + waveOffset,
                center = Offset(center, size.height * 0.4f)
            )
            drawCircle(
                color = Color(0xFFE91E63).copy(alpha = 0.03f),
                radius = 200f - waveOffset,
                center = Offset(center - 100f, size.height * 0.3f)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxHeight()
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Main Logo Title Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🫧",
                        fontSize = 38.sp,
                        modifier = Modifier.offset(y = waveOffset.dp * 0.3f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "FLAPPY",
                        fontSize = 46.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        color = Color(0xFF00E5FF),
                        textAlign = TextAlign.Center,
                        style = androidx.compose.ui.text.TextStyle(
                            shadow = Shadow(
                                color = Color(0xFF00B0FF),
                                offset = Offset(2f, 4f),
                                blurRadius = 8f
                            )
                        )
                    )
                }
                
                Text(
                    text = "A Q U A T I C A",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFE91E63),
                    modifier = Modifier.offset(y = -waveOffset.dp * 0.15f)
                )

                Text(
                    text = "Colorful Deep-Sea Adventure",
                    fontSize = 13.sp,
                    fontFamily = FontFamily.SansSerif,
                    color = Color.LightGray.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // High Score Banner Badge
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF132A3E).copy(alpha = 0.85f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .border(1.dp, Color(0xFFFFD54F).copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .shadow(8.dp, RoundedCornerShape(20.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Personal Best Score",
                        tint = Color(0xFFFFD54F),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "BEST DEPTH: ",
                        color = Color.LightGray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$highScore meters",
                        color = Color(0xFFFFD54F),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // Central Navigation Actions
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Button(
                    onClick = { onNavigate(GameState.CHARACTER_SELECT) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("start_game_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00E676),
                        contentColor = Color(0xFF051421)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Start Game", modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DIVE INTO OCEAN",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Button(
                    onClick = { onNavigate(GameState.HIGH_SCORES) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("leaderboard_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00C3F7),
                        contentColor = Color(0xFF051421)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Menu, contentDescription = "Leaderboard", modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "LEADERBOARD",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Button(
                    onClick = { onNavigate(GameState.HELP) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("help_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE91E63),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = "Help Guide", modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "HOW TO PLAY & POWER-UPS",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
fun CharacterSelectScreen(
    selected: CharacterType,
    onSelect: (CharacterType) -> Unit,
    onStartGame: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    text = "SELECT YOUR CREATURE",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Text(
                text = "Each animal has balanced swimming features fit for the corals!",
                fontSize = 12.sp,
                color = Color.LightGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp)
            )
        }

        // Horizontal selections
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            CharacterType.values().forEach { type ->
                val isSel = type == selected
                val borderCol = if (isSel) Color(0xFF00E5FF) else Color.Transparent
                val shadowEl = if (isSel) 8.dp else 2.dp
                val cardColor = if (isSel) Color(0xFF163C57) else Color(0xFF0F2638)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { onSelect(type) }
                        .border(
                            width = if (isSel) 2.dp else 1.dp,
                            color = if (isSel) Color(0xFF00E5FF) else Color(0xFF00E5FF).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tiny Preview Render
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(type.primaryColor.copy(alpha = 0.2f))
                                .border(1.dp, type.primaryColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            val symbol = when (type) {
                                CharacterType.SUBMARINE -> "💛"
                                CharacterType.PUFFERFISH -> "🐡"
                                CharacterType.TURTLE -> "🐢"
                                CharacterType.MANTA_RAY -> "🐟"
                            }
                            Text(text = symbol, fontSize = 24.sp)
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = type.displayName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) Color(0xFF00E5FF) else Color.White
                            )
                            Text(
                                text = type.description,
                                fontSize = 11.sp,
                                color = Color.LightGray,
                                maxLines = 2
                            )
                        }

                        if (isSel) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }

        // Start Diving Button
        Button(
            onClick = onStartGame,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("launch_dive_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00E676),
                contentColor = Color(0xFF051421)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "DIVE WITH ${selected.displayName.uppercase()}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.PlayArrow, contentDescription = "Swim starts")
            }
        }
    }
}

@Composable
fun GameOverScreen(
    score: Int,
    highScore: Int,
    onSubmit: (String) -> Unit,
    onRestart: () -> Unit,
    onQuit: () -> Unit
) {
    var nameInput by remember { mutableStateOf("") }
    val isNewRecord = score >= highScore && score > 0
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Big Game over title Header
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "CRASHED!",
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFE91E63),
                style = androidx.compose.ui.text.TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.5f),
                        offset = Offset(2f, 4f),
                        blurRadius = 6f
                    )
                )
            )
            Text(
                text = "Your buoyancy tank burst on the colorful corals!",
                fontSize = 12.sp,
                color = Color.LightGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Leaderboard metrics stats Box
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0E2233).copy(alpha = 0.9f)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "SCORE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.LightGray
                        )
                        Text(
                            text = "$score",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF00E5FF)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "RECORD",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.LightGray
                        )
                        Text(
                            text = "$highScore",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFFD54F)
                        )
                    }
                }

                if (isNewRecord) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFFD54F).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFFFD54F), RoundedCornerShape(8.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🏆 ", fontSize = 16.sp)
                            Text(
                                text = "NEW UNDERWATER RECORD!",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFFD54F)
                            )
                        }
                    }
                }
            }
        }

        // Leaderboard registration details
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1926).copy(alpha = 0.8f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SAVE YOUR ADVENTURE",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.LightGray
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = nameInput,
                    onValueChange = { if (it.length <= 15) nameInput = it },
                    placeholder = { Text("Your initials/name", fontSize = 13.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("score_name_input"),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF0E2233),
                        unfocusedContainerColor = Color(0xFF0E2233),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        disabledTextColor = Color.Gray,
                        focusedIndicatorColor = Color(0xFF00E5FF),
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        onSubmit(nameInput)
                    })
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        onSubmit(nameInput)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("submit_score_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "SUBMIT RECORD LEADERBOARD", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF051421))
                }
            }
        }

        // Action controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onQuit,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("menu_quit_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF162D3F)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "MAIN MENU", fontWeight = FontWeight.Bold, color = Color.White)
            }

            Button(
                onClick = onRestart,
                modifier = Modifier
                    .weight(1.3f)
                    .height(48.dp)
                    .testTag("retry_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "RE-DIVE", fontWeight = FontWeight.Bold, color = Color(0xFF051421))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun LeaderboardScreen(
    topScores: List<HighScoreEntity>,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                text = "AQUA DEPTH LEADERBOARD",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (topScores.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "🌊", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "The deep sea remains unexplored!",
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Complete a dive to record your high score.",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(topScores.size) { index ->
                    val entity = topScores[index]
                    val rankColor = when (index) {
                        0 -> Color(0xFFFFD54F) // Golden Medal
                        1 -> Color(0xFFB0BEC5) // Silver Medal
                        2 -> Color(0xFFFF8A65) // Bronze Medal
                        else -> Color(0xFF00E5FF).copy(alpha = 0.3f)
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D2132)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, rankColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Rank Number Badge
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(rankColor.copy(alpha = 0.2f))
                                    .border(1.5.dp, rankColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = rankColor
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entity.playerName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                val date = Date(entity.timestamp)
                                val format = SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault())
                                Text(
                                    text = format.format(date),
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }

                            Text(
                                text = "${entity.score}m",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF00E676)
                            )
                        }
                    }
                }
            }
        }

        Button(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF132D42)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(text = "BACK TO MENU", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun HelpInstructionScreen(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    text = "HOW TO SURVIVE THE OCEAN",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D2539)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp).border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🕹️ CORE CONTROLS",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF)
                    )
                    Text(
                        text = "• Tap anywhere on the screen to trigger upward hydraulic buoyancy.\n" +
                               "• Steer between the openings in the colorful coral archways.\n" +
                               "• Do not hit the ocean floor. Hitting the seabed is game over!",
                        fontSize = 12.sp,
                        color = Color.LightGray,
                        modifier = Modifier.padding(top = 8.dp),
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "🐟 CRITTERS & POWER-UPS",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.align(Alignment.Start).padding(start = 4.dp, bottom = 4.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(PowerUpType.values().size) { idx ->
                    val type = PowerUpType.values()[idx]
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF081C2B)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, type.bubbleColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(type.bubbleColor.copy(alpha = 0.2f))
                                    .border(2.dp, type.bubbleColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = type.iconSymbol, fontSize = 20.sp)
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = type.displayName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = type.bubbleColor
                                )
                                Text(
                                    text = type.description,
                                    fontSize = 11.sp,
                                    color = Color.LightGray
                                )
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF132D42)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(text = "BACK TO ADVENTURE", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ActiveGamePlayScreen(
    viewModel: GameViewModel
) {
    val playerY by viewModel.playerY.collectAsStateWithLifecycle()
    val playerRadius by viewModel.playerRadius.collectAsStateWithLifecycle()
    val score by viewModel.score.collectAsStateWithLifecycle()
    val bubbleScore by viewModel.bubbleScore.collectAsStateWithLifecycle()
    val isInvincible by viewModel.isInvincible.collectAsStateWithLifecycle()
    val isFlickerOn by viewModel.isInvincibleFlickerOn.collectAsStateWithLifecycle()

    val corals by viewModel.corals.collectAsStateWithLifecycle()
    val powerUps by viewModel.powerUps.collectAsStateWithLifecycle()
    val collectibles by viewModel.collectibles.collectAsStateWithLifecycle()
    val backgroundBubbles by viewModel.backgroundBubbles.collectAsStateWithLifecycle()
    val activePowerUp by viewModel.activePowerUp.collectAsStateWithLifecycle()
    val powerUpTimeLeft by viewModel.powerUpTimeLeft.collectAsStateWithLifecycle()
    val selectedCharacter by viewModel.selectedCharacter.collectAsStateWithLifecycle()

    // Sound-less score increase expand visual trigger
    val lastScoreIncrTime by viewModel.lastScoreIncreaseTime.collectAsStateWithLifecycle()
    val scoreAnimateScale by animateFloatAsState(
        targetValue = if (System.currentTimeMillis() - lastScoreIncrTime < 250) 1.25f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy),
        label = "scoreScale"
    )

    val characterPropellerAngle = rememberInfiniteTransition(label = "animProp")
    val angleRad by characterPropellerAngle.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing)),
        label = "mprop"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        viewModel.tapBuoyancy()
                    }
                )
            }
            .testTag("tap_area_physics_trigger")
    ) {
        // Core Game elements Canvas renderer
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height

            // Scale coefficient vectors mapping our 1000x1000 grid to actual physical pixels
            val scaleX = canvasW / GameConfig.GRID_WIDTH
            val scaleY = canvasH / GameConfig.GRID_HEIGHT

            // 1. Draw Background Bubbles particles
            backgroundBubbles.forEach { bubble ->
                drawCircle(
                    color = Color.White.copy(alpha = bubble.opacity),
                    radius = bubble.radius * scaleX,
                    center = Offset(bubble.x * scaleX, bubble.y * scaleY),
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // 2. Beautiful Animated Swaying Seaweed kelp forest at bottom floor background
            for (i in 0..10) {
                val baseGridX = i * 110f
                val seaweedH = 140f + (kotlin.math.sin((System.currentTimeMillis() + i * 500) / 1000.0) * 40f).toFloat()
                val topSwayX = baseGridX + (kotlin.math.cos((System.currentTimeMillis() + i * 400) / 800.0) * 35f).toFloat()

                val path = Path().apply {
                    moveTo(baseGridX * scaleX, canvasH)
                    quadraticBezierTo(
                        (baseGridX - 30f) * scaleX, (GameConfig.GRID_HEIGHT - seaweedH / 2f) * scaleY,
                        topSwayX * scaleX, (GameConfig.GRID_HEIGHT - seaweedH) * scaleY
                    )
                    quadraticBezierTo(
                        (baseGridX + 30f) * scaleX, (GameConfig.GRID_HEIGHT - seaweedH / 2f) * scaleY,
                        (baseGridX + 40f) * scaleX, canvasH
                    )
                    close()
                }
                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF00E676).copy(alpha = 0.2f),
                            Color(0xFF1B5E20).copy(alpha = 0.6f)
                        )
                    )
                )
            }

            // 3. Draw Coral pillars (Obstacles)
            corals.forEach { coral ->
                val cw = GameConfig.CORAL_WIDTH * scaleX
                val cxInPixels = coral.x * scaleX
                val actualGapY = coral.gapY + coral.swayOffset
                val gapSizeHalf = (coral.gapSize / 2f) * scaleY

                // Outer boundaries
                val topPillarBottomY = (actualGapY * scaleY) - gapSizeHalf
                val botPillarTopY = (actualGapY * scaleY) + gapSizeHalf

                // Draw Top Coral Pillar rectangle
                val topRect = Rect(
                    left = cxInPixels - cw / 2f,
                    right = cxInPixels + cw / 2f,
                    top = 0f,
                    bottom = topPillarBottomY
                )
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        listOf(coral.color, coral.color.copy(alpha = 0.7f), coral.color.copy(alpha = 0.5f))
                    ),
                    topLeft = Offset(topRect.left, topRect.top),
                    size = Size(topRect.width, topRect.height),
                    cornerRadius = CornerRadius(16f * scaleX, 16f * scaleY)
                )

                // Render coral branch growths/nodes so pipes look biological and colorful!
                drawCircle(
                    color = coral.color.copy(alpha = 0.9f),
                    radius = 28f * scaleX,
                    center = Offset(cxInPixels - cw / 3f, topPillarBottomY * 0.7f)
                )
                drawCircle(
                    color = coral.color,
                    radius = 18f * scaleX,
                    center = Offset(cxInPixels + cw / 3f, topPillarBottomY * 0.4f)
                )

                // Draw Bottom Coral Pillar rectangle
                val botRect = Rect(
                    left = cxInPixels - cw / 2f,
                    right = cxInPixels + cw / 2f,
                    top = botPillarTopY,
                    bottom = canvasH
                )
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        listOf(coral.color, coral.color.copy(alpha = 0.7f), coral.color.copy(alpha = 0.5f))
                    ),
                    topLeft = Offset(botRect.left, botRect.top),
                    size = Size(botRect.width, botRect.height),
                    cornerRadius = CornerRadius(16f * scaleX, 16f * scaleY)
                )

                // Bottom Coral growths
                drawCircle(
                    color = coral.color.copy(alpha = 0.9f),
                    radius = 32f * scaleX,
                    center = Offset(cxInPixels + cw / 3f, botPillarTopY + (canvasH - botPillarTopY) * 0.3f)
                )
                drawCircle(
                    color = coral.color,
                    radius = 22f * scaleX,
                    center = Offset(cxInPixels - cw / 3f, botPillarTopY + (canvasH - botPillarTopY) * 0.6f)
                )

                // Inner warning lights on swaying corals
                if (coral.isSwaying) {
                    drawCircle(
                        color = Color.Yellow,
                        radius = 8f * scaleX,
                        center = Offset(cxInPixels, topPillarBottomY - 20f * scaleY)
                    )
                    drawCircle(
                        color = Color.Yellow,
                        radius = 8f * scaleX,
                        center = Offset(cxInPixels, botPillarTopY + 20f * scaleY)
                    )
                }
            }

            // 4. Draw Collectibles (Pearls inside Coral gaps)
            collectibles.forEach { coll ->
                if (!coll.isCollected) {
                    val cx = coll.x * scaleX
                    val cy = coll.y * scaleY
                    val cr = 18f * scaleX * coll.pulseScale

                    // Outer Pearl halo glow
                    drawCircle(
                        color = Color(0xFFFFD54F).copy(alpha = 0.25f),
                        radius = cr * 1.5f,
                        center = Offset(cx, cy)
                    )

                    // Pearl body
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White, Color(0xFFFFD54F)),
                            center = Offset(cx - cr*0.3f, cy - cr*0.3f),
                            radius = cr
                        ),
                        radius = cr,
                        center = Offset(cx, cy)
                    )

                    // Tiny reflection sparkle
                    drawCircle(
                        color = Color.White,
                        radius = cr * 0.25f,
                        center = Offset(cx - cr * 0.3f, cy - cr * 0.3f)
                    )
                }
            }

            // 5. Draw Power-up Items floating around
            powerUps.forEach { pu ->
                if (!pu.isCollected) {
                    val pX = pu.x * scaleX
                    val pY = pu.y * scaleY
                    val pRadius = 32f * scaleX
                    val glowCol = pu.type.bubbleColor

                    // Translucent bubble orb
                    drawCircle(
                        color = glowCol.copy(alpha = 0.15f),
                        radius = pRadius * 1.3f,
                        center = Offset(pX, pY)
                    )
                    drawCircle(
                        color = glowCol.copy(alpha = 0.7f),
                        radius = pRadius,
                        style = Stroke(width = 2.5f * scaleX),
                        center = Offset(pX, pY)
                    )

                    // Draw concentric rings inside the bubble
                    drawCircle(
                        color = glowCol.copy(alpha = 0.3f),
                        radius = pRadius * 0.7f,
                        style = Stroke(width = 1f * scaleX),
                        center = Offset(pX, pY)
                    )
                }
            }

            // 6. Draw Player Sea Creature
            val bx = GameConfig.BIRD_X * scaleX
            val by = playerY * scaleY
            val br = playerRadius * scaleX

            // Handle post-shield collision invincibility flashing
            val shouldRenderPlayer = !isInvincible || !isFlickerOn

            if (shouldRenderPlayer) {
                // Background shadow glow based on active buffs
                val buffGlow = when (activePowerUp) {
                    PowerUpType.SHIELD -> Color(0xFF00E676)
                    PowerUpType.SHRINK -> Color(0xFFE91E63)
                    PowerUpType.SLOW_MO -> Color(0xFF9C27B0)
                    PowerUpType.MAGNET -> Color(0xFFFFD54F)
                    else -> null
                }
                
                buffGlow?.let {
                    drawCircle(
                        color = it.copy(alpha = 0.35f),
                        radius = br * 1.5f,
                        center = Offset(bx, by)
                    )
                    drawCircle(
                        color = it,
                        radius = br * 1.25f,
                        style = Stroke(width = 2.dp.toPx()),
                        center = Offset(bx, by)
                    )
                }

                // Render customized creature vectors based on selected character type
                when (selectedCharacter) {
                    CharacterType.SUBMARINE -> {
                        // Submarine body capsule
                        val subBrush = Brush.linearGradient(
                            colors = listOf(Color(0xFFFFD54F), Color(0xFFF57C00))
                        )
                        drawRoundRect(
                            brush = subBrush,
                            topLeft = Offset(bx - br, by - br * 0.75f),
                            size = Size(br * 2f, br * 1.5f),
                            cornerRadius = CornerRadius(br * 0.75f, br * 0.75f)
                        )

                        // Periscope dome on top
                        val periPath = Path().apply {
                            moveTo(bx - br * 0.2f, by - br * 0.75f)
                            lineTo(bx - br * 0.2f, by - br * 1.2f)
                            lineTo(bx + br * 0.3f, by - br * 1.2f)
                            lineTo(bx + br * 0.3f, by - br * 1.0f)
                            lineTo(bx + br * 0.0f, by - br * 1.0f)
                            lineTo(bx + br * 0.0f, by - br * 0.75f)
                            close()
                        }
                        drawPath(path = periPath, color = Color(0xFFF57C00))
                        drawCircle(color = Color(0xFF00E5FF), radius = br * 0.15f, center = Offset(bx + br * 0.2f, by - br * 1.1f))

                        // Dome Glass window
                        drawCircle(color = Color(0xFF0C1926), radius = br * 0.35f, center = Offset(bx + br * 0.2f, by))
                        drawCircle(color = Color(0xFF00E5FF), radius = br * 0.28f, center = Offset(bx + br * 0.2f, by))
                        drawCircle(color = Color.White, radius = br * 0.08f, center = Offset(bx + br * 0.12f, by - br * 0.1f))

                        // Propeller propeller at the tail / back
                        val propX = bx - br
                        val angleRadVal = Math.toRadians(angleRad.toDouble()).toFloat()
                        val propLen = br * 0.6f
                        drawLine(
                            color = Color(0xFF455A64),
                            start = Offset(propX, by),
                            end = Offset(propX - br * 0.15f, by),
                            strokeWidth = 4f * scaleX
                        )
                        drawLine(
                            color = Color(0xFF37474F),
                            start = Offset(propX - br * 0.15f, by - propLen * kotlin.math.sin(angleRadVal)),
                            end = Offset(propX - br * 0.15f, by + propLen * kotlin.math.sin(angleRadVal)),
                            strokeWidth = 6f * scaleX
                        )
                    }
                    CharacterType.PUFFERFISH -> {
                        // Spiky circle puffer body
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFFFB74D), Color(0xFFE65100))
                            ),
                            radius = br,
                            center = Offset(bx, by)
                        )

                        // Fun drawing little spikes pointing outward
                        for (i in 0..12) {
                            val angle = Math.toRadians((i * 30).toDouble())
                            val sx = bx + (br * kotlin.math.cos(angle)).toFloat()
                            val sy = by + (br * kotlin.math.sin(angle)).toFloat()
                            val ex = bx + ((br + 12f * scaleX) * kotlin.math.cos(angle)).toFloat()
                            val ey = by + ((br + 12f * scaleX) * kotlin.math.sin(angle)).toFloat()
                            drawLine(
                                color = Color(0xFFE65100),
                                start = Offset(sx, sy),
                                end = Offset(ex, ey),
                                strokeWidth = 3f * scaleX
                            )
                        }

                        // Big funny eye
                        drawCircle(color = Color.White, radius = br * 0.28f, center = Offset(bx + br * 0.4f, by - br * 0.2f))
                        drawCircle(color = Color.Black, radius = br * 0.14f, center = Offset(bx + br * 0.45f, by - br * 0.2f))
                        drawCircle(color = Color.White, radius = br * 0.05f, center = Offset(bx + br * 0.42f, by - br * 0.25f))

                        // Mouth
                        val mouthPath = Path().apply {
                            moveTo(bx + br * 0.5f, by + br * 0.15f)
                            quadraticBezierTo(bx + br * 0.7f, by + br * 0.25f, bx + br * 0.5f, by + br * 0.35f)
                        }
                        drawPath(path = mouthPath, color = Color(0xFF4E342E), style = Stroke(width = 2.5f * scaleX))

                        // Little tail fin
                        val tailPath = Path().apply {
                            moveTo(bx - br, by)
                            lineTo(bx - br * 1.4f, by - br * 0.5f)
                            lineTo(bx - br * 1.2f, by)
                            lineTo(bx - br * 1.4f, by + br * 0.5f)
                            close()
                        }
                        drawPath(path = tailPath, color = Color(0xFFE65100))
                    }
                    CharacterType.TURTLE -> {
                        // Green shell plate
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF81C784), Color(0xFF2E7D32))
                            ),
                            radius = br,
                            center = Offset(bx, by)
                        )

                        // Shell mosaic details
                        drawCircle(
                            color = Color(0xFF1B5E20).copy(alpha = 0.4f),
                            radius = br * 0.7f,
                            center = Offset(bx, by),
                            style = Stroke(width = 2f * scaleX)
                        )
                        for (i in 0..5) {
                            val angle = Math.toRadians((i * 60).toDouble())
                            drawLine(
                                color = Color(0xFF1B5E20).copy(alpha = 0.4f),
                                start = Offset(bx, by),
                                end = Offset(bx + (br * kotlin.math.cos(angle)).toFloat(), by + (br * kotlin.math.sin(angle)).toFloat()),
                                strokeWidth = 2f * scaleX
                            )
                        }

                        // Turtle little green head flapping forward
                        val headX = bx + br * 0.9f
                        val headY = by - br * 0.1f
                        drawCircle(color = Color(0xFF66BB6A), radius = br * 0.35f, center = Offset(headX, headY))
                        drawCircle(color = Color.Black, radius = br * 0.05f, center = Offset(headX + br*0.15f, headY - br*0.1f))

                        // Front moving paddle flipper
                        val flipAngle = kotlin.math.sin(System.currentTimeMillis() / 200.0).toFloat() * 18f
                        val flipperPath = Path().apply {
                            moveTo(bx + br * 0.2f, by + br * 0.4f)
                            lineTo(bx + br * 0.5f + flipAngle * scaleX, by + br * 1.1f)
                            lineTo(bx - br * 0.1f + flipAngle * scaleX, by + br * 0.8f)
                            close()
                        }
                        drawPath(path = flipperPath, color = Color(0xFF4CAF50))
                    }
                    CharacterType.MANTA_RAY -> {
                        // Aero dynamic winged sea manta
                        val mantaColor = Color(0xFF4FC3F7)
                        val wingYOffset = kotlin.math.sin(System.currentTimeMillis() / 150.0).toFloat() * 15f * scaleY

                        val mantaPath = Path().apply {
                            // Nose head tip
                            moveTo(bx + br * 1.3f, by)
                            // Left wide sweeping wing tip
                            quadraticBezierTo(
                                bx + br * 0.4f, by - br * 0.3f,
                                bx - br * 0.2f, by - br * 1.4f - wingYOffset
                            )
                            quadraticBezierTo(
                                bx - br * 0.4f, by - br * 0.5f,
                                bx - br * 1.1f, by // Back spine
                            )
                            // Right sweeping wing tip
                            quadraticBezierTo(
                                bx - br * 0.4f, by + br * 0.5f,
                                bx - br * 0.2f, by + br * 1.4f + wingYOffset
                            )
                            quadraticBezierTo(
                                bx + br * 0.4f, by + br * 0.3f,
                                bx + br * 1.3f, by
                            )
                            close()
                        }
                        drawPath(
                            path = mantaPath,
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF81D4FA), Color(0xFF0288D1))
                            )
                        )

                        // Stringy cute whip tail
                        drawLine(
                            color = Color(0xFF0288D1),
                            start = Offset(bx - br * 1.1f, by),
                            end = Offset(bx - br * 2.3f, by + (wingYOffset * 0.5f)),
                            strokeWidth = 3f * scaleX
                        )

                        // Tiny glowing eyes
                        drawCircle(color = Color.White, radius = 4f * scaleX, center = Offset(bx + br * 0.9f, by - br * 0.2f))
                        drawCircle(color = Color.White, radius = 4f * scaleX, center = Offset(bx + br * 0.9f, by + br * 0.2f))
                    }
                }
            }
        }

        // Active Power-up overlay emoji float icons on-screen corresponding to coordinates
        powerUps.forEach { pu ->
            if (!pu.isCollected) {
                Box(
                    modifier = Modifier
                        .offset(
                            x = (pu.x - 18f).dp,
                            y = (pu.y - 18f).dp
                        )
                        .size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = pu.type.iconSymbol, fontSize = 18.sp)
                }
            }
        }

        // 7. HUD Screen metrics Overlay
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Score metric indicator with bounce scale animation
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF091722).copy(alpha = 0.85f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DEPTH",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.LightGray
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$score",
                            fontSize = (18.sp.value * scoreAnimateScale).sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF00E5FF)
                        )
                        Text(
                            text = "m",
                            fontSize = 12.sp,
                            color = Color(0xFF00E5FF),
                            modifier = Modifier.align(Alignment.Bottom)
                        )
                    }
                }

                // Bubble collection pearls score indicator
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF091722).copy(alpha = 0.85f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.border(1.dp, Color(0xFFFFD54F).copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "✨", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "PEARLS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.LightGray
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$bubbleScore",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFFD54F)
                        )
                    }
                }
            }

            // Power-up count down HUD indicator bars
            activePowerUp?.let { pu ->
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0E2233).copy(alpha = 0.9f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(10.dp))
                        .border(1.dp, pu.bubbleColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(pu.bubbleColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = pu.iconSymbol, fontSize = 13.sp)
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = pu.displayName.uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = pu.bubbleColor
                                )
                                Text(
                                    text = "${String.format("%.1f", powerUpTimeLeft / 1000.0)}s",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.LightGray
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            // Visual horizontal timeline fill percent bar
                            val progress = (powerUpTimeLeft.toFloat() / GameConfig.POWER_UP_DURATION.toFloat()).coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color.DarkGray.copy(alpha = 0.5f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progress)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(pu.bubbleColor)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Tap helper overlay prompt initially visible for first few levels
        if (score < 1) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 120.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(30.dp),
                    modifier = Modifier.border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.3f), RoundedCornerShape(30.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(text = "👆", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tap screen to swim & floats!",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
