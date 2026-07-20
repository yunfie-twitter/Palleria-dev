package com.yunfie.illustia.nativebridge

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class NativeIntentRouterTest : FunSpec({
    test("process text normalization removes the leading hash") {
        NativeIntentRouter.normalizeProcessText("  #初音ミク  ") shouldBe "初音ミク"
    }

    test("process text collapses whitespace and removes control characters") {
        NativeIntentRouter.normalizeProcessText("  blue\n archive\u0000\t tag  ") shouldBe
            "blue archive tag"
    }

    test("blank process text is ignored") {
        NativeIntentRouter.normalizeProcessText(" \n\t\u0000 ") shouldBe null
    }

    test("process text is capped without splitting a surrogate pair") {
        val text = "絵".repeat(NativeIntentRouter.MAX_PROCESS_TEXT_CODE_POINTS - 1) + "😀末尾"
        val normalized = NativeIntentRouter.normalizeProcessText(text)!!

        normalized.codePointCount(0, normalized.length) shouldBe
            NativeIntentRouter.MAX_PROCESS_TEXT_CODE_POINTS
        normalized.endsWith("😀") shouldBe true
    }
})
