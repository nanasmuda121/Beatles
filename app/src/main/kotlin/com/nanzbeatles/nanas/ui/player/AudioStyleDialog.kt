package com.nanzbeatles.nanas.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nanzbeatles.nanas.LocalPlayerConnection
import com.nanzbeatles.nanas.R
import com.nanzbeatles.nanas.constants.AudioStyleMode

@Composable
fun AudioStyleDialog(
    onDismiss: () -> Unit,
    onSaveToDevice: (() -> Unit)? = null,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val currentStyle by playerConnection.audioStyleMode.collectAsState()
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = "Gaya suara",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("audio_style_confirm_button"),
            ) {
                Text(
                    text = "Oke",
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = onSaveToDevice?.let { saveAction ->
            {
                TextButton(
                    onClick = {
                        onDismiss()
                        saveAction()
                    },
                    modifier = Modifier.testTag("audio_style_save_to_device_button"),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.download),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Simpan ke Perangkat",
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                AudioStyleMode.entries.forEach { mode ->
                    val selected = currentStyle == mode
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                playerConnection.setAudioStyleMode(mode)
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                            .testTag("audio_style_option_${mode.name.lowercase()}"),
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = {
                                playerConnection.setAudioStyleMode(mode)
                            },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = mode.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
    )
}
