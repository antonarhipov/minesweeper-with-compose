package minesweeper

import kotlinx.serialization.Serializable

@Serializable
enum class Difficulty(
    val width: Int,
    val height: Int,
    val mines: Int,
    val displayName: String
) {
    EASY(9, 9, 10, "Easy"), // 12.35% density
    MEDIUM(10, 10, 15, "Medium"), // 15% density
    HARD(11, 11, 20, "Hard")  // 16.53% density
}
