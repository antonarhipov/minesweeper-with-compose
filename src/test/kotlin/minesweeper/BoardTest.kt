package minesweeper

import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.assertThrows

class BoardTest {
    
    @Test
    fun `board initialization with valid parameters creates correct size board`() {
        val board = Board(width = 9, height = 9, mineCount = 10)
        
        assertThat(board.width).isEqualTo(9)
        assertThat(board.height).isEqualTo(9)
        assertThat(board.mineCount).isEqualTo(10)
        assertThat(board.grid.size).isEqualTo(9)
        assertThat(board.grid[0].size).isEqualTo(9)
    }
    
    @Test
    fun `board initialization places correct number of mines`() {
        val board = Board(width = 9, height = 9, mineCount = 10)
        
        val mineCount = board.grid.sumOf { row ->
            row.count { it.hasMine }
        }
        
        assertThat(mineCount).isEqualTo(10)
    }
    
    @Test
    fun `board initialization with invalid mine count throws exception`() {
        assertThrows<IllegalArgumentException> {
            Board(width = 9, height = 9, mineCount = 82) // More mines than cells
        }
        
        assertThrows<IllegalArgumentException> {
            Board(width = 9, height = 9, mineCount = -1)
        }
    }
    
    @Test
    fun `board initialization calculates adjacent mine counts correctly`() {
        val board = Board(width = 3, height = 3, mineCount = 1)
        // Force mine placement for testing
        board.setMineForTesting(1, 1)
        
        // Check all surrounding cells have adjacentMines = 1
        for (x in 0..2) {
            for (y in 0..2) {
                if (x == 1 && y == 1) continue // Skip the mine cell
                assertThat(board.grid[x][y].adjacentMines).isEqualTo(1)
            }
        }
    }
    
    @Test
    fun `cell revelation updates cell state`() {
        val board = Board(width = 3, height = 3, mineCount = 0)
        
        board.revealCell(1, 1)
        
        assertThat(board.grid[1][1].state).isEqualTo(CellState.REVEALED)
    }
    
    @Test
    fun `revealing mine cell results in game over`() {
        val board = Board(width = 3, height = 3, mineCount = 1)
        board.setMineForTesting(1, 1)
        
        board.revealCell(1, 1)
        
        assertThat(board.getGameState()).isEqualTo(GameState.LOST)
    }
}
