package com.yunfie.illustia.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.yunfie.illustia.R
import com.yunfie.illustia.models.NetworkMode

fun pixivNetworkModeOptions(): List<String> {
    return listOf(
        NetworkMode.Ech.code,
        NetworkMode.Compat.code,
        NetworkMode.Standard.code,
    )
}

@Composable
fun pixivNetworkModeLabel(value: String): String {
    return when (NetworkMode.fromCode(value)) {
        NetworkMode.Ech -> stringResource(R.string.pixiv_network_mode_ech)
        NetworkMode.Compat -> stringResource(R.string.pixiv_network_mode_compat)
        NetworkMode.Standard -> stringResource(R.string.pixiv_network_mode_standard)
    }
}
