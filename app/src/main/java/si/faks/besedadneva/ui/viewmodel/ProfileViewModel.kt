package si.faks.besedadneva.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import si.faks.besedadneva.data.db.GameRepository
import si.faks.besedadneva.ui.auth.SignInResult
import si.faks.besedadneva.ui.auth.UserData

data class ProfileState(
    val isSignInSuccessful: Boolean = false,
    val signInError: String? = null,
    val userData: UserData? = null,
    val totalGames: Int = 0,
    val wins: Int = 0
)

class ProfileViewModel(private val repo: GameRepository) : ViewModel() {
    private val _state = MutableStateFlow(ProfileState())
    val state = _state.asStateFlow()

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            repo.getAllGames().collect { games ->
                _state.update { it.copy(
                    totalGames = games.size,
                    wins = games.count { g -> g.won }
                ) }
            }
        }
    }

    fun onSignInResult(result: SignInResult) {
        _state.update { it.copy(
            isSignInSuccessful = result.data != null,
            signInError = result.errorMessage,
            userData = result.data
        ) }
    }

    fun onSignOut() {
        _state.update { ProfileState(totalGames = it.totalGames, wins = it.wins) }
    }

    fun setInitialUser(userData: UserData?) {
        if (userData != null) {
            _state.update { it.copy(userData = userData, isSignInSuccessful = true) }
        }
    }
}

class ProfileViewModelFactory(private val repo: GameRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ProfileViewModel(repo) as T
    }
}