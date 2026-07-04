package com.yunfie.illustia.data

import androidx.compose.runtime.Immutable
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Immutable
data class PixivImageProxy(
    val name: String,
    val baseUrl: String,
)

private const val PALLERIA_WEBP_PROXY_BASE_URL = "https://proxy.yunfi.f5.si/image.webp?url="

val PixivImageProxyOptions = listOf(
    PixivImageProxy("Palleria", "https://i.yunfi.f5.si/"),
    PixivImageProxy("Palleria(Webp)", PALLERIA_WEBP_PROXY_BASE_URL),
    PixivImageProxy("suimoe.com", "https://i.suimoe.com/"),
    PixivImageProxy("pixiv.re", "https://i.pixiv.re/"),
)

fun proxyPixivImageUrl(url: String, proxyBaseUrl: String): String {
    if (url.isBlank() || proxyBaseUrl.isBlank()) return url

    val sourcePrefix = url.pixivImagePrefixOrNull() ?: return url

    if (proxyBaseUrl == PALLERIA_WEBP_PROXY_BASE_URL) {
        return proxyBaseUrl + URLEncoder.encode(url, StandardCharsets.UTF_8)
    }

    val proxy = proxyBaseUrl.trim().trimEnd('/')
    if (!proxy.startsWith("https://") && !proxy.startsWith("http://")) return url

    return proxy + "/" + url.removePrefix(sourcePrefix)
}

private fun String.pixivImagePrefixOrNull(): String? {
    return when {
        startsWith("https://i.pximg.net/") -> "https://i.pximg.net/"
        startsWith("http://i.pximg.net/") -> "http://i.pximg.net/"
        startsWith("https://i-f.pximg.net/") -> "https://i-f.pximg.net/"
        startsWith("http://i-f.pximg.net/") -> "http://i-f.pximg.net/"
        else -> null
    }
}
