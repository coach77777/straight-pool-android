package com.example.straightpool.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.straightpool.scorer.MatchHistoryScreen
import com.example.straightpool.scorer.ScorerV2Screen
import com.example.straightpool.scorer.ScorerViewModel
import com.example.straightpool.ui.admin.AdminExportPlayersScreen
import com.example.straightpool.ui.admin.AdminGateScreen
import com.example.straightpool.ui.admin.AdminImportPlayersScreen
import com.example.straightpool.ui.admin.AdminMenuScreen
import com.example.straightpool.ui.admin.AdminEditPlayerScreen
import com.example.straightpool.ui.admin.AdminPlayersScreen
import com.example.straightpool.ui.admin.AdminSettingsScreen
import com.example.straightpool.ui.breakintro.BreakIntroScreen
import com.example.straightpool.ui.contacts.ContactsScreen
import com.example.straightpool.ui.help.HelpSpottingScreen
import com.example.straightpool.ui.setup.SetupScreen
import com.example.straightpool.ui.start.StartScreen
import com.example.straightpool.ui.stats.StandingsScreen

@Composable
fun AppNav(vm: ScorerViewModel) {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = "start") {

        composable("start") {
            StartScreen(
                onStart = { nav.navigate("setup") },
                onContacts = { nav.navigate("contacts") },
                onStats = { nav.navigate("stats") },
                onAdmin = { nav.navigate("admin") }
            )
        }

        composable("contacts") {
            ContactsScreen()
        }

        composable("stats") {
            StandingsScreen(onBack = { nav.popBackStack() })
        }

        composable("setup") {
            SetupScreen(
                vm = vm,
                onStart = { target, aId, aName, bId, bName, weekKey, weekLabel ->
                    vm.startMatch(
                        target = target,
                        aId = aId, aName = aName,
                        bId = bId, bName = bName,
                        weekKey = weekKey, weekLabel = weekLabel
                    )
                    nav.navigate("break") { popUpTo("setup") { inclusive = true } }
                },
                onBack = { nav.popBackStack() }
            )
        }

        composable("break") {
            BreakIntroScreen(
                vm = vm,
                onStart = {
                    nav.navigate("scorer") { popUpTo("break") { inclusive = true } }
                },
                onBack = { nav.popBackStack() }
            )
        }

        composable("scorer") {
            ScorerV2Screen(
                vm = vm,
                onBack = { nav.popBackStack() },
                onHelp = { nav.navigate("help") },
                onHistory = { nav.navigate("match_history") }
            )
        }

        composable("match_history") {
            MatchHistoryScreen(onBack = { nav.popBackStack() })
        }

        composable("help") {
            HelpSpottingScreen(onBack = { nav.popBackStack() })
        }

        // ADMIN gate (passcode)
        composable("admin") {
            AdminGateScreen(
                appName = "StraightPool",
                defaultPasscode = "7777",
                onSuccess = { nav.navigate("admin_menu") },
                onBack = { nav.popBackStack() }
            )
        }

        // ADMIN menu
        composable("admin_menu") {
            AdminMenuScreen(
                onBack = { nav.popBackStack() },
                onAdminSettings = { nav.navigate("admin_settings") },
                onPlayers = { nav.navigate("admin_players") },
                onSchedule = { nav.navigate("admin_schedule") },
                onFixMatchResults = { nav.navigate("admin_fix_results") },
                onPlayerStats = { nav.navigate("admin_player_stats") },
                onImportMatches = { nav.navigate("admin_import_matches") }
            )
        }

        composable("admin_settings") {
            AdminSettingsScreen(onBack = { nav.popBackStack() })
        }

        composable("admin_players") {
            AdminPlayersScreen(
                onBack = { nav.popBackStack() },
                onEditPlayer = { roster -> nav.navigate("admin_player_edit/$roster") },
                onAddPlayer = {
                    // placeholder until we make a real Add screen
                    nav.navigate("admin_player_edit/999")
                },
                onImport = { nav.navigate("admin_players_import") },
                onExport = { nav.navigate("admin_players_export") }
            )
        }

        composable("admin_player_edit/{roster}") { backStack ->
            val roster = backStack.arguments?.getString("roster")?.toIntOrNull() ?: 0
            AdminEditPlayerScreen(
                roster = roster,
                onBack = { nav.popBackStack() }
            )
        }

        composable("admin_players_import") {
            AdminImportPlayersScreen(onBack = { nav.popBackStack() })
        }

        composable("admin_players_export") {
            AdminExportPlayersScreen(onBack = { nav.popBackStack() })
        }

        // Admin stubs so menu clicks don’t crash
        composable("admin_schedule") { AdminSettingsScreen(onBack = { nav.popBackStack() }) }
        composable("admin_fix_results") { AdminSettingsScreen(onBack = { nav.popBackStack() }) }
        composable("admin_player_stats") { AdminSettingsScreen(onBack = { nav.popBackStack() }) }
        composable("admin_import_matches") { AdminSettingsScreen(onBack = { nav.popBackStack() }) }
    }
}
