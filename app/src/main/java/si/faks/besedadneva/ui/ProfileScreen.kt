package si.faks.besedadneva.ui

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import si.faks.besedadneva.data.db.GameRepository
import si.faks.besedadneva.ui.auth.GoogleAuthClient
import si.faks.besedadneva.ui.viewmodel.ProfileViewModel
import si.faks.besedadneva.ui.viewmodel.ProfileViewModelFactory

@Composable
fun ProfileScreen(
    googleAuthClient: GoogleAuthClient,
    repo: GameRepository,
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModelFactory(repo))
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Stanja za Email/Password prijavo
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                scope.launch {
                    val signInResult = googleAuthClient.signInWithIntent(result.data ?: return@launch)
                    viewModel.onSignInResult(signInResult)
                }
            }
        }
    )

    // Prikaz napak preko Toast sporočil
    LaunchedEffect(state.signInError) {
        state.signInError?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Moj Profil", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))

        if (state.userData != null) {
            val user = state.userData!!
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(60.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(user.username?.firstOrNull()?.toString() ?: "?", fontSize = 24.sp)
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(user.username ?: "Neznanec", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(user.userId.take(10) + "...", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        } else {
            // --- PRIJAVA Z EMAILOM IN GESLOM ---
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("E-pošta") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Geslo") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { viewModel.signInWithEmail(email, password) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Prijava z geslom")
            }

            TextButton(onClick = { viewModel.signUpWithEmail(email, password) }) {
                Text("Nimaš računa? Registriraj se")
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            // --- PRIJAVA Z GOOGLE ---
            Button(
                onClick = {
                    scope.launch {
                        val intentSender = googleAuthClient.signIn()
                        launcher.launch(IntentSenderRequest.Builder(intentSender ?: return@launch).build())
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Prijava z Google računom")
            }
        }

        Spacer(Modifier.height(32.dp))

        // STATISTIKA
        Text("Statistika igranja", fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start))
        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Igre", state.totalGames.toString(), Modifier.weight(1f))
            StatCard("Zmage", state.wins.toString(), Modifier.weight(1f))
            val winPercent = if (state.totalGames > 0) (state.wins * 100 / state.totalGames) else 0
            StatCard("Uspeh", "$winPercent%", Modifier.weight(1f))
        }

        Spacer(Modifier.weight(1f))

        if (state.userData != null) {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    scope.launch {
                        googleAuthClient.signOut()
                        viewModel.onSignOut()
                    }
                }
            ) {
                Text("Odjava")
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(label, fontSize = 12.sp)
        }
    }
}