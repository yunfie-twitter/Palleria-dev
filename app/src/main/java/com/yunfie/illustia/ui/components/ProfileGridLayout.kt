package com.yunfie.illustia.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal const val ProfileGridColumnCount = 2
internal val ProfileGridHorizontalSpacing = 10.dp
internal val ProfileGridVerticalSpacing = 18.dp

internal fun profileGridContentPadding(
    top: Dp = 14.dp,
    bottom: Dp = 96.dp,
): PaddingValues {
    return PaddingValues(
        start = 14.dp,
        end = 14.dp,
        top = top,
        bottom = bottom,
    )
}
