package com.rsstraightpoolscorer.app.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.rsstraightpoolscorer.app.db.AppDatabase
import com.rsstraightpoolscorer.app.db.MatchEntity
import com.rsstraightpoolscorer.app.data.PlayersRepoV2
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@Composable
fun AdminEditMatchScreen(
    week: Int,
    aRoster: Int,
    bRoster: Int,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val dao = remember { AppDatabase.get(ctx).matchDao() }

    val scope = rememberCoroutineScope()

    val playersRepo = remember { PlayersRepoV2(ctx) }

    var loaded by remember { mutableStateOf<MatchEntity?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    var aScoreStr by remember { mutableStateOf("") }
    var bScoreStr by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("scheduled") }
    var note by remember { mutableStateOf("") }
    var counted by remember { mutableStateOf(false) }

    fun nameFor(rosterId: Int): String =
        playersRepo.readAll().firstOrNull { it.roster == rosterId }?.name ?: "#$rosterId"

    LaunchedEffect(Unit) {
        val row = dao.getOneEitherOrder(week, aRoster, bRoster)
        if (row == null) {
            error = "Match not found for Week $week ($aRoster vs $bRoster)."
            loaded = null
            return@LaunchedEffect
        }

        loaded = row
        aScoreStr = row.aScore?.toString() ?: ""
        bScoreStr = row.bScore?.toString() ?: ""
        status = row.status.ifBlank { "scheduled" }
        note = row.note ?: ""
        counted = row.countsForStandings
    }

    fun parseScore(s: String): Int? = s.trim().toIntOrNull()

    Surface {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Edit Match", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                OutlinedButton(onClick = onBack) { Text("Back") }
            }

            val row = loaded
            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
                return@Surface
            }

            if (row == null) {
                Text("Loading...")
                return@Surface
            }

            Text("Week $week")
            Text("${row.aRoster}. ${nameFor(row.aRoster)}  vs  ${row.bRoster}. ${nameFor(row.bRoster)}")

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = aScoreStr,
                onValueChange = { aScoreStr = it.filter { ch -> ch.isDigit() }.take(4) },
                label = { Text("Score for ${row.aRoster}") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = bScoreStr,
                onValueChange = { bScoreStr = it.filter { ch -> ch.isDigit() }.take(4) },
                label = { Text("Score for ${row.bRoster}") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = status,
                onValueChange = { status = it },
                label = { Text("Status (played / scheduled / refund)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                OutlinedButton(
                    onClick = { counted = !counted }
                ) {
                    Text(if (counted) "Counted: YES" else "Counted: NO")
                }

                Button(onClick = {
                    val updated = row.copy(
                        aScore = parseScore(aScoreStr),
                        bScore = parseScore(bScoreStr),
                        status = status.trim(),
                        note = note.trim().ifBlank { null },
                        countsForStandings = counted
                    )
                    scope.launch {
                        dao.update(updated)
                        onBack()
                    }
                }) { Text("Save") }
            }
        }
    }
}


