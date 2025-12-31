package si.faks.besedadneva.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import si.faks.besedadneva.data.db.GameRepository
import si.faks.besedadneva.data.db.entities.GameEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DailyMenuViewModel(
    private val repo: GameRepository
) : ViewModel() {

    val todayDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    // POPRAVEK: Reaktivno na prijavo/odjavo
    @OptIn(ExperimentalCoroutinesApi::class)
    val playedStatus: StateFlow<Map<Int, GameEntity>> = repo.getUserFlow()
        .flatMapLatest { user ->
            // Ta blok se izvede vsakič, ko se spremeni 'user'
            // Če je user != null, repo.getDailyGames avtomatsko uporabi Firestore (ker gleda auth.currentUser)
            // Če je user == null, uporabi Room.
            // flatMapLatest poskrbi, da se prejšnji flow prekine in začne nov.
            repo.getDailyGames(todayDate)
        }
        .map { games ->
            val status = mutableMapOf<Int, GameEntity>()
            games.forEach { game ->
                if (game.mode.startsWith("DAILY_")) {
                    val parts = game.mode.split("_")
                    if (parts.size == 2) {
                        val len = parts[1].toIntOrNull()
                        if (len != null) {
                            status[len] = game
                        }
                    }
                }
            }
            status
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )
}

class DailyMenuViewModelFactory(private val repo: GameRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DailyMenuViewModel(repo) as T
    }
}