package si.faks.besedadneva.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import si.faks.besedadneva.data.db.dao.GameDao
import si.faks.besedadneva.data.db.dao.GuessDao
import si.faks.besedadneva.data.db.entities.GameEntity
import si.faks.besedadneva.data.db.entities.GuessEntity

@Database(
    entities = [GameEntity::class, GuessEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun guessDao(): GuessDao
}
