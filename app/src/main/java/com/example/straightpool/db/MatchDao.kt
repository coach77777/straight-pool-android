package com.example.straightpool.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface MatchDao {

    @Query("SELECT * FROM league_matches ORDER BY week ASC, aRoster ASC, bRoster ASC")
    suspend fun getAll(): List<MatchEntity>

    @Query(
        """
        SELECT * FROM league_matches
        WHERE aRoster = :roster OR bRoster = :roster
        ORDER BY week ASC
        """
    )
    suspend fun getForPlayer(roster: Int): List<MatchEntity>

    @Query(
        """
    SELECT * FROM league_matches
    WHERE week = :week AND aRoster = :aRoster AND bRoster = :bRoster
    LIMIT 1
    """
    )
    suspend fun getOne(week: Int, aRoster: Int, bRoster: Int): MatchEntity?

    @Query(
        """
    SELECT * FROM league_matches
    WHERE week = :week AND (
        (aRoster = :r1 AND bRoster = :r2) OR (aRoster = :r2 AND bRoster = :r1)
    )
    LIMIT 1
    """
    )
    suspend fun getOneEitherOrder(week: Int, r1: Int, r2: Int): MatchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rows: List<MatchEntity>)

    @Update
    suspend fun update(row: MatchEntity)

    @Query("DELETE FROM league_matches")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM league_matches")
    suspend fun count(): Int
}


