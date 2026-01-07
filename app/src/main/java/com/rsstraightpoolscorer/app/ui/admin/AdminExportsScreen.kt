package com.rsstraightpoolscorer.app.ui.admin

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rsstraightpoolscorer.app.db.AppDatabase
import com.rsstraightpoolscorer.app.data.PlayersRepoV2
import kotlinx.coroutines.launch

@Composable
fun AdminExportsScreen(
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val dao = AppDatabase.get(ctx).matchDao()
    val playersRepo = PlayersRepoV2(ctx)

    val saveMatchesCsv = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val rows = dao.getAll()
            val csv = CsvExports.buildMatchesCsv(rows)
            ctx.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(csv.toByteArray(Charsets.UTF_8))
            }
        }
    }

    val saveWeekGridCsv = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val players = playersRepo.readAll()
            val matches = dao.getAll()
            val csv = CsvExports.buildWeekGridCsv(players, matches)
            ctx.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(csv.toByteArray(Charsets.UTF_8))
            }
        }
    }

    Surface {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Admin Exports", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                OutlinedButton(onClick = onBack) { Text("Back") }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { saveMatchesCsv.launch("remote/matches_3.csv") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Export matches_3.csv")
            }

            Button(
                onClick = { saveWeekGridCsv.launch("week_grid.csv") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Export week grid CSV")
            }

            Text(
                "Week grid export uses two rows per player: opponents row then scores row. Scheduled scores are blank. A played match with a dropped player is marked with $ in the opponent row.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
