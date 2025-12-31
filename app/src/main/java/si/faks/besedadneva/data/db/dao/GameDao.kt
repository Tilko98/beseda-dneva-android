package si.faks.besedadneva.data.db.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import si.faks.besedadneva.data.db.entities.GameEntity
import si.faks.besedadneva.data.db.entities.GuessEntity

@Dao
interface GameDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(game: GameEntity): Long

    @Update
    suspend fun update(game: GameEntity)

    @Delete
    suspend fun delete(game: GameEntity)

    @Query("DELETE FROM games WHERE mode = :mode")
    suspend fun deleteByMode(mode: String)

    @Query("SELECT * FROM games WHERE id = :id")
    suspend fun getById(id: Long): GameEntity?

    @Query("SELECT id FROM games WHERE date = :date AND mode = :mode LIMIT 1")
    suspend fun getGameIdByDateAndMode(date: String, mode: String): Long?

    // NOVO: Hitra poizvedba samo za današnje igre
    @Query("SELECT * FROM games WHERE date = :date")
    fun getGamesForDate(date: String): Flow<List<GameEntity>>

    @Query("SELECT * FROM games ORDER BY finishedAtMillis DESC")
    fun getAll(): Flow<List<GameEntity>>

    // POPRAVEK: Uporabi LIKE za iskanje vseh DAILY iger (DAILY_4, DAILY_5...)
    @Query("SELECT * FROM games WHERE mode LIKE :modePattern ORDER BY finishedAtMillis DESC")
    fun getGamesByModePattern(modePattern: String): Flow<List<GameEntity>>

    // Za natančno ujemanje (PRACTICE)
    @Query("SELECT * FROM games WHERE mode = :mode ORDER BY finishedAtMillis DESC")
    fun getGamesByMode(mode: String): Flow<List<GameEntity>>
}