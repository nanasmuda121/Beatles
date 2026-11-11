/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.ui.component

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import com.nanzbeatles.nanas.R
import com.nanzbeatles.nanas.constants.EnableBetterLyricsKey
import com.nanzbeatles.nanas.constants.EnableKugouKey
import com.nanzbeatles.nanas.constants.EnableLrcLibKey
import com.nanzbeatles.nanas.constants.EnableMusixmatchKey
import com.nanzbeatles.nanas.constants.EnablePaxsenixKey
import com.nanzbeatles.nanas.constants.EnableRushLyricsKey
import com.nanzbeatles.nanas.constants.EnableSimpMusicKey
import com.nanzbeatles.nanas.constants.LyricsProviderOrderKey
import com.nanzbeatles.nanas.constants.PreferredLyricsProvider
import com.nanzbeatles.nanas.constants.PreferredLyricsProviderKey
import com.nanzbeatles.nanas.db.MusicDatabase
import com.nanzbeatles.nanas.db.entities.LyricsEntity
import com.nanzbeatles.nanas.di.LyricsHelperEntryPoint
import com.nanzbeatles.nanas.lyrics.LyricsHelper
import com.nanzbeatles.nanas.lyrics.LyricsProviderRegistry
import com.nanzbeatles.nanas.lyrics.LyricsTranslationHelper
import com.nanzbeatles.nanas.models.MediaMetadata
import com.nanzbeatles.nanas.playback.PlayerConnection
import com.nanzbeatles.nanas.utils.dataStore
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Coordinates live, zero-cache-clear lyrics provider switching directly from the Player.
 */
object LyricsProviderSwitchState {
    private val switchScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var activeSwitchJob: Job? = null

    private val _isSwitching = MutableStateFlow(false)
    val isSwitching: StateFlow<Boolean> = _isSwitching.asStateFlow()

    private val _switchingProviderName = MutableStateFlow<String?>(null)
    val switchingProviderName: StateFlow<String?> = _switchingProviderName.asStateFlow()

    private val _switchGeneration = MutableStateFlow(0)
    val switchGeneration: StateFlow<Int> = _switchGeneration.asStateFlow()

    fun switchProviderAsync(
        context: Context,
        database: MusicDatabase,
        playerConnection: PlayerConnection?,
        mediaMetadata: MediaMetadata?,
        newProvider: PreferredLyricsProvider,
        showToast: Boolean = true,
    ) {
        val appContext = context.applicationContext
        activeSwitchJob?.cancel()
        activeSwitchJob = switchScope.launch {
            switchProviderImmediately(
                context = appContext,
                database = database,
                playerConnection = playerConnection,
                mediaMetadata = mediaMetadata,
                newProvider = newProvider,
                showToast = showToast,
            )
        }
    }

