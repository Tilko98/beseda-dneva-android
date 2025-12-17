package si.faks.besedadneva.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "games",
    indices = [Index(value = ["date", "mode"], unique = true)]
)
data class GameEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,              // "YYYY-MM-DD"
    val mode: String,              // "DAILY" ali "PRACTICE"
    val solution: String,          // 5 črk
    val won: Boolean,
    val attemptsUsed: Int,         // 1..6
    val finishedAtMillis: Long     // System.currentTimeMillis()
)
