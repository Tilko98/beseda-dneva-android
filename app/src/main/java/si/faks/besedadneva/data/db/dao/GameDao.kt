package si.faks.besedadneva.data.db.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import si.faks.besedadneva.data.db.entities.GameEntity

@Dao
interface GameDao {

    // INSERT
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(game: GameEntity): Long

    // UPDATE
    @Update
    suspend fun update(game: GameEntity)

    // DELETE
    @Delete
    suspend fun delete(game: GameEntity)

    // GET ONE
    @Query("SELECT * FROM games WHERE id = :id")
    suspend fun getById(id: Long): GameEntity?

    // GET ALL
    @Query("SELECT * FROM games ORDER BY finishedAtMillis DESC")
    fun getAll(): Flow<List<GameEntity>>

    // ZANIMIVA POIZVEDBA: zadnjih 30 daily iger
    @Query("""
        SELECT * FROM games
        WHERE mode = 'DAILY'
        ORDER BY date DESC
        LIMIT 30
    """)
    fun getLast30Daily(): Flow<List<GameEntity>>
}
