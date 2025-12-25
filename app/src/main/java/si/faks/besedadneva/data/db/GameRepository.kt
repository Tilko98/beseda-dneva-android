package si.faks.besedadneva.data.db

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import si.faks.besedadneva.data.db.dao.GameDao
import si.faks.besedadneva.data.db.dao.GuessDao
import si.faks.besedadneva.data.db.entities.GameEntity
import si.faks.besedadneva.data.db.entities.GuessEntity

// Pomožni razred za prikaz v UI (Igra + njeni ugibi)
data class GameHistoryItem(
    val game: GameEntity,
    val guesses: List<GuessEntity>
)

class GameRepository(
    private val gameDao: GameDao,
    private val guessDao: GuessDao
) {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // --- SHRANJEVANJE (Nespremenjeno) ---
    suspend fun saveFinishedGame(game: GameEntity, guesses: List<GuessEntity>) {
        val gameId = gameDao.insert(game)
        val guessesWithId = guesses.map { it.copy(gameId = gameId) }
        guessDao.insertAll(guessesWithId)

        val user = auth.currentUser
        if (user != null) {
            saveToCloud(user.uid, game, guesses)
        }
    }

    private fun saveToCloud(userId: String, game: GameEntity, guesses: List<GuessEntity>) {
        val gameData = hashMapOf(
            "date" to game.date,
            "mode" to game.mode,
            "solution" to game.solution,
            "won" to game.won,
            "attemptsUsed" to game.attemptsUsed,
            "finishedAtMillis" to game.finishedAtMillis,
            "guesses" to guesses.map {
                mapOf("word" to it.guessWord, "pattern" to it.pattern)
            }
        )
        db.collection("users").document(userId).collection("games").add(gameData)
    }

    // --- BRANJE ---

    // Sprememba: Vrača GameHistoryItem namesto GameEntity
    fun getHistory(mode: String): Flow<List<GameHistoryItem>> {
        val user = auth.currentUser
        return if (user != null) {
            getCloudHistory(user.uid, mode)
        } else {
            // Lokalno branje: Room Flow<List<Game>> mapiramo v Flow<List<GameHistoryItem>>
            gameDao.getGamesByMode(mode).map { games ->
                games.map { game ->
                    val guesses = guessDao.getForGame(game.id)
                    GameHistoryItem(game, guesses)
                }
            }
        }
    }

    fun getAllGames(): Flow<List<GameEntity>> {
        val user = auth.currentUser
        return if (user != null) getCloudAllGames(user.uid) else gameDao.getAll()
    }

    // --- PRIVATE HELPERS ---

    private fun getCloudHistory(userId: String, mode: String): Flow<List<GameHistoryItem>> = callbackFlow {
        val query = db.collection("users")
            .document(userId)
            .collection("games")
            .whereEqualTo("mode", mode)
            .orderBy("finishedAtMillis", Query.Direction.DESCENDING)

        val listener = query.addSnapshotListener { snapshot, e ->
            if (e != null) { close(e); return@addSnapshotListener }

            if (snapshot != null) {
                val items = snapshot.documents.map { doc ->
                    val game = GameEntity(
                        id = doc.id.hashCode().toLong(),
                        date = doc.getString("date") ?: "",
                        mode = doc.getString("mode") ?: "",
                        solution = doc.getString("solution") ?: "",
                        won = doc.getBoolean("won") ?: false,
                        attemptsUsed = (doc.getLong("attemptsUsed") ?: 0).toInt(),
                        finishedAtMillis = doc.getLong("finishedAtMillis") ?: 0L
                    )

                    // Parsanje ugibov iz polja "guesses" v dokumentu
                    val guessesList = doc.get("guesses") as? List<Map<String, String>> ?: emptyList()
                    val guessesEntities = guessesList.mapIndexed { index, map ->
                        GuessEntity(
                            gameId = game.id,
                            guessIndex = index,
                            guessWord = map["word"] ?: "",
                            pattern = map["pattern"] ?: ""
                        )
                    }

                    GameHistoryItem(game, guessesEntities)
                }
                trySend(items)
            }
        }
        awaitClose { listener.remove() }
    }

    // Ta funkcija ostane enaka, ker za statistiko profila ne rabimo ugibov
    private fun getCloudAllGames(userId: String): Flow<List<GameEntity>> = callbackFlow {
        val query = db.collection("users").document(userId).collection("games")
            .orderBy("finishedAtMillis", Query.Direction.DESCENDING)
        val listener = query.addSnapshotListener { snapshot, e ->
            if (e != null) { close(e); return@addSnapshotListener }
            if (snapshot != null) {
                val games = snapshot.documents.map { doc ->
                    GameEntity(
                        id = doc.id.hashCode().toLong(),
                        date = doc.getString("date") ?: "",
                        mode = doc.getString("mode") ?: "",
                        solution = doc.getString("solution") ?: "",
                        won = doc.getBoolean("won") ?: false,
                        attemptsUsed = (doc.getLong("attemptsUsed") ?: 0).toInt(),
                        finishedAtMillis = doc.getLong("finishedAtMillis") ?: 0L
                    )
                }
                trySend(games)
            }
        }
        awaitClose { listener.remove() }
    }
}