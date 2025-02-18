package minesweeper

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.random.Random

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

    private fun placeMines(random: Random = Random) {
        val totalCells = width * height
        val availablePositions = (0 until totalCells).toMutableList()
        var remainingMines = mineCount

        while (remainingMines > 0) {
            val position = getRandomPosition(availablePositions, random)
            val (x, y) = calculateCoordinates(position)
            updateCell(x, y) { it.copy(hasMine = true) }
            remainingMines--
        }
    }

    private fun getRandomPosition(positions: MutableList<Int>, random: Random): Int {
        val index = random.nextInt(positions.size)
        return positions.removeAt(index)
    }

    private fun calculateCoordinates(position: Int): Pair<Int, Int> {
        val x = position / height
        val y = position % height
        return Pair(x, y)
    }

    private fun calculateAdjacentMines() {
        getNonMineCells()
            .forEach { (x, y) ->
                updateCellAdjacentMines(x, y)
            }
    }

    private fun getNonMineCells(): Sequence<Pair<Int, Int>> = sequence {
        for (x in 0 until width) {
            for (y in 0 until height) {
                if (!grid[x][y].hasMine) {
                    yield(x to y)
                }
            }
        }
    }

    private fun updateCellAdjacentMines(x: Int, y: Int) {
        val adjacentMinesCount = countAdjacentMines(x, y)
        updateCell(x, y) { it.copy(adjacentMines = adjacentMinesCount) }
    }

    private fun countAdjacentMines(x: Int, y: Int): Int {
        val adjacentOffsets = sequenceOf(
            -1 to -1, -1 to 0, -1 to 1,
            0 to -1, 0 to 1,
            1 to -1, 1 to 0, 1 to 1
        )

        return adjacentOffsets
            .map { (deltaX, deltaY) -> Coordinate(x + deltaX, y + deltaY) }
            .filter { (adjacentX, adjacentY) -> isValidPosition(adjacentX, adjacentY) }
            .count { (adjacentX, adjacentY) -> grid[adjacentX][adjacentY].hasMine }
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
        getAdjacentCoordinates(x, y)
            .filter { (newX, newY) -> isValidPosition(newX, newY) }
            .filter { (newX, newY) -> grid[newX][newY].state == CellState.HIDDEN }
            .forEach { (newX, newY) -> revealCell(newX, newY) }
    }

    private fun getAdjacentCoordinates(x: Int, y: Int): Sequence<Coordinate> = sequence {
        for (dx in -1..1) {
            for (dy in -1..1) {
                if (dx == 0 && dy == 0) continue
                yield(Coordinate(x + dx, y + dy))
            }
        }
    }

    private fun revealAllMines() {
        grid.asSequence()
            .flatMapIndexed { x, column ->
                column.mapIndexed { y, cell ->
                    Coordinate(x, y) to cell
                }
            }
            .filter { (_, cell) -> cell.hasMine }
            .forEach { (coordinate, _) ->
                updateCell(coordinate.x, coordinate.y) {
                    it.revealed()
                }
            }
    }

    private data class Coordinate(val x: Int, val y: Int)

    private fun Cell.revealed() = copy(state = CellState.REVEALED)

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
