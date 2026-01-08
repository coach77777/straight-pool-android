package com.rsstraightpoolscorer.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rsstraightpoolscorer.app.data.SeedData
import com.rsstraightpoolscorer.app.scorer.ScorerViewModel
import com.rsstraightpoolscorer.app.ui.AppNav

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val f = java.io.File(filesDir, "remote/players.csv")
        android.util.Log.d("Seed", "players exists=${f.exists()} size=${f.length()}")

        // Seed CSVs into /files/remote/ so the app never crashes on first launch
        // Seed CSVs into /files/remote/
        SeedData.copyAssetIfMissing(this, "remote/players.csv", "players.csv")
        SeedData.copyAssetIfMissing(this, "remote/matches_3.csv", "matches_3.csv")
        SeedData.copyAssetIfMissing(this, "remote/schedule.csv", "schedule.csv")
        SeedData.copyAssetIfMissing(this, "remote/weeks_extracted.csv", "weeks_extracted.csv")

        setContent {
            val vm: ScorerViewModel = viewModel()
            MaterialTheme {
                Surface { AppNav(vm) }
            }
        }
    }
}

