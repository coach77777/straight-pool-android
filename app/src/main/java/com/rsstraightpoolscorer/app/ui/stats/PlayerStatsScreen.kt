package com.rsstraightpoolscorer.app.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

enum class MatchFilter { All, Pending, Played, Scheduled }

private fun LeagueMatch.isBye(): Boolean =
    status.equals("bye", ignoreCase = true)

private fun LeagueMatch.isPlayed(): Boolean =
    status.equals("played", ignoreCase = true)

private fun LeagueMatch.isScheduled(): Boolean =
    status.equals("scheduled", ignoreCase = true)

/**
 * "Pending" in iOS UI:
 * - match played but NOT counted yet
 * - OR explicitly tagged pending
 */
private fun LeagueMatch.isPending(): Boolean =
    (isPlayed() && !countsForStandings) ||
            (note?.contains("PENDING", ignoreCase = true) == true) ||
            status.equals("pending", ignoreCase = true)

@Composable
private fun MatchFilterRow(
    value: MatchFilter,
    onChange: (MatchFilter) -> Unit,
) {
    val tabs = listOf(
        MatchFilter.All to "All",
        MatchFilter.Pending to "Pending",
        MatchFilter.Played to "Played",
        MatchFilter.Scheduled to "Scheduled"
    )

    val outerShape = RoundedCornerShape(50)
    val innerShape = RoundedCornerShape(50)

    Surface(
        shape = outerShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tabs.forEach { (filter, title) ->
                val isSelected = value == filter

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(innerShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.surface
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable { onChange(filter) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun PlayerStatsScreen(
    roster: Int,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val playersRepo = remember { PlayersRepoV2(ctx) }
    val fsRepo = remember { MatchesFirestoreRepo() }

    var playerName by remember { mutableStateOf("") }
    var allPlayers by remember { mutableStateOf(playersRepo.readAll()) }
    var allMatches by remember { mutableStateOf<List<LeagueMatch>>(emptyList()) }
    var filter by remember { mutableStateOf(MatchFilter.All) }

    LaunchedEffect(roster) {
        allPlayers = playersRepo.readAll()
        playerName = allPlayers.firstOrNull { it.roster == roster }?.name ?: "Player #$roster"
        allMatches = fsRepo.getAllMatchesServer()
    }

    fun nameFor(rosterId: Int): String =
        allPlayers.firstOrNull { it.roster == rosterId }?.name ?: "#$rosterId"

    val allRows = remember(allMatches, roster) {
        allMatches
            .filter { it.aRoster == roster || it.bRoster == roster }
            .sortedBy { it.week }
    }

    val rows = remember(allRows, filter) {
        when (filter) {
            MatchFilter.All -> allRows
            MatchFilter.Pending -> allRows.filter { it.isPending() }
            MatchFilter.Played -> allRows.filter { it.isPlayed() }
            MatchFilter.Scheduled -> allRows.filter { it.isScheduled() }
        }
    }

    val countedPlayed = remember(allRows) {
        allRows.filter { it.isPlayed() && it.countsForStandings }
    }

    val wins = countedPlayed.count { m ->
        val a = m.aScore
        val b = m.bScore
        if (a == null || b == null) return@count false
        if (m.aRoster == roster) a > b else b > a
    }

    val losses = countedPlayed.count { m ->
        val a = m.aScore
        val b = m.bScore
        if (a == null || b == null) return@count false
        if (m.aRoster == roster) a < b else b < a
    }

    val gp = wins + losses

    val labelWidth = 88.dp
    val green = Color(0xFF2E7D32)
    val red = Color(0xFFC62828)
    val blue = Color(0xFF1565C0)

    Surface {
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Back on its own row (top-right)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(onClick = onBack) { Text("Back") }
            }

            Spacer(Modifier.height(8.dp))

            // Header block
            Text(
                text = "Player Stats",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "#$roster  $playerName",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = green, fontWeight = FontWeight.SemiBold)) {
                        append("W: $wins")
                    }
                    append("   ")
                    withStyle(SpanStyle(color = red, fontWeight = FontWeight.SemiBold)) {
                        append("L: $losses")
                    }
                    append("   ")
                    withStyle(SpanStyle(color = blue, fontWeight = FontWeight.SemiBold)) {
                        append("GP: $gp")
                    }
                },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1
            )

            Spacer(Modifier.height(12.dp))

            MatchFilterRow(value = filter, onChange = { filter = it })

            Spacer(Modifier.height(14.dp))

            Text(
                text = "Matches",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            if (rows.isEmpty()) {
                Text("No matches found for this player.")
                return@Surface
            }

            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        m.isBye() -> "BYE"
                        m.isPlayed() -> "PLAYED"
                        m.isScheduled() -> "SCHEDULED"
                        else -> m.status.uppercase()
                    }

                    val playedColor = when {
                        m.isBye() -> Color.Unspecified
                        m.isPlayed() -> green
                        m.isScheduled() -> red
                        else -> Color.Unspecified
                    }

                    val countedText = if (m.countsForStandings) "COUNTED" else "NOT COUNTED"
                    val countedColor = when {
                        m.isBye() -> Color.Unspecified
                        m.countsForStandings -> green
                        else -> red
                    }

                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // iOS-like top section: Week then Opponent on next line (wraps)
                            Text(
                                text = "Week ${m.week}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )

                            Text(
                                text = "Opponent: #$oppRoster  $oppName",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(Modifier.height(4.dp))

                            // Score row
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Score:",
                                    modifier = Modifier.width(labelWidth),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

                            // Status row
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Status:",
                                    modifier = Modifier.width(labelWidth),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = playedText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = playedColor
                                )
                            }

                            // Counted row
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Counted:",
                                    modifier = Modifier.width(labelWidth),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = countedText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = countedColor
                                )
                            }

                            // Note
                            if (!m.note.isNullOrBlank()) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "Note:",
                                        modifier = Modifier.width(labelWidth),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = m.note ?: "",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}