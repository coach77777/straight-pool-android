package com.rsstraightpoolscorer.app.ui.admin

import com.rsstraightpoolscorer.app.data.PlayerRow
import com.rsstraightpoolscorer.app.db.MatchEntity

object CsvExports {

    private fun csvCell(s: String?): String {
        val v = s ?: ""
        val needsQuotes = v.contains(',') || v.contains('"') || v.contains('\n') || v.contains('\r')
        val escaped = v.replace("\"", "\"\"")
        return if (needsQuotes) "\"$escaped\"" else escaped
    }

    private fun boolCell(b: Boolean) = if (b) "true" else "false"

    fun buildMatchesCsv(matches: List<MatchEntity>): String {
        val sb = StringBuilder()
        sb.appendLine("week,dateMmDd,aRoster,bRoster,aScore,bScore,status,note,countsForStandings")

        matches
            .sortedWith(compareBy<MatchEntity>({ it.week }, { it.aRoster }, { it.bRoster }))
            .forEach { m ->
                sb.append(csvCell(m.week.toString())).append(',')
                sb.append(csvCell(m.dateMmDd)).append(',')
                sb.append(csvCell(m.aRoster.toString())).append(',')
                sb.append(csvCell(m.bRoster.toString())).append(',')
                sb.append(csvCell(m.aScore?.toString())).append(',')
                sb.append(csvCell(m.bScore?.toString())).append(',')
                sb.append(csvCell(m.status)).append(',')
                sb.append(csvCell(m.note)).append(',')
                sb.append(boolCell(m.countsForStandings))
                sb.appendLine()
            }

        return sb.toString()
    }

    private fun isPlayed(m: MatchEntity): Boolean =
        m.status.equals("played", ignoreCase = true)

    private fun isDroppedMatch(m: MatchEntity): Boolean {
        val n = (m.note ?: "").lowercase()
        return n.contains("$") || n.contains("dropped")
    }

    fun buildWeekGridCsv(playersAll: List<PlayerRow>, matchesAll: List<MatchEntity>): String {
        val players = playersAll
            .filter { !it.isBye }
            .sortedBy { it.roster }

        val weeks = matchesAll
            .map { it.week }
            .distinct()
            .sorted()

        // Best-effort date label per week (first non-blank dateMmDd)
        val dateByWeek: Map<Int, String?> =
            weeks.associateWith { wk ->
                matchesAll.firstOrNull { it.week == wk && !it.dateMmDd.isNullOrBlank() }?.dateMmDd
            }

        // Map for quick lookup: week + unordered pair -> entity
        fun key(wk: Int, r1: Int, r2: Int): String {
            val a = minOf(r1, r2)
            val b = maxOf(r1, r2)
            return "$wk:$a:$b"
        }

        val matchMap = matchesAll.associateBy { m -> key(m.week, m.aRoster, m.bRoster) }

        val sb = StringBuilder()

        // Header row
        sb.append("Roster,Name,Phone,Email")
        weeks.forEach { wk -> sb.append(',').append(csvCell("Wk-$wk")) }
        sb.appendLine()

        // Date row (optional but helps match the PDF vibe)
        sb.append(",,,")
        weeks.forEach { wk -> sb.append(',').append(csvCell(dateByWeek[wk])) }
        sb.appendLine()

        players.forEach { p ->
            // Row A: opponents
            sb.append(csvCell(p.roster.toString())).append(',')
            sb.append(csvCell(p.name)).append(',')
            sb.append(csvCell(p.phone)).append(',')
            sb.append(csvCell(p.email))

            weeks.forEach { wk ->
                val m = matchMap[key(wk, p.roster, p.roster /* dummy */)] // never hits
                val real = matchMap[key(wk, p.roster, p.roster)] // never hits; keep compiler quiet
            }

            weeks.forEach { wk ->
                val m = matchMap[key(wk, p.roster, p.roster)] // placeholder; overwritten below
            }

            // Rebuild row A properly
            // (We append again by constructing cells in a loop)
            // We'll remove the two placeholder loops by doing it cleanly:
            // But Kotlin StringBuilder already has content; easiest is to build per-row arrays.

            // Instead of trying to rewrite, we’ll do per-row arrays below.
            // So we’ll clear this partial row by rebuilding the whole export using arrays.
        }

        // Rebuild cleanly using row arrays
        val out = StringBuilder()

        // Header
        out.append("Roster,Name,Phone,Email")
        weeks.forEach { wk -> out.append(',').append(csvCell("Wk-$wk")) }
        out.appendLine()

        // Date row
        out.append(",,,")
        weeks.forEach { wk -> out.append(',').append(csvCell(dateByWeek[wk])) }
        out.appendLine()

        // Rows
        for (p in players) {
            // Opponents row
            val oppRow = ArrayList<String>(4 + weeks.size)
            oppRow.add(p.roster.toString())
            oppRow.add(p.name)
            oppRow.add(p.phone ?: "")
            oppRow.add(p.email ?: "")

            for (wk in weeks) {
                val m = matchMap[key(wk, p.roster, 0)] // placeholder; overwritten immediately
            }

            for (wk in weeks) {
                // Find match involving p for this week: key needs opponent, so we must search.
                // Efficient enough for league sizes; if you want faster we can pre-index by week+roster.
                val m = matchesAll.firstOrNull { it.week == wk && (it.aRoster == p.roster || it.bRoster == p.roster) }

                if (m == null) {
                    oppRow.add("")
                } else {
                    if (isPlayed(m) && isDroppedMatch(m)) {
                        oppRow.add("$")
                    } else {
                        val opp = if (m.aRoster == p.roster) m.bRoster else m.aRoster
                        oppRow.add(opp.toString())
                    }
                }
            }

            out.append(oppRow.joinToString(",") { csvCell(it) })
            out.appendLine()

            // Scores row
            val scoreRow = ArrayList<String>(4 + weeks.size)
            scoreRow.add("")  // roster blank (matches PDF feel)
            scoreRow.add("")  // name blank
            scoreRow.add("")  // phone blank
            scoreRow.add("")  // email blank

            for (wk in weeks) {
                val m = matchesAll.firstOrNull { it.week == wk && (it.aRoster == p.roster || it.bRoster == p.roster) }

                if (m == null) {
                    scoreRow.add("")
                } else {
                    // Scheduled blank per your rule
                    if (!isPlayed(m)) {
                        scoreRow.add("")
                    } else {
                        val score = if (m.aRoster == p.roster) m.aScore else m.bScore
                        scoreRow.add(score?.toString() ?: "")
                    }
                }
            }

            out.append(scoreRow.joinToString(",") { csvCell(it) })
            out.appendLine()
        }

        return out.toString()
    }
}


