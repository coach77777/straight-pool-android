package com.rsstraightpoolscorer.app.data

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

object PlayersFirestoreImporter {

    suspend fun importFromAssets(context: Context) = withContext(Dispatchers.IO) {
        val db = FirebaseFirestore.getInstance()

        val inputStream = context.assets.open("remote/players.csv")
        val reader = BufferedReader(InputStreamReader(inputStream))

        val lines = reader.readLines()
        reader.close()

        // Skip header
        val dataLines = lines.drop(1)

        var successCount = 0

        for (line in dataLines) {
            if (line.isBlank()) continue

            val parts = line.split(",")

            if (parts.size < 2) continue

            val rosterNumber = parts[0].trim().toInt()
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
                .addOnSuccessListener {
                    Log.d("PlayersImport", "Imported $docId")
                }
                .addOnFailureListener { e ->
                    Log.e("PlayersImport", "Failed $docId", e)
                }

            successCount++
        }

        Log.d("PlayersImport", "Import finished: $successCount players")
    }
}

