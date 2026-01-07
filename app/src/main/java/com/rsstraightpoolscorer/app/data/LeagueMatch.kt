package com.rsstraightpoolscorer.app.data

data class LeagueMatch(
    val week: Int,
    val dateMmDd: String?,
    val aRoster: Int,
    val bRoster: Int,
    val aScore: Int?,
    val bScore: Int?,
    val status: String,
    val note: String?,
    val countsForStandings: Boolean
) {
    val isPlayed: Boolean get() = status.equals("played", ignoreCase = true)
}

