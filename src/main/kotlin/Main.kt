package io.github.alelk.obsidian_bible_utils

import io.github.alelk.bolls_api_client.BollsApiClient
import io.github.alelk.bolls_api_client.BollsApiClientConfig
import io.github.alelk.obsidian_bible_utils.model.*
import io.github.alelk.obsidian_bible_utils.transliterator.ALittleHebrewTranslatorClient
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retry
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.File
import java.util.Properties
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTimedValue

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

const val parallelism = 10

suspend fun BollsApiClient.downloadBible(translationSlug: String): Bible = coroutineScope {
  val books = retryOnException { getBooksInfoByTranslation(translationSlug) }.map { bi ->
    val chapters = (1..bi.countChapters).chunked(parallelism).flatMap { chapterNumbers ->
      chapterNumbers.map { chapter ->
        async {
          val verses = retryOnException {
            getChapterText(translationSlug, bi.bookId, chapter, withTranslatorComments = true)
          }
          Chapter(
            number = chapter,
            verses = verses.map { v -> Verse(v.verse, mapOf(BibleTranslation(translationSlug) to Verse.Data(v.text, v.comment))) }
          )
        }
      }.awaitAll()
    }
    Book(id = bi.bookId, chapters = chapters) to BookInfo(name = bi.name, id = bi.bookId, variant = BibleTranslation(translationSlug))
  }
  Bible(
    books = books.map { it.first }.sortedBy { it.id },
    booksInfoByBibleVariant = books.map { it.second }.groupBy { it.variant }.mapValues { e -> e.value.sortedBy { it.id } }
  )
}

suspend fun ALittleHebrewTranslatorClient.addHebrewTransliteration(chapter: Chapter, hebrewTranslationSlug: String): Chapter {
  val nextVerses = chapter.verses.map { verse ->
    val hebrewText = verse.text[BibleTranslation(hebrewTranslationSlug)]?.text
    val hebrewTransliteration = verse.text[BibleTransliteration(hebrewTranslationSlug)]?.text
    if (hebrewText != null && hebrewTransliteration == null) {
      val transliterated = retryOnException { this.getTransliteration(hebrewText) }
      verse + Verse(verse.number, mapOf(BibleTransliteration(hebrewTranslationSlug) to Verse.Data(transliterated)))
    } else verse
  }
  return chapter.copy(verses = nextVerses)
}

suspend fun ALittleHebrewTranslatorClient.addHebrewTransliteration(book: Book, hebrewTranslationSlug: String): Book {
  val nextChapters = book.chapters.map { chapter -> this.addHebrewTransliteration(chapter, hebrewTranslationSlug) }
  return book.copy(chapters = nextChapters)
}

@OptIn(ExperimentalSerializationApi::class)
fun loadLibrary(file: File): Bible? =
  file.takeIf { it.exists() }?.inputStream()?.use { Json.decodeFromStream(Bible.serializer(), it) }

fun Bible.saveLibrary(file: File) = file.outputStream().use { out ->
  out.bufferedWriter(Charsets.UTF_8).use { bw ->
    bw.write(json.encodeToString(Bible.serializer(), this))
    bw.flush()
  }
  out.flush()
}

suspend fun loadOrCreateLibrary(file: File, bollsClient: BollsApiClient, translationSlug: String): Bible {
  val library = loadLibrary(file)
  if (library != null && library.booksInfoByBibleVariant[BibleTranslation(translationSlug)] == null)
    throw IllegalStateException("Library doesn't contain translation '$translationSlug'")
  return library
    ?.also {
      println("Library $translationSlug loaded from ${file.absolutePath}. Variants: ${it.booksInfoByBibleVariant.keys.joinToString(", ")}")
    }
    ?: measureTimedValue {
      bollsClient
        .also { println("Bible ${file.absolutePath} not found. Download it from Bolls Api.") }
        .downloadBible(translationSlug)
    }.also {
      println("Library $translationSlug downloaded from Bolls API (time: ${it.duration}). Save to ${file.absolutePath}...")
      it.value.saveLibrary(file)
      println("Library $translationSlug saved to ${file.absolutePath}")
    }.value
}

@OptIn(ExperimentalSerializationApi::class)
val json = Json {
  prettyPrint = true
  prettyPrintIndent = "  "
}


suspend fun main(args: Array<String>) {
  val prop = Properties().apply { File(args.getOrNull(0) ?: "config/obu.properties").inputStream().use { load(it) } }

  val bollsClient = BollsApiClient(BollsApiClientConfig(connectTimeout = 30.seconds, requestTimeout = 30.seconds, socketTimeout = 30.seconds))
  val transliteratorClient = ALittleHebrewTranslatorClient(sessionId = prop.getProperty("alittlehebrew.translator.client.sessionId"))

  val mainTranslation = prop.getProperty("bible.translations.main").trim()
  val hebrewTranslation = prop.getProperty("bible.translations.hebrew").trim()

  val targetLibraryFile = File("${prop.getProperty("output-path")}/bible-library.json")
  val mainTranslationLibrary = File("${prop.getProperty("output-path")}/bible-library-main.json")
  val hebrewTranslationLibrary = File("${prop.getProperty("output-path")}/bible-library-hebrew.json")

  val library = loadLibrary(targetLibraryFile)
    ?.also { println("Library loaded from ${targetLibraryFile.absolutePath}. Variants: ${it.booksInfoByBibleVariant.keys.joinToString(", ")}") }
    ?: loadOrCreateLibrary(mainTranslationLibrary, bollsClient, mainTranslation)
      .merge(loadOrCreateLibrary(hebrewTranslationLibrary, bollsClient, hebrewTranslation))

  val transliterateHebrew = prop.getProperty("bible.translations.hebrew.transliterate").trim().toBoolean()

  if (transliterateHebrew) {
    println("Check hebrew transliteration...")
    val booksInfo = library.booksInfoByBibleVariant[BibleTranslation(hebrewTranslation)]
    if (booksInfo == null)
      System.err.println("Hebrew translation $hebrewTranslation not found in the library.")
    else if (library.booksInfoByBibleVariant[BibleTransliteration(hebrewTranslation)] != null)
      println("Hebrew transliteration already exists in the library.")
    else {
      val transliteratedLibrary = library.books.fold(library) { acc, book ->
        val bi = booksInfo.find { it.id == book.id } ?: throw IllegalStateException("No book info found for book ${book.id} in translation $hebrewTranslation")
        val nextBook = measureTimedValue { transliteratorClient.addHebrewTransliteration(book, hebrewTranslation) }
        println("Hebrew transliteration added to book ${book.id} (${bi.variant}, ${bi.name}). Time: ${nextBook.duration}")
        acc.copy(
          books = acc.books.map { if (it.id == book.id) nextBook.value else it }.sortedBy { it.id },
          booksInfoByBibleVariant =
          acc.booksInfoByBibleVariant + Pair(
            BibleTranslation(hebrewTranslation),
            (acc.booksInfoByBibleVariant[BibleTranslation(hebrewTranslation)] ?: emptyList()) + bi.copy(variant = BibleTransliteration(hebrewTranslation))
          ).also {
            val bkpFile = File("${prop.getProperty("output-path")}/bible-library-transliteration-backup.json")
            println("Save library backup to ${bkpFile}...")
            acc.saveLibrary(bkpFile)
          }
        )
      }
      println("Hebrew transliteration added. Save to $targetLibraryFile...")
      transliteratedLibrary.saveLibrary(targetLibraryFile)
      println("Transliterated library saved to ${targetLibraryFile.absolutePath}")
    }
  }
}