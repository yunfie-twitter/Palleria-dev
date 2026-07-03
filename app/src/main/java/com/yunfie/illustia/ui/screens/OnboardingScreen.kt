package com.yunfie.illustia.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.yunfie.illustia.IllustiaUiState
import com.yunfie.illustia.IllustiaViewModel
import com.yunfie.illustia.R
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.icon.extended.FavoritesFill
import top.yukonga.miuix.kmp.icon.extended.Ok
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
