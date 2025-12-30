package com.example.straightpool.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.straightpool.data.PlayerRow
import com.example.straightpool.data.PlayersRepoV2
import com.example.straightpool.data.WeekEntry
import com.example.straightpool.data.loadWeeksFromAssets
import com.example.straightpool.scorer.ScorerViewModel

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
    val repo = remember { PlayersRepoV2(ctx) }

    var roster by remember { mutableStateOf<List<PlayerRow>>(emptyList()) }
    var weeks by remember { mutableStateOf<List<WeekEntry>>(emptyList()) }

    var targetStr by remember { mutableStateOf("125") }
    var aSel by remember { mutableStateOf<Pair<Int?, String>?>(null) }
    var bSel by remember { mutableStateOf<Pair<Int?, String>?>(null) }
    var wSel by remember { mutableStateOf<WeekEntry?>(null) }

    LaunchedEffect(Unit) {
        roster = repo.readAll().filter { !it.isBye }
        weeks = try { loadWeeksFromAssets(ctx) } catch (_: Throwable) { emptyList() }
        if (wSel == null && weeks.isNotEmpty()) wSel = weeks.first()
    }

    val rosterForA = remember(roster, bSel) {
        val bId = bSel?.first
        roster.filter { it.roster != bId }
    }
    val rosterForB = remember(roster, aSel) {
        val aId = aSel?.first
        roster.filter { it.roster != aId }
    }

    Surface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Match Setup", style = MaterialTheme.typography.headlineSmall)
                OutlinedButton(onClick = onBack) { Text("Back") }
            }

            ExposedDropdown(
                label = "Week / Date",
                value = wSel?.let { "${it.key} — ${it.label}" } ?: "",
                items = weeks.map { it.key to "${it.key} — ${it.label}" },
                onPick = { key -> wSel = weeks.firstOrNull { it.key == key } }
            )

            ExposedDropdown(
                label = "Player A",
                value = aSel?.let { "#${it.first ?: "-"}  ${it.second}" } ?: "",
                items = rosterForA.map { it.roster.toString() to "#${it.roster}  ${it.name}" },
                onPick = { key ->
                    val p = roster.firstOrNull { it.roster.toString() == key }
                    aSel = (p?.roster to (p?.name ?: "Player A"))
                    if (p?.roster == bSel?.first) bSel = null
                }
            )

            ExposedDropdown(
                label = "Player B",
                value = bSel?.let { "#${it.first ?: "-"}  ${it.second}" } ?: "",
                items = rosterForB.map { it.roster.toString() to "#${it.roster}  ${it.name}" },
                onPick = { key ->
                    val p = roster.firstOrNull { it.roster.toString() == key }
                    bSel = (p?.roster to (p?.name ?: "Player B"))
                    if (p?.roster == aSel?.first) aSel = null
                }
            )

            OutlinedTextField(
                value = targetStr,
                onValueChange = { targetStr = it },
                label = { Text("Target score") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(6.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(
                    onClick = {
                        val t = targetStr.toIntOrNull() ?: 125
                        val (aId, aName) = aSel ?: (null to "Player A")
                        val (bId, bName) = bSel ?: (null to "Player B")
                        onStart(t, aId, aName, bId, bName, wSel?.key, wSel?.label)
                    },
                    enabled = aSel != null && bSel != null
                ) { Text("Start Match") }
            }

            if (roster.isEmpty()) {
                Text("No players yet. Import players.csv in Admin > Players > Import.")
            }
            if (weeks.isEmpty()) {
                Text("No weeks yet. Add weeks.csv to assets (for now).")
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
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth()
        )

        // Material3 1.3.x: Use DropdownMenu here (NOT ExposedDropdownMenu)
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.exposedDropdownSize()
        ) {
            items.forEach { (key, labelText) ->
                DropdownMenuItem(
                    text = { Text(labelText) },
                    onClick = {
                        onPick(key)
                        expanded = false
                    }
                )
            }
        }
    }
}
