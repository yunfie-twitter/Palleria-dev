package com.yunfie.illustia.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yunfie.illustia.IllustiaUiState
import com.yunfie.illustia.IllustiaViewModel
import com.yunfie.illustia.R
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.icon.extended.FavoritesFill
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun OnboardingScreen(
    state: IllustiaUiState,
    viewModel: IllustiaViewModel,
    onRefreshTokenLogin: () -> Unit,
    showTokenLogin: Boolean = false,
    onTokenLoginDismiss: () -> Unit = {},
) {
    val slides = listOf(
        OnboardingSlide(
            title = R.string.login_walkthrough_title_1,
            body = R.string.login_walkthrough_body_1,
            accent = MiuixTheme.colorScheme.primary,
            icon = MiuixIcons.Contacts,
            scene = OnboardingScene.Discovery,
        ),
        OnboardingSlide(
            title = R.string.login_walkthrough_title_2,
            body = R.string.login_walkthrough_body_2,
            accent = MiuixTheme.colorScheme.primaryContainer,
            icon = MiuixIcons.FavoritesFill,
            scene = OnboardingScene.Bookmarks,
        ),
        OnboardingSlide(
            title = R.string.login_walkthrough_title_3,
            body = R.string.login_walkthrough_body_3,
            accent = MiuixTheme.colorScheme.secondary,
            icon = MiuixIcons.Ok,
            scene = OnboardingScene.Sync,
        ),
    )
    val pagerState = rememberPagerState(pageCount = { slides.size })

    Scaffold(containerColor = MiuixTheme.colorScheme.surface) { scaffoldPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.surface)
                .padding(
                    start = 20.dp,
                    end = 20.dp,
                    top = scaffoldPadding.calculateTopPadding() + 18.dp,
                    bottom = scaffoldPadding.calculateBottomPadding() + 22.dp,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth(),
                ) { page ->
                    OnboardingPage(
                        slide = slides[page],
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            PagerDots(
                pageCount = slides.size,
                currentPage = pagerState.currentPage,
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = viewModel::openWebLogin,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text(stringResource(R.string.login_web_button))
            }

            Spacer(Modifier.height(10.dp))

            Button(
                onClick = onRefreshTokenLogin,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    color = MiuixTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MiuixTheme.colorScheme.onSurface,
                ),
            ) {
                Text(stringResource(R.string.login_token_title))
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.login_disclaimer),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.footnote1,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
    }

    if (showTokenLogin) {
        RefreshTokenLoginBottomSheet(
            state = state,
            viewModel = viewModel,
            onDismiss = onTokenLoginDismiss,
        )
    }
}

private data class OnboardingSlide(
    val title: Int,
    val body: Int,
    val accent: Color,
    val icon: ImageVector,
    val scene: OnboardingScene,
)

private enum class OnboardingScene {
    Discovery,
    Bookmarks,
    Sync,
}

@Composable
private fun OnboardingPage(slide: OnboardingSlide) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .squircleSurface(
                color = MiuixTheme.colorScheme.surface,
                cornerRadius = 28.dp,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = stringResource(slide.title),
            color = MiuixTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Black,
            style = MiuixTheme.textStyles.title1,
            lineHeight = 30.sp,
            modifier = Modifier.padding(top = 6.dp),
        )

        Text(
            text = stringResource(slide.body),
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.body1,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 10.dp),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 8.dp, bottom = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            when (slide.scene) {
                OnboardingScene.Discovery -> DiscoveryScene(slide)
                OnboardingScene.Bookmarks -> BookmarkScene(slide)
                OnboardingScene.Sync -> SyncScene(slide)
            }
        }
    }
}

@Composable
private fun DiscoveryScene(slide: OnboardingSlide) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircleGlow(
            color = slide.accent.copy(alpha = 0.14f),
            size = 250.dp,
            modifier = Modifier.align(Alignment.Center),
        )
        DeviceFrame(
            accent = slide.accent,
            modifier = Modifier
                .size(width = 260.dp, height = 300.dp)
                .align(Alignment.Center),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                RepeatRow(3, slide.accent)
                Spacer(Modifier.height(6.dp))
                FeedCard(widthFactor = 0.78f, accent = slide.accent, tint = MiuixTheme.colorScheme.primary)
                FeedCard(widthFactor = 0.94f, accent = slide.accent.copy(alpha = 0.85f), tint = MiuixTheme.colorScheme.onSurface)
                FeedCard(widthFactor = 0.72f, accent = slide.accent.copy(alpha = 0.75f), tint = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
        }
        FloatingBadge(
            icon = slide.icon,
            accent = slide.accent,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 34.dp, end = 12.dp),
        )
        FloatingBadge(
            icon = MiuixIcons.Ok,
            accent = MiuixTheme.colorScheme.surfaceContainerHighest,
            tint = MiuixTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 18.dp, bottom = 26.dp),
        )
    }
}

@Composable
private fun BookmarkScene(slide: OnboardingSlide) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircleGlow(
            color = slide.accent.copy(alpha = 0.12f),
            size = 230.dp,
        )
        DeviceFrame(
            accent = slide.accent,
            modifier = Modifier.size(width = 260.dp, height = 320.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CardHeader(slide.accent)
                BookStack(slide.accent)
            }
        }
        BookmarkRibbon(
            accent = slide.accent,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 26.dp, end = 16.dp),
        )
        FloatingBadge(
            icon = MiuixIcons.FavoritesFill,
            accent = MiuixTheme.colorScheme.surfaceContainerHighest,
            tint = slide.accent,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 30.dp, end = 18.dp),
        )
    }
}

