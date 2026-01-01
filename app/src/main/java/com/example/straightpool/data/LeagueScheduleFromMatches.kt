package com.example.straightpool.data

import android.content.Context

data class MatchScheduleRow(
    val week: Int,
    val dateLabel: String,
    val aRoster: Int,
    val bRoster: Int,
    val status: String
)

fun loadMatchScheduleFromAssets(ctx: Context, assetName: String = "matches_3.csv"): List<MatchScheduleRow> {
    val text = ctx.assets.open(assetName).bufferedReader().use { it.readText() }
    return parseMatchScheduleCsv(text)
}

fun parseMatchScheduleCsv(text: String): List<MatchScheduleRow> {
    val lines = text
        .replace("\r\n", "\n")
        .replace("\r", "\n")
        .split("\n")
        .map { it.trimEnd() }
        .filter { it.isNotBlank() }

    if (lines.isEmpty()) return emptyList()

    val header = splitLooseLine(lines.first())
    fun idx(vararg names: String) = header.indexOfFirst { h -> names.any { it.equals(h, true) } }

    val iWeek   = idx("week")
    val iDate   = idx("date_mmdd", "date", "dateLabel")
    val iA      = idx("playerA_roster", "playerA", "aRoster")
    val iB      = idx("playerB_roster", "playerB", "bRoster")
    val iStatus = idx("status")

    if (iWeek < 0 || iDate < 0 || iA < 0 || iB < 0) return emptyList()

    return lines.drop(1).mapNotNull { line ->
        val p = splitLooseLine(line)
        val week = p.getOrNull(iWeek)?.trim()?.toIntOrNull() ?: return@mapNotNull null
        val date = p.getOrNull(iDate)?.trim().orEmpty()
        val a = p.getOrNull(iA)?.trim()?.toIntOrNull() ?: return@mapNotNull null
        val b = p.getOrNull(iB)?.trim()?.toIntOrNull() ?: return@mapNotNull null
        val status = p.getOrNull(iStatus)?.trim().orEmpty()
        MatchScheduleRow(week, date, a, b, status)
    }
}

private fun splitLooseLine(line: String): List<String> {
    val delim = if (line.contains('\t')) '\t' else ','
    return line.split(delim).map { it.trim().trim('"') }
}


