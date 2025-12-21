package si.faks.besedadneva.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val wins: Int = 0
)

class ProfileViewModel(private val repo: GameRepository) : ViewModel() {
    private val _state = MutableStateFlow(ProfileState())
    val state = _state.asStateFlow()

    private val auth = FirebaseAuth.getInstance()

    init {
        loadStats()
        // Ob zagonu preverimo, če je uporabnik že prijavljen preko Firebase
        val currentUser = auth.currentUser
        if (currentUser != null) {
            setInitialUser(
                UserData(
                    userId = currentUser.uid,
                    username = currentUser.email ?: "Uporabnik",
                    profilePictureUrl = null
                )
            )
        }
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

    // --- NOVO: Prijava z e-pošto ---
    fun signInWithEmail(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _state.update { it.copy(signInError = "Vnesi e-pošto in geslo") }
            return
        }

        viewModelScope.launch {
            try {
                val result = auth.signInWithEmailAndPassword(email, pass).await()
                val user = result.user
                if (user != null) {
                    val userData = UserData(
                        userId = user.uid,
                        username = user.email,
                        profilePictureUrl = null
                    )
                    _state.update { it.copy(userData = userData, isSignInSuccessful = true, signInError = null) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(signInError = "Napaka pri prijavi: ${e.localizedMessage}") }
            }
        }
    }

    // --- NOVO: Registracija z e-pošto ---
    fun signUpWithEmail(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _state.update { it.copy(signInError = "Vnesi e-pošto in geslo") }
            return
        }

        viewModelScope.launch {
            try {
                auth.createUserWithEmailAndPassword(email, pass).await()
                // Po uspešni registraciji se uporabnik avtomatsko prijavi
                signInWithEmail(email, pass)
            } catch (e: Exception) {
                _state.update { it.copy(signInError = "Napaka pri registraciji: ${e.localizedMessage}") }
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
        auth.signOut() // Odjava iz Firebase
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