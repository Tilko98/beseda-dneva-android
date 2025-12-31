package si.faks.besedadneva.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import si.faks.besedadneva.data.db.GameHistoryItem
import si.faks.besedadneva.data.db.GameRepository

data class HistoryState(
    val dailyGames: List<GameHistoryItem> = emptyList(),
    val practiceGames: List<GameHistoryItem> = emptyList(),
    val isLoading: Boolean = false
)

class HistoryViewModel(private val repo: GameRepository) : ViewModel() {

    private val _state = MutableStateFlow(HistoryState())
    val state = _state.asStateFlow()

    init {
        // Ob zagonu naložimo obe zgodovini
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            // 1. Naloži DNEVNE (repo funkcija že uporablja pravilen filter 'DAILY%')
            launch {
                repo.getHistory("DAILY").collectLatest { history ->
                    _state.update { it.copy(dailyGames = history) }
                }
            }

            // 2. Naloži VAJE (repo funkcija uporablja 'mode = PRACTICE')
            launch {
                repo.getHistory("PRACTICE").collectLatest { history ->
                    _state.update { it.copy(practiceGames = history) }
                }
            }
        }
    }
}

class HistoryViewModelFactory(private val repo: GameRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HistoryViewModel(repo) as T
    }
}