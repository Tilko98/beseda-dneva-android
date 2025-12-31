package si.faks.besedadneva.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import si.faks.besedadneva.data.db.GameRepository
import si.faks.besedadneva.data.db.entities.GameEntity
import si.faks.besedadneva.ui.auth.SignInResult
import si.faks.besedadneva.ui.auth.UserData

// Pomožni razred za statistiko
data class GameStats(
    val totalGames: Int = 0,
    val wins: Int = 0,
    val winDistribution: Map<Int, Int> = emptyMap()
)

data class ProfileState(
    val isSignInSuccessful: Boolean = false,
    val signInError: String? = null,
    val userData: UserData? = null,
    // LOČENA STATISTIKA:
    val dailyStats: GameStats = GameStats(),
    val practiceStats: GameStats = GameStats()
)

class ProfileViewModel(private val repo: GameRepository) : ViewModel() {
    private val _state = MutableStateFlow(ProfileState())
    val state = _state.asStateFlow()

    private val auth = FirebaseAuth.getInstance()
    private var statsJob: Job? = null

    init {
        loadStats()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            setInitialUser(
                UserData(
                    userId = currentUser.uid,
                    username = currentUser.email ?: "Uporabnik",
                    profilePictureUrl = currentUser.photoUrl?.toString()
                )
            )
        }
    }

    private fun loadStats() {
        statsJob?.cancel()
        statsJob = viewModelScope.launch {
            repo.getAllGames().collectLatest { games ->
                // Filtriramo igre
                val dailyGames = games.filter { it.mode == "DAILY" }
                val practiceGames = games.filter { it.mode == "PRACTICE" }

                _state.update {
                    it.copy(
                        dailyStats = calculateStats(dailyGames),
                        practiceStats = calculateStats(practiceGames)
                    )
                }
            }
        }
    }

    private fun calculateStats(games: List<GameEntity>): GameStats {
        val total = games.size
        val wins = games.count { it.won }

        val distMap = games
            .filter { it.won }
            .groupBy { it.attemptsUsed }
            .mapValues { entry -> entry.value.size }

        val finalDist = (1..6).associateWith { distMap[it] ?: 0 }

        return GameStats(total, wins, finalDist)
    }

    // NOVO: Funkcija za reset vaje
    fun resetPracticeHistory() {
        viewModelScope.launch {
            repo.deletePracticeHistory()
            // Opomba: Ker repo.getAllGames() posluša spremembe (Flow),
            // se bo statistika v loadStats avtomatsko posodobila, ko se baza spremeni.
        }
    }

    // --- AVTENTIKACIJA (Nespremenjeno) ---
    fun signInWithEmail(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _state.update { it.copy(signInError = "Vnesi e-pošto in geslo") }
            return
        }
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, pass).await()
                val user = auth.currentUser
                if (user != null) {
                    onSignInResult(SignInResult(UserData(user.uid, user.email, null), null))
                }
            } catch (e: Exception) {
                _state.update { it.copy(signInError = "Napaka: ${e.localizedMessage}") }
            }
        }
    }

    fun signUpWithEmail(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _state.update { it.copy(signInError = "Vnesi e-pošto in geslo") }
            return
        }
        viewModelScope.launch {
            try {
                auth.createUserWithEmailAndPassword(email, pass).await()
                signInWithEmail(email, pass)
            } catch (e: Exception) {
                _state.update { it.copy(signInError = "Napaka: ${e.localizedMessage}") }
            }
        }
    }

    fun onSignInResult(result: SignInResult) {
        _state.update {
            it.copy(isSignInSuccessful = result.data != null, signInError = result.errorMessage, userData = result.data)
        }
        if (result.data != null) loadStats()
    }

    fun onSignOut() {
        auth.signOut()
        _state.update { ProfileState() }
        loadStats()
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