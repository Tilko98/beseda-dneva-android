package si.faks.besedadneva.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import si.faks.besedadneva.data.db.entities.GameEntity
import si.faks.besedadneva.ui.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(viewModel: HistoryViewModel) {
    // Spremljamo stanje iz ViewModel-a
    val dailyGames by viewModel.dailyGames.collectAsState()
    val practiceGames by viewModel.practiceGames.collectAsState()

    // Stanje za izbran zavihek (0 = Dnevna, 1 = Vaja)
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Dnevna", "Vaja")

    Column(modifier = Modifier.fillMaxSize()) {
        // Zavihki na vrhu
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }

        // Vsebina glede na izbran zavihek
        val gamesToShow = if (selectedTabIndex == 0) dailyGames else practiceGames

        if (gamesToShow.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Ni še odigranih iger.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(gamesToShow) { game ->
                    HistoryItem(game)
                }
            }
        }
    }
}

@Composable
fun HistoryItem(game: GameEntity) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (game.won) Color(0xFFE8F5E9) else Color(0xFFFFEBEE) // Zelena/Rdeča ozadja
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                // Rešitev (beseda)
                Text(
                    text = game.solution,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                // Datum ali čas
                val dateStr = if (game.mode == "DAILY") {
                    game.date // Pri daily je datum že shranjen kot string YYYY-MM-DD
                } else {
                    // Pri vaji pretvorimo timestamp v lep datum/čas
                    formatTimestamp(game.finishedAtMillis)
                }
                Text(text = dateStr, style = MaterialTheme.typography.bodySmall)
            }

            // Rezultat na desni (npr. 4/6)
            Column(horizontalAlignment = Alignment.End) {
                if (game.won) {
                    Text(
                        text = "${game.attemptsUsed}/6",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32) // Temno zelena
                    )
                } else {
                    Text(
                        text = "X",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC62828) // Temno rdeča
                    )
                }
            }
        }
    }
}

// Pomožna funkcija za formatiranje časa
fun formatTimestamp(millis: Long): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(millis))
}