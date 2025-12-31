package si.faks.besedadneva.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import si.faks.besedadneva.data.db.GameRepository
import si.faks.besedadneva.ui.auth.GoogleAuthClient
import si.faks.besedadneva.ui.viewmodel.GameStats
import si.faks.besedadneva.ui.viewmodel.ProfileViewModel
import si.faks.besedadneva.ui.viewmodel.ProfileViewModelFactory

@Composable
fun ProfileScreen(
    googleAuthClient: GoogleAuthClient,
    repo: GameRepository,
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModelFactory(repo))
) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Za reset dialog
    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Ponastavi zgodovino vaje?") },
            text = { Text("To bo izbrisalo vso statistiko in zgodovino odigranih vaj. Tega dejanja ni mogoče razveljaviti.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetPracticeHistory()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) { Text("Izbriši") }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Prekliči") }
            }
        )
    }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState), // Dodan scroll, ker imamo zdaj več vsebine
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Profil & Statistika", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(24.dp))

        if (state.isSignInSuccessful && state.userData != null) {
            Text("Pozdravljen, ${state.userData?.username ?: "Uporabnik"}!")
            Spacer(Modifier.height(8.dp))
        } else {
            // Login forma
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("E-pošta") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Geslo") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = { viewModel.signInWithEmail(email, password) }) {
                    Text("Prijava")
                }
                OutlinedButton(onClick = { viewModel.signUpWithEmail(email, password) }) {
                    Text("Registracija")
                }
            }
            if (state.signInError != null) {
                Spacer(Modifier.height(8.dp))
                Text(state.signInError!!, color = Color.Red, fontSize = 14.sp)
            }
            Spacer(Modifier.height(24.dp))
            Divider()
            Spacer(Modifier.height(24.dp))
        }

        // --- 1. DNEVNA STATISTIKA ---
        Text(
            text = "Dnevna Statistika",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(Modifier.height(16.dp))
        StatsSection(stats = state.dailyStats)

        Spacer(Modifier.height(32.dp))
        Divider()
        Spacer(Modifier.height(32.dp))

        // --- 2. STATISTIKA VAJE ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Statistika Vaje",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            // GUMB ZA RESET
            TextButton(
                onClick = { showResetDialog = true },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Ponastavi")
            }
        }
        Spacer(Modifier.height(16.dp))
        StatsSection(stats = state.practiceStats)

        Spacer(Modifier.height(48.dp))

        if (state.isSignInSuccessful) {
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
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun StatsSection(stats: GameStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard("Igre", stats.totalGames.toString(), Modifier.weight(1f))
        StatCard("Zmage", stats.wins.toString(), Modifier.weight(1f))
        val winPercent = if (stats.totalGames > 0) (stats.wins * 100 / stats.totalGames) else 0
        StatCard("Uspeh", "$winPercent%", Modifier.weight(1f))
    }

    Spacer(Modifier.height(24.dp))

    if (stats.totalGames > 0) {
        WinDistributionChart(stats.winDistribution)
    } else {
        Text("Ni podatkov za prikaz grafa.", color = Color.Gray)
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

@Composable
fun WinDistributionChart(distribution: Map<Int, Int>) {
    Text(
        text = "Porazdelitev ugibov",
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(8.dp))

    val maxVal = distribution.values.maxOrNull()?.coerceAtLeast(1) ?: 1

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        distribution.toSortedMap().forEach { (attempts, count) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "$attempts",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(20.dp)
                )

                val fraction = count.toFloat() / maxVal
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (count > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction)
                                .fillMaxHeight()
                                .background(Color(0xFF66BB6A))
                        ) {
                            Text(
                                text = "$count",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 4.dp)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(Color.LightGray)
                        )
                        Text(
                            text = "0",
                            color = Color.Black,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
            }
        }
    }
}