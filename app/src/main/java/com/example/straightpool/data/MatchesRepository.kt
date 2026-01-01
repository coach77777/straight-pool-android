package com.example.straightpool.data

import android.content.Context
import com.example.straightpool.db.AppDatabase
import com.example.straightpool.db.MatchEntity

class MatchesRepository(private val ctx: Context) {

    private val db = AppDatabase.get(ctx)
    private val dao = db.matchDao()

    private val prefs = ctx.getSharedPreferences("straightpool_prefs", Context.MODE_PRIVATE)

    suspend fun ensureSeededFromAssets(assetFile: String = "matches_3.csv") {
        val seeded = prefs.getBoolean("matches_seeded_v1", false)
        if (seeded) return

        val rows = loadLeagueMatchesFromAssets(ctx, assetFile)
        if (rows.isNotEmpty()) {
            dao.upsertAll(rows.map { it.toEntity() })
            prefs.edit().putBoolean("matches_seeded_v1", true).apply()
        }
    }

    suspend fun getAll(): List<LeagueMatch> =
        dao.getAll().map { it.toModel() }

    suspend fun getForPlayer(roster: Int): List<LeagueMatch> =
        dao.getForPlayer(roster).map { it.toModel() }

    suspend fun upsertAll(models: List<LeagueMatch>) {
        dao.upsertAll(models.map { it.toEntity() })
        prefs.edit().putBoolean("matches_seeded_v1", true).apply()
    }

    suspend fun update(model: LeagueMatch) {
        dao.update(model.toEntity())
        prefs.edit().putBoolean("matches_seeded_v1", true).apply()
    }
}

private fun LeagueMatch.toEntity() = MatchEntity(
    week = week,
    dateMmDd = dateMmDd,
    aRoster = aRoster,
    bRoster = bRoster,
    aScore = aScore,
    bScore = bScore,
    status = status,
    note = note,
    countsForStandings = countsForStandings
)

private fun MatchEntity.toModel() = LeagueMatch(
    week = week,
    dateMmDd = dateMmDd,
    aRoster = aRoster,
    bRoster = bRoster,
    aScore = aScore,
    bScore = bScore,
    status = status,
    note = note,
    countsForStandings = countsForStandings
)


