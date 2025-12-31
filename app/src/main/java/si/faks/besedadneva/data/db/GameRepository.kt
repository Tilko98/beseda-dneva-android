package si.faks.besedadneva.data.db

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import si.faks.besedadneva.data.db.dao.GameDao
import si.faks.besedadneva.data.db.dao.GuessDao
import si.faks.besedadneva.data.db.entities.GameEntity
import si.faks.besedadneva.data.db.entities.GuessEntity

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

    fun getUserFlow(): Flow<com.google.firebase.auth.FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun savePracticeGame(game: GameEntity, guesses: List<GuessEntity>): Long {
        val newGameId = gameDao.insert(game)
        val guessesWithId = guesses.map { it.copy(gameId = newGameId) }
        guessDao.insertAll(guessesWithId)

        val user = auth.currentUser
        if (user != null) {
            // POPRAVEK: Uporabi isto funkcijo za update, da se izognemo logiki .add() vs .set()
            saveToCloudUniversal(user.uid, game, guesses)
        }
        return newGameId
    }

    suspend fun startDailyGame(game: GameEntity): Long {
        val rowId = gameDao.insert(game)
        val user = auth.currentUser
        if (user != null) {
            val docId = "${game.date}_${game.mode}"
            val gameData = hashMapOf(
                "date" to game.date,
                "mode" to game.mode,
                "solution" to game.solution,
                "won" to false,
                "attemptsUsed" to 0,
                "finishedAtMillis" to game.finishedAtMillis
            )
            db.collection("users").document(user.uid).collection("games").document(docId).set(gameData).await()
        }
        return rowId
    }

    suspend fun updateGame(game: GameEntity, guesses: List<GuessEntity>) {
        gameDao.update(game)
        guessDao.deleteForGame(game.id)
        val guessesWithId = guesses.map { it.copy(gameId = game.id) }
        guessDao.insertAll(guessesWithId)

        val user = auth.currentUser
        if (user != null) {
            // POPRAVEK: Zdaj kličemo univerzalno funkcijo za OBA načina (Daily in Practice)
            saveToCloudUniversal(user.uid, game, guesses)
        }
    }

    // NOVO: Ena funkcija za shranjevanje v oblak, ki preprečuje podvajanje
    private fun saveToCloudUniversal(userId: String, game: GameEntity, guesses: List<GuessEntity>) {
        val docId = if (game.mode.startsWith("DAILY")) {
            "${game.date}_${game.mode}" // ID za daily: 2024-05-20_DAILY_5
        } else {
            // ID za practice: PRACTICE_1715234235 (uporabimo timestamp začetka/konca)
            // S tem preprečimo, da bi vsak 'save' ustvaril nov dokument
            "PRACTICE_${game.finishedAtMillis}"
        }

        val gameData = hashMapOf(
            "date" to game.date,
            "mode" to game.mode,
            "solution" to game.solution,
            "won" to game.won,
            "attemptsUsed" to game.attemptsUsed,
            "finishedAtMillis" to game.finishedAtMillis,
            "guesses" to guesses.map { mapOf("word" to it.guessWord, "pattern" to it.pattern) }
        )

        // Uporabimo .set(), ki povozi podatke (update), namesto .add() (insert)
        db.collection("users")
            .document(userId)
            .collection("games")
            .document(docId)
            .set(gameData)
    }

    suspend fun resolveLocalGameId(cloudGame: GameEntity): Long {
        val localId = gameDao.getGameIdByDateAndMode(cloudGame.date, cloudGame.mode)
        if (localId != null) {
            return localId
        } else {
            val newLocalGame = cloudGame.copy(id = 0)
            return gameDao.insert(newLocalGame)
        }
    }

    fun getDailyGames(date: String): Flow<List<GameEntity>> {
        val user = auth.currentUser
        return if (user != null) {
            getCloudDailyGames(user.uid, date)
        } else {
            gameDao.getGamesForDate(date)
        }
    }

    suspend fun getGuessesForGame(gameId: Long): List<GuessEntity> {
        return guessDao.getForGame(gameId)
    }

    fun getHistory(mode: String): Flow<List<GameHistoryItem>> {
        val user = auth.currentUser
        if (user != null) {
            return getCloudHistory(user.uid, mode)
        } else {
            val gamesFlow = if (mode == "DAILY") {
                gameDao.getGamesByModePattern("DAILY%")
            } else {
                gameDao.getGamesByMode(mode)
            }

            return gamesFlow.map { games ->
                games.map { game ->
                    val guesses = guessDao.getForGame(game.id)
                    GameHistoryItem(game, guesses)
                }
            }
        }
    }

    private fun getCloudHistory(userId: String, mode: String): Flow<List<GameHistoryItem>> = callbackFlow {
        var query = db.collection("users").document(userId).collection("games")

        if (mode == "DAILY") {
            query.whereGreaterThanOrEqualTo("mode", "DAILY").whereLessThanOrEqualTo("mode", "DAILY\uf8ff")
        } else {
            query.whereEqualTo("mode", mode)
        }

        val listener = query.orderBy("finishedAtMillis", Query.Direction.DESCENDING).addSnapshotListener { snapshot, e ->
            if (e != null) { close(e); return@addSnapshotListener }
            if (snapshot != null) {
                val items = snapshot.documents.map { doc ->
                    val game = mapDocToGame(doc)
                    val guessesList = doc.get("guesses") as? List<Map<String, String>> ?: emptyList()
                    val guessesEntities = guessesList.mapIndexed { index, map ->
                        GuessEntity(gameId = game.id, guessIndex = index, guessWord = map["word"]?:"", pattern = map["pattern"]?:"")
                    }
                    GameHistoryItem(game, guessesEntities)
                }
                trySend(items)
            }
        }
        awaitClose { listener.remove() }
    }

    fun getCloudDailyGames(userId: String, date: String): Flow<List<GameEntity>> = callbackFlow {
        val query = db.collection("users")
            .document(userId)
            .collection("games")
            .whereEqualTo("date", date)

        val listener = query.addSnapshotListener { snapshot, e ->
            if (e != null) {
                android.util.Log.e("GameRepo", "Firestore error: ${e.message}")
                close(e)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val games = snapshot.documents.map { doc ->
                    mapDocToGame(doc)
                }
                trySend(games)
            }
        }
        awaitClose { listener.remove() }
    }

    // Potrebno za ViewModel (da dostopa do lokalne baze)
    fun getLocalDailyGames(date: String): Flow<List<GameEntity>> {
        return gameDao.getGamesForDate(date)
    }

    fun getAllGames(): Flow<List<GameEntity>> {
        val user = auth.currentUser
        return if (user != null) getCloudAllGames(user.uid) else gameDao.getAll()
    }

    private fun getCloudAllGames(userId: String): Flow<List<GameEntity>> = callbackFlow {
        val query = db.collection("users").document(userId).collection("games").orderBy("finishedAtMillis", Query.Direction.DESCENDING)
        val listener = query.addSnapshotListener { snapshot, e ->
            if (e != null) { close(e); return@addSnapshotListener }
            if (snapshot != null) { trySend(snapshot.documents.map { mapDocToGame(it) }) }
        }
        awaitClose { listener.remove() }
    }

    suspend fun deletePracticeHistory() {
        gameDao.deleteByMode("PRACTICE")
        val user = auth.currentUser
        if (user != null) {
            val snapshot = db.collection("users").document(user.uid).collection("games").whereEqualTo("mode", "PRACTICE").get().await()
            val batch = db.batch()
            for (doc in snapshot.documents) batch.delete(doc.reference)
            batch.commit().await()
        }
    }

    private fun mapDocToGame(doc: com.google.firebase.firestore.DocumentSnapshot): GameEntity {
        return GameEntity(
            id = doc.id.hashCode().toLong(),
            date = doc.getString("date") ?: "",
            mode = doc.getString("mode") ?: "",
            solution = doc.getString("solution") ?: "",
            won = doc.getBoolean("won") ?: false,
            attemptsUsed = (doc.getLong("attemptsUsed") ?: 0).toInt(),
            finishedAtMillis = doc.getLong("finishedAtMillis") ?: 0L
        )
    }
}