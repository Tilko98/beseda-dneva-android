package si.faks.besedadneva.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.*
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

    // dialog se pokaže, ko igra postane finished (enkrat)
    var showResultDialog by remember { mutableStateOf(false) }
    LaunchedEffect(state.isFinished) {
        if (state.isFinished) showResultDialog = true
    }

    if (showResultDialog) {
        AlertDialog(
            onDismissRequest = { showResultDialog = false },
            title = {
                Text(
                    text = if (state.isWin) "Bravo! 🎉" else "Konec igre",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Rešitev: ${state.solution}")

                    // poskusi: če win, je currentRowIndex+1 (ker si v isti vrstici), če lose je 6
                    val attempts = if (state.isWin) (state.currentRowIndex + 1) else 6
                    Text("Poskusi: $attempts/6")

                    // (kasneje tu dodamo definicijo iz API/DB)
                    Text(
                        text = "Definicija: (kasneje dodamo iz slovarja)",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showResultDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

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
            keyStates = state.keyboard,
            enabled = !state.isFinished,          // ⬅️ blokira UI tipkovnico po koncu
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

private fun keyBg(p: Char?): Color = when (p) {
    'G' -> Color(0xFF2E7D32)
    'Y' -> Color(0xFFF9A825)
    'X' -> Color(0xFF616161)
    else -> Color(0xFF718096)
}

@Composable
private fun Keyboard(
    keyStates: Map<Char, Char>,
    enabled: Boolean,
    onLetter: (Char) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit
) {
    val row1 = listOf('Q','E','R','T','Z','U','I','O','P')
    val row2 = listOf('A','S','D','F','G','H','J','K','L','Č')
    val row3 = listOf('Y','X','C','V','B','N','M','Š','Ž')

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        KeyRow(row1, keyStates, enabled, onLetter)
        Spacer(Modifier.height(6.dp))
        KeyRow(row2, keyStates, enabled, onLetter)
        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onEnter,
                enabled = enabled,
                modifier = Modifier
                    .height(46.dp)
                    .weight(1.6f),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4A5568),
                    contentColor = Color.White
                )
            ) {
                Text("ENTER", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            row3.forEach { ch ->
                val p = keyStates[ch]
                Button(
                    onClick = { onLetter(ch) },
                    enabled = enabled,
                    modifier = Modifier
                        .height(46.dp)
                        .weight(1f),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = keyBg(p),
                        contentColor = Color.White
                    )
                ) {
                    Text(ch.toString(), fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }

            Button(
                onClick = onBackspace,
                enabled = enabled,
                modifier = Modifier
                    .height(46.dp)
                    .weight(1.6f),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4A5568),
                    contentColor = Color.White
                )
            ) {
                Text("⌫", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun KeyRow(
    keys: List<Char>,
    keyStates: Map<Char, Char>,
    enabled: Boolean,
    onLetter: (Char) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        keys.forEach { ch ->
            val p = keyStates[ch]
            Button(
                onClick = { onLetter(ch) },
                enabled = enabled,
                modifier = Modifier
                    .height(46.dp)
                    .weight(1f),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = keyBg(p),
                    contentColor = Color.White
                )
            ) {
                Text(ch.toString(), fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}
