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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.rsstraightpoolscorer.app.data.LeagueMatch
import com.rsstraightpoolscorer.app.data.MatchesRepository
import com.rsstraightpoolscorer.app.data.PlayersRepoV2
import com.rsstraightpoolscorer.app.data.RosterPlayer
import com.rsstraightpoolscorer.app.standings.calculateStandings
import androidx.compose.ui.text.style.TextOverflow


@Composable
fun StandingsScreen(
    onBack: () -> Unit,
    onPlayerClick: (Int) -> Unit = {}
) {
    val ctx = LocalContext.current

    val playersRepo = remember { PlayersRepoV2(ctx) }
    val matchesRepo = remember { MatchesRepository(ctx) }

    var roster by remember { mutableStateOf<List<RosterPlayer>>(emptyList()) }
    var matches by remember { mutableStateOf<List<LeagueMatch>>(emptyList()) }

    LaunchedEffect(Unit) {
        matchesRepo.ensureSeededFromAssets("remote/matches_3.csv")
        matches = matchesRepo.getAll()
    }

    LaunchedEffect(Unit) {
        roster = playersRepo.readAll()
            .filter { !it.isBye }
            .map { RosterPlayer(playerId = it.roster, name = it.name) }
            .sortedBy { it.playerId }
    }

    val rows = remember(roster, matches) {
        calculateStandings(roster, matches)
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

            Text(
                text = buildAnnotatedString {
                    append("Counted matches: ")
                    withStyle(
                        SpanStyle(
                            color = Color(0xFF1565C0),
                            fontWeight = FontWeight.SemiBold
                        )
                    ) {
                        append(matches.count { it.isPlayed && it.countsForStandings }.toString())
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

                            // EXACT format you want:
                            // "#14  Eric Roberts"
                            Text(
                                text = "#${r.roster}  ${r.name}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(Modifier.height(6.dp))

                            // EXACT format you want:
                            // "W: 10   L: 6   GP: 16" (with colors)
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
