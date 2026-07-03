package com.yunfie.illustia.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yunfie.illustia.R
import com.yunfie.illustia.models.NovelPreview
import com.yunfie.illustia.ui.components.ElevatedPanel
import com.yunfie.illustia.ui.components.PixivImage
import com.yunfie.illustia.ui.components.miuixClickable
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun NovelCard(
    novel: NovelPreview,
    onClick: () -> Unit,
) {
    ElevatedPanel(modifier = Modifier.fillMaxWidth().miuixClickable(onClick = onClick)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(104.dp)
                    .aspectRatio(0.76f)
                    .clip(RoundedCornerShape(18.dp)),
            ) {
                PixivImage(
                    url = novel.coverUrl,
                    contentDescription = novel.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    crossfade = true,
                    thumbnail = true,
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)),
                            ),
                        ),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = novel.title,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = novel.userName,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                Text(
                    text = if (novel.caption.isBlank()) {
                        stringResource(R.string.novel_length_label, novel.textLength)
                    } else {
                        novel.caption
                    },
                    style = MiuixTheme.textStyles.footnote1,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NovelMetaPill(text = stringResource(R.string.novel_page_count, novel.pageCount))
                    NovelMetaPill(text = stringResource(R.string.novel_bookmark_count, novel.totalBookmarks))
                }
            }
        }
    }
}

@Composable
internal fun NovelMetaPill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MiuixTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MiuixTheme.textStyles.footnote1,
            fontWeight = FontWeight.Medium,
        )
    }
}
