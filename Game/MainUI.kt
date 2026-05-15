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

val ColorBackground = Color(0xFF070907)
val ColorTerminalGreen = Color(0xFF33FF33)
val ColorTerminalDim = Color(0xFF195419)
val ColorRustRed = Color(0xFF8B0000)
val ColorGunWood = Color(0xFF2B1D19)
val ColorGunSteel = Color(0xFF1C201C)

val TerminalFont = FontFamily.Monospace

@Composable
fun TerminalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isDangerous: Boolean = false
) {
    val activeColor = if (isDangerous) ColorRustRed else ColorTerminalGreen
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
            fontFamily = TerminalFont,
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
            color = if (isError) ColorRustRed else ColorTerminalGreen,
            fontFamily = TerminalFont,
            fontSize = 18.sp
        ),
        colors = TextFieldDefaults.textFieldColors(
            textColor = ColorTerminalGreen,
            backgroundColor = Color(0xFF0F140F),
            cursorColor = ColorTerminalGreen,
            focusedIndicatorColor = ColorTerminalGreen,
            unfocusedIndicatorColor = ColorTerminalDim,
            errorIndicatorColor = ColorRustRed,
            errorCursorColor = ColorRustRed
        ),
        singleLine = true,
        shape = RoundedCornerShape(0.dp),
        modifier = modifier.border(1.dp, if (isError) ColorRustRed else ColorTerminalDim)
    )
}
@Composable
fun MainAppContainer() {
    val dbService = remember { StatisticsService() }

    var playerCount by remember { mutableStateOf(0) }
    val playerNames = remember { mutableStateListOf<String>() }
    val loggedInPlayers = remember { mutableStateListOf<Player>() }
    var showLeaderboard by remember { mutableStateOf(false) }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(ColorBackground)
        .drawBehind {
            val lineSpacing = 6f
            var y = 0f
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
        when {
            showLeaderboard -> {
                LeaderboardScreen(
                    dbService = dbService,
                    onBack = { showLeaderboard = false }
                )
            }

            playerCount == 0 -> {
                SelectionScreen(onSelected = { playerCount = it })
                TerminalButton(
                    text = "VIEW PLAYER RATINGS",
                    onClick = { showLeaderboard = true },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp)
                )
            }

            loggedInPlayers.size < playerCount -> {
                LoginScreen(
                    playerNum = loggedInPlayers.size + 1,
                    dbService = dbService,
                    alreadyExist = loggedInPlayers,
                    onLogin = { name ->
                        dbService.getOrCreateStats(name)
                        playerNames.add(name)
                        loggedInPlayers.add(Player(name = name, initialHealth = 4))
                    }
                )
            }

            else -> {
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
                            val winner = loggedInPlayers.find { it.health > 0 }
                            winner?.let {
                                dbService.updateStats(it.name, loggedInPlayers.map { p -> p.name })
                            }
                        }
                        // Передача события во ViewModel
                        viewModel.handleEvent(event)
                    }
                    session.startRound()
                }

                TableScreen(
                    viewModel = viewModel,
                    dbService = dbService,
                    onResetToMenu = {
                        loggedInPlayers.clear()
                        playerCount = 0
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

@Composable
fun SelectionScreen(onSelected: (Int) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "HOW MANY SOULS WILL ENTER?",
            color = ColorRustRed,
            fontFamily = TerminalFont,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "THE DISTRIBUTOR REQUIRES LIVES.",
            color = ColorTerminalDim,
            fontFamily = TerminalFont,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(48.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            (2..4).forEach { count ->
                TerminalButton(
                    text = "[ $count PLAYERS ]",
                    onClick = { onSelected(count) }
                )
            }
        }
    }
}

@Composable
fun LoginScreen(
    playerNum: Int,
    onLogin: (String) -> Unit,
    dbService: StatisticsService,
    alreadyExist: List<Player>
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
            color = ColorTerminalDim,
            fontFamily = TerminalFont,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "ENTER YOUR NAME TO SIGN THE WAIVER",
            color = ColorTerminalGreen,
            fontFamily = TerminalFont,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.height(40.dp))

        TerminalTextField(
            value = name,
            onValueChange = { name = it },
            isError = isDuplicate,
            modifier = Modifier.width(360.dp)
        )

        if (isDuplicate) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "ERROR: IDENTITY DUPLICATION DETECTED.",
                color = ColorRustRed,
                fontFamily = TerminalFont,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(modifier = Modifier.height(100.dp).padding(top = 24.dp)) {
            if (stats != null && !isDuplicate) {
                Column(
                    modifier = Modifier
                        .width(360.dp)
                        .border(1.dp, ColorTerminalDim)
                        .background(Color(0xFF0F140F))
                        .padding(12.dp)
                ) {
                    Text("RETRIEVING DOSSIER...", color = ColorTerminalDim, fontFamily = TerminalFont, fontSize = 11.sp)
                    Text("PAST ENCOUNTERS: ${stats.totalGames} GAMES", color = ColorTerminalGreen, fontFamily = TerminalFont, fontSize = 14.sp)
                    Text(
                        text = "SURVIVAL RATE: ${(stats.calculateWinRate() * 100).toInt()}%",
                        color = if (stats.calculateWinRate() >= 0.5) ColorTerminalGreen else ColorRustRed,
                        fontFamily = TerminalFont,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        TerminalButton(
            text = "SIGN THE CONTRACT",
            onClick = {
                if (name.isNotBlank() && !isDuplicate) {
                    onLogin(name)
                    name = ""
                }
            },
            enabled = name.isNotBlank() && !isDuplicate
        )
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
                modifier = Modifier.align(Alignment.Center).zIndex(5f).border(2.dp, ColorRustRed).background(Color.Black).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("MATCH OVER // SYSTEM HALTED", color = ColorRustRed, fontFamily = TerminalFont, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                TerminalButton(text = "EXECUTE REMATCH", onClick = onRematch, modifier = Modifier.width(240.dp))
                TerminalButton(text = "RETURN TO MENU", onClick = onResetToMenu, modifier = Modifier.width(240.dp), isDangerous = true)
            }
        } else if (state.infoMessage.isNotEmpty()) {
            Box(modifier = Modifier.align(Alignment.Center).padding(bottom = 200.dp).border(1.dp, ColorTerminalGreen).background(Color(0xFF0A100A)).padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(text = ">> ${state.infoMessage.uppercase()}", color = ColorTerminalGreen, fontFamily = TerminalFont, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .width(280.dp)
                .border(1.dp, ColorTerminalDim)
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(12.dp)
        ) {
            Text("CRITICAL_LOG.TXT", color = ColorTerminalDim, fontFamily = TerminalFont, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.6.dp))

            // Алгоритм фильтрации: убирает дубликат, если он идет сразу за точно таким же логом
            val filteredLogs = remember(state.logs) {
                state.logs.fold(mutableListOf<String>()) { acc, current ->
                    if (acc.isEmpty() || acc.last() != current) {
                        acc.add(current)
                    }
                    acc
                }.takeLast(5)
            }

            filteredLogs.forEach { log ->
                Text(
                    text = "> $log",
                    color = ColorTerminalGreen.copy(alpha = 0.8f),
                    fontFamily = TerminalFont,
                    fontSize = 12.sp
                )
            }
        }

        if (!isMatchEnded) {
            val targetRotation = when (state.targetPlayerIdx) {
                null -> 0f
                else -> {
                    val relPos = (state.targetPlayerIdx - state.activePlayerIdx + playerCount) % playerCount
                    when (relPos) {
                        0 -> 90f
                        1 -> 0f
                        2 -> 270f
                        3 -> 180f
                        else -> 180f
                    }
                }
            }
            ShotgunView(isSawedOff = state.isShotgunSawedOff, targetRotation = targetRotation, modifier = Modifier.align(Alignment.Center))
        }

        state.players.forEachIndexed { index, player ->
            val relativePos = (index - state.activePlayerIdx + playerCount) % playerCount
            val alignment = when (relativePos) {
                0 -> Alignment.BottomCenter
                1 -> Alignment.CenterEnd
                2 -> Alignment.TopCenter
                3 -> Alignment.CenterStart
                else -> Alignment.BottomStart
            }
            val isSelected = (index == state.activePlayerIdx)

            PlayerCard(
                player = player,
                isLarge = isSelected,
                clickOnItem = { item -> viewModel.useItem(player, item) },
                modifier = Modifier
                    .align(alignment)
                    .padding(32.dp)
                    .clickable {
                        if (isMatchEnded) {
                            selectedPlayerForStats = player.name
                        } else {
                            viewModel.handlePlayerClick(player)
                        }
                    }
            )
        }

        if (selectedPlayerForStats != null && currentClickedStats != null) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)).clickable { selectedPlayerForStats = null }.zIndex(10f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.width(400.dp).border(2.dp, ColorTerminalGreen).background(ColorBackground).padding(24.dp).clickable(enabled = false) {},
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("CLASSIFIED DIRECTIVE // DOSSIER: ${selectedPlayerForStats!!.uppercase()}", color = ColorTerminalGreen, fontFamily = TerminalFont, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Divider(color = ColorTerminalDim, modifier = Modifier.fillMaxWidth())
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("TOTAL OPERATIONS: ${currentClickedStats.totalGames}", color = Color.White, fontFamily = TerminalFont)
                        Text("CONFIRMED SURVIVALS: ${currentClickedStats.wins}", color = ColorTerminalGreen, fontFamily = TerminalFont)
                        Text("SURVIVAL RATE: ${currentClickedStats.formattedWinRate}", color = Color.Gray, fontFamily = TerminalFont)
                    }
                    Spacer(Modifier.height(8.dp))
                    TerminalButton(text = "DISMISS PROFILE", onClick = { selectedPlayerForStats = null }, modifier = Modifier.fillMaxWidth())
                }
            }
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
            color = ColorRustRed,
            fontFamily = TerminalFont,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(Modifier.height(32.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ColorTerminalDim)
                .background(Color(0xFF0F140F))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("SUBJECT ID", color = ColorTerminalDim, fontFamily = TerminalFont, modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold)
            Text("SURVIVALS", color = ColorTerminalDim, fontFamily = TerminalFont, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Text("EFFICIENCY", color = ColorTerminalDim, fontFamily = TerminalFont, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().border(1.dp, ColorTerminalDim)
        ) {
            items(allStats) { (name, stats) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            drawLine(ColorTerminalDim, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1f)
                        }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(name.uppercase(), color = ColorTerminalGreen, fontFamily = TerminalFont, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                    Text("${stats.wins}", color = Color.White, fontFamily = TerminalFont, modifier = Modifier.weight(1f))
                    Text(stats.formattedWinRate, color = Color.Gray, fontFamily = TerminalFont, modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        TerminalButton(text = "RETURN TO ENCOUNTER", onClick = onBack)
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
    val barrelLength by animateDpAsState(if (isSawedOff) 60.dp else 130.dp)

    Column(
        modifier = modifier.graphicsLayer(rotationZ = animatedRotation),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.border(1.dp, ColorTerminalDim).background(Color.Black).padding(4.dp)
        ) {
            Box(Modifier.size(45.dp, 24.dp).background(ColorGunWood).border(1.dp, Color.Black))
            Box(Modifier.size(20.dp, 16.dp).background(Color.DarkGray))
            Box(Modifier.size(barrelLength, 12.dp).background(ColorGunSteel).border(1.dp, Color.Black))
        }
    }
}

@Composable
fun Health(health: Int, maxHealth: Int = 4) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 4.dp)
        ) {
            repeat(maxHealth) { index ->
                val isCharged = index < health
                Box(
                    modifier = Modifier
                        .border(1.dp, if (isCharged) ColorTerminalGreen else ColorTerminalDim)
                        .background(if (isCharged) Color(0xFF142B14) else Color.Transparent)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isCharged) "ON" else "-",
                        color = if (isCharged) ColorTerminalGreen else ColorTerminalDim,
                        fontFamily = TerminalFont,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
    }
}

@Composable
fun InventoryGrid(
    inventory: List<Item>,
    enabled: Boolean,
    clickOnItem: (Item) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(2) { rowIndex ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(4) { colIndex ->
                    val itemIdx = rowIndex * 4 + colIndex
                    val item = inventory.getOrNull(itemIdx)
                    val isInteractive = item != null && enabled

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .border(width = 1.dp, color = if (isInteractive) ColorTerminalGreen else ColorTerminalDim)
                            .background(if (isInteractive) Color(0xFF090D09) else Color.Black)
                            .clickable(enabled = isInteractive) { item?.let { clickOnItem(it) } },
                        contentAlignment = Alignment.Center
                    ) {
                        if (item != null) {
                            Text(
                                text = item.name.take(3).uppercase(),
                                color = if (enabled) ColorTerminalGreen else ColorTerminalDim,
                                fontFamily = TerminalFont,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        } else {
                            Text(".", color = Color(0xFF152215), fontFamily = TerminalFont, fontSize = 14.sp)
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
    val scale by animateFloatAsState(if (isLarge) 1.15f else 0.9f)
    val cardAlpha by animateFloatAsState(if (isLarge) 1f else 0.5f)

    Column(
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .alpha(cardAlpha)
            .border(
                width = if (isLarge) 2.dp else 1.dp,
                color = if (!player.isAlive) ColorRustRed else if (isLarge) ColorTerminalGreen else ColorTerminalDim
            )
            .background(if (isLarge) Color(0xFF070A07) else Color.Black)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = player.name.uppercase(),
            color = if (player.isAlive) Color.White else ColorRustRed,
            fontFamily = TerminalFont,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        Spacer(modifier = Modifier.height(8.dp))
        Health(health = player.health)
        Spacer(modifier = Modifier.height(12.dp))

        if (player.isAlive) {
            InventoryGrid(
                inventory = player.inventory,
                enabled = isLarge,
                clickOnItem = clickOnItem
            )
        } else {
            Text("SYSTEM HALTED // DEAD", color = ColorRustRed, fontFamily = TerminalFont, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        if (player.isCuffed) {
            Spacer(Modifier.height(10.dp))
            Box(modifier = Modifier.border(1.dp, ColorRustRed).background(Color(0xFF260000)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                Text("RESTRICTED // CUFFED", color = ColorRustRed, fontFamily = TerminalFont, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}