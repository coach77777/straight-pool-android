package com.rsstraightpoolscorer.app.ui.contacts

import androidx.activity.compose.BackHandler
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rsstraightpoolscorer.app.data.PlayerRow
import com.rsstraightpoolscorer.app.data.PlayersRepoV2
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.foundation.layout.PaddingValues

private val PillShape = RoundedCornerShape(999.dp)

private object ActionColors {
    val call = Color(0xFF2E7D32)   // green
    val text = Color(0xFFC62828)   // red
    val email = Color(0xFF1565C0)  // blue
    val edit = Color(0xFF6A1B9A)   // purple (not used in contacts, but shared palette)
    val on = Color.White
}

@Composable
private fun PillActionButton(
    label: String,
    bg: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = PillShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = bg,
            contentColor = ActionColors.on,
            disabledContainerColor = bg.copy(alpha = 0.35f),
            disabledContentColor = ActionColors.on.copy(alpha = 0.65f)
        ),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp),
        modifier = modifier.height(44.dp)
    ) {
        Text(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun ActionRow3(
    canCallOrText: Boolean,
    canEmail: Boolean,
    onCall: () -> Unit,
    onText: () -> Unit,
    onEmail: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PillActionButton("Call", ActionColors.call, canCallOrText, onCall, Modifier.weight(1f))
        PillActionButton("Text", ActionColors.text, canCallOrText, onText, Modifier.weight(1f))
        PillActionButton("Email", ActionColors.email, canEmail, onEmail, Modifier.weight(1f))

        // Empty 4th slot so widths match the Admin 4-button row
        Spacer(Modifier.weight(1f).height(44.dp))
    }
}

@Composable
fun ContactsScreen(onBack: () -> Unit) {
    BackHandler { onBack() }

    val ctx = LocalContext.current
    val repo = remember { PlayersRepoV2(ctx) }

    var query by remember { mutableStateOf("") }
    var players by remember { mutableStateOf(emptyList<PlayerRow>()) }

    fun refreshPlayers() {
        val list = repo.readAll()
        players = list

        val p5 = list.firstOrNull { it.roster == 5 }
        android.util.Log.d("PLAYERS_UI", "Contacts refresh -> roster5=${p5?.name} (count=${list.size})")
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshPlayers()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) { refreshPlayers() }

    val q = query.trim().lowercase()
    val filtered = players
        .filter { p ->
            q.isEmpty() || p.name.lowercase().contains(q) || p.roster.toString().contains(q)
        }
        .sortedBy { it.roster }

    fun dial(phone: String) {
        ctx.startActivity(Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:$phone") })
    }

    fun sms(phone: String) {
        ctx.startActivity(Intent(Intent.ACTION_SENDTO).apply { data = Uri.parse("smsto:$phone") })
    }

    fun email(addr: String) {
        ctx.startActivity(Intent(Intent.ACTION_SENDTO).apply { data = Uri.parse("mailto:$addr") })
    }


    Surface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "Contacts",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(onClick = onBack) { Text("Back") }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search") }
            )

            Spacer(Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                filtered.forEach { p ->
                    ContactCard(
                        p = p,
                        onCall = { p.phone?.let { dial(it) } },
                        onText = { p.phone?.let { sms(it) } },
                        onEmail = { p.email?.let { email(it) } }
                    )
                }

                if (players.isEmpty()) {
                    Text("No players yet. Import players.csv in Admin > Players > Import.")
                }
            }
        }
    }
}

@Composable
private fun ContactCard(
    p: PlayerRow,
    onCall: () -> Unit,
    onText: () -> Unit,
    onEmail: () -> Unit
) {
    val canCallOrText = !p.phone.isNullOrBlank()
    val canEmail = !p.email.isNullOrBlank()

    Surface(tonalElevation = 1.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "#${p.roster} ${p.name}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            ActionRow3(
                canCallOrText = canCallOrText,
                canEmail = canEmail,
                onCall = onCall,
                onText = onText,
                onEmail = onEmail
            )
        }
    }
}
