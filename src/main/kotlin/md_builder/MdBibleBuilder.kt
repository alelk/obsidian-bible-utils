package io.github.alelk.obsidian_bible_utils.md_builder

import io.github.alelk.obsidian_bible_utils.model.*

data class MdBook(val name: String, val chapters: List<MdChapter>)

data class MdChapter(val number: Int, val name: String, val text: String)

fun Bible.toMd(mainTranslationSlug: String, strongNumberBaseDir: String = "../../Dict/StrongNumbers/"): List<MdBook> =
  books
    .sortedBy { it.id }
    .map { book ->
      val bookInfo =
        booksInfoByBibleVariant[BibleVariant.Translation(mainTranslationSlug)]
          ?.find { it.id == book.id }
          ?: throw IllegalStateException("No book info found for book ${book.id} and translation $mainTranslationSlug")

      MdBook(
        name = bookInfo.name,
        chapters = book.chapters.sortedBy { it.number }.map { chapter ->
          val chapterBody = chapter.toMd(mainTranslationSlug, strongNumberBaseDir)
          val chapterText = buildString {
            appendLine(chapterBody)
            appendLine("---")
            val prevChapter = book.chapters.find { it.number == chapter.number - 1 }
            val nextChapter = book.chapters.find { it.number == chapter.number + 1 }
            listOfNotNull(prevChapter, nextChapter).forEach { c ->
              val chapterName = "${bookInfo.name} - ${c.number}"
              appendLine("[$chapterName|$chapterName]")
            }
          }
          MdChapter(number = chapter.number, name = "${bookInfo.name} - ${chapter.number}", text = chapterText)
        }
      )
    }

fun Chapter.toMd(mainTranslationSlug: String, strongNumberBaseDir: String = "../../Dict/StrongNumbers/"): String =
  verses.mapNotNull { it.toMd(mainTranslationSlug, strongNumberBaseDir) }.joinToString("\n\n")

fun Verse.toMd(mainTranslationSlug: String, strongNumberBaseDir: String = "../../Dict/StrongNumbers/"): String? {
  val mainTranslation = this.text[BibleVariant.Translation(mainTranslationSlug)] ?: return null
  return buildString {
    appendLine("##### $number")
    appendLine("**${mainTranslation.text}**")
    val otherTranslations = text.filterNot { it.key == BibleVariant.Translation(mainTranslationSlug) }
    val transliterations = text.filter { it.key is BibleVariant.Transliteration }
    otherTranslations.values.forEach {
      appendLine("> ${it.text.skipStrongNumbers()}")
    }
    transliterations.forEach {
      appendLine("> ${it.value.text.transliterationToMd(strongNumberBaseDir)}")
    }
  }
}

val r = "".replace(Regex("""<S>\d+</S>"""), "")

fun String.skipStrongNumbers(): String =
  replace(Regex("""<S>\d+</S>"""), " ")
    .replace(Regex("""<br/>"""), " ")
    .replace(Regex("""\s+"""), " ")
    .trim()

fun String.transliterationToMd(strongNumberBaseDir: String): String =
  replace(Regex("""(?<word>[\p{L}\p{InCombiningDiacriticalMarks}\u0590-\u05fe]+)(?<delim>[:.])?<S>(?<strongNum>\d+)</S>""", RegexOption.IGNORE_CASE)) {
    val word = it.groups["word"]!!.value
    val delim = it.groups["delim"]?.value ?: ""
    val strongNum = it.groups["strongNum"]!!.value
    """[$strongNumberBaseDir$strongNum|$word]$delim"""
  }.skipStrongNumbers()