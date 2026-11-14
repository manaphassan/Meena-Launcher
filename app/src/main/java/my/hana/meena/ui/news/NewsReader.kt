package my.hana.meena.ui.news

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.hana.meena.R
import my.hana.meena.data.news.ArticleBlock
import my.hana.meena.data.news.ArticleHtml
import my.hana.meena.data.news.InlineSpan
import my.hana.meena.data.news.NewsArticle
import my.hana.meena.data.news.NewsTime
import my.hana.meena.data.news.ReaderBody
import my.hana.meena.data.news.RichText
import my.hana.meena.theme.MeenaBlack
import my.hana.meena.theme.MeenaSpacing
import my.hana.meena.theme.MeenaTextMuted
import my.hana.meena.theme.MeenaTextSecondary
import my.hana.meena.theme.MeenaTextWhite
import my.hana.meena.ui.components.MetroMessageContainer
import my.hana.meena.ui.components.metroClickable

/**
 * Full-screen, in-app article reader. The body is drawn natively from the feed's HTML (no WebView).
 * Hosted in a full-screen dialog so it covers the pager and back closes it.
 */
@Composable
fun NewsReader(article: NewsArticle, blurPictures: Boolean = false, onClose: () -> Unit) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        NewsReaderContent(article = article, blurPictures = blurPictures, onClose = onClose)
    }
}

/** A blurred picture with a note; tapping shows every picture in the article. */
@Composable
private fun ReaderImage(url: String, targetPx: Int, blurred: Boolean, onReveal: () -> Unit) {
    if (!blurred) {
        NewsImage(url = url, targetPx = targetPx, fitWidth = true)
        return
    }
    Column(
        modifier = Modifier.metroClickable(onClick = onReveal),
        verticalArrangement = Arrangement.spacedBy(MeenaSpacing.s)
    ) {
        NewsImage(url = url, targetPx = targetPx, fitWidth = true, blurred = true)
        Text(
            text = stringResource(R.string.news_nsfw_tap_to_show),
            style = MaterialTheme.typography.labelSmall,
            color = MeenaTextMuted
        )
    }
}

@Composable
private fun NewsReaderContent(article: NewsArticle, blurPictures: Boolean, onClose: () -> Unit) {
    val context = LocalContext.current
    var revealed by remember(article.id) { mutableStateOf(false) }
    val blurred = blurPictures && !revealed
    val density = LocalDensity.current
    val openFailed = stringResource(R.string.news_open_failed)
    val separator = stringResource(R.string.news_meta_separator)
    val linkColor = MaterialTheme.colorScheme.primary
    val onLink: (String) -> Unit = { url -> openInBrowser(context, url, openFailed) }

    val windowWidthPx = LocalWindowInfo.current.containerSize.width
    val imageTargetPx = remember(windowWidthPx, density) {
        val margins = with(density) { (MeenaSpacing.l * 2).roundToPx() }
        (windowWidthPx - margins).takeIf { it > 0 } ?: 1080
    }

    val meta = remember(article.id, separator) {
        listOf(article.source.lowercase(), NewsTime.readerDate(article.pubDateMs, System.currentTimeMillis()))
            .filter { it.isNotBlank() }
            .joinToString(separator)
    }
    val host = remember(article.link) { linkHost(article.link) }

    val body by produceState<ReaderBody?>(initialValue = null, article.id) {
        value = withContext(Dispatchers.Default) {
            ArticleHtml.readerBody(article.contentHtml, article.description, article.imageUrl)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MeenaBlack)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = MeenaSpacing.l,
                end = MeenaSpacing.l,
                top = MeenaSpacing.m,
                bottom = MeenaSpacing.l * 2
            ),
            verticalArrangement = Arrangement.spacedBy(MeenaSpacing.m)
        ) {
            item(key = "back") { ReaderBackRow(onClose) }

            if (meta.isNotBlank()) {
                item(key = "meta") {
                    Text(text = meta, style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                }
            }

            item(key = "title") {
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Light,
                    color = MeenaTextWhite
                )
            }

            val hero = body?.heroUrl ?: article.imageUrl
            if (hero != null) {
                item(key = "hero") {
                    Column(
                        modifier = Modifier.padding(top = MeenaSpacing.s),
                        verticalArrangement = Arrangement.spacedBy(MeenaSpacing.s)
                    ) {
                        ReaderImage(url = hero, targetPx = imageTargetPx, blurred = blurred, onReveal = { revealed = true })
                        body?.heroCaption?.let { ImageCaption(it) }
                    }
                }
            }

            body?.let { readerBody ->
                itemsIndexed(readerBody.blocks, key = { index, _ -> "block-$index" }) { _, block ->
                    ReaderBlock(block, imageTargetPx, linkColor, onLink, blurred) { revealed = true }
                }
            }

            if (host != null) {
                item(key = "open") {
                    Box(
                        modifier = Modifier
                            .padding(top = MeenaSpacing.m)
                            .heightIn(min = 48.dp)
                            .metroClickable { onLink(article.link) },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = stringResource(R.string.news_open_on, host),
                            style = MaterialTheme.typography.titleMedium,
                            color = linkColor
                        )
                    }
                }
            }
        }
        // The home screen's message bar sits behind this dialog, so show messages here too.
        MetroMessageContainer()
    }
}

