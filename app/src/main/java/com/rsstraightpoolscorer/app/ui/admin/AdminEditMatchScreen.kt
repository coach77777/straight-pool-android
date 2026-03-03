package com.rsstraightpoolscorer.app.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.rsstraightpoolscorer.app.data.LeagueMatch
import com.rsstraightpoolscorer.app.data.MatchesFirestoreRepo
import com.rsstraightpoolscorer.app.data.PlayersRepoV2
import kotlin.math.max
import kotlin.math.min

@Composable
fun AdminEditMatchScreen(
    week: Int,
    aRoster: Int,
    bRoster: Int,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val playersRepo = remember { PlayersRepoV2(ctx) }
    val fsRepo = remember { MatchesFirestoreRepo() }

    var loaded by remember { mutableStateOf<LeagueMatch?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    var aScoreStr by remember { mutableStateOf("") }
    var bScoreStr by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("scheduled") }
    var note by remember { mutableStateOf("") }
    var counted by remember { mutableStateOf(false) }

    val statusOptions = listOf("played", "scheduled", "refund", "bye")
    var statusExpanded by remember { mutableStateOf(false) }

    fun nameFor(rosterId: Int): String =
        playersRepo.readAll().firstOrNull { it.roster == rosterId }?.name ?: "#$rosterId"

    fun parseScore(s: String): Int? = s.trim().toIntOrNull()

    // Canonical doc id so we never depend on A/B ordering
    val ra = min(aRoster, bRoster)
    val rb = max(aRoster, bRoster)
    val docId = "${week}_${ra}_${rb}"

    LaunchedEffect(week, aRoster, bRoster) {
        error = null
        loaded = null

        val all = fsRepo.getAllMatchesServer()

        val match = all.firstOrNull { m ->
            m.week == week && (
                    (m.aRoster == aRoster && m.bRoster == bRoster) ||
                            (m.aRoster == bRoster && m.bRoster == aRoster) ||
                            (m.aRoster == ra && m.bRoster == rb)
                    )
        }

        if (match == null) {
            error = "Match not found in Firestore for Week $week ($aRoster vs $bRoster)."
            return@LaunchedEffect
        }

        loaded = match
        aScoreStr = match.aScore?.toString() ?: ""
        bScoreStr = match.bScore?.toString() ?: ""
        status = match.status.ifBlank { "scheduled" }.lowercase()
        note = match.note ?: ""
        counted = match.countsForStandings
    }

    Surface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "Edit Match",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(onClick = onBack) { Text("Back") }
            }

            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
                return@Surface
            }

            val row = loaded
            if (row == null) {
                Text("Loading...")
                return@Surface
            }

            Text("Week $week")
            Text("$aRoster. ${nameFor(aRoster)}  vs  $bRoster. ${nameFor(bRoster)}")

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = aScoreStr,
                onValueChange = { aScoreStr = it.filter { ch -> ch.isDigit() }.take(4) },
                label = { Text("Score for $aRoster") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = bScoreStr,
                onValueChange = { bScoreStr = it.filter { ch -> ch.isDigit() }.take(4) },
                label = { Text("Score for $bRoster") },
                modifier = Modifier.fillMaxWidth()
            )

            // No icons. Button opens a dropdown.
            Column(Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { statusExpanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Status: ${status.uppercase()}")
                }

                DropdownMenu(
                    expanded = statusExpanded,
                    onDismissRequest = { statusExpanded = false }
                ) {
                    statusOptions.forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(opt.uppercase()) },
                            onClick = {
                                status = opt
                                statusExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            // APPROVED ROW (own line)  ✅ FIXED
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (counted) "Approved (Counted): YES" else "Approved (Counted): NO",
                    fontWeight = FontWeight.SemiBold
                )

                Switch(
                    checked = counted,
                    onCheckedChange = { counted = it }
                )
            }

            Spacer(Modifier.height(16.dp))

            // SAVE BUTTON ROW (own line)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = {
                        val db = FirebaseFirestore.getInstance()

                        val finalNote =
                            if (status == "played" && counted) null
                            else note.trim().ifBlank { null }

                        val data = mapOf(
                            "week" to week,
                            "matchKey" to docId,
                            "playerA_roster" to ra,
                            "playerB_roster" to rb,
                            "scoreA" to parseScore(aScoreStr),
                            "scoreB" to parseScore(bScoreStr),
                            "status" to status.trim(),
                            "note" to finalNote,
                            "countsForStandings" to counted
                        )

                        db.collection("matches")
                            .document(docId)
                            .set(data)
                            .addOnSuccessListener { onBack() }
                            .addOnFailureListener { e ->
                                error = "Save failed: ${e.message}"
                            }
                    }
                ) {
                    Text("Save")
                }
            }
        }
    }
}