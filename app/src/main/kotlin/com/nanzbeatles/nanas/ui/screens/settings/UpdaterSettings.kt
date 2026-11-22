/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nanzbeatles.nanas.BuildConfig
import com.nanzbeatles.nanas.LocalPlayerAwareWindowInsets
import com.nanzbeatles.nanas.R
import com.nanzbeatles.nanas.constants.CheckForUpdatesKey
import com.nanzbeatles.nanas.ui.component.IconButton
import com.nanzbeatles.nanas.ui.component.Material3SettingsGroup
import com.nanzbeatles.nanas.ui.component.Material3SettingsItem
import com.nanzbeatles.nanas.ui.component.UpdateAvailableDialog
import com.nanzbeatles.nanas.ui.utils.backToMain
import com.nanzbeatles.nanas.utils.UpdateInfo
import com.nanzbeatles.nanas.utils.Updater
import com.nanzbeatles.nanas.utils.rememberPreference
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdaterScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val (checkForUpdates, onCheckForUpdatesChange) = rememberPreference(CheckForUpdatesKey, true)

    var isChecking by remember { mutableStateOf(false) }
    var checkedUpdateInfo by remember { mutableStateOf(Updater.getCachedUpdateInfo()) }
    var updateAvailable by remember { mutableStateOf(Updater.hasNewerCachedUpdate(context)) }
    var hasCheckedOnce by remember { mutableStateOf(checkedUpdateInfo != null) }
    var checkError by remember { mutableStateOf<String?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    fun performManualCheck() {
        coroutineScope.launch {
            isChecking = true
            checkError = null
            Updater.checkForUpdateManual(context)
                .onSuccess { (info, hasUpdate) ->
                    checkedUpdateInfo = info
                    updateAvailable = hasUpdate
                    hasCheckedOnce = true
                    if (hasUpdate) {
                        showUpdateDialog = true
                    }
                }
                .onFailure {
                    checkError = it.message ?: "Gagal memeriksa pembaruan"
                }
            isChecking = false
        }
    }

    if (showUpdateDialog && checkedUpdateInfo != null && updateAvailable) {
        val info = checkedUpdateInfo!!
        UpdateAvailableDialog(
            updateInfo = info,
            onUpdate = {
                showUpdateDialog = false
                Updater.openUpdateUrl(context, info.url)
            },
            onLater = {
                showUpdateDialog = false
                coroutineScope.launch {
                    Updater.dismissUpdateVersion(context, info.versionCode)
                }
            },
            onDismiss = {
                showUpdateDialog = false
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Top
                )
            )
        )

        Spacer(Modifier.height(4.dp))

        // Current Version Info
        Material3SettingsGroup(
            title = "Versi Saat Ini",
            items = listOf(
                Material3SettingsItem(
                    title = {
                        Text("Versi: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                    },
                    description = {
                        val arch = BuildConfig.ARCHITECTURE
                        val variant = when {
                            BuildConfig.CAST_AVAILABLE -> "GMS"
                            arch != "universal" -> "Standalone"
                            else -> "FOSS"
                        }
                        Text("$arch - $variant")
                    }
                )
            )
        )

        Spacer(Modifier.height(16.dp))

        // Auto Update Settings
        Material3SettingsGroup(
            title = "Pengaturan Pembaruan",
            items = listOf(
                Material3SettingsItem(
                    title = { Text(stringResource(R.string.check_for_updates)) },
                    description = { Text("Periksa versi terbaru secara otomatis saat aplikasi dibuka") },
                    icon = painterResource(R.drawable.update),
                    trailingContent = {
                        Switch(
                            checked = checkForUpdates,
                            onCheckedChange = onCheckForUpdatesChange
                        )
                    },
                    onClick = { onCheckForUpdatesChange(!checkForUpdates) }
                )
            )
        )

        Spacer(Modifier.height(16.dp))

        // Manual Check
        Material3SettingsGroup(
            title = "Periksa Pembaruan",
            items = buildList {
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.refresh),
                        title = {
                            when {
                                isChecking -> Text("Memeriksa pembaruan...")
                                updateAvailable && checkedUpdateInfo != null ->
                                    Text("Versi terbaru ${checkedUpdateInfo!!.versionName}")
                                hasCheckedOnce -> Text("Aplikasi sudah versi terbaru")
                                else -> Text("Periksa Pembaruan")
                            }
                        },
                        description = if (updateAvailable && checkedUpdateInfo != null) {
                            {
                                Text(
                                    checkedUpdateInfo!!.changelog.ifBlank {
                                        "Ketuk untuk melihat detail pembaruan"
                                    }
                                )
                            }
                        } else {
                            null
                        },
                        trailingContent = {
                            if (isChecking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.padding(end = 16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else if (updateAvailable) {
                                Icon(
                                    painter = painterResource(R.drawable.download),
                                    contentDescription = "Pembaruan tersedia",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        onClick = {
                            if (!isChecking) {
                                if (updateAvailable && checkedUpdateInfo != null) {
                                    showUpdateDialog = true
                                } else {
                                    performManualCheck()
                                }
                            }
                        }
                    )
                )

                if (updateAvailable) {
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.refresh),
                            title = { Text("Periksa Ulang") },
                            onClick = { if (!isChecking) performManualCheck() }
                        )
                    )
                }
            }
        )

        checkError?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Spacer(Modifier.height(32.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.updater)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        }
    )
}
