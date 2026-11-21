/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.ui.component

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import com.nanzbeatles.nanas.R
import com.nanzbeatles.nanas.ui.screens.OptionStats
import com.nanzbeatles.nanas.ui.theme.LocalAppVisualStyle
import com.nanzbeatles.nanas.ui.utils.editorialPillContentColor
import com.nanzbeatles.nanas.ui.utils.liquidGlassPill

@Composable
fun <E> ChipsRow(
    chips: List<Pair<E, String>>,
    currentValue: E,
    onValueUpdate: (E) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val isEditorial = LocalAppVisualStyle.current == "EDITORIAL"
    val chipShape = if (isEditorial) RoundedCornerShape(10.dp) else CircleShape

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)),
    ) {
        Spacer(Modifier.width(16.dp))

        chips.forEach { (value, label) ->
            val isSelected = currentValue == value
            val defaultTextColor = when {
                isSelected && isDark -> Color(0xFF63B3FF)
                isSelected && !isDark -> Color(0xFF0B65C6)
                isDark -> Color.White.copy(alpha = 0.88f)
                else -> Color(0xFF181A20)
            }
            val textColor = editorialPillContentColor(
                isSelected = isSelected,
                defaultColor = defaultTextColor,
                isDark = isDark
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .liquidGlassPill(
                        isSelected = isSelected,
                        shape = chipShape,
                        accentColor = Color(0xFF1E90FF)
                    )
                    .clickable { onValueUpdate(value) }
                    .padding(horizontal = 16.dp, vertical = 9.dp)
            ) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    color = textColor,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                )
            }

            Spacer(Modifier.width(8.dp))
        }

        Spacer(Modifier.width(8.dp))
    }
}

@SuppressLint("UnusedContentLambdaTargetStateParameter")
@Composable
fun <Int> ChoiceChipsRow(
    chips: List<Pair<Int, String>>,
    options: List<Pair<OptionStats, String>>,
    selectedOption: OptionStats,
    onSelectionChange: (OptionStats) -> Unit,
    currentValue: Int,
    onValueUpdate: (Int) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val isEditorial = LocalAppVisualStyle.current == "EDITORIAL"
    val chipShape = if (isEditorial) RoundedCornerShape(10.dp) else CircleShape
    var expandIconDegree by remember { mutableFloatStateOf(0f) }
    val rotationAnimation by animateFloatAsState(
        targetValue = expandIconDegree,
        animationSpec = tween(durationMillis = 400),
        label = "",
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)),
    ) {
        var expanded by remember { mutableStateOf(false) }

        Column {
            val activeFg = editorialPillContentColor(
                isSelected = true,
                defaultColor = if (isDark) Color(0xFF63B3FF) else Color(0xFF0B65C6),
                isDark = isDark
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .liquidGlassPill(
                        isSelected = true,
                        shape = chipShape,
                        accentColor = Color(0xFF1E90FF)
                    )
                    .clickable {
                        expanded = !expanded
                        expandIconDegree -= 180
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = when (selectedOption) {
                        OptionStats.WEEKS -> stringResource(id = R.string.weeks)
                        OptionStats.MONTHS -> stringResource(id = R.string.months)
                        OptionStats.YEARS -> stringResource(id = R.string.years)
                        OptionStats.CONTINUOUS -> stringResource(id = R.string.continuous)
                    },
                    color = activeFg,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    painter = painterResource(R.drawable.expand_more),
                    contentDescription = null,
                    tint = activeFg,
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer(rotationZ = rotationAnimation),
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandIn() + fadeIn(),
                exit = shrinkOut() + fadeOut(),
            ) {
                DropdownMenu(
                    modifier = Modifier.padding(start = 12.dp),
                    expanded = expanded,
                    onDismissRequest = {
                        expanded = false
                        expandIconDegree -= 180
                    },
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(text = option.second) },
                            onClick = {
                                onSelectionChange(option.first)
                                expandIconDegree -= 180
                                expanded = false
                            },
                        )
                    }
                }
            }
        }

        AnimatedContent(
            targetState = selectedOption,
            transitionSpec = { slideInHorizontally() + fadeIn() togetherWith slideOutHorizontally() + fadeOut() },
            label = "",
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)),
            ) {
                chips.forEach { (value, label) ->
                    val isSelected = currentValue == value
                    val defaultTextColor = when {
                        isSelected && isDark -> Color(0xFF63B3FF)
                        isSelected && !isDark -> Color(0xFF0B65C6)
                        isDark -> Color.White.copy(alpha = 0.88f)
                        else -> Color(0xFF181A20)
                    }
                    val textColor = editorialPillContentColor(
                        isSelected = isSelected,
                        defaultColor = defaultTextColor,
                        isDark = isDark
                    )
                    Spacer(Modifier.width(8.dp))

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .liquidGlassPill(
                                isSelected = isSelected,
                                shape = chipShape,
                                accentColor = Color(0xFF1E90FF)
                            )
                            .clickable { onValueUpdate(value) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = label,
                            color = textColor,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
