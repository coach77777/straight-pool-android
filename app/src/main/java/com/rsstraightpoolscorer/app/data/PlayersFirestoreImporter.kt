package com.rsstraightpoolscorer.app.data

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.File

object PlayersFirestoreImporter {

    suspend fun importFromLocalFile(context: Context) = withContext(Dispatchers.IO) {

        // Uncomment if you want to hard-disable in release:
        // if (!BuildConfig.DEBUG) {
        //     Log.w("PlayersImport", "Importer disabled in release builds")
        //     return@withContext
        // }

        val db = FirebaseFirestore.getInstance()

        val file = File(context.filesDir, "remote/players.csv")
        if (!file.exists()) {
            Log.e("PlayersImport", "Missing local file: ${file.absolutePath}")
            return@withContext
        }

        val reader = BufferedReader(InputStreamReader(file.inputStream()))
        val lines = reader.readLines()
        reader.close()

        val dataLines = lines.drop(1)

        var successCount = 0

        for (line in dataLines) {
            if (line.isBlank()) continue

            val parts = line.split(",")

            if (parts.size < 2) continue

            val rosterNumber = parts[0].trim().toIntOrNull() ?: continue
            val name = parts[1].trim()
            val phone = parts.getOrNull(2)?.trim()?.ifEmpty { null }
            val email = parts.getOrNull(3)?.trim()?.ifEmpty { null }

            val docId = "roster_%03d".format(rosterNumber)

            val data = mutableMapOf<String, Any>(
                "rosterNumber" to rosterNumber,
                "name" to name
            )
            phone?.let { data["phone"] = it }
            email?.let { data["email"] = it }

            db.collection("players")
                .document(docId)
                .set(data)
                .await()

            successCount++
            Log.d("PlayersImport", "Imported $docId")
        }

        Log.d("PlayersImport", "Import finished: $successCount players")
    }
}
