/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.ui.component

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanzbeatles.innertube.YouTube
import com.nanzbeatles.nanas.LocalDatabase
import com.nanzbeatles.nanas.R
import com.nanzbeatles.nanas.constants.InnerTubeCookieKey
import com.nanzbeatles.nanas.db.entities.PlaylistEntity
import com.nanzbeatles.nanas.extensions.isSyncEnabled
import com.nanzbeatles.nanas.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.logging.Logger

@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    initialTextFieldValue: String? = null,
    allowSyncing: Boolean = true,
    onOpenSpotifyImport: (() -> Unit)? = null,
) {
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()
    var syncedPlaylist by remember { mutableStateOf(false) }
    var showInternalSpotifyDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    val isSignedIn = innerTubeCookie.isNotEmpty()

    if (showInternalSpotifyDialog) {
        SpotifyImportDialog(
            onDismiss = {
                showInternalSpotifyDialog = false
                onDismiss()
            }
        )
        return
    }

    TextFieldDialog(
        icon = { Icon(painter = painterResource(R.drawable.add), contentDescription = null) },
        title = { Text(text = stringResource(R.string.create_playlist)) },
        initialTextFieldValue = TextFieldValue(initialTextFieldValue ?: ""),
        onDismiss = onDismiss,
        onDone = { playlistName ->
            coroutineScope.launch(Dispatchers.IO) {
                val browseId = if (syncedPlaylist && isSignedIn) {
                    YouTube.createPlaylist(playlistName)
                } else if (syncedPlaylist) {
                    Logger.getLogger("CreatePlaylistDialog").warning("Not signed in")
                    return@launch
                } else null

                database.query {
                    insert(
                        PlaylistEntity(
                            name = playlistName,
                            browseId = browseId,
                            bookmarkedAt = LocalDateTime.now(),
                            isEditable = true,
                        )
                    )
                }
            }
        },
        extraContent = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (allowSyncing) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 40.dp)
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.sync_playlist),
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(
                                text = stringResource(R.string.allows_for_sync_witch_youtube),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.fillMaxWidth(0.7f)
                            )
                        }
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Switch(
                                checked = syncedPlaylist,
                                onCheckedChange = {
                                    val isYtmSyncEnabled = context.isSyncEnabled()
                                    if (!isSignedIn && !syncedPlaylist) {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.not_logged_in_youtube),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else if (!isYtmSyncEnabled) {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.sync_disabled),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        syncedPlaylist = !syncedPlaylist
                                    }
                                }
                            )
                        }
                    }
                }

                val spotifyGreen = Color(0xFF1DB954)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = spotifyGreen.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, spotifyGreen.copy(alpha = 0.38f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            if (onOpenSpotifyImport != null) {
                                onOpenSpotifyImport()
                            } else {
                                showInternalSpotifyDialog = true
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        SpotifyLogoIcon(
                            tint = spotifyGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Atau Import Playlist dari URL Spotify",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = spotifyGreen
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    )
}
