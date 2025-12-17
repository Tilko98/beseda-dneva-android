package si.faks.besedadneva.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import si.faks.besedadneva.ui.viewmodel.GameMode
import si.faks.besedadneva.ui.viewmodel.GameViewModel
import si.faks.besedadneva.ui.viewmodel.GuessRowUi

@Composable
fun GameScreen(
    vm: GameViewModel,
    modifier: Modifier = Modifier,
    onEndOk: (() -> Unit)? = null
) {
    val state by vm.state.collectAsState()

    var showResultDialog by remember { mutableStateOf(false) }
    LaunchedEffect(state.isFinished) {
        if (state.isFinished) showResultDialog = true
    }

    if (showResultDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                Text(
                    text = if (state.isWin) "Bravo! 🎉" else "Konec igre",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Rešitev: ${state.solution}")
                    val attempts = if (state.isWin) state.currentRowIndex + 1 else 6
                    Text("Poskusi: $attempts / 6")
                }
            },
            confirmButton = {
                Button(onClick = {
                    showResultDialog = false
                    onEndOk?.invoke()
                }) { Text("OK") }
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
            text = if (state.mode == GameMode.PRACTICE) "Vaja" else "Beseda dneva",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // DEBUG samo za PRACTICE
        if (state.mode == GameMode.PRACTICE) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "DEBUG: ${state.solution}",
                color = Color.Red,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(8.dp))

        state.message?.let {
            Text(text = it)
            Spacer(Modifier.height(8.dp))
        }

        WordGrid(rows = state.rows)

        Spacer(Modifier.weight(1f))

        KeyboardFixedSl(
            keyStates = state.keyboard,
            enabled = !state.isFinished,
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
private fun KeyboardFixedSl(
    keyStates: Map<Char, Char>,
    enabled: Boolean,
    onLetter: (Char) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit
) {
    // ✅ Slovenska tipkovnica brez Q/W/X/Y
    val row1 = listOf('E','R','T','Z','U','I','O','P')              // 8
    val row2 = listOf('A','S','D','F','G','H','J','K','L','Č')      // 10
    val row3 = listOf('Ž','Š','C','V','B','N','M')                  // 7

    // ✅ manjši gumbi + manjši razmiki -> ENTER + ⌫ vedno vidna
    val keyW = 30.dp
    val keyH = 46.dp
    val gap = 4.dp

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        KeyRowFixed(row1, keyStates, enabled, keyW, keyH, gap, onLetter)
        Spacer(Modifier.height(gap))
        KeyRowFixed(row2, keyStates, enabled, keyW, keyH, gap, onLetter)
        Spacer(Modifier.height(gap))

        Row(
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.wrapContentWidth()
        ) {
            ActionKey(text = "ENTER", width = 60.dp, height = keyH, enabled = enabled, onClick = onEnter)

            row3.forEach { ch ->
                LetterKey(
                    ch = ch,
                    pattern = keyStates[ch],
                    width = keyW,
                    height = keyH,
                    enabled = enabled,
                    onClick = { onLetter(ch) }
                )
            }

            // ✅ BACKSPACE (brisanje)
            ActionKey(text = "⌫", width = 54.dp, height = keyH, enabled = enabled, onClick = onBackspace)
        }
    }
}

@Composable
private fun KeyRowFixed(
    keys: List<Char>,
    keyStates: Map<Char, Char>,
    enabled: Boolean,
    keyW: androidx.compose.ui.unit.Dp,
    keyH: androidx.compose.ui.unit.Dp,
    gap: androidx.compose.ui.unit.Dp,
    onLetter: (Char) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.wrapContentWidth()
    ) {
        keys.forEach { ch ->
            LetterKey(
                ch = ch,
                pattern = keyStates[ch],
                width = keyW,
                height = keyH,
                enabled = enabled,
                onClick = { onLetter(ch) }
            )
        }
    }
}

@Composable
private fun LetterKey(
    ch: Char,
    pattern: Char?,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(width, height),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = keyBg(pattern),
            contentColor = Color.White
        )
    ) {
        Text(ch.toString(), fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
    }
}

@Composable
private fun ActionKey(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(width, height),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF4A5568),
            contentColor = Color.White
        )
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
    }
}
