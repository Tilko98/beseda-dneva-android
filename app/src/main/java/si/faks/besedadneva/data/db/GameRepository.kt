package si.faks.besedadneva.data.db

import kotlinx.coroutines.flow.Flow
import si.faks.besedadneva.data.db.dao.GameDao
import si.faks.besedadneva.data.db.dao.GuessDao
import si.faks.besedadneva.data.db.entities.GameEntity
import si.faks.besedadneva.data.db.entities.GuessEntity

class GameRepository(
    private val gameDao: GameDao,
    private val guessDao: GuessDao
) {
    fun getAllGames(): Flow<List<GameEntity>> = gameDao.getAll()

    suspend fun insertGame(game: GameEntity): Long =
        gameDao.insert(game)

    suspend fun saveFinishedGame(
        game: GameEntity,
        guesses: List<GuessEntity>
    ) {
        val gameId = gameDao.insert(game)
        val guessesWithId = guesses.map { it.copy(gameId = gameId) }
        guessDao.insertAll(guessesWithId)
    }
}
