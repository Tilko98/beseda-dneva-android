package si.faks.besedadneva.data.db.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import si.faks.besedadneva.data.db.entities.GameEntity

@Dao
interface GameDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(game: GameEntity): Long

    @Update
    suspend fun update(game: GameEntity)

    @Delete
    suspend fun delete(game: GameEntity)

    // NOVO: Brisanje vseh iger določenega načina (za reset vaje)
    @Query("DELETE FROM games WHERE mode = :mode")
    suspend fun deleteByMode(mode: String)

    @Query("SELECT * FROM games WHERE id = :id")
    suspend fun getById(id: Long): GameEntity?

    @Query("SELECT * FROM games ORDER BY finishedAtMillis DESC")
    fun getAll(): Flow<List<GameEntity>>

    @Query("""
        SELECT * FROM games
        WHERE mode = 'DAILY'
        ORDER BY date DESC
        LIMIT 30
    """)
    fun getLast30Daily(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE mode = :mode ORDER BY finishedAtMillis DESC")
    fun getGamesByMode(mode: String): Flow<List<GameEntity>>
}