    suspend fun switchProviderImmediately(
        context: Context,
        database: MusicDatabase,
        playerConnection: PlayerConnection?,
        mediaMetadata: MediaMetadata?,
        newProvider: PreferredLyricsProvider,
        showToast: Boolean = true,
    ) {
        val displayName = LyricsHelper.displayNameOf(newProvider)
        val registryName = LyricsHelper.providerNameOf(newProvider)
        _isSwitching.value = true
        _switchingProviderName.value = displayName

        try {
            // 1. Persist new preferred provider & ensure it is enabled in DataStore
            context.dataStore.edit { prefs ->
                prefs[PreferredLyricsProviderKey] = newProvider.name
                when (newProvider) {
                    PreferredLyricsProvider.SIMPMUSIC -> prefs[EnableSimpMusicKey] = true
                    PreferredLyricsProvider.BETTER_LYRICS -> prefs[EnableBetterLyricsKey] = true
                    PreferredLyricsProvider.LRCLIB -> prefs[EnableLrcLibKey] = true
                    PreferredLyricsProvider.RUSH_LYRICS -> prefs[EnableRushLyricsKey] = true
                    PreferredLyricsProvider.PAXSENIX -> prefs[EnablePaxsenixKey] = true
                    PreferredLyricsProvider.MUSIXMATCH -> prefs[EnableMusixmatchKey] = true
                    PreferredLyricsProvider.KUGOU -> prefs[EnableKugouKey] = true
                }
                val currentOrder = prefs[LyricsProviderOrderKey]
                if (!currentOrder.isNullOrBlank()) {
                    val orderedList = LyricsProviderRegistry.deserializeProviderOrder(currentOrder)
                        .filter { it != registryName }
                    prefs[LyricsProviderOrderKey] = LyricsProviderRegistry.serializeProviderOrder(
                        listOf(registryName) + orderedList
                    )
                }
            }

            // 2. Automatically clear in-memory lyrics & translation caches (no manual cache clear needed)
            val appContext = context.applicationContext
            val entryPoint = EntryPointAccessors.fromApplication(
                appContext,
                LyricsHelperEntryPoint::class.java
            )
            val lyricsHelper = entryPoint.lyricsHelper()
            lyricsHelper.invalidateCache(mediaMetadata?.id)
            LyricsTranslationHelper.clearCache()

            // 3. Immediately fetch lyrics from the selected provider for the active song and overwrite Room DB
            if (mediaMetadata != null) {
                val fetched = withContext(Dispatchers.IO) {
                    lyricsHelper.getLyricsFromProvider(mediaMetadata, newProvider)
                }
                val isTv = context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_LEANBACK)
                if (isTv) {
                    playerConnection?.updateTvLyrics(mediaMetadata.id, fetched.lyrics, fetched.provider)
                } else {
                    database.query {
                        upsert(
                            LyricsEntity(
                                id = mediaMetadata.id,
                                lyrics = fetched.lyrics,
                                provider = fetched.provider,
                            )
                        )
                    }
                }
                _switchGeneration.value = _switchGeneration.value + 1

                if (showToast) {
                    withContext(Dispatchers.Main) {
                        val msg = if (fetched.lyrics != LyricsEntity.LYRICS_NOT_FOUND) {
                            if (fetched.provider.equals(registryName, ignoreCase = true)) {
                                "Penyedia lirik diganti ke $displayName"
                            } else {
                                "Tidak ada di $displayName, memuat dari ${fetched.provider}"
                            }
                        } else {
                            "Lirik tidak ditemukan di $displayName"
                        }
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            } else if (showToast) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Penyedia lirik utama diatur ke $displayName",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } catch (e: Exception) {
            if (showToast) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Gagal memuat lirik dari $displayName",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } finally {
            _isSwitching.value = false
            _switchingProviderName.value = null
        }
    }
}

/**
 * Compact interactive pill shown in the Player & Lyrics header/footer to switch providers on the fly.
 */
@Composable
fun LyricsProviderQuickPill(
    activeProviderName: String?,
    preferredProvider: PreferredLyricsProvider,
    isSwitching: Boolean,
    switchingToName: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = when {
        isSwitching && !switchingToName.isNullOrBlank() -> "Memuat $switchingToName..."
        !activeProviderName.isNullOrBlank() &&
            activeProviderName != "Unknown" &&
            activeProviderName != "Manual" -> activeProviderName
        else -> LyricsHelper.displayNameOf(preferredProvider)
    }

    val isEditorial = com.nanzbeatles.nanas.ui.theme.LocalAppVisualStyle.current == "EDITORIAL"
    val pillShape = if (isEditorial) RoundedCornerShape(8.dp) else RoundedCornerShape(50)
    val fgColor = if (isEditorial) MaterialTheme.colorScheme.onSurface else Color.White

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .clip(pillShape)
            .background(fgColor.copy(alpha = 0.08f))
            .border(1.dp, fgColor.copy(alpha = 0.18f), pillShape)
            .clickable(enabled = !isSwitching, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        if (isSwitching) {
            CircularProgressIndicator(
                modifier = Modifier.size(13.dp),
                strokeWidth = 1.8.dp,
                color = if (isEditorial) fgColor else MaterialTheme.colorScheme.primary
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.lyrics),
                contentDescription = null,
                tint = if (isEditorial) fgColor else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
        }

        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = fgColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )

        Icon(
            painter = painterResource(R.drawable.expand_more),
            contentDescription = "Ganti Penyedia Lirik",
            tint = fgColor.copy(alpha = 0.75f),
            modifier = Modifier.size(14.dp)
        )
    }
}

/**
 * Dialog for switching the lyrics provider directly in the Player without clearing cache.
 */
@Composable
fun LyricsProviderSwitcherDialog(
    currentPreferred: PreferredLyricsProvider,
    activeLoadedProviderName: String?,
    onSelectProvider: (PreferredLyricsProvider) -> Unit,
    onDismiss: () -> Unit,
) {
    DefaultDialog(
        onDismiss = onDismiss,
        icon = {
            Icon(
                painter = painterResource(R.drawable.lyrics),
                contentDescription = null
            )
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Pilih Penyedia Lirik",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Langsung ganti lirik secara instan tanpa hapus cache",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        buttons = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PreferredLyricsProvider.entries.forEach { provider ->
                val isSelected = provider == currentPreferred
                val providerInternalName = LyricsHelper.providerNameOf(provider)
                val isCurrentlyLoaded = !activeLoadedProviderName.isNullOrBlank() &&
                    activeLoadedProviderName.equals(providerInternalName, ignoreCase = true)
                val isPrimarySimpMusic = provider == PreferredLyricsProvider.SIMPMUSIC

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectProvider(provider) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelectProvider(provider) }
                        )

                        Spacer(Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = LyricsHelper.displayNameOf(provider),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                if (isPrimarySimpMusic) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "UTAMA",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                if (isCurrentlyLoaded) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.20f))
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Aktif",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(2.dp))

                            Text(
                                text = LyricsHelper.subtitleOf(provider),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