@Composable
private fun SyncScene(slide: OnboardingSlide) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircleGlow(
            color = slide.accent.copy(alpha = 0.12f),
            size = 250.dp,
        )
        DeviceFrame(
            accent = slide.accent,
            modifier = Modifier.size(width = 250.dp, height = 320.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RepeatRow(2, slide.accent)
                SyncPanel(slide.accent)
                SyncPanel(slide.accent.copy(alpha = 0.82f))
                SyncPanel(slide.accent.copy(alpha = 0.68f))
            }
        }
        FloatingBadge(
            icon = slide.icon,
            accent = slide.accent,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 34.dp, start = 16.dp),
        )
        FloatingBadge(
            icon = MiuixIcons.Ok,
            accent = MiuixTheme.colorScheme.surfaceContainerHighest,
            tint = slide.accent,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 22.dp, end = 18.dp),
        )
    }
}

@Composable
private fun DeviceFrame(
    accent: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .squircleSurface(
                color = MiuixTheme.colorScheme.surfaceContainerHighest,
                cornerRadius = 28.dp,
            )
            .padding(10.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .squircleSurface(
                    color = Color.White,
                    cornerRadius = 22.dp,
                )
                .padding(14.dp),
        ) {
            content()
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
                .size(width = 54.dp, height = 8.dp)
                .squircleSurface(
                    color = accent.copy(alpha = 0.85f),
                    cornerRadius = 99.dp,
                ),
        )
    }
}

@Composable
private fun FeedCard(
    widthFactor: Float,
    accent: Color,
    tint: Color,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(widthFactor)
            .height(58.dp)
            .squircleSurface(
                color = accent.copy(alpha = 0.08f),
                cornerRadius = 18.dp,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .squircleSurface(
                        color = accent.copy(alpha = 0.25f),
                        cornerRadius = 10.dp,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = MiuixIcons.Ok,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(16.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .height(10.dp)
                        .squircleSurface(
                            color = tint.copy(alpha = 0.82f),
                            cornerRadius = 99.dp,
                        ),
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.46f)
                        .height(8.dp)
                        .squircleSurface(
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.24f),
                            cornerRadius = 99.dp,
                        ),
                )
            }
        }
    }
}

@Composable
private fun CardHeader(accent: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .squircleSurface(
                    color = accent.copy(alpha = 0.16f),
                    cornerRadius = 18.dp,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = MiuixIcons.FavoritesFill,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(24.dp),
            )
        }
        Box(
            modifier = Modifier
                .size(width = 52.dp, height = 12.dp)
                .squircleSurface(
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.18f),
                    cornerRadius = 99.dp,
                ),
        )
    }
}

@Composable
private fun BookStack(accent: Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(width = 110.dp, height = 160.dp)
                .squircleSurface(
                    color = accent.copy(alpha = 0.16f),
                    cornerRadius = 18.dp,
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(width = 126.dp, height = 178.dp)
                .squircleSurface(
                    color = Color.White,
                    cornerRadius = 18.dp,
                )
                .padding(8.dp)
                .squircleSurface(
                    color = accent.copy(alpha = 0.08f),
                    cornerRadius = 14.dp,
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(width = 92.dp, height = 140.dp)
                .squircleSurface(
                    color = accent.copy(alpha = 0.24f),
                    cornerRadius = 18.dp,
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(width = 84.dp, height = 18.dp)
                .squircleSurface(
                    color = accent.copy(alpha = 0.55f),
                    cornerRadius = 99.dp,
                ),
        )
    }
}

@Composable
private fun SyncPanel(accent: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .squircleSurface(
                color = accent.copy(alpha = 0.09f),
                cornerRadius = 18.dp,
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .squircleSurface(
                    color = accent.copy(alpha = 0.22f),
                    cornerRadius = 8.dp,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = MiuixIcons.Ok,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(15.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.66f)
                    .height(10.dp)
                    .squircleSurface(
                        color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.70f),
                        cornerRadius = 99.dp,
                    ),
            )
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.42f)
                    .height(8.dp)
                    .squircleSurface(
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.22f),
                        cornerRadius = 99.dp,
                    ),
            )
        }
    }
}

@Composable
private fun RepeatRow(count: Int, accent: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        repeat(count) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .squircleSurface(
                        color = accent.copy(alpha = 0.3f),
                        cornerRadius = 99.dp,
                    ),
            )
        }
    }
}

@Composable
private fun FloatingBadge(
    icon: ImageVector,
    accent: Color,
    tint: Color = Color.White,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .squircleSurface(
                color = accent,
                cornerRadius = 18.dp,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun BookmarkRibbon(
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(width = 72.dp, height = 28.dp)
            .squircleSurface(
                color = accent.copy(alpha = 0.92f),
                cornerRadius = 14.dp,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = MiuixIcons.Contacts,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun CircleGlow(
    color: Color,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(color, CircleShape),
    )
}

@Composable
private fun PagerDots(
    pageCount: Int,
    currentPage: Int,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(if (index == currentPage) 22.dp else 8.dp)
                    .squircleSurface(
                        color = if (index == currentPage) {
                            MiuixTheme.colorScheme.primary
                        } else {
                            MiuixTheme.colorScheme.surfaceContainerHighest
                        },
                        cornerRadius = 99.dp,
                    ),
            )
        }
    }
}
