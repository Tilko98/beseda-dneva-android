package si.faks.besedadneva.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import si.faks.besedadneva.data.db.GameHistoryItem
import si.faks.besedadneva.ui.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(viewModel: HistoryViewModel) {
    val dailyGames by viewModel.dailyGames.collectAsState()
    val practiceGames by viewModel.practiceGames.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Dnevna", "Vaja")

    // Stanje za izbrano igro (za prikaz dialoga)
    var selectedGameItem by remember { mutableStateOf<GameHistoryItem?>(null) }

    // --- DIALOG ZA REPLAY ---
    if (selectedGameItem != null) {
        val item = selectedGameItem!!
        AlertDialog(
            onDismissRequest = { selectedGameItem = null },
            title = {
                Text(
                    text = "Rešitev: ${item.game.solution}",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                // Izris mreže ugibov
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    item.guesses.forEach { guess ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            guess.guessWord.forEachIndexed { i, char ->
                                val patternChar = guess.pattern.getOrNull(i)
                                // Mini verzija LetterBox-a
                                MiniLetterBox(char, patternChar)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedGameItem = null }) {
                    Text("Zapri")
                }
            }
        )
    }
    // -----------------------

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }

        val listToShow = if (selectedTabIndex == 0) dailyGames else practiceGames

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(listToShow) { item ->
                HistoryItemRow(
                    item = item,
                    onClick = { selectedGameItem = item } // Ob kliku nastavimo izbrano igro
                )
            }
        }
    }
}

@Composable
fun HistoryItemRow(item: GameHistoryItem, onClick: () -> Unit) {
    val game = item.game
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().clickable { onClick() } // Omogočimo klik
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = game.solution,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                val dateStr = if (game.mode == "DAILY") {
                    game.date
                } else {
                    formatTimestamp(game.finishedAtMillis)
                }
                Text(text = dateStr, style = MaterialTheme.typography.bodySmall)
            }

            Column(horizontalAlignment = Alignment.End) {
                if (game.won) {
                    Text(
                        text = "${game.attemptsUsed}/6",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                } else {
                    Text(
                        text = "X",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC62828)
                    )
                }
            }
        }
    }
}

@Composable
fun MiniLetterBox(char: Char, patternChar: Char?) {
    val color = when (patternChar) {
        'G' -> Color(0xFF66BB6A)
        'Y' -> Color(0xFFFFEE58)
        'X' -> Color(0xFFBDBDBD)
        else -> Color.LightGray
    }
    val textColor = if (patternChar == 'Y') Color.Black else Color.White

    Box(
        modifier = Modifier
            .size(30.dp) // Manjša velikost za dialog
            .background(color, androidx.compose.foundation.shape.RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = char.toString(), fontWeight = FontWeight.Bold, color = textColor)
    }
}

fun formatTimestamp(millis: Long): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(millis))
}