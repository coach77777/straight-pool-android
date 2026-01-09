package com.rsstraightpoolscorer.app.standings

import com.rsstraightpoolscorer.app.data.LeagueMatch
import com.rsstraightpoolscorer.app.data.RosterPlayer

data class StandingsRow(
    val roster: Int,
    val name: String,
    val wins: Int,
    val losses: Int,
    val played: Int
)

fun calculateStandings(
    roster: List<RosterPlayer>,
    matches: List<LeagueMatch>
): List<StandingsRow> {

    data class Acc(var w: Int = 0, var l: Int = 0, var p: Int = 0)
    val acc = mutableMapOf<Int, Acc>()
    fun getAcc(id: Int) = acc.getOrPut(id) { Acc() }

    matches
        .asSequence()
        .filter { it.isPlayed && it.countsForStandings }
        .forEach { m ->
            val aId = m.aRoster
            val bId = m.bRoster

            // If scores are missing, treat as not countable (even if status says played)
            val aScore = m.aScore
            val bScore = m.bScore
            if (aScore == null || bScore == null) return@forEach

            val a = getAcc(aId)
            val b = getAcc(bId)

            a.p += 1
            b.p += 1

            when {
                aScore > bScore -> { a.w += 1; b.l += 1 }
                bScore > aScore -> { b.w += 1; a.l += 1 }
                else -> {
                    // tie: counts as played but no W/L change
                }
            }
        }

    return roster
        .map { rp ->
            val a = acc[rp.playerId] ?: Acc()
            StandingsRow(
                roster = rp.playerId,
                name = rp.name,
                wins = a.w,
                losses = a.l,
                played = a.p
            )
        }
        .sortedWith(
            compareByDescending<StandingsRow> { it.wins }
                .thenBy { it.losses }
                .thenBy { it.roster }
        )
}
