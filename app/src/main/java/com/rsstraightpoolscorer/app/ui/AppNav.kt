package com.rsstraightpoolscorer.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rsstraightpoolscorer.app.scorer.MatchHistoryScreen
import com.rsstraightpoolscorer.app.scorer.ScorerV2Screen
import com.rsstraightpoolscorer.app.scorer.ScorerViewModel
import com.rsstraightpoolscorer.app.ui.admin.AdminExportPlayersScreen
import com.rsstraightpoolscorer.app.ui.admin.AdminGateScreen
import com.rsstraightpoolscorer.app.ui.admin.AdminImportPlayersScreen
import com.rsstraightpoolscorer.app.ui.admin.AdminMenuScreen
import com.rsstraightpoolscorer.app.ui.admin.AdminEditPlayerScreen
import com.rsstraightpoolscorer.app.ui.admin.AdminPlayersScreen
import com.rsstraightpoolscorer.app.ui.admin.AdminSettingsScreen
import com.rsstraightpoolscorer.app.ui.breakintro.BreakIntroScreen
import com.rsstraightpoolscorer.app.ui.contacts.ContactsScreen
import com.rsstraightpoolscorer.app.ui.help.HelpSpottingScreen
import com.rsstraightpoolscorer.app.ui.setup.SetupScreen
import com.rsstraightpoolscorer.app.ui.start.StartScreen
import com.rsstraightpoolscorer.app.ui.stats.StandingsScreen
import com.rsstraightpoolscorer.app.ui.stats.PlayerStatsScreen
import com.rsstraightpoolscorer.app.ui.admin.AdminPlayerMatchesScreen
import com.rsstraightpoolscorer.app.ui.admin.AdminExportsScreen
import com.rsstraightpoolscorer.app.ui.admin.AdminEditMatchScreen
import com.rsstraightpoolscorer.app.ui.admin.AdminImportMatchesScreen

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
            ContactsScreen(onBack = { nav.popBackStack() })
        }

        composable("stats") {
            StandingsScreen(
                onBack = { nav.popBackStack() },
                onPlayerClick = { roster -> nav.navigate("stats_player/$roster") }
            )
        }

        composable("stats_player/{roster}") { backStack ->
            val roster = backStack.arguments?.getString("roster")?.toIntOrNull() ?: 0
            PlayerStatsScreen(
                roster = roster,
                onBack = { nav.popBackStack() }
            )
        }

        composable("setup") {
            SetupScreen(
                vm = vm,
                onStart = { target, aId, aName, bId, bName, weekKey, weekLabel ->
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
                onSuccess = {
                    nav.navigate("admin_menu") {
                        popUpTo("admin") { inclusive = true }
                    }
                },
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
                onAdminStats = { nav.navigate("admin_stats") },
                onImportMatches = { nav.navigate("admin_import_matches") },
                onExports = { nav.navigate("admin_exports") }


            )
        }

        composable("admin_settings") {
            AdminSettingsScreen(onBack = { nav.popBackStack() })
        }

        composable("admin_stats") {
            StandingsScreen(
                onBack = { nav.popBackStack() },
                onPlayerClick = { roster -> nav.navigate("admin_stats_player/$roster") }
            )
        }

        composable("admin_stats_player/{roster}") { backStack ->
            val roster = backStack.arguments?.getString("roster")?.toIntOrNull() ?: 0
            AdminPlayerMatchesScreen(
                roster = roster,
                onBack = { nav.popBackStack() },
                onEditMatch = { week, aRoster, bRoster ->
                    nav.navigate("admin_edit_match/$week/$aRoster/$bRoster")
                }
            )
        }

        composable("admin_edit_match/{week}/{aRoster}/{bRoster}") { backStack ->
            val week = backStack.arguments?.getString("week")?.toIntOrNull() ?: 0
            val aRoster = backStack.arguments?.getString("aRoster")?.toIntOrNull() ?: 0
            val bRoster = backStack.arguments?.getString("bRoster")?.toIntOrNull() ?: 0

            AdminEditMatchScreen(
                week = week,
                aRoster = aRoster,
                bRoster = bRoster,
                onBack = { nav.popBackStack() }
            )
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
        composable("admin_exports") {
            AdminExportsScreen(onBack = { nav.popBackStack() })
        }

        composable("admin_import_matches") {
            AdminImportPlayersScreen(onBack = { nav.popBackStack() })
        }

        // Admin stubs so menu clicks don’t crash
        composable("admin_schedule") { AdminSettingsScreen(onBack = { nav.popBackStack() }) }

    }
}
