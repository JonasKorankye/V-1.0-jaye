package com.flipverse.shared.epub

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

actual class EpubParser actual constructor() {
    private var chapters = mutableListOf<ChapterInfo>()
    private var metadata: EpubMetadata? = null
    private var contentFiles = mutableMapOf<String, String>() // filename to content
    private var binaryFiles = mutableMapOf<String, ByteArray>() // filename to binary data (images)
    private var isInitialized = false
    private var tocMap = mutableMapOf<String, String>() // href to title mapping
    private var coverImageId: String? = null // manifest id of the cover image
    private var manifestItems = mutableMapOf<String, String>() // id to href

    actual suspend fun initialize(fileBytes: ByteArray): Boolean = withContext(Dispatchers.IO) {
        try {
            // Extract EPUB (ZIP) contents
            extractEpubContents(fileBytes)

            // Parse content.opf to get metadata and reading order
            parseContentOpf()

            // Try to parse TOC (toc.ncx or nav.xhtml) for better chapter titles
            parseToc()

            isInitialized = true
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    actual fun getTitle(): String? = metadata?.title

    actual fun getAuthors(): List<String> = metadata?.authors ?: emptyList()

    actual fun getChapterCount(): Int = chapters.size

    actual fun getChapterTitle(chapterIndex: Int): String? {
        return chapters.getOrNull(chapterIndex)?.title
    }

    actual suspend fun getChapterText(chapterIndex: Int): String = withContext(Dispatchers.IO) {
        try {
            if (chapterIndex < 0 || chapterIndex >= chapters.size) {
                return@withContext "Chapter $chapterIndex is out of bounds"
            }

            val chapter = chapters[chapterIndex]
            val htmlContent = contentFiles[chapter.href] ?: ""

            if (htmlContent.isEmpty()) {
                return@withContext """
                    Chapter content not found.
                    
                    File path: ${chapter.href}
                    Chapter title: ${chapter.title ?: "Unknown"}
                    
                    This might indicate:
                    1. The EPUB file structure is non-standard
                    2. The content file is missing
                    3. The file path resolution failed
                """.trimIndent()
            }

            // Strip HTML tags and extract text
            stripHtmlTags(htmlContent)
        } catch (e: Exception) {
            "Error extracting chapter text: ${e.message}"
        }
    }

    actual fun getTableOfContents(): List<TocItem> {
        return chapters.mapIndexed { index, chapter ->
            TocItem(chapter.title ?: "Chapter ${index + 1}", index)
        }
    }

    actual fun getCoverImage(): ByteArray? {
        if (!isInitialized) return null
        // Try to find cover image from the manifest using the cover image ID
        if (coverImageId != null) {
            // Look through manifest items stored during parsing
            val coverHref = manifestItems[coverImageId]
            if (coverHref != null) {
                return binaryFiles[coverHref]
            }
        }
        // Fallback: look for common cover image file names
        val coverPatterns = listOf("cover", "Cover", "COVER", "front")
        for (pattern in coverPatterns) {
            val match = binaryFiles.entries.firstOrNull { it.key.contains(pattern, ignoreCase = true) }
            if (match != null) return match.value
        }
        // Last resort: return the first image found
        return binaryFiles.values.firstOrNull()
    }

    actual fun close() {
        chapters.clear()
        contentFiles.clear()
        binaryFiles.clear()
        manifestItems.clear()
        metadata = null
        tocMap.clear()
        coverImageId = null
        isInitialized = false
    }

    private fun extractEpubContents(fileBytes: ByteArray) {
        ZipInputStream(ByteArrayInputStream(fileBytes)).use { zip ->
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val bytes = zip.readBytes()
                    val name = entry.name.lowercase()
                    // Store images as binary, everything else as text
                    if (name.endsWith(".jpg") || name.endsWith(".jpeg") ||
                        name.endsWith(".png") || name.endsWith(".gif") ||
                        name.endsWith(".svg") || name.endsWith(".webp")
                    ) {
                        binaryFiles[entry.name] = bytes
                    }
                    contentFiles[entry.name] = bytes.decodeToString()
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }

    private fun parseContentOpf() {
        // Find content.opf location from container.xml
        val containerXml = contentFiles["META-INF/container.xml"] ?: return
        val opfPath = extractOpfPath(containerXml)

        val opfContent = contentFiles[opfPath] ?: return
        val doc = parseXml(opfContent)

        // Extract metadata
        metadata = extractMetadata(doc)

        // Extract spine (reading order)
        extractSpine(doc, opfPath)
    }

    private fun parseToc() {
        // Try to find and parse toc.ncx or nav.xhtml for chapter titles
        val tocNcx = contentFiles.entries.firstOrNull { it.key.endsWith("toc.ncx") }
        if (tocNcx != null) {
            parseTocNcx(tocNcx.value)
            return
        }

        // Try nav.xhtml
        val navXhtml = contentFiles.entries.firstOrNull { 
            it.key.contains("nav") && (it.key.endsWith(".xhtml") || it.key.endsWith(".html"))
        }
        if (navXhtml != null) {
            parseNavXhtml(navXhtml.value)
        }
    }

    private fun parseTocNcx(ncxContent: String) {
        try {
            val doc = parseXml(ncxContent)
            val navPoints = doc.getElementsByTagName("navPoint")
            
            for (i in 0 until navPoints.length) {
                val navPoint = navPoints.item(i) as Element
                
                // Get title
                val navLabel = navPoint.getElementsByTagName("navLabel").item(0) as? Element
                val text = navLabel?.getElementsByTagName("text")?.item(0)?.textContent
                
                // Get content src
                val content = navPoint.getElementsByTagName("content").item(0) as? Element
                val src = content?.getAttribute("src")?.substringBefore("#") // Remove anchor
                
                if (text != null && src != null) {
                    tocMap[src] = text
                }
            }

            // Update chapter titles based on TOC
            chapters.forEach { chapter ->
                val href = chapter.href.substringAfterLast("/")
                val title = tocMap[href] ?: tocMap[chapter.href]
                if (title != null) {
                    chapter.title = title
                }
            }
        } catch (e: Exception) {
            println("Error parsing toc.ncx: ${e.message}")
        }
    }

    private fun parseNavXhtml(navContent: String) {
        try {
            // Simple regex-based parsing for nav.xhtml
            val linkRegex = """<a[^>]*href="([^"]*)"[^>]*>(.*?)</a>""".toRegex(RegexOption.DOT_MATCHES_ALL)
            
            linkRegex.findAll(navContent).forEach { match ->
                val href = match.groupValues.getOrNull(1)?.substringBefore("#") ?: return@forEach
                val title = match.groupValues.getOrNull(2)?.let { stripHtmlTags(it) } ?: return@forEach
                
                tocMap[href] = title
            }

            // Update chapter titles
            chapters.forEach { chapter ->
                val href = chapter.href.substringAfterLast("/")
                val title = tocMap[href] ?: tocMap[chapter.href]
                if (title != null) {
                    chapter.title = title
                }
            }
        } catch (e: Exception) {
            println("Error parsing nav.xhtml: ${e.message}")
        }
    }

    private fun extractOpfPath(containerXml: String): String {
        val doc = parseXml(containerXml)
        val rootfiles = doc.getElementsByTagName("rootfile")
        if (rootfiles.length > 0) {
            val element = rootfiles.item(0) as Element
            return element.getAttribute("full-path")
        }
        return "content.opf"
    }

    private fun extractMetadata(doc: Document): EpubMetadata {
        val metadataNode = doc.getElementsByTagName("metadata").item(0) as? Element

        val title = metadataNode?.getElementsByTagName("dc:title")?.item(0)?.textContent

        val authors = mutableListOf<String>()
        metadataNode?.getElementsByTagName("dc:creator")?.let { nodes ->
            for (i in 0 until nodes.length) {
                authors.add(nodes.item(i).textContent)
            }
        }

        val publisher = metadataNode?.getElementsByTagName("dc:publisher")?.item(0)?.textContent
        val language = metadataNode?.getElementsByTagName("dc:language")?.item(0)?.textContent
        val description = metadataNode?.getElementsByTagName("dc:description")?.item(0)?.textContent

        // Extract cover image ID from <meta name="cover" content="cover-image-id"/>
        metadataNode?.getElementsByTagName("meta")?.let { metas ->
            for (i in 0 until metas.length) {
                val meta = metas.item(i) as? Element ?: continue
                if (meta.getAttribute("name") == "cover") {
                    coverImageId = meta.getAttribute("content")
                }
            }
        }

        return EpubMetadata(title, authors, publisher, language, description)
    }

    private fun extractSpine(doc: Document, opfPath: String) {
        // Get manifest (maps idref to href)
        val manifest = mutableMapOf<String, ManifestItem>()
        val manifestNode = doc.getElementsByTagName("manifest").item(0) as? Element
        manifestNode?.getElementsByTagName("item")?.let { items ->
            for (i in 0 until items.length) {
                val item = items.item(i) as Element
                val id = item.getAttribute("id")
                val href = item.getAttribute("href")
                val mediaType = item.getAttribute("media-type")

                // Resolve relative path
                val basePath = opfPath.substringBeforeLast("/", "")
                val fullPath = if (basePath.isNotEmpty()) "$basePath/$href" else href

                manifest[id] = ManifestItem(fullPath, mediaType)
                // Store for cover image lookup
                manifestItems[id] = fullPath
            }
        }

        // Get spine (reading order)
        val spineNode = doc.getElementsByTagName("spine").item(0) as? Element
        spineNode?.getElementsByTagName("itemref")?.let { items ->
            for (i in 0 until items.length) {
                val item = items.item(i) as Element
                val idref = item.getAttribute("idref")
                manifest[idref]?.let { manifestItem ->
                    if (manifestItem.mediaType.contains("html") || manifestItem.mediaType.contains("xhtml")) {
                        // Try to extract title from the HTML content itself
                        val htmlContent = contentFiles[manifestItem.href]
                        val chapterTitle = htmlContent?.let { extractTitleFromHtml(it) }
                            ?: "Chapter ${chapters.size + 1}"
                        
                        chapters.add(
                            ChapterInfo(
                                title = chapterTitle,
                                href = manifestItem.href
                            )
                        )
                    }
                }
            }
        }
    }

    private fun extractTitleFromHtml(html: String): String? {
        // Try to extract title from <title> tag or first <h1> tag
        val titleRegex = Regex(
            """<title>(.*?)</title>""",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
        )
        val title = titleRegex.find(html)?.groupValues?.getOrNull(1)?.let { stripHtmlTags(it).trim() }
        if (title != null && title.isNotEmpty()) {
            return title
        }

        // Try h1 tag
        val h1Regex = Regex(
            """<h1[^>]*>(.*?)</h1>""",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
        )
        val h1 = h1Regex.find(html)?.groupValues?.getOrNull(1)?.let { stripHtmlTags(it).trim() }
        if (h1 != null && h1.isNotEmpty()) {
            return h1
        }

        return null
    }

    private fun parseXml(xmlContent: String): Document {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        val builder = factory.newDocumentBuilder()
        return builder.parse(ByteArrayInputStream(xmlContent.toByteArray()))
    }

    private fun stripHtmlTags(html: String): String {
        var text = html
            // Remove script and style blocks entirely
            .replace(Regex("<script[^>]*>.*?</script>", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<style[^>]*>.*?</style>", RegexOption.DOT_MATCHES_ALL), "")

        // Insert double newlines before block-level elements to preserve paragraph structure
        val blockTags = listOf(
            "p", "div", "h1", "h2", "h3", "h4", "h5", "h6",
            "blockquote", "section", "article", "aside", "header", "footer",
            "ul", "ol", "li", "tr", "table", "figure", "figcaption", "pre"
        )
        for (tag in blockTags) {
            text = text.replace(Regex("<$tag[^>]*>", RegexOption.IGNORE_CASE), "\n\n")
            text = text.replace(Regex("</$tag>", RegexOption.IGNORE_CASE), "\n\n")
        }

        // Convert <br> tags to single newline
        text = text.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")

        // Convert <hr> to a separator line
        text = text.replace(Regex("<hr\\s*/?>", RegexOption.IGNORE_CASE), "\n\n---\n\n")

        // Strip all remaining HTML tags (replace with space to avoid merging words)
        text = text.replace(Regex("<[^>]+>"), " ")

        // Decode HTML entities
        text = text
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace("&#x27;", "'")
            .replace("&#x2019;", "\u2019")
            .replace("&#x2018;", "\u2018")
            .replace("&#x201C;", "\u201C")
            .replace("&#x201D;", "\u201D")
            .replace("&mdash;", "\u2014")
            .replace("&ndash;", "\u2013")
            .replace("&hellip;", "\u2026")
            .replace("&lsquo;", "\u2018")
            .replace("&rsquo;", "\u2019")
            .replace("&ldquo;", "\u201C")
            .replace("&rdquo;", "\u201D")
            .replace(Regex("&#(\\d+);")) { match ->
                val code = match.groupValues[1].toIntOrNull()
                if (code != null) code.toChar().toString() else " "
            }
            .replace(Regex("&#x([0-9a-fA-F]+);")) { match ->
                val code = match.groupValues[1].toIntOrNull(16)
                if (code != null) code.toChar().toString() else " "
            }

        // Clean up whitespace: collapse spaces within lines but preserve newlines
        text = text
            .lines()
            .joinToString("\n") { it.replace(Regex("[ \\t]+"), " ").trim() }

        // Collapse 3+ consecutive newlines into 2 (paragraph break)
        text = text.replace(Regex("\\n{3,}"), "\n\n")

        return text.trim()
    }

    private data class ChapterInfo(
        var title: String?,
        val href: String
    )

    private data class ManifestItem(
        val href: String,
        val mediaType: String
    )
}
