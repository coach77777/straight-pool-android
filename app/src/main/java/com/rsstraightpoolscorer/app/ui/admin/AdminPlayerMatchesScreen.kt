package com.rsstraightpoolscorer.app.ui.admin

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
import androidx.compose.foundation.layout.width
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

        matchesRepo.ensureSeededFromAssets("remote/matches_3.csv")
        matches = matchesRepo.getForPlayer(roster)
    }

    fun nameFor(rosterId: Int): String =
        allPlayers.firstOrNull { it.roster == rosterId }?.name ?: "#$rosterId"

    val rows = matches.sortedBy { it.week }

    val labelWidth = 72.dp
    val green = Color(0xFF2E7D32)
    val red = Color(0xFFC62828)

    fun isBye(m: LeagueMatch) = m.status.equals("bye", ignoreCase = true)
    fun isPlayed(m: LeagueMatch) = m.status.equals("played", ignoreCase = true)
    fun isScheduled(m: LeagueMatch) = m.status.equals("scheduled", ignoreCase = true)

    Surface {
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(
                        text = "Admin: Player Matches",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "#$roster  $playerName",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
                OutlinedButton(onClick = onBack) { Text("Back") }
            }

            Spacer(Modifier.height(12.dp))

            if (rows.isEmpty()) {
                Text("No matches found for this player.")
                return@Surface
            }

            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rows.forEach { m ->
                    val oppRoster = if (m.aRoster == roster) m.bRoster else m.aRoster
                    val oppName = nameFor(oppRoster)

                    val a = m.aScore
                    val b = m.bScore
                    val haveScore = (a != null && b != null)

                    val myScore = if (!haveScore) null else if (m.aRoster == roster) a else b
                    val oppScore = if (!haveScore) null else if (m.aRoster == roster) b else a

                    val didWin = if (myScore == null || oppScore == null) null else myScore > oppScore
                    val didLose = if (myScore == null || oppScore == null) null else myScore < oppScore

                    val playedText = when {
                        isBye(m) -> "BYE"
                        isPlayed(m) -> "PLAYED"
                        isScheduled(m) -> "SCHEDULED"
                        else -> m.status.uppercase()
                    }

                    val countedWord = if (m.countsForStandings) "COUNTED" else "NOT COUNTED"

                    val playedColor = when {
                        isBye(m) -> Color.Unspecified
                        isPlayed(m) -> green
                        isScheduled(m) -> red
                        else -> Color.Unspecified
                    }

                    val countedColor = when {
                        isBye(m) -> Color.Unspecified
                        m.countsForStandings -> green
                        else -> red
                    }

                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEditMatch(m.week, m.aRoster, m.bRoster) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Week line
                            Text(
                                text = "Week ${m.week}  vs  #$oppRoster  $oppName",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )

                            // Score row (aligned)
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Score:",
                                    modifier = Modifier.width(labelWidth),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Text(
                                    text = buildAnnotatedString {
                                        if (!haveScore) {
                                            append("—")
                                        } else {
                                            val myColor = when {
                                                didWin == true -> green
                                                didLose == true -> red
                                                else -> Color.Unspecified
                                            }
                                            val oppColor = when {
                                                didWin == true -> red
                                                didLose == true -> green
                                                else -> Color.Unspecified
                                            }

                                            withStyle(SpanStyle(color = myColor, fontWeight = FontWeight.SemiBold)) {
                                                append(myScore.toString())
                                            }
                                            append(" - ")
                                            withStyle(SpanStyle(color = oppColor, fontWeight = FontWeight.SemiBold)) {
                                                append(oppScore.toString())
                                            }
                                        }
                                    },
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }

                            // Status row (aligned)
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Status:",
                                    modifier = Modifier.width(labelWidth),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(SpanStyle(color = playedColor, fontWeight = FontWeight.SemiBold)) {
                                            append(playedText)
                                        }
                                        append("   ")
                                        withStyle(SpanStyle(color = countedColor, fontWeight = FontWeight.SemiBold)) {
                                            append(countedWord)
                                        }
                                    },
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }

                            // Note (aligned)
                            if (!m.note.isNullOrBlank()) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "Note:",
                                        modifier = Modifier.width(labelWidth),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = m.note!!,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }

                            Spacer(Modifier.height(2.dp))

                            // Tap to edit (aligned + subtle)
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Spacer(Modifier.width(labelWidth))
                                Text(
                                    text = "Tap to edit",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Unspecified
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
