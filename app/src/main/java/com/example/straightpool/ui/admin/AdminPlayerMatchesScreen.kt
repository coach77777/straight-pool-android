package com.example.straightpool.ui.admin

import androidx.compose.foundation.clickable
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
import com.example.straightpool.data.LeagueMatch
import com.example.straightpool.data.MatchesRepository
import com.example.straightpool.data.PlayersRepoV2

@Composable
fun AdminPlayerMatchesScreen(
    roster: Int,
    onBack: () -> Unit,
    onEditMatch: (week: Int, aRoster: Int, bRoster: Int) -> Unit
) {
    val ctx = LocalContext.current
    val playersRepo = remember { PlayersRepoV2(ctx) }
    val matchesRepo = remember { MatchesRepository(ctx) }

    var playerName by remember { mutableStateOf("") }
    var allPlayers by remember { mutableStateOf(playersRepo.readAll()) }
    var matches by remember { mutableStateOf<List<LeagueMatch>>(emptyList()) }

    LaunchedEffect(Unit) {
        allPlayers = playersRepo.readAll()
        playerName = allPlayers.firstOrNull { it.roster == roster }?.name ?: "Player #$roster"

        matchesRepo.ensureSeededFromAssets("matches_3.csv")
        matches = matchesRepo.getForPlayer(roster)
    }

    fun nameFor(rosterId: Int): String =
        allPlayers.firstOrNull { it.roster == rosterId }?.name ?: "#$rosterId"

    val rows = matches.sortedBy { it.week }

    Surface {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Admin: Player Matches", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("$roster. $playerName")
                }
                OutlinedButton(onClick = onBack) { Text("Back") }
            }

            Spacer(Modifier.height(12.dp))

            if (rows.isEmpty()) {
                Text("No matches found for this player.")
                return@Surface
            }

            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rows.forEach { m ->
                    val oppRoster = if (m.aRoster == roster) m.bRoster else m.aRoster
                    val oppName = nameFor(oppRoster)

                    val scoreLine = when {
                        m.aScore == null || m.bScore == null -> "—"
                        m.aRoster == roster -> "${m.aScore} - ${m.bScore}"
                        else -> "${m.bScore} - ${m.aScore}"
                    }

                    val statusText = when (m.status.lowercase()) {
                        "played" -> "PLAYED"
                        "scheduled" -> "SCHEDULED"
                        "refund" -> "REFUND"
                        else -> m.status.uppercase()
                    }

                    val countedText = if (m.countsForStandings) "COUNTED" else "NOT COUNTED"

                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEditMatch(m.week, m.aRoster, m.bRoster) }
                    ) {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            Text("Week ${m.week}  vs  $oppRoster. $oppName", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(6.dp))
                            Text("Score: $scoreLine")
                            Text("Status: $statusText   $countedText")
                            if (!m.note.isNullOrBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text("Note: ${m.note}")
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("Tap to edit", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}


