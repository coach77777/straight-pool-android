package com.rsstraightpoolscorer.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.FirebaseFirestore
import com.rsstraightpoolscorer.app.data.PlayersRepoV2
import com.rsstraightpoolscorer.app.data.SeedData
import com.rsstraightpoolscorer.app.scorer.ScorerViewModel
import com.rsstraightpoolscorer.app.ui.AppNav
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var vm: ScorerViewModel

    // Throttle resume-sync so we don't spam Firestore on quick app switches
    private var lastPlayersSyncMs: Long = 0L
    private val PLAYERS_SYNC_COOLDOWN_MS = 30_000L // 30 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Seed CSVs into /files/remote/ so the app never crashes on first launch
        SeedData.copyAssetIfMissing(this, "remote/players.csv", "players.csv")
        SeedData.copyAssetIfMissing(this, "remote_OFF/schedule.csv", "schedule.csv")
        SeedData.copyAssetIfMissing(this, "remote_OFF/weeks_extracted.csv", "weeks_extracted.csv")
        SeedData.copyAssetIfMissing(this, "remote_OFF/matches_3_OFF.csv", "matches_3.csv")

        // Create VM BEFORE setContent, so Activity + Compose use the SAME instance
        vm = ViewModelProvider(this)[ScorerViewModel::class.java]

        // First launch sync
        syncPlayersNow(force = true)

        setContent {
            MaterialTheme {
                Surface {
                    AppNav(vm)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Sync each time app returns to foreground (throttled)
        syncPlayersNow(force = false)
    }

    private fun syncPlayersNow(force: Boolean) {
        val now = System.currentTimeMillis()
        if (!force && (now - lastPlayersSyncMs) < PLAYERS_SYNC_COOLDOWN_MS) return
        lastPlayersSyncMs = now

        lifecycleScope.launch {
            try {
                val repo = PlayersRepoV2(applicationContext)

                val n = repo.syncFromFirestore(
                    firestore = FirebaseFirestore.getInstance(),
                    forceServer = true
                )

                val local5 = repo.readAll().firstOrNull { it.roster == 5 }
                android.util.Log.d("SYNC", "players synced count=$n; local roster5=${local5?.name}")

                // Reload VM roster from local (just overwritten by Firestore sync)
                vm.reloadPlayers(applicationContext)

            } catch (t: Throwable) {
                android.util.Log.e("SYNC", "Players sync failed", t)
            }
        }
    }
}
