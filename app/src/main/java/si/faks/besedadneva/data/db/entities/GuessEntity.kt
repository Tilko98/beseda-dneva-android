package si.faks.besedadneva.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "guesses",
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["gameId"])]
)
data class GuessEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameId: Long,          // FK na games.id
    val guessIndex: Int,       // 0..5
    val guessWord: String,     // "MIZA?"
    val pattern: String        // npr. "GYXGX" (G=green, Y=yellow, X=gray)
)
