package io.github.alelk.obsidian_bible_utils.md_builder

import io.github.alelk.obsidian_bible_utils.model.*

data class MdBook(val name: String, val chapters: List<MdChapter>)

data class MdChapter(val number: Int, val name: String, val text: String)

fun Bible.toMd(mainTranslation: BibleVariant.Translation, dictRef: (refTopic: DictDefinition.Topic) -> DictReference): List<MdBook> =
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
              appendLine("[$chapterName](${chapterName}.md)")
            }
            appendLine()
            appendLine("#bible")
          }
          MdChapter(number = chapter.number, name = "${bookInfo.name} ${chapter.number}", text = chapterText)
        }
      )
    }

fun Chapter.toMdBody(mainTranslation: BibleVariant.Translation, dictReference: (refTopic: DictDefinition.Topic) -> DictReference): String =
  verses.mapNotNull { it.toMd(mainTranslation, dictReference) }.joinToString("\n\n")

fun Verse.toMd(mainTranslation: BibleVariant.Translation, dictReference: (refTopic: DictDefinition.Topic) -> DictReference): String? {
  val mainTranslationText = this.text[mainTranslation] ?: return null
  return buildString {
    appendLine("##### $number")
    appendLine("**${mainTranslationText.text}**")
    appendLine()
    val otherTranslations = text.filterNot { it.key == mainTranslation }
    val transliterations = otherTranslations.filter { it.key is BibleVariant.Transliteration }
    (otherTranslations - transliterations.keys).values.forEach {
      appendLine("> ${it.text.skipStrongNumbers()}")
    }
    transliterations.forEach {
      appendLine("> ${it.value.text.transliterationToMd(dictReference)}")
    }
  }
}

val r = "".replace(Regex("""<S>\d+</S>"""), "")

fun String.skipStrongNumbers(): String =
  replace(Regex("""<S>\d+</S>"""), " ")
    .replace(Regex("""<br/>"""), " ")
    .replace(Regex("""\s+"""), " ")
    .trim()

fun String.transliterationToMd(dictReference: (refTopic: DictDefinition.Topic) -> DictReference): String =
  replace(Regex("""(?<word>[\p{L}\p{InCombiningDiacriticalMarks}\u0590-\u05fe]+)(?<delim>[:.])?<S>(?<strongNum>\d+)</S>""", RegexOption.IGNORE_CASE)) {
    val word = it.groups["word"]!!.value
    val delim = it.groups["delim"]?.value ?: ""
    val strongNum = it.groups["strongNum"]!!.value
    val dictRef = dictReference(DictDefinition.Topic(DictDefinition.TopicType.HEBREW, strongNum.toInt()))
    """${dictRef.toMdLink(text = word)}$delim"""
  }.skipStrongNumbers()