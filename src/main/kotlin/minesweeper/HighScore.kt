package minesweeper

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class HighScore(
    val name: String,
    val timeInSeconds: Int,
    val difficulty: Difficulty
)

class HighScoreManager {
    private val scoresFile = File("high_scores.json")
    private val json = Json { prettyPrint = true }
    
    fun getTopScores(difficulty: Difficulty, limit: Int = 10): List<HighScore> {
        if (!scoresFile.exists()) return emptyList()
        
        return try {
            val scores = json.decodeFromString<List<HighScore>>(scoresFile.readText())
            scores.filter { it.difficulty == difficulty }
                .sortedBy { it.timeInSeconds }
                .take(limit)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun addScore(score: HighScore) {
        val currentScores = if (scoresFile.exists()) {
            try {
                json.decodeFromString<List<HighScore>>(scoresFile.readText())
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
        
        val newScores = (currentScores + score)
            .groupBy { it.difficulty }
            .flatMap { (_, scores) ->
                scores.sortedBy { it.timeInSeconds }.take(10)
            }
        
        scoresFile.writeText(json.encodeToString(newScores))
    }
    
    fun wouldBeTopScore(timeInSeconds: Int, difficulty: Difficulty): Int? {
        val topScores = getTopScores(difficulty)
        if (topScores.size < 10) return topScores.size
        
        return topScores.indexOfFirst { it.timeInSeconds > timeInSeconds }
            .takeIf { it >= 0 }
    }
}
