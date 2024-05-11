package io.github.alelk.obsidian_bible_utils

import io.github.alelk.bolls_api_client.BollsApiClient
import io.github.alelk.bolls_api_client.BollsApiClientConfig
import io.github.alelk.obsidian_bible_utils.model.*
import io.github.alelk.obsidian_bible_utils.transliterator.ALittleHebrewTranslatorClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retry
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Properties
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

suspend fun <T> retryOnException(
  countRetries: Long = 10,
  delay: Duration = 10.seconds,
  excFilter: (exc: Throwable) -> Boolean = { true },
  block: suspend () -> T
): T =
  flow {
    emit(block())
  }.retry(countRetries) { e ->
    if (excFilter(e)) {
      System.err.println("Error occurred: ${e.message}. Retry after $delay...")
      delay(delay)
      true
    } else false
  }.first()

suspend fun BollsApiClient.loadBible(translationSlug: String): Bible {
  val books = retryOnException { getBooksInfoByTranslation(translationSlug) }.map { bi ->
    val chapters = List(bi.countChapters) { chapter ->
      val verses = retryOnException {
        getChapterText(translationSlug, bi.bookId, chapter + 1, withTranslatorComments = true)
      }
      Chapter(
        number = chapter + 1,
        verses = verses.map { v -> Verse(v.verse, mapOf(BibleTranslation(translationSlug) to Verse.Data(v.text, v.comment))) }
      )
    }
    Book(id = bi.bookId, chapters = chapters) to BookInfo(name = bi.name, id = bi.bookId, variant = BibleTranslation(translationSlug))
  }
  return Bible(books.map { it.first }.sortedBy { it.id }, books.map { it.second }.associateBy { it.variant })
}

val json = Json {
  prettyPrint = true
}


suspend fun main(args: Array<String>) {
  val prop = Properties().apply { File(args.getOrNull(0) ?: "config/obu.properties").inputStream().use { load(it) } }

  val bollsClient = BollsApiClient(BollsApiClientConfig(connectTimeout = 30.seconds, requestTimeout = 30.seconds, socketTimeout = 30.seconds))
  val transliteratorClient = ALittleHebrewTranslatorClient(sessionId = prop.getProperty("alittlehebrew.translator.client.sessionId"))

  val mainTranslation = prop.getProperty("bible.translations.main").trim()
  val hebrewTranslation = prop.getProperty("bible.translations.hebrew").trim()
  val transliterateHebrew = prop.getProperty("bible.translations.hebrew.transliterate").trim().toBoolean()

  val mainBible = bollsClient.loadBible(mainTranslation)
  val hebrewBible = bollsClient.loadBible(hebrewTranslation)

  val result = mainBible.merge(hebrewBible)

  // to json
  File("${prop.getProperty("output-path")}/bible-data.json").outputStream().use { out ->
    out.bufferedWriter(Charsets.UTF_8).write(json.encodeToString(Bible.serializer(), result))
  }

}