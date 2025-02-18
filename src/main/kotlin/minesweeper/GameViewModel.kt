package minesweeper

import androidx.compose.runtime.*
import kotlinx.coroutines.*
import java.util.Timer
import kotlin.concurrent.fixedRateTimer

class GameViewModel(
    difficulty: Difficulty = Difficulty.EASY,
    private val highScoreManager: HighScoreManager = HighScoreManager()
) {
    var currentDifficulty by mutableStateOf(difficulty)
        private set
        
    var board by mutableStateOf<Board>(Board(difficulty.width, difficulty.height, difficulty.mines))
        private set
    
    var gameState by mutableStateOf(GameState.ONGOING)
        private set
    
    var remainingFlags by mutableStateOf(difficulty.mines)
        private set
        
    var elapsedSeconds by mutableStateOf(0)
        private set
        
    var showHighScoreDialog by mutableStateOf(false)
        private set
        
    var topScores by mutableStateOf<List<HighScore>>(emptyList())
        private set
        
    private var timer: Timer? = null
    private var isFirstClick = true
    
    init {
        loadTopScores()
    }
    
    private fun startTimer() {
        timer?.cancel()
        elapsedSeconds = 0
        timer = fixedRateTimer(period = 1000L) {
            if (gameState == GameState.ONGOING) {
                elapsedSeconds++
            }
        }
    }
    
    private fun loadTopScores() {
        topScores = highScoreManager.getTopScores(currentDifficulty)
    }
    
    fun changeDifficulty(newDifficulty: Difficulty) {
        if (gameState == GameState.ONGOING && !isFirstClick) {
            // Don't allow changing difficulty during an active game
            return
        }
        
        currentDifficulty = newDifficulty
        resetGame()
        loadTopScores()
    }
    
    fun onCellClick(x: Int, y: Int) {
        if (gameState != GameState.ONGOING) return
        
        val cell = board.grid[x][y]
        if (cell.state == CellState.FLAGGED) return
        
        if (isFirstClick) {
            isFirstClick = false
            startTimer()
        }
        
        board.revealCell(x, y)
        gameState = board.getGameState()
        
        if (gameState == GameState.WON) {
            timer?.cancel()
            val position = highScoreManager.wouldBeTopScore(elapsedSeconds, currentDifficulty)
            if (position != null) {
                showHighScoreDialog = true
            }
        } else if (gameState == GameState.LOST) {
            timer?.cancel()
        }
    }
    
    fun onCellRightClick(x: Int, y: Int) {
        if (gameState != GameState.ONGOING) return
        
        val cell = board.grid[x][y]
        if (cell.state == CellState.REVEALED) return
        
        if (isFirstClick) {
            isFirstClick = false
            startTimer()
        }
        
        if (cell.state == CellState.HIDDEN && remainingFlags > 0) {
            board.toggleFlag(x, y)
            remainingFlags--
        } else if (cell.state == CellState.FLAGGED) {
            board.toggleFlag(x, y)
            remainingFlags++
        }
        
        gameState = board.getGameState()
        if (gameState == GameState.WON) {
            timer?.cancel()
            val position = highScoreManager.wouldBeTopScore(elapsedSeconds, currentDifficulty)
            if (position != null) {
                showHighScoreDialog = true
            }
        }
    }
    
    fun addHighScore(name: String) {
        val score = HighScore(name, elapsedSeconds, currentDifficulty)
        highScoreManager.addScore(score)
        loadTopScores()
        showHighScoreDialog = false
    }
    
    fun dismissHighScoreDialog() {
        showHighScoreDialog = false
    }
    
    fun resetGame() {
        board = Board(currentDifficulty.width, currentDifficulty.height, currentDifficulty.mines)
        gameState = GameState.ONGOING
        remainingFlags = currentDifficulty.mines
        showHighScoreDialog = false
        isFirstClick = true
        timer?.cancel()
        elapsedSeconds = 0
    }
    
    fun formatTime(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return "%02d:%02d".format(minutes, remainingSeconds)
    }
}
