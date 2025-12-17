package si.faks.besedadneva.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import si.faks.besedadneva.ui.viewmodel.GameViewModel
import si.faks.besedadneva.ui.viewmodel.GuessRowUi

@Composable
fun GameScreen(
    vm: GameViewModel,
    modifier: Modifier = Modifier
) {
    val state by vm.state.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Beseda dneva",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        state.message?.let {
            Text(text = it)
            Spacer(Modifier.height(8.dp))
        }

        WordGrid(rows = state.rows)

        Spacer(Modifier.weight(1f))

        Keyboard(
            onLetter = { vm.onLetter(it) },
            onBackspace = { vm.onBackspace() },
            onEnter = { vm.onEnter() }
        )
    }
}

@Composable
private fun WordGrid(rows: List<GuessRowUi>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (i in 0 until 5) {
                    val ch = row.letters.getOrNull(i) ?: ' '
                    val p = row.pattern?.getOrNull(i)
                    Tile(letter = ch, patternChar = p)
                }
            }
        }
    }
}

@Composable
private fun Tile(letter: Char, patternChar: Char?) {
    val bg = when (patternChar) {
        'G' -> Color(0xFF2E7D32)
        'Y' -> Color(0xFFF9A825)
        'X' -> Color(0xFF616161)
        else -> Color.Transparent
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(56.dp)
            .border(2.dp, Color(0xFFBDBDBD), RoundedCornerShape(6.dp))
            .background(bg, RoundedCornerShape(6.dp))
    ) {
        Text(
            text = if (letter == ' ') "" else letter.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (patternChar != null) Color.White else Color.Black
        )
    }
}

@Composable
private fun Keyboard(
    onLetter: (Char) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit
) {
    val row1 = listOf('Q','W','E','R','T','Y','U','I','O','P')
    val row2 = listOf('A','S','D','F','G','H','J','K','L','Č')
    val row3 = listOf('Z','X','C','V','B','N','M','Š','Ž')

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        KeyRow(keys = row1, onLetter = onLetter)
        Spacer(Modifier.height(6.dp))
        KeyRow(keys = row2, onLetter = onLetter)
        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ENTER
            Button(
                onClick = onEnter,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4A5568),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .height(48.dp)
                    .weight(1.6f)
            ) {
                Text("ENTER", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            // Črke tretje vrstice
            row3.forEach { ch ->
                Button(
                    onClick = { onLetter(ch) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4A5568),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .weight(1f)
                ) {
                    Text(ch.toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            // BACKSPACE
            Button(
                onClick = onBackspace,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4A5568),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .height(48.dp)
                    .weight(1.6f)
            ) {
                Text("⌫", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun KeyRow(keys: List<Char>, onLetter: (Char) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        keys.forEach { ch ->
            Button(
                onClick = { onLetter(ch) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4A5568),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .height(48.dp)
                    .weight(1f)
            ) {
                Text(ch.toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
