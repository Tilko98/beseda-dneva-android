package si.faks.besedadneva.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import si.faks.besedadneva.data.db.GameHistoryItem
import si.faks.besedadneva.data.db.GameRepository
import si.faks.besedadneva.data.db.entities.GuessEntity
import si.faks.besedadneva.ui.viewmodel.HistoryViewModel
import si.faks.besedadneva.ui.viewmodel.HistoryViewModelFactory

@Composable
fun HistoryScreen(repo: GameRepository) {
    val vm: HistoryViewModel = viewModel(factory = HistoryViewModelFactory(repo))
    val state by vm.state.collectAsState()

    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val titles = listOf("Dnevne igre", "Vaja")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            titles.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = { Text(title) }
                )
            }
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            if (page == 0) {
                HistoryList(games = state.dailyGames, emptyText = "Ni odigranih dnevnih iger")
            } else {
                HistoryList(games = state.practiceGames, emptyText = "Ni odigranih vaj")
            }
        }
    }
}

@Composable
fun HistoryList(games: List<GameHistoryItem>, emptyText: String) {
    if (games.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(emptyText, color = Color.Gray)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(games) { item ->
                HistoryItemCard(item)
            }
        }
    }
}

@Composable
fun HistoryItemCard(item: GameHistoryItem) {
    Card(
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Glava: Beseda in Datum
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = item.game.solution.uppercase(),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = item.game.date,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                if (item.game.won) {
                    Text("✓ V ${item.game.attemptsUsed}. poskusu", color = Color(0xFF66BB6A), fontWeight = FontWeight.Bold)
                } else {
                    Text("X Poraz", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Telo: Seznam ugibov (Grid)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                item.guesses.forEach { guess ->
                    HistoryGuessRow(guess)
                }
            }
        }
    }
}

@Composable
fun HistoryGuessRow(guess: GuessEntity) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        guess.guessWord.forEachIndexed { index, char ->
            val colorChar = guess.pattern.getOrNull(index) ?: 'X'
            val bgColor = when (colorChar) {
                'G' -> Color(0xFF66BB6A) // Green
                'Y' -> Color(0xFFFFEE58) // Yellow
                else -> Color(0xFFE0E0E0) // Gray
            }

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(bgColor, RoundedCornerShape(2.dp))
                    .border(1.dp, Color.LightGray, RoundedCornerShape(2.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = char.toString().uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (colorChar == 'Y') Color.Black else Color.White
                )
            }
        }
    }
}