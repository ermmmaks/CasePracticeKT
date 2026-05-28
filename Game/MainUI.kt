import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.system.exitProcess
import java.util.Date
import java.text.SimpleDateFormat

// ==================== ТЕМА И СТИЛИ ====================

object GameTheme {
    // Основные цвета
    val background = Color(0xFF0F0E12)
    val terminalGreen = Color(0xFF33FF33)
    val terminalDim = Color(0xFF195419)
    val rustRed = Color(0xFFD62828)
    val orangeWarning = Color(0xFFF77F00)
    val blueTech = Color(0xFF00B4D8)
    val yellowAmber = Color(0xFFFCBF49)
    val gunWood = Color(0xFF5C3D2E)
    val gunSteel = Color(0xFF3A3F44)

    // Цвета для предметов
    fun getItemColor(item: Item): Color = when (item.name) {
        "Handsaw" -> rustRed
        "Handcuffs" -> orangeWarning
        "Cigarette" -> blueTech
        "Beer" -> yellowAmber
        else -> terminalGreen
    }

    fun getItemIcon(item: Item): String = when (item.name) {
        "Handsaw" -> "🪚"
        "Handcuffs" -> "🔗"
        "Cigarette" -> "🚬"
        "Beer" -> "🍺"
        "Magnifier" -> "🔎"
        "Phone" -> "📞"
        "Inverter" -> "🔄"
        else -> ""
    }

    // Цвет границы карточки игрока
    fun getPlayerBorderColor(isAlive: Boolean, isSelected: Boolean): Color = when {
        !isAlive -> rustRed
        isSelected -> terminalGreen
        else -> terminalDim
    }

    // Шрифт
    val terminalFont = FontFamily.Monospace
}

// ==================== EXTENSION FUNCTIONS ====================

fun Modifier.terminalPanel(borderColor: Color = GameTheme.terminalDim): Modifier = this
    .border(1.dp, borderColor)
    .background(Color(0xFF0F140F))
    .padding(12.dp)

fun Modifier.terminalCardBorder(isAlive: Boolean, isSelected: Boolean): Modifier = this
    .border(
        width = if (isSelected) 2.5.dp else 1.dp,
        color = GameTheme.getPlayerBorderColor(isAlive, isSelected)
    )

fun List<String>.deduplicateConsecutive(): List<String> {
    return buildList {
        var last: String? = null
        this@deduplicateConsecutive.forEach { current ->
            if (last != current) {
                add(current)
                last = current
            }
        }
    }.takeLast(5)
}

// ==================== ПОЗИЦИОНИРОВАНИЕ ИГРОКОВ ====================

fun getPlayerAlignment(playerCount: Int, index: Int): Alignment {
    return when (playerCount) {
        2 -> when (index) {
            0 -> Alignment.BottomCenter
            1 -> Alignment.TopCenter
            else -> Alignment.BottomCenter
        }
        3 -> when (index) {
            0 -> Alignment.BottomCenter
            1 -> Alignment.CenterEnd
            2 -> Alignment.CenterStart
            else -> Alignment.BottomCenter
        }
        else -> when (index) {
            0 -> Alignment.BottomCenter
            1 -> Alignment.CenterEnd
            2 -> Alignment.TopCenter
            3 -> Alignment.CenterStart
            else -> Alignment.BottomCenter
        }
    }
}

fun getRotationForPlayer(playerCount: Int, playerIdx: Int): Float {
    return when (playerCount) {
        2 -> if (playerIdx == 0) 90f else -90f
        3 -> when (playerIdx) {
            0 -> 90f
            1 -> 0f
            2 -> 180f
            else -> 90f
        }
        else -> when (playerIdx) {
            0 -> 90f
            1 -> 0f
            2 -> -90f
            3 -> 180f
            else -> 90f
        }
    }
}

// ==================== UI КОМПОНЕНТЫ ====================

