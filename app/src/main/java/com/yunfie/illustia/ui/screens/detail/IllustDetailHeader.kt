package com.yunfie.illustia.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yunfie.illustia.R
import com.yunfie.illustia.models.Illust
import com.yunfie.illustia.ui.components.HeaderOverlayIcon
import com.yunfie.illustia.ui.components.LoadingIndicator
import com.yunfie.illustia.ui.components.LocalAppHapticMode
import com.yunfie.illustia.ui.components.PixivImage
import com.yunfie.illustia.ui.components.performAppHapticFeedback
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.menu.WindowIconDropdownMenu
import top.yukonga.miuix.kmp.theme.MiuixTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun IllustDetailHeader(
    illust: Illust,
    highQualityImages: Boolean,
    detailQuality: String,
    prefetchImages: Boolean,
    confirmOnLongPressSave: Boolean,
    skipConfirmOnDetailSave: Boolean,
    pixivUrl: String,
    onBack: () -> Unit,
    onOpenImage: (Int) -> Unit,
    onSaveImage: (String, String, Boolean) -> Unit,
    onSaveAllImages: (List<String>, String) -> Unit,
    onMuteIllust: () -> Unit,
    onMuteUser: () -> Unit,
    onMessage: (String) -> Unit,
    showImage: Boolean,
    maskMutedArtwork: Boolean,
    onRevealMutedArtwork: () -> Unit,
    mutedArtworkTitle: String,
    mutedArtworkSummary: String,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val hapticMode = LocalAppHapticMode.current
    val clipboard = remember(context) { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }
    val openInBrowserLabel = stringResource(R.string.detail_open_in_browser)
    val shareLabel = stringResource(R.string.detail_share)
    val saveImageLabel = stringResource(R.string.detail_save_image)
    val saveAllPagesLabel = stringResource(R.string.detail_save_all_pages)
    val copyUrlLabel = stringResource(R.string.detail_copy_url)
    val muteWorkLabel = stringResource(R.string.detail_mute_work)
    val muteArtistLabel = stringResource(R.string.detail_mute_artist)
    val moreLabel = stringResource(R.string.detail_more)
    val browserFailedMessage = stringResource(R.string.error_browser_failed)
    val shareFailedMessage = stringResource(R.string.error_share_failed)
    val urlCopiedMessage = stringResource(R.string.msg_url_copied)
    val imageUrls = remember(illust.id, highQualityImages, detailQuality) {
        when {
            !highQualityImages || detailQuality == "low" -> illust.mediumImagePages.ifEmpty {
                listOf(illust.mediumImageUrl.ifBlank { illust.squareImageUrl.ifBlank { illust.imageUrl } })
            }
            detailQuality == "medium" -> illust.imagePages.ifEmpty { listOf(illust.imageUrl) }
            else -> illust.originalImagePages.ifEmpty {
                illust.imagePages.ifEmpty { listOfNotNull(illust.originalImageUrl ?: illust.imageUrl) }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .heightIn(min = 320.dp)
            .background(MiuixTheme.colorScheme.surfaceContainer),
    ) {
        if (showImage) {
            val pagerState = rememberPagerState(pageCount = { imageUrls.size })

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
                beyondViewportPageCount = if (prefetchImages) 1 else 0,
                key = { it },
            ) { page ->
                Box(modifier = Modifier.fillMaxWidth()) {
                    PixivImage(
                        url = imageUrls[page],
                        contentDescription = illust.title,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (maskMutedArtwork) Modifier.blur(18.dp) else Modifier)
                            .combinedClickable(
                                enabled = !maskMutedArtwork,
                                onClick = { onOpenImage(page) },
                                onLongClick = {
                                    performAppHapticFeedback(context, haptic, hapticMode)
                                    onSaveImage(imageUrls[page], "illustia_${illust.id}_p$page", confirmOnLongPressSave)
                                },
                            ),
                        crossfade = true,
                    )
                    if (maskMutedArtwork) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.38f)),
                        )
                    }
                }
            }

            if (maskMutedArtwork) {
                MutedArtworkOverlay(
                    title = mutedArtworkTitle,
                    summary = mutedArtworkSummary,
                    onReveal = onRevealMutedArtwork,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            if (imageUrls.size > 1) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(14.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.4f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text("${pagerState.currentPage + 1} / ${imageUrls.size}", color = MiuixTheme.colorScheme.onSurface, style = MiuixTheme.textStyles.footnote1, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                LoadingIndicator()
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeaderOverlayIcon(MiuixIcons.Back, onBack)
            WindowIconDropdownMenu(
                entries = listOf(
                    DropdownEntry(
                        items = listOfNotNull(
                            DropdownItem(
                                text = openInBrowserLabel,
                                onClick = {
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(pixivUrl)))
                                    }.onFailure { onMessage(browserFailedMessage) }
                                },
                            ),
                            DropdownItem(
                                text = shareLabel,
                                onClick = {
                                    runCatching {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, "${illust.title} by ${illust.artistName}\n$pixivUrl")
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, shareLabel))
                                    }.onFailure { onMessage(shareFailedMessage) }
                                },
                            ),
                            DropdownItem(
                                text = saveImageLabel,
                                onClick = {
                                    onSaveImage(illust.originalImageUrl ?: illust.imageUrl, "illustia_${illust.id}", !skipConfirmOnDetailSave)
                                },
                            ),
                            if (imageUrls.size > 1) {
                                DropdownItem(
                                    text = saveAllPagesLabel,
                                    onClick = { onSaveAllImages(imageUrls, "illustia_${illust.id}") },
                                )
                            } else {
                                null
                            },
                            DropdownItem(
                                text = copyUrlLabel,
                                onClick = {
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Pixiv URL", pixivUrl))
                                    onMessage(urlCopiedMessage)
                                },
                            ),
                        ),
                    ),
                    DropdownEntry(
                        items = listOf(
                            DropdownItem(
                                text = muteWorkLabel,
                                onClick = {
                                    onMuteIllust()
                                    onBack()
                                },
                            ),
                            DropdownItem(
                                text = muteArtistLabel,
                                onClick = {
                                    onMuteUser()
                                    onBack()
                                },
                            ),
                        ),
                    ),
                ),
                backgroundColor = Color.Black.copy(alpha = 0.35f),
                cornerRadius = 19.dp,
                minWidth = 38.dp,
                minHeight = 38.dp,
            ) {
                Icon(MiuixIcons.More, contentDescription = moreLabel, tint = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.9f), modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
internal fun MutedArtworkOverlay(
    title: String,
    summary: String,
    onReveal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier
            .padding(24.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
            .background(Color.Black.copy(alpha = 0.58f))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = title,
            color = Color.White,
            style = MiuixTheme.textStyles.title4,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = summary,
            color = Color.White.copy(alpha = 0.82f),
            style = MiuixTheme.textStyles.footnote1,
            fontWeight = FontWeight.Bold,
        )
        top.yukonga.miuix.kmp.basic.Button(
            onClick = onReveal,
            colors = top.yukonga.miuix.kmp.basic.ButtonDefaults.buttonColors(
                color = MiuixTheme.colorScheme.error,
            ),
            insideMargin = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
        ) {
            Text(stringResource(R.string.action_show), color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}
