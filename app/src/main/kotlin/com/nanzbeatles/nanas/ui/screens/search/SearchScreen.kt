/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.ui.screens.search

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.nanzbeatles.nanas.LocalDatabase
import com.nanzbeatles.nanas.LocalIsPlayerExpanded
import com.nanzbeatles.nanas.LocalPlayerAwareWindowInsets
import com.nanzbeatles.nanas.R
import com.nanzbeatles.nanas.constants.LiquidGlassEffectKey
import com.nanzbeatles.nanas.constants.PauseSearchHistoryKey
import com.nanzbeatles.nanas.constants.SearchSource
import com.nanzbeatles.nanas.constants.SearchSourceKey
import com.nanzbeatles.nanas.db.entities.SearchHistory
import com.nanzbeatles.nanas.utils.rememberEnumPreference
import com.nanzbeatles.nanas.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
    pureBlack: Boolean
) {
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val isPlayerExpanded = LocalIsPlayerExpanded.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val liquidGlassEnabled by rememberPreference(LiquidGlassEffectKey, defaultValue = true)

    var searchSource by rememberEnumPreference(SearchSourceKey, SearchSource.ONLINE)
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }
    val pauseSearchHistory by rememberPreference(PauseSearchHistoryKey, defaultValue = false)

    val onSearch: (String) -> Unit = { searchQuery ->
        val trimmed = searchQuery.trim()
        if (trimmed.isNotEmpty()) {
            focusManager.clearFocus()
            keyboardController?.hide()
            if (searchSource == SearchSource.ONLINE) {
                navController.navigate("search/${URLEncoder.encode(trimmed, "UTF-8")}")
            }

            if (!pauseSearchHistory) {
                coroutineScope.launch(Dispatchers.IO) {
                    database.query {
                        insert(SearchHistory(query = trimmed))
                    }
                }
            }
        }
    }

    val onSearchFromSuggestion: (String) -> Unit = { searchQuery ->
        val trimmed = searchQuery.trim()
        if (trimmed.isNotEmpty()) {
            focusManager.clearFocus()
            keyboardController?.hide()
            navController.navigate("search/${URLEncoder.encode(trimmed, "UTF-8")}")

            if (!pauseSearchHistory) {
                coroutineScope.launch(Dispatchers.IO) {
                    database.query {
                        insert(SearchHistory(query = trimmed))
                    }
                }
            }
        }
    }

    val accentColor = MaterialTheme.colorScheme.primary
    val searchPillBg = if (isDark) Color(0xFF0E121B).copy(alpha = 0.48f) else Color.White.copy(alpha = 0.72f)
    val searchPillFrost = Brush.linearGradient(
        listOf(
            if (isDark) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.92f),
            accentColor.copy(alpha = if (isDark) 0.14f else 0.10f),
            if (isDark) Color.White.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.55f),
            if (isDark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.78f)
        )
    )
    val searchPillBorder = Brush.linearGradient(
        listOf(
            if (isDark) Color.White.copy(alpha = 0.48f) else Color.White.copy(alpha = 0.96f),
            accentColor.copy(alpha = if (isDark) 0.34f else 0.24f),
            if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
            if (isDark) Color.White.copy(alpha = 0.24f) else Color.Black.copy(alpha = 0.12f)
        )
    )

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.search),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = if (isDark) Color.White else Color.Black
                    )

                    // Segmented Online / Library 3D Liquid Glass Pill
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(searchPillBg, CircleShape)
                            .background(searchPillFrost, CircleShape)
                            .border(BorderStroke(1.1.dp, searchPillBorder), CircleShape)
                            .padding(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            SearchSource.ONLINE to "Online",
                            SearchSource.LOCAL to "Pustaka"
                        ).forEach { (source, label) ->
                            val selected = searchSource == source
                            val tabBg by animateColorAsState(
                                targetValue = if (selected) {
                                    accentColor.copy(alpha = if (isDark) 0.34f else 0.22f)
                                } else Color.Transparent,
                                animationSpec = tween(180),
                                label = "searchSourceBg"
                            )
                            val tabColor by animateColorAsState(
                                targetValue = if (selected) {
                                    if (isDark) Color.White else Color(0xFF0A0D14)
                                } else {
                                    if (isDark) Color(0xFFB4B8C5) else Color(0xFF5A5A60)
                                },
                                animationSpec = tween(180),
                                label = "searchSourceColor"
                            )
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(tabBg, CircleShape)
                                    .then(
                                        if (selected) {
                                            Modifier.border(
                                                1.dp,
                                                Color.White.copy(alpha = if (isDark) 0.45f else 0.85f),
                                                CircleShape
                                            )
                                        } else Modifier
                                    )
                                    .clickable { searchSource = source }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = tabColor,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3D Liquid Glass Search Bar Pill
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(CircleShape)
                        .border(BorderStroke(1.2.dp, searchPillBorder), CircleShape),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .then(
                                if (liquidGlassEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    Modifier.blur(28.dp)
                                } else {
                                    Modifier
                                }
                            )
                            .background(searchPillBg, CircleShape)
                            .background(searchPillFrost, CircleShape)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.search),
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF9E9E9E) else Color(0xFF5A5A60),
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                            textStyle = TextStyle(
                                color = if (isDark) Color.White else Color.Black,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(if (isDark) Color.White else Color.Black),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                if (query.text.isEmpty()) {
                                    Text(
                                        text = stringResource(
                                            when (searchSource) {
                                                SearchSource.LOCAL -> R.string.search_library
                                                SearchSource.ONLINE -> R.string.search_yt_music
                                            }
                                        ),
                                        style = TextStyle(
                                            color = if (isDark) Color(0xFF7E7E85) else Color(0xFF6E6E75),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Normal
                                        )
                                    )
                                }
                                innerTextField()
                            },
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Search
                            ),
                            keyboardActions = KeyboardActions(
                                onSearch = { onSearch(query.text) }
                            )
                        )

                        if (query.text.isNotEmpty()) {
                            IconButton(
                                onClick = { query = TextFieldValue("") },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.close),
                                    contentDescription = null,
                                    tint = if (isDark) Color.White else Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                HorizontalDivider(
                    thickness = 1.dp,
                    color = if (isDark) Color.White.copy(alpha = 0.07f) else Color.Black.copy(alpha = 0.07f)
                )
            }
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        val bottomPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()

        Box(
            modifier = Modifier
                .padding(paddingValues)
                .padding(bottom = bottomPadding)
                .fillMaxSize()
        ) {
            when (searchSource) {
                SearchSource.LOCAL -> LocalSearchScreen(
                    query = query.text,
                    navController = navController,
                    onDismiss = { navController.navigateUp() },
                    pureBlack = pureBlack,
                    onQueryChange = { query = it }
                )
                SearchSource.ONLINE -> OnlineSearchScreen(
                    query = query.text,
                    onQueryChange = { query = it },
                    navController = navController,
                    onSearch = onSearchFromSuggestion,
                    onDismiss = { /* Don't dismiss when searching from suggestions */ },
                    pureBlack = pureBlack
                )
            }
        }
    }

    // Hide keyboard when player is expanded
    DisposableEffect(lifecycleOwner, isPlayerExpanded) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && isPlayerExpanded) {
                keyboardController?.hide()
                focusManager.clearFocus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
