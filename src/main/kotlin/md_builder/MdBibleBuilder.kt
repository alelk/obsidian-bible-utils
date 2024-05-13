package io.github.alelk.obsidian_bible_utils.md_builder

import io.github.alelk.obsidian_bible_utils.model.*

data class MdBook(val name: String, val chapters: List<MdChapter>)

data class MdChapter(val number: Int, val name: String, val text: String)

fun Bible.toMd(
  mainTranslation: BibleVariant.Translation,
  dictRef: (refTopic: DictDefinition.Topic) -> MdReference?
): List<MdBook> =
  books
    .sortedBy { it.id }
    .map { book ->
      val bookInfo =
        booksInfoByBibleVariant[mainTranslation]
          ?.find { it.id == book.id }
          ?: throw IllegalStateException("No book info found for book ${book.id} and translation $mainTranslation")

      MdBook(
        name = bookInfo.name,
        chapters = book.chapters.sortedBy { it.number }.map { chapter ->
          val chapterBody = chapter.toMdBody(mainTranslation, dictRef)
          val chapterText = buildString {
            appendLine("---")
            appendLine("type: bible")
            appendLine("book: ${bookInfo.name}")
            appendLine("book_id: ${book.id}")
            appendLine("chapter: ${chapter.number}")
            appendLine("---")
            appendLine()
            chapter.attrs["error"]?.let { error ->
              appendLine("\n\n#todo $error\n\n")
            }
            appendLine(chapterBody)
            appendLine()
            appendLine("---")
            val prevChapter = book.chapters.find { it.number == chapter.number - 1 }
            val nextChapter = book.chapters.find { it.number == chapter.number + 1 }
            listOfNotNull(prevChapter, nextChapter).forEach { c ->
              val chapterName = "${bookInfo.name} ${c.number}"
              appendLine("[[$chapterName|${chapterName}]]")
            }
            appendLine()
            appendLine("#bible #bible_chapter #generated #unchanged")
          }
          MdChapter(number = chapter.number, name = "${bookInfo.name} ${chapter.number}", text = chapterText)
        }
      )
    }

fun Chapter.toMdBody(
  mainTranslation: BibleVariant.Translation,
  dictReference: (refTopic: DictDefinition.Topic) -> MdReference?
): String =
  verses.mapNotNull { it.toMd(mainTranslation, dictReference) }.joinToString("\n\n")

fun Verse.toMd(
  mainTranslation: BibleVariant.Translation,
  dictReference: (refTopic: DictDefinition.Topic) -> MdReference?
): String? {
  val mainTranslationText = this.text[mainTranslation] ?: return null
  return buildString {
    appendLine("##### $number")
    appendLine("**${mainTranslationText.text}**")
    appendLine()
    val otherTranslations = text.filterNot { it.key == mainTranslation }
    val transliterations = otherTranslations.filter { it.key is BibleVariant.Transliteration }
    (otherTranslations - transliterations.keys).forEach { (t, v) ->
      // fixme: store dict language in book info
      if (t is BibleVariant.Translation && t.translationSlug == "TISCH")
        appendLine("> ${v.text.transliterationToMd(DictDefinition.TopicType.GREEK, dictReference, useWikiLinks = true)}")
      else
        appendLine("> ${v.text.skipStrongNumbers()}")
    }
    transliterations.forEach {
      appendLine("> ${it.value.text.transliterationToMd(DictDefinition.TopicType.HEBREW, dictReference, useWikiLinks = true)}")
    }
  }
}

val r = "".replace(Regex("""<S>\d+</S>"""), "")

fun String.skipStrongNumbers(): String =
  replace(Regex("""<S>\d+</S>"""), "")
    .replace(Regex("""<br/>"""), " ")
    .replace(Regex("""\s+"""), " ")
    .trim()

fun String.transliterationToMd(
  topicType: DictDefinition.TopicType,
  dictReference: (refTopic: DictDefinition.Topic) -> MdReference?, useWikiLinks: Boolean = false
): String =
  replace(Regex("""(?<word>[\p{L}\p{InCombiningDiacriticalMarks}\u0590-\u05fe]+)(?<delim>[:.])?<S>(?<strongNum>\d+)</S>""", RegexOption.IGNORE_CASE)) {
    val word = it.groups["word"]!!.value
    val delim = it.groups["delim"]?.value ?: ""
    val strongNum = it.groups["strongNum"]!!.value
    val dictRef = dictReference(DictDefinition.Topic(topicType, strongNum.toInt()))
    if (dictRef != null) """${dictRef.toMdLink(text = word, wiki = useWikiLinks)}$delim""" else "$word$delim"
  }.skipStrongNumbers()