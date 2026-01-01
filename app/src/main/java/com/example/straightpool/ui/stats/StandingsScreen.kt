package com.example.straightpool.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.straightpool.data.PlayersRepoV2
import com.example.straightpool.data.RosterPlayer
import androidx.compose.foundation.clickable
import com.example.straightpool.standings.calculateStandings

@Composable
fun StandingsScreen(
    onBack: () -> Unit,
    onPlayerClick: (Int) -> Unit = {}
) {
    val ctx = LocalContext.current

    val repo = remember { PlayersRepoV2(ctx) }
    var roster by remember { mutableStateOf<List<RosterPlayer>>(emptyList()) }

    val matchesRepo = remember { com.example.straightpool.data.MatchesRepository(ctx) }
    var matches by remember { mutableStateOf<List<com.example.straightpool.data.LeagueMatch>>(emptyList()) }

    LaunchedEffect(Unit) {
        matchesRepo.ensureSeededFromAssets("matches_3.csv")
        matches = matchesRepo.getAll()
    }

    LaunchedEffect(Unit) {
        roster = repo.readAll()
            .filter { !it.isBye }
            .map { pr -> RosterPlayer(playerId = pr.roster, name = pr.name) }
            .sortedBy { it.playerId }
    }

    val rows = remember(roster, matches) { calculateStandings(roster, matches) }

    Surface {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Standings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                OutlinedButton(onClick = onBack) { Text("Back") }
            }

            Spacer(Modifier.height(12.dp))

            Text("Counted matches: ${matches.count { it.isPlayed && it.countsForStandings }}")

            Spacer(Modifier.height(12.dp))

            if (roster.isEmpty()) {
                Text("No players yet. Import players.csv in Admin > Players > Import.")
                return@Surface
            }

            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                rows.forEach { r ->
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable {
                                android.util.Log.d("StandingsR", "Clicked roster=${r.roster}")
                                onPlayerClick(r.roster)
                            }
                    ) {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            Text("${r.roster}. ${r.name}", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(6.dp))
                            Text("W: ${r.wins}  L: ${r.losses}  GP: ${r.played}")

                        }
                    }
                }
            }
        }
    }
}
