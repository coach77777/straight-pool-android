package com.example.straightpool.db

import androidx.room.Entity

@Entity(
    tableName = "league_matches",
    primaryKeys = ["week", "aRoster", "bRoster"]
)
data class MatchEntity(
    val week: Int,
    val dateMmDd: String?,
    val aRoster: Int,
    val bRoster: Int,
    val aScore: Int?,
    val bScore: Int?,
    val status: String,
    val note: String?,
    val countsForStandings: Boolean
)


