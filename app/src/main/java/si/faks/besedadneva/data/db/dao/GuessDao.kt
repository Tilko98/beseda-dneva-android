package si.faks.besedadneva.data.db.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import si.faks.besedadneva.data.db.entities.GuessEntity

@Dao
interface GuessDao {

    // INSERT
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(guess: GuessEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(guesses: List<GuessEntity>)

    // UPDATE
    @Update
    suspend fun update(guess: GuessEntity)

    // DELETE
    @Delete
    suspend fun delete(guess: GuessEntity)

    // GET ONE
    @Query("SELECT * FROM guesses WHERE id = :id")
    suspend fun getById(id: Long): GuessEntity?

    // GET ALL
    @Query("SELECT * FROM guesses ORDER BY id DESC")
    fun getAll(): Flow<List<GuessEntity>>

    // ZANIMIVA POIZVEDBA: vsi ugibi za game po vrstnem redu
    @Query("SELECT * FROM guesses WHERE gameId = :gameId ORDER BY guessIndex ASC")
    suspend fun getForGame(gameId: Long): List<GuessEntity>

    @Query("DELETE FROM guesses WHERE gameId = :gameId")
    suspend fun deleteForGame(gameId: Long)
}
