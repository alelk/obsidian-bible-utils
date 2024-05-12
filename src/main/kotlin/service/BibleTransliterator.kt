package io.github.alelk.obsidian_bible_utils.service

import io.github.alelk.obsidian_bible_utils.client.ALittleHebrewTranslatorClient
import io.github.alelk.obsidian_bible_utils.model.*
import io.github.alelk.obsidian_bible_utils.utils.retryOnException
import mu.KotlinLogging
import kotlin.time.measureTimedValue

private val log = KotlinLogging.logger { }

class BibleTransliterator(private val transliteratorClient: ALittleHebrewTranslatorClient) {

  suspend fun addHebrewTransliteration(chapter: Chapter, hebrewTranslation: BibleVariant.Translation): Chapter {
    val hebrewTransliteration = BibleVariant.Transliteration(hebrewTranslation.translationSlug)
    val nextVerses = chapter.verses.map { verse ->
      val hebrewText = verse.text[hebrewTranslation]?.text
      val hebrewTransliterationText = verse.text[hebrewTransliteration]?.text
      if (hebrewText != null && hebrewTransliterationText == null) {
        val transliterated = measureTimedValue { retryOnException { transliteratorClient.getTransliteration(hebrewText) } }
        log.debug { "chapter ${chapter.number}, verse ${verse.number} transliterated (${transliterated.duration}): '${transliterated.value}'" }
        verse + Verse(verse.number, mapOf(hebrewTransliteration to Verse.Data(transliterated.value)))
      } else verse
    }
    return chapter.copy(verses = nextVerses)
  }

  suspend fun addHebrewTransliteration(book: Book, hebrewTranslation: BibleVariant.Translation): Book {
    val nextChapters = book.chapters.map { chapter ->
      measureTimedValue { addHebrewTransliteration(chapter, hebrewTranslation) }
        .also { log.info { "book ${book.id}, chapter ${chapter.number} of ${book.chapters.size}: hebrew transliteration loaded: ${it.duration}" } }
        .value
    }
    return book.copy(chapters = nextChapters)
  }
}