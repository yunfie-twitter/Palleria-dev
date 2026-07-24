package com.yunfie.illustia.ui.screens

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.yunfie.illustia.IllustiaUiState
import com.yunfie.illustia.IllustiaViewModel
import com.yunfie.illustia.R
import com.yunfie.illustia.settings.isAppDarkTheme
import com.yunfie.illustia.ui.components.miuixClickable
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun OnboardingScreen(
    state: IllustiaUiState,
    viewModel: IllustiaViewModel,
    onRefreshTokenLogin: () -> Unit,
    showTokenLogin: Boolean = false,
    onTokenLoginDismiss: () -> Unit = {},
) {
    var showDetails by remember { mutableStateOf(false) }
    val backgroundColor = Color.Black
    val contentColor = Color.White
    val activity = LocalContext.current as? Activity
    val restoreLightSystemBars = !isAppDarkTheme(
        state.settings.themeMode,
        isSystemInDarkTheme(),
    )

    DisposableEffect(activity, restoreLightSystemBars) {
        val window = activity?.window
        val controller = window?.let {
            WindowCompat.getInsetsController(it, it.decorView)
        }
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false

        onDispose {
            controller?.isAppearanceLightStatusBars = restoreLightSystemBars
            controller?.isAppearanceLightNavigationBars = restoreLightSystemBars
        }
    }

    Scaffold(containerColor = backgroundColor) { scaffoldPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(
                    start = 22.dp,
                    end = 22.dp,
                    top = scaffoldPadding.calculateTopPadding(),
                    bottom = scaffoldPadding.calculateBottomPadding() + 18.dp,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            BrandLockup(
                contentColor = contentColor,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(72.dp))

            LoginActions(
                backgroundColor = backgroundColor,
                contentColor = contentColor,
                onWebLogin = viewModel::openWebLogin,
                onShowDetails = { showDetails = true },
                onRefreshTokenLogin = onRefreshTokenLogin,
            )
        }
    }

    OverlayDialog(
        show = showDetails,
        title = stringResource(R.string.about_title),
        summary = stringResource(R.string.login_disclaimer),
        backgroundColor = MiuixTheme.colorScheme.surfaceContainerHighest,
        onDismissRequest = { showDetails = false },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = stringResource(R.string.login_web_description),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.body1,
            )
            Button(
                onClick = { showDetails = false },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColorsPrimary(),
                insideMargin = PaddingValues(vertical = 12.dp),
            ) {
                Text(
                    text = stringResource(R.string.action_close),
                    fontWeight = FontWeight.Bold,
                )
            }
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

@Composable
private fun BrandLockup(
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    val appName = stringResource(R.string.app_name)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.onboarding_app_icon),
            contentDescription = null,
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(17.dp)),
        )
        Text(
            text = appName,
            color = contentColor,
            fontSize = 48.sp,
            lineHeight = 52.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-1.4).sp,
        )
    }
}

@Composable
private fun LoginActions(
    backgroundColor: Color,
    contentColor: Color,
    onWebLogin: () -> Unit,
    onShowDetails: () -> Unit,
    onRefreshTokenLogin: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(
            onClick = onWebLogin,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(
                color = contentColor,
                contentColor = backgroundColor,
            ),
            insideMargin = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        ) {
            Text(
                text = stringResource(R.string.login_web_button),
                color = backgroundColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = contentColor.copy(alpha = 0.72f),
            )
            Text(
                text = stringResource(R.string.login_or),
                color = contentColor.copy(alpha = 0.82f),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = contentColor.copy(alpha = 0.72f),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BottomAction(
                label = stringResource(R.string.login_details),
                contentColor = contentColor,
                onClick = onShowDetails,
                modifier = Modifier.weight(1f),
            )
            BottomAction(
                label = stringResource(R.string.login_token_short),
                contentColor = contentColor,
                onClick = onRefreshTokenLogin,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun BottomAction(
    label: String,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(68.dp)
            .miuixClickable(haptic = true, onClick = onClick)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = contentColor,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}
