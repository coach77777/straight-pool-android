package com.rsstraightpoolscorer.app.ui.stats

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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.rsstraightpoolscorer.app.data.LeagueMatch
import com.rsstraightpoolscorer.app.data.MatchesFirestoreRepo
import com.rsstraightpoolscorer.app.data.PlayersRepoV2
import com.rsstraightpoolscorer.app.data.RosterPlayer
import com.rsstraightpoolscorer.app.standings.calculateStandings

@Composable
fun StandingsScreen(
    onBack: () -> Unit,
    onPlayerClick: (Int) -> Unit = {}
) {
    val ctx = LocalContext.current
    val playersRepo = remember { PlayersRepoV2(ctx) }
    val fsRepo = remember { MatchesFirestoreRepo() }

    var roster by remember { mutableStateOf<List<RosterPlayer>>(emptyList()) }
    var matches by remember { mutableStateOf<List<LeagueMatch>>(emptyList()) }

    var fsInfo by remember { mutableStateOf("FS: loading...") }

    // One clean init
    LaunchedEffect(Unit) {
        // 1) Load roster (local players)
        roster = playersRepo.readAll()
            .filter { !it.isBye }
            .map { RosterPlayer(playerId = it.roster, name = it.name) }
            .sortedBy { it.playerId }

        // 2) Load matches (Firestore)
        val fsMatches = fsRepo.getAllMatchesServer()
        matches = fsMatches

        // 3) Debug label (so we always know what we got)
        fsInfo = "FS matches=${fsMatches.size}"
    }

    val countedMatches = remember(matches) {
        matches.count { it.isPlayed && it.countsForStandings }
    }

    val rows by remember(roster, matches) {
        derivedStateOf { calculateStandings(roster, matches) }
    }

    Surface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Standings",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(onClick = onBack) { Text("Back") }
            }

            Spacer(Modifier.height(12.dp))

            // Cleaner info line:
            // FS matches=630   Counted: 123
            Text(
                text = buildAnnotatedString {
                    append(fsInfo)
                    append("   ")
                    append("Counted: ")
                    withStyle(
                        SpanStyle(
                            color = Color(0xFF1565C0),
                            fontWeight = FontWeight.SemiBold
                        )
                    ) {
                        append(countedMatches.toString())
                    }
                },
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(12.dp))

            if (roster.isEmpty()) {
                Text("No players yet. Import players.csv in Admin > Players > Import.")
                return@Surface
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                rows.forEach { r ->
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { onPlayerClick(r.roster) }
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            // "#14  Eric Roberts"
                            Text(
                                text = "#${r.roster}  ${r.name}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(Modifier.height(6.dp))

                            // "W: 10   L: 6   GP: 16"
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(
                                        SpanStyle(
                                            color = Color(0xFF2E7D32),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    ) { append("W: ${r.wins}") }

                                    append("   ")

                                    withStyle(
                                        SpanStyle(
                                            color = Color(0xFFC62828),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    ) { append("L: ${r.losses}") }

                                    append("   ")

                                    withStyle(
                                        SpanStyle(
                                            color = Color(0xFF1565C0),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    ) { append("GP: ${r.played}") }
                                },
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }
}
