package si.faks.besedadneva.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import si.faks.besedadneva.ui.viewmodel.GameViewModel
import si.faks.besedadneva.ui.viewmodel.GuessRowUi
import si.faks.besedadneva.ui.viewmodel.GameMode

@Composable
fun GameScreen(
    vm: GameViewModel,
    modifier: Modifier = Modifier,
    onEndOk: (() -> Unit)? = null
) {
    val state by vm.state.collectAsState()
    val wordLength = state.solution.length

    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(vm) {
        vm.shakeEvent.collect {
            for (i in 0..1) {
                shakeOffset.animateTo(20f, tween(50))
                shakeOffset.animateTo(-20f, tween(50))
            }
            shakeOffset.animateTo(0f, tween(50))
        }
    }

    if (state.isDialogShown) {
        AlertDialog(
            onDismissRequest = { vm.dismissDialog() },
            title = {
                Text(
                    text = if (state.isWin) "Bravo! 🎉" else "Konec igre",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text("Rešitev je bila: ${state.solution}")
                    Spacer(modifier = Modifier.height(8.dp))
                    if (state.isWin) {
                        Text("Uganil si v ${state.rows.count { it.pattern != null }} poskusih.")
                    } else {
                        Text("Več sreče prihodnjič!")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.dismissDialog()
                    onEndOk?.invoke()
                }) {
                    val btnText = if (state.mode == GameMode.DAILY) "OK" else "Nova igra"
                    Text(btnText)
                }
            }
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            vm.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "BESEDA DNEVA",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(20.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                state.rows.forEachIndexed { index, row ->
                    val isCurrent = index == state.currentRowIndex
                    val rowModifier = if (isCurrent) {
                        Modifier.graphicsLayer { translationX = shakeOffset.value }
                    } else {
                        Modifier
                    }

                    Box(modifier = rowModifier) {
                        GuessRow(
                            row = row,
                            length = wordLength
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Keyboard(
                onLetter = { vm.onLetter(it) },
                onBackspace = { vm.onBackspace() },
                onEnter = { vm.onEnter() },
                keyStates = calculateKeyStates(state.rows)
            )
        }
    }
}

@Composable
fun GuessRow(row: GuessRowUi, length: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        val boxSize = if (length > 5) 46.dp else 56.dp
        val spacing = if (length > 5) 4.dp else 6.dp

        for (i in 0 until length) {
            val char = row.letters.getOrNull(i)
            val patternChar = row.pattern?.getOrNull(i)

            LetterBox(
                ch = char,
                patternChar = patternChar,
                size = boxSize
            )

            if (i < length - 1) Spacer(modifier = Modifier.width(spacing))
        }
    }
}

@Composable
fun LetterBox(ch: Char?, patternChar: Char?, size: androidx.compose.ui.unit.Dp) {
    val bgColor = when (patternChar) {
        'G' -> Color(0xFF66BB6A)
        'Y' -> Color(0xFFFFEE58)
        'X' -> Color(0xFFBDBDBD)
        else -> MaterialTheme.colorScheme.surface
    }

    val borderColor = if (ch != null && patternChar == null) {
        MaterialTheme.colorScheme.onSurface
    } else {
        Color.LightGray
    }

    Box(
        modifier = Modifier
            .size(size)
            .background(bgColor, shape = RoundedCornerShape(4.dp))
            .border(2.dp, borderColor, shape = RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (ch != null) {
            Text(
                text = ch.toString(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = if (patternChar != null && patternChar != 'Y') Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun Keyboard(
    onLetter: (Char) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    keyStates: Map<Char, Char?>
) {
    val keys = listOf(
        "ERTZUIOP",
        "ASDFGHJKLČ",
        "CVBNMŠŽ"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        keys.forEachIndexed { i, rowKeys ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (i == 2) {
                    ActionKey("ENTER", 58.dp, 52.dp, true, onEnter)
                }
                rowKeys.forEach { ch ->
                    val keyW = 30.dp
                    val keyH = 52.dp
                    LetterKey(
                        ch = ch,
                        pattern = keyStates[ch],
                        width = keyW,
                        height = keyH,
                        enabled = true,
                        onClick = { onLetter(ch) }
                    )
                }
                if (i == 2) {
                    ActionKey("⌫", 42.dp, 52.dp, true, onBackspace)
                }
            }
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
            contentColor = if (pattern == null || pattern == 'Y') Color.Black else Color.White
        ),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(ch.toString(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
            containerColor = Color.LightGray,
            contentColor = Color.Black
        ),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
fun keyBg(pattern: Char?): Color {
    return when (pattern) {
        'G' -> Color(0xFF66BB6A)
        'Y' -> Color(0xFFFFEE58)
        'X' -> Color(0xFF757575)
        else -> Color(0xFFE0E0E0)
    }
}

fun calculateKeyStates(rows: List<GuessRowUi>): Map<Char, Char?> {
    val states = mutableMapOf<Char, Char?>()
    rows.forEach { row ->
        val pat = row.pattern ?: return@forEach
        row.letters.forEachIndexed { i, c ->
            val p = pat[i]
            val current = states[c]
            if (current == 'G') return@forEachIndexed
            if (p == 'G') {
                states[c] = 'G'
            } else if (p == 'Y' && current != 'G') {
                states[c] = 'Y'
            } else if (p == 'X' && current == null) {
                states[c] = 'X'
            }
        }
    }
    return states
}