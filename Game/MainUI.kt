import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*

@Composable
fun TableScreen(viewModel: ViewModel)
{
    val state = viewModel.uiState
    val playerCount = state.players.size

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F0F0F))) {
        val targetRotation = when (state.targetPlayerIdx) {
            null -> 0f
            else -> {
                val relPos = (state.targetPlayerIdx - state.activePlayerIdx + playerCount) % playerCount
                when (relPos) {
                    0 -> 90f // down
                    1 -> 180f // left
                    2 -> 270f // up
                    3 -> 0f // right
                    else -> 180f
                }
            }
        }

        ShotgunView (
            isSawedOff = state.isShotgunSawedOff,
            targetRotation = targetRotation,
            modifier = Modifier.align(Alignment.Center)
        )

        state.players.forEachIndexed { index, player -> 
            val relativePos = (index - state.activePlayerIdx + playerCount) % playerCount

            val aligment = when (relativePos) {
                0 -> Alignment.BottomCenter
                1 -> Alignment.CenterStart
                2 -> Alignment.TopCenter
                3 -> Alignment.CenterEnd
                else -> Alignment.BottomCenter
            }

            val isSelected = (index == state.activePlayerIdx)

            PlayerCard (
                player = player,
                isLarge = isSelected,
                clickOnItem = { item -> viewModel.useItem(player, item) },
                modifier = Modifier
                    .align(aligment)
                    .padding(16.dp)
                    .clickable { viewModel.handlePlayerClick(player) }
            )
        }
    }
}

@Composable
fun ShotgunView
(
    isSawedOff: Boolean,
    targetRotation: Float,
    modifier: Modifier = Modifier
) {
    val animatedRotation by animateFloatAsState(targetValue = targetRotation)
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
fun Health(health: Int, maxHealth: Int = 4)
{
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(maxHealth) { index ->
        val isCharged = index < health
    
        Text (
            text = "⚡",
            color = if (isCharged) Color.Yellow else Color.DarkGray,
            fontSize = 24.sp
        )
        }
    }
}

@Composable
fun InventoryGrid(inventory: List<Item>, clickOnItem: (Item) -> Unit)
{
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(2) { rowIndex ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(4) { colIndex ->
                    val itemIdx = rowIndex * 4 + colIndex
                    val item = inventory.getOrNull(itemIdx)

                    Box (
                        modifier = Modifier
                            .size(50.dp)
                            .background(Color.DarkGray, RoundedCornerShape(4.dp))
                            .clickable(enabled = item != null) { item?.let { clickOnItem(it) } },
                        contentAlignment = Alignment.Center
                    ) {
                        if (item != null) {
                            Text(item.name.take(1), color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerCard
(
    player: PlayerUiState,
    isLarge: Boolean,
    clickOnItem: (Item) -> Unit,
    modifier: Modifier = Modifier
 ) {
    val scale by animateFloatAsState(if (isLarge) 1.2f else 1.0f)

    Column (
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .background(if (isLarge) Color(0xFF2A2A2A) else Color.Transparent, RoundedCornerShape(8.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text (
            text = player.name,
            color = if (player.isAlive) Color.White else Color.Red,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))
        Health(health = player.health)
        Spacer(modifier = Modifier.height(8.dp))

        InventoryGrid(inventory = player.inventory, clickOnItem = clickOnItem)

        if (player.isCuffed) {
            Text("CUFFED LOL", color = Color.Cyan, fontSize = 10.sp)
        }
    }
 }