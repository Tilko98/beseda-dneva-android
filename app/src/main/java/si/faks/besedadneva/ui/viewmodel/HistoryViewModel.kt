package si.faks.besedadneva.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import si.faks.besedadneva.data.db.GameRepository
import si.faks.besedadneva.data.db.entities.GameEntity

class HistoryViewModel(private val repo: GameRepository) : ViewModel() {

    // Seznam dnevnih iger
    val dailyGames: StateFlow<List<GameEntity>> =
        repo.getHistory("DAILY")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Seznam iger za vajo
    val practiceGames: StateFlow<List<GameEntity>> =
        repo.getHistory("PRACTICE")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

class HistoryViewModelFactory(private val repo: GameRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HistoryViewModel(repo) as T
    }
}
