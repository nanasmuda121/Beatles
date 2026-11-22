/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.nanzbeatles.nanas.R
import com.nanzbeatles.nanas.utils.UpdateInfo

/**
 * Optional update dialog shown when a newer versionCode is available in version.json.
 * - Title: "Versi terbaru {versionName}"
 * - Body: changelog text (scrollable for multi-line notes)
 * - Buttons: "Nanti" (persists dismissed versionCode) and "Update" (hidden if url is empty or not http/https)
 */
@Composable
fun UpdateAvailableDialog(
    updateInfo: UpdateInfo,
    onUpdate: () -> Unit,
    onLater: () -> Unit,
    onDismiss: () -> Unit,
) {
    DefaultDialog(
        onDismiss = onDismiss,
        icon = {
            Icon(
                painter = painterResource(R.drawable.update),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = {
            Text(text = "Versi terbaru ${updateInfo.versionName}")
        },
        horizontalAlignment = Alignment.Start,
        buttons = {
            TextButton(onClick = onLater) {
                Text(text = "Nanti")
            }
            if (updateInfo.hasValidDownloadUrl) {
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onUpdate) {
                    Text(text = "Update")
                }
            }
        },
    ) {
        val changelogText = updateInfo.changelog.ifBlank {
            "Versi terbaru ${updateInfo.versionName} telah tersedia."
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 280.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = changelogText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
