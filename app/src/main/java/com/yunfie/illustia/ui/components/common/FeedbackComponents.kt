package com.yunfie.illustia.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yunfie.illustia.models.LoadState
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun StateBanner(loadState: LoadState) {
    when (loadState) {
        LoadState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                contentAlignment = Alignment.Center,
            ) {
                LoadingIndicator()
            }
        }
        is LoadState.Error -> {
            Text(
                text = loadState.message,
                color = MiuixTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 6.dp),
                textAlign = TextAlign.Center,
            )
        }
        LoadState.Idle,
        LoadState.Loaded -> Unit
    }
}

@Composable
fun EmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = message, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
    }
}

@Composable
fun HeaderIcon(
    icon: ImageVector,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick ?: {},
        enabled = onClick != null,
        modifier = modifier,
        minWidth = 44.dp,
        minHeight = 44.dp,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.onBackground,
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
fun HeaderOverlayIcon(icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(38.dp),
        backgroundColor = Color.Black.copy(alpha = 0.35f),
        cornerRadius = 19.dp,
        minWidth = 38.dp,
        minHeight = 38.dp,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.9f),
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
fun DividerLine() {
    HorizontalDivider(
        modifier = Modifier.fillMaxWidth(),
        color = MiuixTheme.colorScheme.dividerLine,
    )
}

@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
    InfiniteProgressIndicator(
        modifier = modifier,
        color = MiuixTheme.colorScheme.onBackground,
        size = 36.dp,
        strokeWidth = 3.dp,
        orbitingDotSize = 4.dp,
    )
}

@Composable
fun CenteredLoadingIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        LoadingIndicator()
    }
}
