package minesweeper

import kotlin.random.Random
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class CellState {
    HIDDEN,
    REVEALED,
    FLAGGED
}

enum class GameState {
    ONGOING,
    WON,
    LOST
}

data class Cell(
    val hasMine: Boolean = false,
    val state: CellState = CellState.HIDDEN,
    val adjacentMines: Int = 0
)

class Board(
    val width: Int,
    val height: Int,
    val mineCount: Int
) {
    private var gameState = GameState.ONGOING
    private var _grid by mutableStateOf<Array<Array<Cell>>>(Array(width) { Array(height) { Cell() } })
    val grid: Array<Array<Cell>> get() = _grid
    
    init {
        require(mineCount >= 0) { "Mine count must be non-negative" }
        require(mineCount < width * height) { "Mine count must be less than total number of cells" }
        
        // Place mines randomly
        placeMines()
        
        // Calculate adjacent mines
        calculateAdjacentMines()
    }
    
    private fun placeMines() {
        var remainingMines = mineCount
        val positions = (0 until width * height).toMutableList()
        
        while (remainingMines > 0) {
            val index = Random.nextInt(positions.size)
            val position = positions.removeAt(index)
            val x = position / height
            val y = position % height
            updateCell(x, y) { it.copy(hasMine = true) }
            remainingMines--
        }
    }
    
    private fun calculateAdjacentMines() {
        for (x in 0 until width) {
            for (y in 0 until height) {
                if (!grid[x][y].hasMine) {
                    val count = countAdjacentMines(x, y)
                    updateCell(x, y) { it.copy(adjacentMines = count) }
                }
            }
        }
    }
    
    private fun countAdjacentMines(x: Int, y: Int): Int {
        var count = 0
        for (dx in -1..1) {
            for (dy in -1..1) {
                if (dx == 0 && dy == 0) continue
                val newX = x + dx
                val newY = y + dy
                if (isValidPosition(newX, newY) && grid[newX][newY].hasMine) {
                    count++
                }
            }
        }
        return count
    }
    
    private fun isValidPosition(x: Int, y: Int): Boolean {
        return x in 0 until width && y in 0 until height
    }
    
    private fun updateCell(x: Int, y: Int, update: (Cell) -> Cell) {
        val newGrid = _grid.map { it.clone() }.toTypedArray()
        newGrid[x][y] = update(newGrid[x][y])
        _grid = newGrid
        
        // Check win condition after every cell update
        checkWinCondition()
    }
    
    fun revealCell(x: Int, y: Int): Boolean {
        require(isValidPosition(x, y)) { "Invalid position" }
        
        if (grid[x][y].state != CellState.HIDDEN) return false
        
        updateCell(x, y) { it.copy(state = CellState.REVEALED) }
        
        if (grid[x][y].hasMine) {
            gameState = GameState.LOST
            revealAllMines()
            return true
        }
        
        if (grid[x][y].adjacentMines == 0) {
            // Reveal adjacent cells (flood fill)
            revealAdjacentCells(x, y)
        }
        
        return true
    }
    
    private fun revealAdjacentCells(x: Int, y: Int) {
        for (dx in -1..1) {
            for (dy in -1..1) {
                if (dx == 0 && dy == 0) continue
                val newX = x + dx
                val newY = y + dy
                if (isValidPosition(newX, newY) && grid[newX][newY].state == CellState.HIDDEN) {
                    revealCell(newX, newY)
                }
            }
        }
    }
    
    private fun revealAllMines() {
        for (x in 0 until width) {
            for (y in 0 until height) {
                if (grid[x][y].hasMine) {
                    updateCell(x, y) { it.copy(state = CellState.REVEALED) }
                }
            }
        }
    }
    
    private fun checkWinCondition() {
        // Game is won when:
        // 1. All non-mine cells are revealed
        // 2. All mine cells are either hidden or flagged
        val allNonMinesRevealed = grid.all { row ->
            row.all { cell ->
                (cell.hasMine && (cell.state == CellState.HIDDEN || cell.state == CellState.FLAGGED)) ||
                (!cell.hasMine && cell.state == CellState.REVEALED)
            }
        }
        
        if (allNonMinesRevealed) {
            gameState = GameState.WON
        }
    }
    
    fun getGameState(): GameState = gameState
    
    // Test helper function
    internal fun setMineForTesting(x: Int, y: Int) {
        // Clear all mines first
        for (i in 0 until width) {
            for (j in 0 until height) {
                updateCell(i, j) { it.copy(hasMine = false, adjacentMines = 0) }
            }
        }
        // Set the specified mine
        updateCell(x, y) { it.copy(hasMine = true) }
        // Recalculate adjacent mines
        calculateAdjacentMines()
    }
    
    fun toggleFlag(x: Int, y: Int) {
        require(isValidPosition(x, y)) { "Invalid position" }
        val cell = grid[x][y]
        
        if (cell.state == CellState.REVEALED) return
        
        updateCell(x, y) { 
            it.copy(state = if (it.state == CellState.FLAGGED) CellState.HIDDEN else CellState.FLAGGED)
        }
    }
}
