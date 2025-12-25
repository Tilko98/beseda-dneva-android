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
import si.faks.besedadneva.ui.auth.SignInResult
import si.faks.besedadneva.ui.auth.UserData

data class ProfileState(
    val isSignInSuccessful: Boolean = false,
    val signInError: String? = null,
    val userData: UserData? = null,
    val totalGames: Int = 0,
    val wins: Int = 0,
    val winDistribution: Map<Int, Int> = emptyMap()
)

class ProfileViewModel(private val repo: GameRepository) : ViewModel() {
    private val _state = MutableStateFlow(ProfileState())
    val state = _state.asStateFlow()

    private val auth = FirebaseAuth.getInstance()

    // Hranimo referenco na opravilo (Job) zbiranja statistike, da ga lahko resetiramo
    private var statsJob: Job? = null

    init {
        // Naložimo statistiko ob zagonu
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
        // 1. Prekličemo prejšnje zbiranje (npr. če smo zamenjali uporabnika)
        statsJob?.cancel()

        // 2. Zaženemo novo zbiranje
        statsJob = viewModelScope.launch {
            // repo.getAllGames() bo zdaj preveril trenutni auth.currentUser
            // in vrnil ustrezen Flow (Cloud ali Local)
            repo.getAllGames().collectLatest { games ->
                val total = games.size
                val wins = games.count { it.won }

                val distMap = games
                    .filter { it.won }
                    .groupBy { it.attemptsUsed }
                    .mapValues { entry -> entry.value.size }

                val finalDist = (1..6).associateWith { distMap[it] ?: 0 }

                _state.update {
                    it.copy(
                        totalGames = total,
                        wins = wins,
                        winDistribution = finalDist
                    )
                }
            }
        }
    }

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
                    onSignInResult(
                        SignInResult(
                            data = UserData(user.uid, user.email, user.photoUrl?.toString()),
                            errorMessage = null
                        )
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(signInError = "Napaka pri prijavi: ${e.localizedMessage}") }
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
                _state.update { it.copy(signInError = "Napaka pri registraciji: ${e.localizedMessage}") }
            }
        }
    }

    fun onSignInResult(result: SignInResult) {
        _state.update {
            it.copy(
                isSignInSuccessful = result.data != null,
                signInError = result.errorMessage,
                userData = result.data
            )
        }
        // KLJUČNO: Ko se uspešno prijavimo, osvežimo statistiko (preklop na Cloud)
        if (result.data != null) {
            loadStats()
        }
    }

    fun onSignOut() {
        auth.signOut()

        // Resetiramo stanje na začetno (prazno)
        _state.update { ProfileState() }

        // KLJUČNO: Ko se odjavimo, osvežimo statistiko (preklop nazaj na Local)
        loadStats()
    }

    fun setInitialUser(userData: UserData?) {
        if (userData != null) {
            _state.update { it.copy(userData = userData, isSignInSuccessful = true) }
            // Opomba: loadStats() je že poklican v init{}, kjer auth.currentUser že obstaja,
            // zato tukaj ni nujno ponovno klicati, a ne škodi.
        }
    }
}

class ProfileViewModelFactory(private val repo: GameRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ProfileViewModel(repo) as T
    }
}