@Composable
fun TerminalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isDangerous: Boolean = false
) {
    val activeColor = if (isDangerous) GameTheme.rustRed else GameTheme.terminalGreen
    val disabledColor = Color.DarkGray

    Box(
        modifier = modifier
            .border(width = 2.dp, color = if (enabled) activeColor else disabledColor, shape = RoundedCornerShape(0.dp))
            .background(Color.Black)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (enabled) activeColor else disabledColor,
            fontFamily = GameTheme.terminalFont,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TerminalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        isError = isError,
        textStyle = androidx.compose.ui.text.TextStyle(
            color = if (isError) GameTheme.rustRed else GameTheme.terminalGreen,
            fontFamily = GameTheme.terminalFont,
            fontSize = 18.sp
        ),
        colors = TextFieldDefaults.textFieldColors(
            textColor = GameTheme.terminalGreen,
            backgroundColor = Color(0xFF0F140F),
            cursorColor = GameTheme.terminalGreen,
            focusedIndicatorColor = GameTheme.terminalGreen,
            unfocusedIndicatorColor = GameTheme.terminalDim,
            errorIndicatorColor = GameTheme.rustRed,
            errorCursorColor = GameTheme.rustRed
        ),
        singleLine = true,
        shape = RoundedCornerShape(0.dp),
        modifier = modifier.border(1.dp, if (isError) GameTheme.rustRed else GameTheme.terminalDim)
    )
}

