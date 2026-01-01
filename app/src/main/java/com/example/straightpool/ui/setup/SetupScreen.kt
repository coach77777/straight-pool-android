package com.example.straightpool.ui.setup

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.straightpool.data.MatchScheduleRow
import com.example.straightpool.data.PlayersRepoV2
import com.example.straightpool.data.loadMatchScheduleFromAssets
import com.example.straightpool.scorer.ScorerViewModel

data class WeekEntry(val key: String, val label: String) // key like "Wk-1"

@Composable
fun SetupScreen(
    vm: ScorerViewModel,
    onStart: (
        target: Int,
        aId: Int?, aName: String,
        bId: Int?, bName: String,
        weekKey: String?, weekLabel: String?
    ) -> Unit,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val playersRepo = remember { PlayersRepoV2(ctx) }

    var roster by remember { mutableStateOf<List<Pair<Int, String>>>(emptyList()) }
    var schedule by remember { mutableStateOf<List<MatchScheduleRow>>(emptyList()) }
    var weeks by remember { mutableStateOf<List<WeekEntry>>(emptyList()) }

    var targetStr by remember { mutableStateOf("125") }
    var aSel by remember { mutableStateOf<Pair<Int, String>?>(null) } // (roster, "#11 Name")
    var bSel by remember { mutableStateOf<Pair<Int, String>?>(null) }
    var wSel by remember { mutableStateOf<WeekEntry?>(null) }

    var lagSel by remember { mutableStateOf<Pair<Int, String>?>(null) } // (roster, "#11 Name")
    var winnerBreaks by remember { mutableStateOf(true) } // true = lag winner breaks

    var matchupWarning by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        // Load players
        val rows = try {
            playersRepo.readAll()
        } catch (_: Throwable) {
            emptyList()
        }
            .filter { !it.isBye }
            .sortedWith(compareBy({ it.name.lowercase() }, { it.roster }))

        roster = rows.map { it.roster to "#${it.roster}  ${it.name}" }

        // Load schedule
        schedule = try {
            loadMatchScheduleFromAssets(ctx, "matches_3.csv")
        } catch (_: Throwable) {
            emptyList()
        }

        // Build Week list from schedule unique weeks
        weeks = schedule
            .map { it.week to it.dateLabel }
            .distinct()
            .sortedBy { it.first }
            .map { (wk, date) -> WeekEntry(key = "Wk-$wk", label = date) }

        if (wSel == null && weeks.isNotEmpty()) wSel = weeks.first()
    }

    fun findWeekForPair(a: Int, b: Int): WeekEntry? {
        val row = schedule.firstOrNull { r ->
            (r.aRoster == a && r.bRoster == b) || (r.aRoster == b && r.bRoster == a)
        } ?: return null

        return WeekEntry(key = "Wk-${row.week}", label = row.dateLabel)
    }

    // Auto-fill week when both players are chosen
    LaunchedEffect(aSel?.first, bSel?.first) {
        val a = aSel?.first
        val b = bSel?.first
        if (a == null || b == null || a == b) {
            matchupWarning = null
            return@LaunchedEffect
        }

        val wk = findWeekForPair(a, b)
        if (wk != null) {
            wSel = wk
            matchupWarning = null
        } else {
            matchupWarning = "These two players don’t appear as a scheduled matchup (in matches_3.csv)."
        }
    }

    LaunchedEffect(aSel?.first, bSel?.first) {
        val a = aSel?.first
        val b = bSel?.first
        val lag = lagSel?.first

        // if lag winner isn't one of the selected players anymore, clear it
        if (lag != null && (a == null || b == null || (lag != a && lag != b))) {
            lagSel = null
        }
    }

    // With these (sorted by roster number):
    val rosterForA = remember(roster, bSel) {
        roster
            .filter { it.first != bSel?.first }
            .sortedBy { it.first }
    }
    val rosterForB = remember(roster, aSel) {
        roster
            .filter { it.first != aSel?.first }
            .sortedBy { it.first }
    }

    Surface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Match Setup", style = MaterialTheme.typography.headlineSmall)
                OutlinedButton(onClick = onBack) { Text("Back") }
            }

            // Tiny debug counters (remove later)
            Text("Players loaded: ${roster.size}")
            Text("Schedule rows: ${schedule.size}")
            Text("Weeks: ${weeks.size}")

            ExposedDropdown(
                label = "Week / Date",
                value = wSel?.let { "${it.key} — ${it.label}" } ?: "",
                items = weeks.map { it.key to "${it.key} — ${it.label}" },
                onPick = { key -> wSel = weeks.firstOrNull { it.key == key } }
            )

            ExposedDropdown(
                label = "Player A",
                value = aSel?.second ?: "",
                items = rosterForA.map { it.first.toString() to it.second },
                onPick = { key ->
                    val pick = roster.firstOrNull { it.first.toString() == key }
                    aSel = pick
                    if (pick?.first == bSel?.first) bSel = null
                }
            )

            ExposedDropdown(
                label = "Player B",
                value = bSel?.second ?: "",
                items = rosterForB.map { it.first.toString() to it.second },
                onPick = { key ->
                    val pick = roster.firstOrNull { it.first.toString() == key }
                    bSel = pick
                    if (pick?.first == aSel?.first) aSel = null
                }
            )

            val lagChoices = remember(aSel, bSel) {
                listOfNotNull(aSel, bSel).distinctBy { it.first }
            }

            ExposedDropdown(
                label = "Lag winner",
                value = lagSel?.second ?: "",
                items = lagChoices.map { it.first.toString() to it.second },
                onPick = { key ->
                    val pick = lagChoices.firstOrNull { it.first.toString() == key }
                    lagSel = pick
                }
            )

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                SegmentedButton(
                    selected = winnerBreaks,
                    onClick = { winnerBreaks = true },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text("Winner breaks") }

                SegmentedButton(
                    selected = !winnerBreaks,
                    onClick = { winnerBreaks = false },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text("Opponent breaks") }
            }


            OutlinedTextField(
                value = targetStr,
                onValueChange = { targetStr = it.filter(Char::isDigit).take(3) },
                label = { Text("Target score") },
                modifier = Modifier.fillMaxWidth()
            )

            if (matchupWarning != null) {
                Text(matchupWarning!!, color = MaterialTheme.colorScheme.error)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = {
                        val t = targetStr.toIntOrNull() ?: 125
                        val a = aSel
                        val b = bSel
                        val lag = lagSel

                        if (a == null || b == null || lag == null) return@Button

                        val aName = a.second.substringAfter("  ").trim()
                        val bName = b.second.substringAfter("  ").trim()

                        vm.startMatch(
                            target = t,
                            aId = a.first, aName = aName,
                            bId = b.first, bName = bName,
                            weekKey = wSel?.key, weekLabel = wSel?.label,
                            lagWinnerId = lag.first,
                            winnerBreaks = winnerBreaks
                        )

                        // optional: navigate to break screen if your flow needs it
                        onStart(t, a.first, aName, b.first, bName, wSel?.key, wSel?.label)
                    },
                    enabled = (aSel != null && bSel != null && lagSel != null && matchupWarning == null)
                ) {
                    Text("Start Match")
                }

            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExposedDropdown(
    label: String,
    value: String,
    items: List<Pair<String, String>>,
    onPick: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEach { (key, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        onPick(key)
                        expanded = false
                    }
                )
            }
        }
    }
}