@Composable
private fun ReaderBackRow(onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .metroClickable(onClick = onClose),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MeenaSpacing.s)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_thin_back),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(MeenaSpacing.l)
        )
        Text(
            text = stringResource(R.string.news_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

private val ReaderLineHeight = 26.sp

@Composable
private fun ReaderBlock(
    block: ArticleBlock,
    imageTargetPx: Int,
    linkColor: Color,
    onLink: (String) -> Unit,
    blurred: Boolean,
    onReveal: () -> Unit
) {
    val bodyStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = ReaderLineHeight, color = MeenaTextWhite)
    when (block) {
        is ArticleBlock.Paragraph -> Text(
            text = block.text.toAnnotated(linkColor, onLink),
            style = bodyStyle
        )

        is ArticleBlock.Heading -> Text(
            text = block.text.toAnnotated(linkColor, onLink),
            style = headingStyle(block.level),
            color = MeenaTextWhite,
            modifier = Modifier.padding(top = MeenaSpacing.m)
        )

        is ArticleBlock.ListItem -> ListItemRow(block, bodyStyle, linkColor, onLink)

        is ArticleBlock.Quote -> Text(
            text = block.text.toAnnotated(linkColor, onLink),
            style = bodyStyle.copy(color = MeenaTextSecondary, fontStyle = FontStyle.Italic),
            modifier = Modifier.padding(start = MeenaSpacing.l)
        )

        is ArticleBlock.Image -> Column(
            modifier = Modifier.padding(vertical = MeenaSpacing.s),
            verticalArrangement = Arrangement.spacedBy(MeenaSpacing.s)
        ) {
            ReaderImage(url = block.src, targetPx = imageTargetPx, blurred = blurred, onReveal = onReveal)
            block.caption?.let { ImageCaption(it) }
        }
    }
}

@Composable
private fun headingStyle(level: Int): TextStyle = when (level) {
    1, 2 -> MaterialTheme.typography.titleLarge
    3 -> MaterialTheme.typography.headlineSmall
    else -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium)
}

@Composable
private fun ListItemRow(
    item: ArticleBlock.ListItem,
    bodyStyle: TextStyle,
    linkColor: Color,
    onLink: (String) -> Unit
) {
    val density = LocalDensity.current
    val bulletSize = MeenaSpacing.s
    // Centre the square bullet on the first line of text.
    val bulletTop = with(density) { ((ReaderLineHeight.toDp() - bulletSize) / 2).coerceAtLeast(0.dp) }
    Row(horizontalArrangement = Arrangement.spacedBy(MeenaSpacing.m)) {
        if (item.number != null) {
            Text(text = "${item.number}.", style = bodyStyle.copy(color = MeenaTextMuted))
        } else {
            Box(
                modifier = Modifier
                    .padding(top = bulletTop)
                    .size(bulletSize)
                    .background(MeenaTextSecondary)
            )
        }
        Text(
            text = item.text.toAnnotated(linkColor, onLink),
            style = bodyStyle,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ImageCaption(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MeenaTextMuted,
        modifier = Modifier.fillMaxWidth()
    )
}

private fun RichText.toAnnotated(linkColor: Color, onLink: (String) -> Unit): AnnotatedString =
    buildAnnotatedString {
        append(text)
        for (span in spans) {
            val start = span.start.coerceIn(0, text.length)
            val end = span.end.coerceIn(start, text.length)
            if (start == end) continue
            when (span.kind) {
                InlineSpan.Kind.BOLD -> addStyle(SpanStyle(fontWeight = FontWeight.Medium), start, end)
                InlineSpan.Kind.ITALIC -> addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
                InlineSpan.Kind.LINK -> span.url?.let { url ->
                    addLink(
                        LinkAnnotation.Url(
                            url = url,
                            styles = TextLinkStyles(
                                style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)
                            ),
                            linkInteractionListener = LinkInteractionListener { onLink(url) }
                        ),
                        start,
                        end
                    )
                }
            }
        }
    }
