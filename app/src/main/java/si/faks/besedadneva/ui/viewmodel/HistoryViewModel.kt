package si.faks.besedadneva.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import si.faks.besedadneva.data.db.GameHistoryItem
import si.faks.besedadneva.data.db.GameRepository

class HistoryViewModel(private val repo: GameRepository) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private var authListener: FirebaseAuth.AuthStateListener? = null

    // Zdaj uporabljamo GameHistoryItem, ki vsebuje igro IN ugibe
    private val _dailyGames = MutableStateFlow<List<GameHistoryItem>>(emptyList())
    val dailyGames = _dailyGames.asStateFlow()

    private val _practiceGames = MutableStateFlow<List<GameHistoryItem>>(emptyList())
    val practiceGames = _practiceGames.asStateFlow()

    private var loadJob: Job? = null

    init {
        authListener = FirebaseAuth.AuthStateListener {
            loadHistory()
        }
        auth.addAuthStateListener(authListener!!)
    }

    private fun loadHistory() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            launch {
                repo.getHistory("DAILY").collect { items ->
                    _dailyGames.value = items
                }
            }
            launch {
                repo.getHistory("PRACTICE").collect { items ->
                    _practiceGames.value = items
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        authListener?.let { auth.removeAuthStateListener(it) }
    }
}

class HistoryViewModelFactory(private val repo: GameRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HistoryViewModel(repo) as T
    }
}