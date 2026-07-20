package com.yunfie.illustia.data

import android.os.Build
import com.yunfie.illustia.models.NetworkMode
import com.yunfie.illustia.models.Illust
import com.yunfie.illustia.models.PageResult
import com.yunfie.illustia.models.PixivSession
import com.yunfie.illustia.models.UserProfile
import com.yunfie.illustia.models.pixiv.IllustSeries
import com.yunfie.illustia.models.pixiv.UgoiraFrame
import com.yunfie.illustia.models.pixiv.UgoiraPlayback
import com.yunfie.illustia.models.pixiv.UgoiraPlaybackFrame
import com.yunfie.illustia.rust.ApiException
import com.yunfie.illustia.rust.PixivHttpClient
import com.yunfie.illustia.settings.currentAcceptLanguage
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import okio.buffer
import okio.sink

/** OkHttp request builder compatibility layer backed by the UniFFI Rust client. */
internal class RustPixivHttpClient(mode: NetworkMode) {
    private val native = PixivHttpClient(
        networkMode = mode.code,
        userAgent = "PixivAndroidApp/${PixivApiConfig.APP_VERSION} (Android ${Build.VERSION.RELEASE}; ${Build.MODEL})",
        appOsVersion = "Android ${Build.VERSION.RELEASE}",
        acceptLanguage = currentAcceptLanguage(),
    )

    fun newCall(request: Request): RustPixivCall = RustPixivCall(native, request)

    suspend fun loginWithRefreshToken(refreshToken: String): PixivSession = withContext(Dispatchers.IO) {
        nativeCall {
            native.loginWithRefreshToken(refreshToken).let {
                PixivSession(it.accessToken, it.refreshToken, it.userId?.toLong())
            }
        }
    }

    suspend fun loginWithAuthorizationCode(code: String, codeVerifier: String): PixivSession = withContext(Dispatchers.IO) {
        nativeCall {
            native.loginWithAuthorizationCode(code, codeVerifier).let {
                PixivSession(it.accessToken, it.refreshToken, it.userId?.toLong())
            }
        }
    }

    fun createWebLoginUrl(createProvisionalAccount: Boolean, codeChallenge: String): String =
        native.createWebLoginUrl(createProvisionalAccount, codeChallenge)

    suspend fun prepareUgoira(
        url: String,
        cacheDir: String,
        frames: List<UgoiraFrame>,
    ): UgoiraPlayback = withContext(Dispatchers.IO) {
        nativeCall {
            native.prepareUgoira(
                url = url,
                headers = mapOf(
                    "Referer" to "https://www.pixiv.net/",
                    "User-Agent" to "PixivAndroidApp/6.184.0 (Android 14; Palleria)",
                ),
                cacheDir = cacheDir,
                frames = frames.map {
                    com.yunfie.illustia.rust.UgoiraFrame(
                        file = it.file,
                        delayMillis = it.delay,
                    )
                },
            ).let { playback ->
                UgoiraPlayback(
                    frames = playback.frames.map {
                        UgoiraPlaybackFrame(
                            filePath = it.file,
                            delayMillis = it.delayMillis,
                        )
                    },
                )
            }
        }
    }
}

internal class RustPixivCall(
    private val native: PixivHttpClient,
    private val request: Request,
) {
    suspend fun awaitBody(): String = withContext(Dispatchers.IO) {
        val nativeRequest = lowerRequest()
        nativeCall {
            native.execute(
                method = nativeRequest.method,
                url = nativeRequest.url,
                headers = nativeRequest.headers,
                body = nativeRequest.body,
                contentType = nativeRequest.contentType,
            ).body
        }
    }

    suspend fun awaitIllustPage(): PageResult<Illust> = withContext(Dispatchers.IO) {
        val nativeRequest = lowerRequest()
        nativeCall {
            native.executeIllustPage(
                method = nativeRequest.method,
                url = nativeRequest.url,
                headers = nativeRequest.headers,
                body = nativeRequest.body,
                contentType = nativeRequest.contentType,
            ).let { page ->
                PageResult(
                    items = page.items.map { it.toAppModel() },
                    nextUrl = page.nextUrl,
                )
            }
        }
    }

    suspend fun awaitIllustDetail(): Illust = withContext(Dispatchers.IO) {
        val nativeRequest = lowerRequest()
        nativeCall {
            native.executeIllustDetail(
                method = nativeRequest.method,
                url = nativeRequest.url,
                headers = nativeRequest.headers,
                body = nativeRequest.body,
                contentType = nativeRequest.contentType,
            ).toAppModel()
        }
    }

    suspend fun awaitUserProfile(fallbackUserId: Long): UserProfile = withContext(Dispatchers.IO) {
        val nativeRequest = lowerRequest()
        nativeCall {
            native.executeUserProfile(
                method = nativeRequest.method,
                url = nativeRequest.url,
                headers = nativeRequest.headers,
                body = nativeRequest.body,
                contentType = nativeRequest.contentType,
                fallbackUserId = fallbackUserId,
            ).toAppModel()
        }
    }

    private fun lowerRequest(): NativeRequest {
        val sink = ByteArrayOutputStream()
        request.body?.let { body ->
            val bufferedSink = sink.sink().buffer()
            body.writeTo(bufferedSink)
            bufferedSink.flush()
        }
        val headers = buildMap {
            request.headers.names().forEach { name -> put(name, request.headers.values(name).joinToString(", ")) }
        }
        return NativeRequest(
            method = request.method,
            url = request.url.toString(),
            headers = headers,
            body = sink.toByteArray(),
            contentType = request.body?.contentType()?.toString(),
        )
    }
}

private data class NativeRequest(
    val method: String,
    val url: String,
    val headers: Map<String, String>,
    val body: ByteArray,
    val contentType: String?,
)

private fun com.yunfie.illustia.rust.Illust.toAppModel(): Illust = Illust(
    id = id,
    title = title,
    type = illustType,
    caption = caption,
    artistId = artistId,
    artistName = artistName,
    artistAvatarUrl = artistAvatarUrl,
    squareImageUrl = squareImageUrl,
    mediumImageUrl = mediumImageUrl,
    imageUrl = imageUrl,
    originalImageUrl = originalImageUrl,
    mediumImagePages = mediumImagePages,
    imagePages = imagePages,
    originalImagePages = originalImagePages,
    tags = tags,
    pageCount = pageCount,
    isBookmarked = isBookmarked,
    totalComments = totalComments,
    series = series?.let { IllustSeries(id = it.id, title = it.title) },
)

private fun com.yunfie.illustia.rust.UserProfile.toAppModel(): UserProfile = UserProfile(
    id = id,
    name = name,
    account = account,
    profileImageUrl = profileImageUrl,
    backgroundImageUrl = backgroundImageUrl,
    comment = comment,
    isFollowed = isFollowed,
)

private inline fun <T> nativeCall(block: () -> T): T = try {
    block()
} catch (error: ApiException.Http) {
    throw PixivApiException(error.status.toInt(), error.detail)
} catch (error: ApiException) {
    val detail = when (error) {
        is ApiException.InvalidRequest -> error.detail
        is ApiException.Network -> error.detail
        is ApiException.Http -> error.detail
    }
    throw PixivApiException(0, detail)
}
