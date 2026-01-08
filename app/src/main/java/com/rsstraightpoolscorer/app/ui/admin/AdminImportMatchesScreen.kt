package com.rsstraightpoolscorer.app.ui.admin

import android.content.Context
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.rsstraightpoolscorer.app.db.AppDatabase
import com.rsstraightpoolscorer.app.db.MatchEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.rsstraightpoolscorer.app.net.RemoteCsvDownloader
import com.rsstraightpoolscorer.app.net.RemoteCsvLinks

@Composable
fun AdminImportMatchesScreen(
    onBack: () -> Unit
) {
    val ctx = LocalContext.current

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var lastSummary by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        scope.launch {
            try {
                val text = ctx.contentResolver.openInputStream(uri)
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    ?: ""

                val parsed = parseMatchesCsv(text)
                if (parsed.isEmpty()) {
                    snackbarHostState.showSnackbar(
                        "No rows imported (check header/format).",
                        duration = SnackbarDuration.Long
                    )
                    return@launch
                }

                val dao = db(ctx).matchDao()

                withContext(Dispatchers.IO) {
                    dao.upsertAll(parsed)
                }

                val msg = "Imported ${parsed.size} matches into Room"
                lastSummary = msg
                snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)

                onBack()
            } catch (e: Exception) {
                Log.e("AdminImportMatches", "Import failed", e)
                snackbarHostState.showSnackbar(
                    "Import failed: ${e.message}",
                    duration = SnackbarDuration.Long
                )
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Surface(Modifier.fillMaxSize().padding(padding)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "Import Matches",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedButton(onClick = onBack) { Text("Back") }
                }

                Text(
                    "Choose a CSV file with header:\n" +
                            "week,dateMmDd,aRoster,bRoster,aScore,bScore,status,note,countsForStandings"
                )

                Button(
                    onClick = {
                        scope.launch {
                            try {
                                snackbarHostState.showSnackbar(
                                    "Downloading matches_3.csv…",
                                    duration = SnackbarDuration.Short
                                )

                                val text = RemoteCsvDownloader.downloadText(
                                    context = ctx,
                                    url = RemoteCsvLinks.MATCHES_3,
                                    filename = "remote/matches_3.csv"
                                )

                                val parsed = parseMatchesCsv(text)
                                if (parsed.isEmpty()) {
                                    snackbarHostState.showSnackbar(
                                        "Downloaded file but no rows imported (check format).",
                                        duration = SnackbarDuration.Long
                                    )
                                    return@launch
                                }

                                val dao = db(ctx).matchDao()
                                withContext(Dispatchers.IO) {
                                    dao.upsertAll(parsed)
                                }

                                val msg = "Downloaded + imported ${parsed.size} matches"
                                lastSummary = msg
                                snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)

                                onBack()
                            } catch (e: Exception) {
                                Log.e("AdminImportMatches", "Download/import failed", e)
                                snackbarHostState.showSnackbar(
                                    "Download/import failed: ${e.message}",
                                    duration = SnackbarDuration.Long
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Download & Import matches_3.csv") }


                Button(
                    onClick = { picker.launch(arrayOf("text/*", "text/csv", "application/csv")) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Pick CSV") }

                if (lastSummary != null) {
                    Text(lastSummary!!)
                }
            }
        }
    }
}

private fun db(ctx: Context): AppDatabase {
    // If your AppDatabase singleton function name differs, change this one line.
    return AppDatabase.get(ctx)
}

private fun parseBoolLoose(s: String): Boolean =
    when (s.trim().lowercase()) {
        "true", "t", "1", "yes", "y" -> true
        else -> false
    }

private fun parseIntLoose(s: String): Int? {
    val t = s.trim()
    if (t.isEmpty()) return null
    return t.toDoubleOrNull()?.toInt() ?: t.toIntOrNull()
}

private fun parseMatchesCsv(csvText: String): List<MatchEntity> {
    val lines = csvText
        .replace("\r\n", "\n")
        .replace("\r", "\n")
        .split('\n')
        .filter { it.isNotBlank() }

    if (lines.isEmpty()) return emptyList()

    val header = lines.first().trim().lowercase()
    val expected =
        "week,datemmdd,aroster,broster,ascore,bscore,status,note,countsforstandings"

    // Light validation (keeps it forgiving)
    if (!header.replace(" ", "").contains("week") || !header.replace(" ", "").contains("countsforstandings")) {
        return emptyList()
    }

    return lines.drop(1).mapNotNull { raw ->
        val p = raw.split(',') // you said matches_3.csv has no quoted commas
        if (p.size < 9) return@mapNotNull null

        val week = p[0].trim().toIntOrNull() ?: return@mapNotNull null
        val date = p[1].trim().ifBlank { null }
        val aRoster = p[2].trim().toIntOrNull() ?: return@mapNotNull null
        val bRoster = p[3].trim().toIntOrNull() ?: return@mapNotNull null

        MatchEntity(
            week = week,
            dateMmDd = date,
            aRoster = aRoster,
            bRoster = bRoster,
            aScore = parseIntLoose(p[4]),
            bScore = parseIntLoose(p[5]),
            status = p[6].trim(),
            note = p[7].trim().ifBlank { null },
            countsForStandings = parseBoolLoose(p[8])
        )
    }
}


