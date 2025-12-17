package si.faks.besedadneva

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.*
import si.faks.besedadneva.data.db.DatabaseProvider
import si.faks.besedadneva.data.db.GameRepository
import si.faks.besedadneva.ui.*
import si.faks.besedadneva.ui.theme.BesedaDnevaTheme
import si.faks.besedadneva.ui.viewmodel.GameMode
import si.faks.besedadneva.ui.viewmodel.GameViewModel
import si.faks.besedadneva.ui.viewmodel.GameViewModelFactory
import androidx.compose.runtime.getValue


sealed class BottomRoute(val route: String, val label: String) {
    data object Daily : BottomRoute("daily", "Daily")
    data object Practice : BottomRoute("practice", "Vaja")
    data object History : BottomRoute("history", "Zgodovina")
    data object Profile : BottomRoute("profile", "Profil")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = DatabaseProvider.get(applicationContext)
        val repo = GameRepository(db.gameDao(), db.guessDao())

        setContent {
            BesedaDnevaTheme {
                AppScaffold(repo = repo)
            }
        }
    }
}

@Composable
private fun AppScaffold(repo: GameRepository) {
    val navController = rememberNavController()
    val items = listOf(
        BottomRoute.Daily,
        BottomRoute.Practice,
        BottomRoute.History,
        BottomRoute.Profile
    )

    Scaffold(
        bottomBar = {
            BottomBar(navController = navController, items = items)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomRoute.Daily.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomRoute.Daily.route) {
                val factory = GameViewModelFactory(
                    repo = repo,
                    solution = "MIZA?", // za zdaj hardcoded (kasneje daily beseda iz baze)
                    mode = GameMode.DAILY
                )
                val vm: GameViewModel = viewModel(factory = factory)
                GameScreen(vm = vm)
            }

            composable(BottomRoute.Practice.route) { PracticeScreen(repo) }
            composable(BottomRoute.History.route) { HistoryScreen() }
            composable(BottomRoute.Profile.route) { ProfileScreen() }
        }
    }
}

@Composable
private fun BottomBar(
    navController: androidx.navigation.NavHostController,
    items: List<BottomRoute>
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        items.forEach { screen ->
            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true

            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                label = { Text(screen.label) },
                icon = { /* za zdaj brez ikon */ }
            )
        }
    }
}
