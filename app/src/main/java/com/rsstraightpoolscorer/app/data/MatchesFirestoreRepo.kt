package com.rsstraightpoolscorer.app.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

class MatchesFirestoreRepo {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    suspend fun getAllMatchesServer(): List<LeagueMatch> {
        return try {
            val snap = db.collection("matches")
                .get(Source.SERVER)
                .await()

            Log.d("FS", "matches docs=${snap.size()}")

            val list = snap.documents.mapNotNull { d ->

                val week = (d.getLong("week") ?: return@mapNotNull null).toInt()
                val aRoster = (d.getLong("playerA_roster") ?: return@mapNotNull null).toInt()
                val bRoster = (d.getLong("playerB_roster") ?: return@mapNotNull null).toInt()

                val status = d.getString("status") ?: "scheduled"

                val scoreA = (d.getLong("scoreA") ?: d.getDouble("scoreA")?.toLong() ?: 0L).toInt()
                val scoreB = (d.getLong("scoreB") ?: d.getDouble("scoreB")?.toLong() ?: 0L).toInt()

                val note = d.getString("note")
                val counts = d.getBoolean("countsForStandings") ?: false
                val date = d.getString("date_mmdd")

                LeagueMatch(
                    week = week,
                    dateMmDd = date,
                    aRoster = aRoster,
                    bRoster = bRoster,
                    aScore = if (scoreA == 0) null else scoreA,
                    bScore = if (scoreB == 0) null else scoreB,
                    status = status,
                    note = note,
                    countsForStandings = counts
                )
            }

            val m = list.firstOrNull { it.week == 1 && it.aRoster == 11 && it.bRoster == 12 }
            Log.d(
                "FS",
                "W1 11v12 = ${m?.aScore}-${m?.bScore} status=${m?.status} counts=${m?.countsForStandings}"
            )

            list
        } catch (t: Throwable) {
            Log.e("FS", "Firestore getAllMatches FAILED", t)
            emptyList()
        }
    }
}
