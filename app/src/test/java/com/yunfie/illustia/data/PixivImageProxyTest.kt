package com.yunfie.illustia.data

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class PixivImageProxyTest : FreeSpec({
    "proxyPixivImageUrl should rewrite pixiv image hosts to the selected proxy host" {
        proxyPixivImageUrl(
            "https://i.pximg.net/img-original/img/2024/01/01/00/00/00/12345678_p0.jpg",
            "https://i.yunfi.f5.si/",
        ) shouldBe "https://i.yunfi.f5.si/img-original/img/2024/01/01/00/00/00/12345678_p0.jpg"
    }

    "proxyPixivImageUrl should rewrite pixiv image hosts to the Webp proxy endpoint" {
        proxyPixivImageUrl(
            "https://i.pximg.net/img-original/img/2024/01/01/00/00/00/12345678_p0.jpg",
            "https://proxy.yunfi.f5.si/image.webp?url=",
        ) shouldBe "https://proxy.yunfi.f5.si/image.webp?url=https%3A%2F%2Fi.pximg.net%2Fimg-original%2Fimg%2F2024%2F01%2F01%2F00%2F00%2F00%2F12345678_p0.jpg"
    }

    "proxyPixivImageUrl should leave non-pixiv image hosts unchanged" {
        proxyPixivImageUrl(
            "https://example.com/image.jpg",
            "https://proxy.yunfi.f5.si/image.webp?url=",
        ) shouldBe "https://example.com/image.jpg"
    }
})
