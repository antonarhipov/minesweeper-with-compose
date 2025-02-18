package minesweeper

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.rememberWindowState

private val UnrevealedCellColor = Color(0xFFBDBDBD)
private val UnrevealedCellBorderColor = Color(0xFF9E9E9E)
private val RevealedCellColor = Color(0xFFE0E0E0)
private val RevealedCellBorderColor = Color(0xFFBDBDBD)
private val HoverCellColor = Color(0xFFCFCFCF)

@Composable
fun HighScoreDialog(
    scores: List<HighScore>,
    currentTime: Int,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var playerName by remember { mutableStateOf("") }
    
    fun formatTime(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return "%02d:%02d".format(minutes, remainingSeconds)
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.padding(16.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .width(300.dp)
            ) {
                Text(
                    "Congratulations! New High Score!",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    "Your time: ${formatTime(currentTime)}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Text(
                    "Top Scores:",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 16.dp)
                ) {
                    itemsIndexed(scores) { index, score ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${index + 1}.",
                                modifier = Modifier.width(32.dp)
                            )
                            Text(
                                score.name,
                                modifier = Modifier.weight(1f)
                            )
                            Text(formatTime(score.timeInSeconds))
                        }
                    }
                }
                
                OutlinedTextField(
                    value = playerName,
                    onValueChange = { playerName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Enter your name") },
                    singleLine = true
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onSubmit(playerName) },
                        enabled = playerName.isNotBlank()
                    ) {
                        Text("Submit")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GameBoard(viewModel: GameViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Game header with status
        Text(
            when (viewModel.gameState) {
                GameState.ONGOING -> "Game in Progress"
                GameState.WON -> "You Won! 🎉"
                GameState.LOST -> "Game Over! 💣"
            },
            color = when (viewModel.gameState) {
                GameState.ONGOING -> Color.Black
                GameState.WON -> Color(0xFF4CAF50)
                GameState.LOST -> Color(0xFFF44336)
            },
            fontWeight = if (viewModel.gameState != GameState.ONGOING) FontWeight.Bold else FontWeight.Normal,
            fontSize = if (viewModel.gameState != GameState.ONGOING) 24.sp else 18.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Game info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Flags", style = MaterialTheme.typography.titleSmall)
                Text(
                    "${viewModel.remainingFlags}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Time", style = MaterialTheme.typography.titleSmall)
                Text(
                    viewModel.formatTime(viewModel.elapsedSeconds),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Mode", style = MaterialTheme.typography.titleSmall)
                Box {
                    var expanded by remember { mutableStateOf(false) }
                    
                    Button(
                        onClick = { expanded = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (viewModel.currentDifficulty) {
                                Difficulty.EASY -> Color(0xFF4CAF50)
                                Difficulty.MEDIUM -> Color(0xFFFFA000)
                                Difficulty.HARD -> Color(0xFFF44336)
                            }
                        ),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(viewModel.currentDifficulty.displayName)
                    }
                    
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        Difficulty.entries.forEach { difficulty ->
                            DropdownMenuItem(
                                text = { Text(difficulty.displayName) },
                                onClick = {
                                    viewModel.changeDifficulty(difficulty)
                                    expanded = false
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = when (difficulty) {
                                        Difficulty.EASY -> Color(0xFF4CAF50)
                                        Difficulty.MEDIUM -> Color(0xFFFFA000)
                                        Difficulty.HARD -> Color(0xFFF44336)
                                    }
                                )
                            )
                        }
                    }
                }
            }
        }
        
        // Game grid
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val cellSize = remember {
                mutableStateOf(32.dp)
            }
            
            LaunchedEffect(viewModel.board.width, viewModel.board.height) {
                // Calculate cell size based on available space
                val screenWidth = 320.dp // Approximate minimum screen width
                val screenHeight = 480.dp // Approximate minimum screen height
                val horizontalCells = viewModel.board.width
                val verticalCells = viewModel.board.height
                
                cellSize.value = minOf(
                    (screenWidth - 32.dp) / horizontalCells,
                    (screenHeight - 200.dp) / verticalCells
                )
            }
            
            Column {
                for (x in 0 until viewModel.board.width) {
                    Row {
                        for (y in 0 until viewModel.board.height) {
                            val cell = viewModel.board.grid[x][y]
                            val interactionSource = remember { MutableInteractionSource() }
                            val isHovered by interactionSource.collectIsHoveredAsState()
                            
                            Box(
                                modifier = Modifier
                                    .size(cellSize.value)
                                    .hoverable(interactionSource)
                                    .border(
                                        width = 0.5.dp,
                                        color = if (cell.state == CellState.REVEALED) 
                                            RevealedCellBorderColor 
                                        else 
                                            UnrevealedCellBorderColor
                                    )
                                    .background(
                                        when {
                                            isHovered && cell.state != CellState.REVEALED -> HoverCellColor
                                            cell.state == CellState.REVEALED -> RevealedCellColor
                                            else -> UnrevealedCellColor
                                        }
                                    )
                                    .padding(1.dp)
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onTap = { viewModel.onCellClick(x, y) },
                                            onLongPress = { viewModel.onCellRightClick(x, y) }
                                        )
                                    }
                            ) {
                                when {
                                    cell.state == CellState.FLAGGED -> {
                                        Text(
                                            "🚩",
                                            modifier = Modifier.align(Alignment.Center),
                                            fontSize = (cellSize.value.value * 0.6f).sp
                                        )
                                    }
                                    cell.state == CellState.REVEALED && cell.hasMine -> {
                                        Text(
                                            "💣",
                                            modifier = Modifier.align(Alignment.Center),
                                            fontSize = (cellSize.value.value * 0.6f).sp
                                        )
                                    }
                                    cell.state == CellState.REVEALED && cell.adjacentMines > 0 -> {
                                        Text(
                                            text = cell.adjacentMines.toString(),
                                            modifier = Modifier.align(Alignment.Center),
                                            fontSize = (cellSize.value.value * 0.7f).sp,
                                            color = when (cell.adjacentMines) {
                                                1 -> Color.Blue
                                                2 -> Color.Green
                                                3 -> Color.Red
                                                else -> Color.Black
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Game controls
        Button(
            onClick = { viewModel.resetGame() },
            modifier = Modifier.padding(top = 16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = when (viewModel.gameState) {
                    GameState.WON -> Color(0xFF4CAF50)
                    GameState.LOST -> Color(0xFFF44336)
                    else -> MaterialTheme.colorScheme.primary
                }
            )
        ) {
            Text(
                when (viewModel.gameState) {
                    GameState.WON -> "Play Again! 🎮"
                    GameState.LOST -> "Try Again! 🔄"
                    else -> "Reset Game"
                },
                color = Color.White
            )
        }
    }
    
    if (viewModel.showHighScoreDialog) {
        HighScoreDialog(
            scores = viewModel.topScores,
            currentTime = viewModel.elapsedSeconds,
            onDismiss = { viewModel.dismissHighScoreDialog() },
            onSubmit = { name -> viewModel.addHighScore(name) }
        )
    }
}

fun main() = application {
    val viewModel = remember { GameViewModel() }
    
    Window(
        onCloseRequest = ::exitApplication,
        title = "Minesweeper",
        state = rememberWindowState(
            width = 400.dp,
            height = 600.dp
        )
    ) {
        MaterialTheme {
            GameBoard(viewModel)
        }
    }
}
