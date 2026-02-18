package com.rsstraightpoolscorer.app.ui.start

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rsstraightpoolscorer.app.R

@Composable
fun StartScreen(
    onStart: () -> Unit,
    onContacts: () -> Unit,
    onStats: () -> Unit,
    onAdmin: () -> Unit
) {
    val ctx = LocalContext.current
    val version = ctx.packageManager
        .getPackageInfo(ctx.packageName, 0).versionName

    Surface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.rs_logo),
                contentDescription = "Red Shoes Billiards",
                modifier = Modifier
                    .width(180.dp)
                    .padding(12.dp),
                contentScale = ContentScale.Fit
            )

            Text(
                text = "Straight Pool 14.1",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text("Start") }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onContacts, modifier = Modifier.fillMaxWidth()) { Text("Contacts") }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onStats, modifier = Modifier.fillMaxWidth()) { Text("Player Stats") }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onAdmin, modifier = Modifier.fillMaxWidth()) { Text("Admin") }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Version $version",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
