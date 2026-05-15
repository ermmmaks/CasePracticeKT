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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.draw.alpha

@Composable
fun MainAppContainer() {
    val dbService = remember { StatisticsService() }

    var playerCount by remember { mutableStateOf(0) }
    val playerNames = remember { mutableStateListOf<String>() }
    val loggedInPlayers = remember { mutableStateListOf<Player>() }
    var showLeaderboard by remember { mutableStateOf(false) }
    var rematchTrigger by remember { mutableStateOf(0) }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color(0xFF0F0F0F))
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
                Button(
                    onClick = { showLeaderboard = true },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.DarkGray)
                ) {
                    Text("VIEW PLAYER RATINGS", color = Color.White)
                }
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
                    val originalOnEvent = session.onEvent
                    session.onEvent = { event ->
                        originalOnEvent?.invoke(event)
                        if (event is GameEvent.GameOver) {
                            val winner = loggedInPlayers.find { it.health > 0 }
                            winner?.let {
                                dbService.updateStats(it.name, loggedInPlayers.map { p -> p.name })
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
        Text("HOW MANY SOULS?", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            (2..4).forEach { count ->
                Button(
                    onClick = { onSelected(count) },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2A2A2A))
                ) {
                    Text(
                        "$count",
                        color = Color.Yellow,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold)
                }
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
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "PLAYER $playerNum",
            color = Color.Gray,
            fontSize = 16.sp
        )
        Text(
            "ENTER YOUR UNIQUE NAME",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(24.dp))

        TextField(
            value = name,
            onValueChange = { name = it },
            isError = isDuplicate,
            colors = TextFieldDefaults.textFieldColors(
                textColor = Color.White,
                backgroundColor = Color(0xFF1A1A1A),
                cursorColor = Color.Yellow,
                focusedIndicatorColor = Color.Yellow,
                unfocusedIndicatorColor = Color.DarkGray
            ),
            singleLine = true,
            modifier = Modifier.width(300.dp)
        )

        if (isDuplicate) {
            Text("Come up with something original!", color = Color.Red, fontSize = 12.sp)
        }

        if (stats != null && !isDuplicate) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "RECORD: ${stats.wins} WINS / ${stats.totalGames} GAMES",
                color = Color.Yellow,
                fontSize = 14.sp,
                fontWeight = FontWeight.Light
            )
            Text(
                text = "WINRATE: ${(stats.calculateWinRate() * 100).toInt()}",
                color = Color.Gray,
                fontSize = 12.sp
            )
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = {
                if (name.isNotBlank() && !isDuplicate) {
                    onLogin(name)
                    name = ""
                }
            },
            enabled = name.isNotBlank() &&!isDuplicate,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.Yellow,
                disabledBackgroundColor = Color.DarkGray
            ),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text("SIGN THE CONTRACT", color = Color.Black, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun TableScreen(
    viewModel: ViewModel,
    dbService: StatisticsService,
    onResetToMenu: () -> Unit,
    onRematch: () -> Unit
)  {
    val state = viewModel.uiState
    val playerCount = state.players.size

    var selectedPlayerForStats by remember { mutableStateOf<String?>(null) }
    val currentClickedStats = remember(selectedPlayerForStats) {
        if (selectedPlayerForStats != null) dbService.getOrCreateStats(selectedPlayerForStats!!) else null
    }

    val isMatchEnded = state.players.count { it.isAlive } == 1

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F0F0F))) {
        if (isMatchEnded) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 220.dp)
                    .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                    .border(2.dp, Color.Red, RoundedCornerShape(12.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("MATCH OVER", color = Color.Red, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = onRematch,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.Yellow),
                    modifier = Modifier.width(220.dp)
                ) {
                    Text("REMATCH", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onResetToMenu,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.DarkGray),
                    modifier = Modifier.width(220.dp)
                ) {
                    Text("MAIN MENU", color = Color.White)
                }
            }
        } else if (state.infoMessage.isNotEmpty()) {
            Text(
                text = state.infoMessage,
                color = Color.Yellow,
                modifier = Modifier.align(Alignment.Center).padding(bottom = 180.dp),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }

        if (state.infoMessage.isNotEmpty()) {
            Text(
                text = state.infoMessage,
                color = Color.Yellow,
                modifier = Modifier.align(Alignment.Center).padding(bottom = 180.dp),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
        ) {
            Text("SESSION LOG", color = Color.DarkGray, fontSize = 10.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            state.logs.forEach { log ->
                Text(text = "> $log", color = Color.Gray, fontSize = 13.sp)
            }
        }

        if (!isMatchEnded) {
            val targetRotation = when (state.targetPlayerIdx) {
                null -> 0f
                else -> {
                    val relPos = (state.targetPlayerIdx - state.activePlayerIdx + playerCount) % playerCount
                    when (relPos) {
                        0 -> 90f // down
                        1 -> 0f // left
                        2 -> 270f // up
                        3 -> 180f // right
                        else -> 180f
                    }
                }
            }

            ShotgunView(
                isSawedOff = state.isShotgunSawedOff,
                targetRotation = targetRotation,
                modifier = Modifier.align(Alignment.Center)
            )
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
                    .padding(40.dp)
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
            AlertDialog(
                onDismissRequest = { selectedPlayerForStats = null },
                title = {
                    Text(
                        "${selectedPlayerForStats}'s CONTRACT PROFILE",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Total Wins: ${currentClickedStats.wins}")
                        Text("Total Matches: ${currentClickedStats.totalGames}", color = Color.White)
                        Text("WinRate: ${currentClickedStats.formattedWinRate}", color = Color.Gray)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { selectedPlayerForStats = null },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.DarkGray)
                    ) {
                        Text("CLOSE", color = Color.White)
                    }
                },
                backgroundColor = Color(0xFF1E1E1E),
                contentColor = Color.White
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
            text = "HALL OF FAME",
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("PLAYER", color = Color.Gray, modifier = Modifier.weight(2f))
            Text("WINS", color = Color.Gray, modifier = Modifier.weight(1f))
            Text("RATE", color = Color.Gray, modifier = Modifier.weight(1f))
        }
        Divider(color = Color.DarkGray)

        LazyColumn {
            items(allStats) { (name, stats) ->
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(name, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                    Text("${stats.wins}", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(stats.formattedWinRate, color = Color.Gray, modifier = Modifier.weight(1f))
                }
            }
        }

        Button(onClick = onBack, colors = ButtonDefaults.buttonColors(backgroundColor = Color.DarkGray)) {
            Text("BACK", color = Color.White)
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
    val barrelLength by animateDpAsState(if (isSawedOff) 70.dp else 140.dp)

    Column(
        modifier = modifier.graphicsLayer(rotationZ = animatedRotation),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(50.dp, 30.dp).background(Color(0xFF3E2723), RoundedCornerShape(4.dp)))
            Box(Modifier.size(barrelLength, 15.dp).background(Color.DarkGray))
        }
    }
}

@Composable
fun Health(health: Int, maxHealth: Int = 4) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(maxHealth) { index ->
        val isCharged = index < health
    
        Text (
            text = "⚡",
            color = if (isCharged) Color.Yellow else Color.DarkGray,
            fontSize = 28.sp
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
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(2) { rowIndex ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(4) { colIndex ->
                    val itemIdx = rowIndex * 4 + colIndex
                    val item = inventory.getOrNull(itemIdx)

                    Box (
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.DarkGray, RoundedCornerShape(6.dp))
                            .border(1.dp, Color.DarkGray, RoundedCornerShape(6.dp))
                            .clickable(enabled = item != null && enabled) { item?.let { clickOnItem(it) } },
                        contentAlignment = Alignment.Center
                    ) {
                        if (item != null) {
                            Text(
                                text = item.name.take(1),
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
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
    val scale by animateFloatAsState(if (isLarge) 1.2f else 0.85f)

    val cardAlpha by animateFloatAsState(if (isLarge) 1f else 0.6f)

    Column(
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .alpha(cardAlpha)
            .background(if (isLarge) Color(0xFF2A2A2A) else Color.Transparent, RoundedCornerShape(8.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = player.name,
            color = if (player.isAlive) Color.White else Color.Red,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )

        Spacer(modifier = Modifier.height(8.dp))
        Health(health = player.health)
        Spacer(modifier = Modifier.height(8.dp))

        InventoryGrid(
            inventory = player.inventory,
            enabled = isLarge && player.isAlive,
            clickOnItem = clickOnItem
        )

        if (player.isCuffed) {
            Spacer(Modifier.height(8.dp))
            Text("CUFFED LOL", color = Color.Cyan, fontSize = 10.sp)
        }
    }
 }