@Composable
fun Health(health: Int, maxHealth: Int = 4) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .border(1.5.dp, GameTheme.orangeWarning)
            .background(Color(0xFF140F07))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        repeat(maxHealth) { index ->
            val isCharged = index < health
            Text(
                text = "⚡",
                color = if (isCharged) GameTheme.yellowAmber else GameTheme.rustRed.copy(alpha = 0.15f),
                fontFamily = GameTheme.terminalFont,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun InventoryGrid(
    inventory: List<Item>,
    enabled: Boolean,
    clickOnItem: (Item) -> Unit
) {
    val allItemsState = rememberUpdatedState(inventory)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(2) { rowIndex ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(4) { colIndex ->
                    val itemIdx = rowIndex * 4 + colIndex
                    val item = allItemsState.value.getOrNull(itemIdx)
                    val isInteractive = item != null && enabled
                    val itemColor = item?.let { GameTheme.getItemColor(it) } ?: GameTheme.terminalGreen
                    val itemIcon = item?.let { GameTheme.getItemIcon(it) } ?: ""

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .border(width = 1.dp, color = if (isInteractive) itemColor else GameTheme.terminalDim)
                            .background(if (isInteractive) Color(0xFF090D09) else Color.Black)
                            .clickable(enabled = isInteractive) { item?.let { clickOnItem(it) } },
                        contentAlignment = Alignment.Center
                    ) {
                        if (item != null) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = itemIcon,
                                    fontSize = 18.sp,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                                Text(
                                    text = item.name.take(3).uppercase(),
                                    color = if (enabled) itemColor else GameTheme.terminalDim,
                                    fontFamily = GameTheme.terminalFont,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        } else {
                            Text(".", color = Color(0xFF152215), fontFamily = GameTheme.terminalFont, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerCard(
    player: PlayerUiState,
    isLarge: Boolean,
    clickOnItem: (Item) -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(if (isLarge) 1.05f else 0.9f)
    val cardAlpha by animateFloatAsState(if (isLarge) 1f else 0.5f)

    Column(
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .alpha(cardAlpha)
            .terminalCardBorder(player.isAlive, isLarge)
            .background(if (isLarge) Color(0xFF0A140D) else Color.Black)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = player.name.uppercase(),
            color = if (player.isAlive) (if (isLarge) GameTheme.terminalGreen else Color.White) else GameTheme.rustRed,
            fontFamily = GameTheme.terminalFont,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )

        Spacer(modifier = Modifier.height(12.dp))
        Health(health = player.health)
        Spacer(modifier = Modifier.height(16.dp))

        if (player.isAlive) {
            InventoryGrid(
                inventory = player.inventory,
                enabled = isLarge,
                clickOnItem = clickOnItem
            )
        } else {
            Text(
                "SYSTEM HALTED // DEAD",
                color = GameTheme.rustRed,
                fontFamily = GameTheme.terminalFont,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (player.isCuffed) {
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier.border(1.dp, GameTheme.orangeWarning).background(Color(0xFF261200))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    "RESTRICTED // CUFFED",
                    color = GameTheme.orangeWarning,
                    fontFamily = GameTheme.terminalFont,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ShotgunView(
    isSawedOff: Boolean,
    targetRotation: Float,
    modifier: Modifier = Modifier
) {
    val animatedRotation by animateFloatAsState(
        targetValue = targetRotation,
        animationSpec = spring(stiffness = Spring.StiffnessLow)
    )
    val barrelLength by animateDpAsState(if (isSawedOff) 75.dp else 165.dp)

    Box(
        modifier = modifier
            .graphicsLayer(rotationZ = animatedRotation)
            .padding(16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Canvas(
            modifier = Modifier.size(width = 300.dp, height = 44.dp)
        ) {
            val scaleX = size.width / 300.dp.toPx()
            val scaleY = size.height / 44.dp.toPx()
            val strokeWidth = 1.5.dp.toPx()

            fun drawOutlinedRect(color: Color, topLeft: Offset, size: Size) {
                drawRect(color = color, topLeft = topLeft, size = size)
                drawRect(color = Color.Black, topLeft = topLeft, size = size, style = Stroke(width = strokeWidth))
            }

            // Stock - инлайним stockWidth
            val stockHeight = 24.dp.toPx() * scaleY
            val stockY = (size.height - stockHeight) / 2
            drawOutlinedRect(
                GameTheme.gunWood,
                Offset(0f, stockY),
                Size(85.dp.toPx() * scaleX, stockHeight)
            )

            // Grip - инлайним gripX
            val gripWidth = 45.dp.toPx() * scaleX
            val gripHeight = 20.dp.toPx() * scaleY
            drawOutlinedRect(
                GameTheme.gunWood,
                Offset(85.dp.toPx() * scaleX, stockY + 4.dp.toPx() * scaleY),
                Size(gripWidth, gripHeight)
            )

            // Receiver
            val receiverWidth = 50.dp.toPx() * scaleX
            val receiverHeight = 22.dp.toPx() * scaleY
            val receiverX = (85.dp.toPx() * scaleX) + gripWidth
            val receiverY = (size.height - receiverHeight) / 2
            drawOutlinedRect(Color.DarkGray, Offset(receiverX, receiverY), Size(receiverWidth, receiverHeight))

            // Barrels
            val currentBarrelLength = barrelLength.toPx() * scaleX
            val barrelHeight = 6.dp.toPx() * scaleY
            val barrelX = receiverX + receiverWidth
            val barrelY1 = (size.height / 2) - 7.dp.toPx() * scaleY
            val barrelY2 = (size.height / 2) + 1.dp.toPx() * scaleY

            drawOutlinedRect(
                GameTheme.gunSteel,
                Offset(barrelX, barrelY1),
                Size(currentBarrelLength, barrelHeight)
            )
            drawOutlinedRect(
                GameTheme.gunSteel,
                Offset(barrelX, barrelY2),
                Size(currentBarrelLength, barrelHeight)
            )

            // Rib
            val ribHeight = 2.dp.toPx() * scaleY
            drawRect(
                color = Color.Black,
                topLeft = Offset(barrelX, barrelY1 - ribHeight),
                size = Size(currentBarrelLength, ribHeight)
            )
        }
    }
}

@Composable
fun SelectionScreen(
    onSelected: (Int) -> Unit,
    onExit: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "CHAMBER CONFIGURATION // MULTIPLAYER PROTOCOL",
            color = GameTheme.terminalDim,
            fontFamily = GameTheme.terminalFont,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "SELECT THE NUMBER OF PARTICIPANTS",
            color = GameTheme.terminalGreen,
            fontFamily = GameTheme.terminalFont,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.height(40.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            for (playersCount in 2..4) {
                TerminalButton(
                    text = "$playersCount PLAYERS",
                    onClick = { onSelected(playersCount) })
            }
        }
        Spacer(Modifier.height(32.dp))
        TerminalButton(text = "EXIT SYSTEM", onClick = onExit, isDangerous = true)
    }
}

@Composable
fun LoginScreen(
    playerNum: Int,
    onLogin: (String) -> Unit,
    dbService: StatisticsService,
    alreadyExist: List<Player>,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    val isDuplicate = alreadyExist.any { it.name.equals(name, ignoreCase = true) }
    val stats = remember(name) { if (name.isNotBlank()) dbService.getOrCreateStats(name) else null }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "SUBJECT IDENTIFICATION // PATIENT $playerNum",
            color = GameTheme.terminalDim,
            fontFamily = GameTheme.terminalFont,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "ENTER YOUR NAME TO SIGN THE WAIVER",
            color = GameTheme.terminalGreen,
            fontFamily = GameTheme.terminalFont,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.height(40.dp))

        TerminalTextField(
            value = name,
            onValueChange = { input ->
                if (input.length <= 10) {
                    name = input
                }
            },
            isError = isDuplicate,
            modifier = Modifier.width(360.dp)
        )

        if (isDuplicate) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "ERROR: IDENTITY DUPLICATION DETECTED.",
                color = GameTheme.rustRed,
                fontFamily = GameTheme.terminalFont,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(modifier = Modifier.height(100.dp).padding(top = 24.dp)) {
            if (stats != null && !isDuplicate) {
                Column(
                    modifier = Modifier
                        .width(360.dp)
                        .terminalPanel()
                ) {
                    Text("RETRIEVING DOSSIER...", color = GameTheme.terminalDim, fontFamily = GameTheme.terminalFont, fontSize = 11.sp)
                    Text(
                        "PAST ENCOUNTERS: ${stats.totalGames} GAMES",
                        color = GameTheme.terminalGreen,
                        fontFamily = GameTheme.terminalFont,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "SURVIVAL RATE: ${stats.formattedWinRate}",
                        color = if (stats.calculateWinRate() >= 0.5) GameTheme.terminalGreen else GameTheme.rustRed,
                        fontFamily = GameTheme.terminalFont,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TerminalButton(
                text = "PREVIOUS STEP",
                onClick = onBack
            )
            TerminalButton(
                text = "SIGN THE CONTRACT",
                onClick = {
                    if (name.isNotBlank() && !isDuplicate) {
                        onLogin(name.trim())
                        name = ""
                    }
                },
                enabled = name.isNotBlank() && !isDuplicate
            )
        }
    }
}

@Composable
fun LeaderboardScreen(dbService: StatisticsService, onBack: () -> Unit) {
    val allStats = remember { dbService.getLeaderboard() }

    Column(
        modifier = Modifier.fillMaxSize().padding(64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "DEALER'S REGISTRY // ARCHIVED CONTRACTS",
            color = GameTheme.rustRed,
            fontFamily = GameTheme.terminalFont,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(Modifier.height(32.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .terminalPanel(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "SUBJECT ID",
                color = GameTheme.terminalGreen,
                fontFamily = GameTheme.terminalFont,
                modifier = Modifier.weight(2f),
                fontWeight = FontWeight.Bold
            )
            Text(
                "SURVIVALS",
                color = GameTheme.terminalGreen,
                fontFamily = GameTheme.terminalFont,
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Bold
            )
            Text(
                "TOTAL GAMES",
                color = GameTheme.terminalGreen,
                fontFamily = GameTheme.terminalFont,
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Bold
            )
            Text(
                "EFFICIENCY",
                color = GameTheme.terminalGreen,
                fontFamily = GameTheme.terminalFont,
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Bold
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().border(1.dp, GameTheme.terminalDim)
        ) {
            items(allStats) { (name, stats) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            drawLine(
                                GameTheme.terminalDim,
                                Offset(0f, size.height),
                                Offset(size.width, size.height),
                                strokeWidth = 1f
                            )
                        }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        name.uppercase(),
                        color = GameTheme.terminalGreen,
                        fontFamily = GameTheme.terminalFont,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(2f)
                    )
                    Text(
                        "${stats.wins}",
                        color = Color.White,
                        fontFamily = GameTheme.terminalFont,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${stats.totalGames}",
                        color = Color.White,
                        fontFamily = GameTheme.terminalFont,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        stats.formattedWinRate,
                        color = Color.Gray,
                        fontFamily = GameTheme.terminalFont,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        TerminalButton(text = "RETURN TO ENCOUNTER", onClick = onBack)
    }
}

@Composable
fun HistoryScreen(dbService: StatisticsService, onBack: () -> Unit) {
    val history = remember { dbService.getMatchHistory() }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss") }

    Column(
        modifier = Modifier.fillMaxSize().padding(64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "ARCHIVED ENCOUNTERS // INCIDENT LOGS",
            color = GameTheme.rustRed,
            fontFamily = GameTheme.terminalFont,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(Modifier.height(32.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .terminalPanel(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "GAME #",
                color = GameTheme.terminalGreen,
                fontFamily = GameTheme.terminalFont,
                modifier = Modifier.weight(0.8f),
                fontWeight = FontWeight.Bold
            )
            Text(
                "WINNER",
                color = GameTheme.terminalGreen,
                fontFamily = GameTheme.terminalFont,
                modifier = Modifier.weight(1.2f),
                fontWeight = FontWeight.Bold
            )
            Text(
                "OTHER PARTICIPANTS",
                color = GameTheme.terminalGreen,
                fontFamily = GameTheme.terminalFont,
                modifier = Modifier.weight(2.5f),
                fontWeight = FontWeight.Bold
            )
            Text(
                "TIMESTAMP",
                color = GameTheme.terminalGreen,
                fontFamily = GameTheme.terminalFont,
                modifier = Modifier.weight(1.5f),
                fontWeight = FontWeight.Bold
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().border(1.dp, GameTheme.terminalDim)
        ) {
            items(history) { match ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            drawLine(
                                GameTheme.terminalDim,
                                Offset(0f, size.height),
                                Offset(size.width, size.height),
                                strokeWidth = 1f
                            )
                        }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MATCH_${match.gameNumber}",
                        color = Color.Gray,
                        fontFamily = GameTheme.terminalFont,
                        modifier = Modifier.weight(0.8f)
                    )

                    Text(
                        text = match.winnerName.uppercase(),
                        color = GameTheme.terminalGreen,
                        fontFamily = GameTheme.terminalFont,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1.2f)
                    )

                    Row(
                        modifier = Modifier.weight(2.5f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        match.participants.filter { it != match.winnerName }.forEach { loser ->
                            Text(
                                text = loser.uppercase(),
                                color = GameTheme.rustRed,
                                fontFamily = GameTheme.terminalFont,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Text(
                        text = dateFormat.format(Date(match.timestamp)),
                        color = Color.White,
                        fontFamily = GameTheme.terminalFont,
                        modifier = Modifier.weight(1.5f)
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        TerminalButton(text = "RETURN TO ENCOUNTER", onClick = onBack)
    }
}

@Composable
fun TableScreen(
    viewModel: ViewModel,
    dbService: StatisticsService,
    onResetToMenu: () -> Unit,
    onRematch: () -> Unit
) {
    val state = viewModel.uiState
    val playerCount = state.players.size

    var selectedPlayerForStats by remember { mutableStateOf<String?>(null) }
    val currentClickedStats = remember(selectedPlayerForStats) {
        if (selectedPlayerForStats != null) dbService.getOrCreateStats(selectedPlayerForStats!!) else null
    }

    val isMatchEnded = state.players.count { it.isAlive } == 1

    Box(modifier = Modifier.fillMaxSize()) {
        if (isMatchEnded) {
            Column(
                modifier = Modifier.align(Alignment.Center).zIndex(5f).border(2.dp, GameTheme.rustRed)
                    .background(Color.Black).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "MATCH OVER // SYSTEM HALTED",
                    color = GameTheme.rustRed,
                    fontFamily = GameTheme.terminalFont,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(8.dp))
                TerminalButton(text = "EXECUTE REMATCH", onClick = onRematch, modifier = Modifier.width(240.dp))
                TerminalButton(
                    text = "RETURN TO MENU",
                    onClick = onResetToMenu,
                    modifier = Modifier.width(240.dp),
                    isDangerous = true
                )
            }
        } else if (state.infoMessage.isNotEmpty()) {
            Box(
                modifier = Modifier.align(Alignment.Center).padding(bottom = 220.dp).border(1.dp, GameTheme.orangeWarning)
                    .background(Color(0xFF1C1308)).padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = ">> ${state.infoMessage.uppercase()}",
                    color = GameTheme.orangeWarning,
                    fontFamily = GameTheme.terminalFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }

        // Лог-панель
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .width(280.dp)
                .terminalPanel(GameTheme.terminalDim)
        ) {
            Text(
                "CRITICAL_LOG.TXT",
                color = GameTheme.terminalGreen,
                fontFamily = GameTheme.terminalFont,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.6.dp))

            val filteredLogs = remember(state.logs) {
                state.logs.deduplicateConsecutive()
            }

            filteredLogs.forEach { log ->
                Text(
                    text = "> $log",
                    color = GameTheme.terminalGreen.copy(alpha = 0.8f),
                    fontFamily = GameTheme.terminalFont,
                    fontSize = 12.sp
                )
            }
        }

        // Визуализация дробовика
        if (!isMatchEnded) {
            val targetRotation = when (state.targetPlayerIdx) {
                null -> getRotationForPlayer(playerCount, state.activePlayerIdx)
                else -> getRotationForPlayer(playerCount, state.targetPlayerIdx)
            }

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-40).dp)
            ) {
                ShotgunView(isSawedOff = state.isShotgunSawedOff, targetRotation = targetRotation)
            }
        }

        // Карточки игроков
        state.players.forEachIndexed { index, player ->
            val alignment = getPlayerAlignment(playerCount, index)
            val isSelected = (index == state.activePlayerIdx)

            PlayerCard(
                player = player,
                isLarge = isSelected,
                clickOnItem = { item -> viewModel.useItem(player, item) },
                modifier = Modifier
                    .align(alignment)
                    .padding(12.dp)
                    .clickable {
                        if (isMatchEnded) {
                            selectedPlayerForStats = player.name
                        } else {
                            viewModel.handlePlayerClick(player)
                        }
                    }
            )
        }

        // Диалог со статистикой
        if (selectedPlayerForStats != null && currentClickedStats != null) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f))
                    .clickable { selectedPlayerForStats = null }.zIndex(10f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.width(400.dp).border(2.dp, GameTheme.terminalGreen).background(GameTheme.background)
                        .padding(24.dp).clickable(enabled = false) {},
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "CLASSIFIED DIRECTIVE // DOSSIER: ${selectedPlayerForStats!!.uppercase()}",
                        color = GameTheme.terminalGreen,
                        fontFamily = GameTheme.terminalFont,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Divider(color = GameTheme.terminalDim, modifier = Modifier.fillMaxWidth())
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "TOTAL OPERATIONS: ${currentClickedStats.totalGames}",
                            color = Color.White,
                            fontFamily = GameTheme.terminalFont
                        )
                        Text(
                            "CONFIRMED SURVIVALS: ${currentClickedStats.wins}",
                            color = GameTheme.terminalGreen,
                            fontFamily = GameTheme.terminalFont
                        )
                        Text(
                            "SURVIVAL RATE: ${currentClickedStats.formattedWinRate}",
                            color = Color.Gray,
                            fontFamily = GameTheme.terminalFont
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    TerminalButton(
                        text = "DISMISS PROFILE",
                        onClick = { selectedPlayerForStats = null },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun MainAppContainer() {
    val dbService = remember { StatisticsService() }

    var playerCount by remember { mutableStateOf(0) }
    val playerNames = remember { mutableStateListOf<String>() }
    val loggedInPlayers = remember { mutableStateListOf<Player>() }
    var currentScreen by remember { mutableStateOf("menu") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GameTheme.background)
            .drawBehind {
                var y = 0f
                val lineSpacing = 6f
                while (y < size.height) {
                    drawLine(
                        color = Color.Black.copy(alpha = 0.18f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 2f
                    )
                    y += lineSpacing
                }
            }
    ) {
        when (currentScreen) {
            "leaderboard" -> {
                LeaderboardScreen(
                    dbService = dbService,
                    onBack = { currentScreen = "menu" }
                )
            }

            "history" -> {
                HistoryScreen(
                    dbService = dbService,
                    onBack = { currentScreen = "menu" }
                )
            }

            "menu" -> {
                if (playerCount == 0) {
                    SelectionScreen(
                        onSelected = { playerCount = it },
                        onExit = { exitProcess(0) }
                    )
                    Row(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        TerminalButton(
                            text = "VIEW PLAYER RATINGS",
                            onClick = { currentScreen = "leaderboard" }
                        )
                        TerminalButton(
                            text = "MATCH LOGS",
                            onClick = { currentScreen = "history" }
                        )
                    }
                } else if (loggedInPlayers.size < playerCount) {
                    LoginScreen(
                        playerNum = loggedInPlayers.size + 1,
                        dbService = dbService,
                        alreadyExist = loggedInPlayers,
                        onLogin = { name ->
                            dbService.getOrCreateStats(name)
                            playerNames.add(name)
                            loggedInPlayers.add(Player(name = name, initialHealth = 4))
                        },
                        onBack = {
                            if (loggedInPlayers.isNotEmpty()) {
                                loggedInPlayers.removeLast()
                                playerNames.removeLast()
                            } else {
                                playerCount = 0
                            }
                        }
                    )
                } else {
                    currentScreen = "table"
                }
            }

            "table" -> {
                var rematchTrigger by remember { mutableStateOf(0) }

                val currentPlayers = remember(rematchTrigger) {
                    playerNames.map { Player(name = it, initialHealth = 4) }
                }
                val session = remember(rematchTrigger) { GameSession(currentPlayers) }
                val viewModel = remember(rematchTrigger) { ViewModel(session, currentPlayers) }

                LaunchedEffect(rematchTrigger) {
                    session.onEvent = null
                    session.onEvent = { event ->
                        if (event is GameEvent.GameOver) {
                            val winner = currentPlayers.find { it.health > 0 }
                            winner?.let {
                                dbService.updateStats(it.name, currentPlayers.map { p -> p.name })
                            }
                        }
                        viewModel.handleEvent(event)
                    }
                    session.startRound()
                }

                TableScreen(
                    viewModel = viewModel,
                    dbService = dbService,
                    onResetToMenu = {
                        loggedInPlayers.clear()
                        playerNames.clear()
                        playerCount = 0
                        currentScreen = "menu"
                    },
                    onRematch = {
                        session.onEvent = null
                        rematchTrigger++
                    }
                )
            }
        }
    }
}