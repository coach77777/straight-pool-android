package com.rsstraightpoolscorer.app.ui.breakintro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rsstraightpoolscorer.app.scorer.GamePhase
import com.rsstraightpoolscorer.app.scorer.ScorerViewModel

@Composable
fun BreakIntroScreen(
    vm: ScorerViewModel,
    onStart: () -> Unit,
    onBack: () -> Unit
) {
    val g = vm.game
    val safeBreakerIndex = g.breakerIndex.coerceIn(0, g.players.lastIndex)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text("Opening Break", style = MaterialTheme.typography.headlineSmall)
        Text("Breaker: ${g.players[safeBreakerIndex].name}")

        when (g.phase) {
            GamePhase.Opening -> {
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { vm.openingLegalBreak(); onStart() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Legal break") }

                    Button(
                        onClick = { vm.openingLegalBreakWithBall(); onStart() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Legal break (called ball)") }

                    Button(
                        onClick = { vm.openingScratchOnLegalBreak(); onStart() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Scratch on legal break (−1)") }

                    OutlinedButton(
                        onClick = { vm.openingBreakFoul() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Breaking foul −2") }
                }
            }

            GamePhase.AwaitChoiceAfterBreakFoul -> {
                Text("Opponent’s choice after the breaking foul:")

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { vm.openingOpponentAcceptsTable(); onStart() },
                        modifier = Modifier.weight(1f)
                    ) { Text("Accept table (start)") }

                    OutlinedButton(
                        onClick = { vm.openingForceRerack() },
                        modifier = Modifier.weight(1f)
                    ) { Text("Re-rack (same breaker)") }
                }
            }

            GamePhase.Scoring -> {
                Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                    Text("Continue to scoring")
                }
            }
        }

        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onBack) { Text("Back") }
    }
}