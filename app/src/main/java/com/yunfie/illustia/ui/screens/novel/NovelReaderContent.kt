package com.yunfie.illustia.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yunfie.illustia.IllustiaViewModel
import com.yunfie.illustia.R
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal sealed interface NovelBlock

internal data class NovelPage(
    val blocks: List<NovelBlock>,
)

private data object NovelSpacerBlock : NovelBlock
private data class NovelParagraphBlock(val text: AnnotatedString) : NovelBlock
private data class NovelChapterBlock(val title: String) : NovelBlock
private data class NovelPixivImageBlock(val illustId: Long) : NovelBlock
private data class NovelJumpBlock(val pageNumber: Int) : NovelBlock

@Composable
internal fun NovelReaderPage(
    page: NovelPage,
    pageIndex: Int,
    pageCount: Int,
    viewModel: IllustiaViewModel,
    uriHandler: UriHandler,
    onJumpPage: (Int) -> Unit,
    scrollBehavior: ScrollBehavior,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            NovelMetaPill(text = "${pageIndex + 1} / $pageCount")
        }
        items(page.blocks) { block ->
            when (block) {
                NovelSpacerBlock -> Box(modifier = Modifier.padding(vertical = 2.dp))
                is NovelChapterBlock -> {
                    Text(
                        text = block.title,
                        style = MiuixTheme.textStyles.title4,
                        fontWeight = FontWeight.Black,
                    )
                }
                is NovelPixivImageBlock -> {
                    Button(onClick = { viewModel.openIllust(block.illustId) }) {
                        Text(stringResource(R.string.action_open))
                    }
                }
                is NovelJumpBlock -> {
                    Button(onClick = { onJumpPage(block.pageNumber - 1) }) {
                        Text(text = "Go to page ${block.pageNumber}")
                    }
                }
                is NovelParagraphBlock -> {
                    if (block.text.text.isNotBlank()) {
                        val urlAnnotations = block.text.getStringAnnotations("URL", 0, block.text.length)
                        if (urlAnnotations.isNotEmpty()) {
                            ClickableText(
                                text = block.text,
                                style = MiuixTheme.textStyles.body1.copy(color = MiuixTheme.colorScheme.onSurface),
                                onClick = { offset ->
                                    block.text.getStringAnnotations("URL", offset, offset)
                                        .firstOrNull()
                                        ?.let { uriHandler.openUri(it.item) }
                                },
                            )
                        } else {
                            Text(
                                text = block.text,
                                style = MiuixTheme.textStyles.body1,
                                lineHeight = 26.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

internal fun parseNovelPages(rawText: String): List<NovelPage> {
    val normalized = rawText.replace("\r\n", "\n")
    return normalized
        .split(Regex("""\s*\[newpage\]\s*"""))
        .map { parseNovelPage(it) }
        .ifEmpty { listOf(NovelPage(emptyList())) }
}

private fun parseNovelPage(rawPage: String): NovelPage {
    val lines = rawPage.replace("\r\n", "\n").split('\n')
    val blocks = mutableListOf<NovelBlock>()
    val paragraphBuffer = mutableListOf<String>()

    fun flushParagraph() {
        if (paragraphBuffer.isEmpty()) return
        val paragraphText = paragraphBuffer.joinToString("\n").trimEnd()
        if (paragraphText.isNotBlank()) {
            blocks += NovelParagraphBlock(parseNovelInlineText(paragraphText))
        }
        paragraphBuffer.clear()
    }

    for (line in lines) {
        val trimmed = line.trim()
        when {
            trimmed.isBlank() -> {
                flushParagraph()
                blocks += NovelSpacerBlock
            }
            trimmed.startsWith("[chapter:") && trimmed.endsWith("]") -> {
                flushParagraph()
                blocks += NovelChapterBlock(trimmed.removePrefix("[chapter:").removeSuffix("]"))
            }
            trimmed.startsWith("[pixivimage:") && trimmed.endsWith("]") -> {
                flushParagraph()
                blocks += NovelPixivImageBlock(trimmed.removePrefix("[pixivimage:").removeSuffix("]").toLongOrNull() ?: continue)
            }
            trimmed.startsWith("[jump:") && trimmed.endsWith("]") -> {
                flushParagraph()
                blocks += NovelJumpBlock(trimmed.removePrefix("[jump:").removeSuffix("]").toIntOrNull() ?: continue)
            }
            else -> paragraphBuffer += line
        }
    }
    flushParagraph()

    return NovelPage(blocks)
}

private fun parseNovelInlineText(text: String): AnnotatedString {
    val pattern = Regex("""(\[\[(?:rb|emphasismark|jumpuri):.*?\]\]|\[(?:b|i):.*?\])""")
    val result = buildAnnotatedString {
        var index = 0
        pattern.findAll(text).forEach { match ->
            if (match.range.first > index) {
                append(text.substring(index, match.range.first))
            }
            appendNovelToken(match.value)
            index = match.range.last + 1
        }
        if (index < text.length) {
            append(text.substring(index))
        }
    }
    return result
}

private fun AnnotatedString.Builder.appendNovelToken(token: String) {
    when {
        token.startsWith("[[rb:") -> {
            val inner = token.removePrefix("[[rb:").removeSuffix("]]")
            val parts = inner.split(" > ", limit = 2)
            val base = parts.getOrNull(0).orEmpty()
            val ruby = parts.getOrNull(1).orEmpty()
            append(base)
            if (ruby.isNotBlank()) {
                append("（")
                withStyle(SpanStyle(fontSize = 0.72.em)) {
                    append(ruby)
                }
                append("）")
            }
        }
        token.startsWith("[[emphasismark:") -> {
            val inner = token.removePrefix("[[emphasismark:").removeSuffix("]]")
            val parts = inner.split(" > ", limit = 2)
            val base = parts.getOrNull(0).orEmpty()
            val mark = parts.getOrNull(1).orEmpty().ifBlank { "﹅" }
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(base)
            }
            if (base.isNotBlank()) {
                val repeated = if (mark.length == 1) mark.repeat(base.length.coerceAtLeast(1)) else mark
                withStyle(SpanStyle(fontSize = 0.72.em)) {
                    append(repeated)
                }
            }
        }
        token.startsWith("[[jumpuri:") -> {
            val inner = token.removePrefix("[[jumpuri:").removeSuffix("]]")
            val parts = inner.split(" > ", limit = 2)
            val title = parts.getOrNull(0).orEmpty()
            val url = parts.getOrNull(1).orEmpty()
            if (url.isNotBlank()) {
                pushStringAnnotation(tag = "URL", annotation = url)
            }
            withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                append(title)
            }
            if (url.isNotBlank()) {
                pop()
            }
        }
        token.startsWith("[b:") -> {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(token.removePrefix("[b:").removeSuffix("]"))
            }
        }
        token.startsWith("[i:") -> {
            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                append(token.removePrefix("[i:").removeSuffix("]"))
            }
        }
    }
}
