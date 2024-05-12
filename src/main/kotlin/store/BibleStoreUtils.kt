package io.github.alelk.obsidian_bible_utils.store

import io.github.alelk.bolls_api_client.BollsApiClient
import io.github.alelk.obsidian_bible_utils.model.Bible
import io.github.alelk.obsidian_bible_utils.model.BibleVariant
import io.github.alelk.obsidian_bible_utils.service.BibleDownloader
import io.github.alelk.obsidian_bible_utils.service.BibleTransliterator
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import mu.KotlinLogging
import java.io.File
import kotlin.time.measureTimedValue

private val log = KotlinLogging.logger { }

@OptIn(ExperimentalSerializationApi::class)
private val json = Json {
  prettyPrint = true
  prettyPrintIndent = "  "
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

suspend fun loadOrCreateLibrary(file: File, bollsClient: BollsApiClient, translation: BibleVariant.Translation): Bible {
  val library = loadLibrary(file)
  if (library != null && library.booksInfoByBibleVariant[translation] == null)
    throw IllegalStateException("Library doesn't contain translation '${translation.translationSlug}'")
  return library
    ?.also {
      log.info { "Library ${translation.translationSlug} loaded from ${file.absolutePath}. Variants: ${it.booksInfoByBibleVariant.keys.joinToString(", ")}" }
    }
    ?: measureTimedValue {
      BibleDownloader(bollsClient)
        .also { log.info { "Bible ${file.absolutePath} not found. Download it from Bolls Api." } }
        .downloadBible(translation.translationSlug)
    }.also {
      log.info { "Library ${translation.translationSlug} downloaded from Bolls API (time: ${it.duration}). Save to ${file.absolutePath}..." }
      it.value.saveLibrary(file)
      log.info { "Library $translation saved to ${file.absolutePath}" }
    }.value
}

suspend fun loadOrCreateHebrewTransliteratedLibrary(
  hebrewTransliteratedLibraryFile: File,
  hebrewLibraryLoader: () -> Bible,
  hebrewTranslation: BibleVariant.Translation,
  transliterator: BibleTransliterator
): Bible {
  val hebrewTransliteratedLib = loadLibrary(hebrewTransliteratedLibraryFile)
  if (hebrewTransliteratedLib != null) return hebrewTransliteratedLib
  val hebrewLib = hebrewLibraryLoader()
  log.info { "Perform hebrew transliteration..." }
  val booksInfo =
    hebrewLib.booksInfoByBibleVariant[hebrewTranslation]
      ?: throw IllegalStateException(
        "Hebrew translation ${hebrewTranslation.translationSlug} not found in the library: ${
          hebrewLib.booksInfoByBibleVariant.keys.joinToString(
            ", "
          )
        }"
      )
  val transliteratedLibrary = hebrewLib.books.sortedBy { it.id }.fold(hebrewLib) { lib, book ->
    val bi = booksInfo.find { it.id == book.id } ?: throw IllegalStateException("No book info found for book ${book.id} in translation $hebrewTranslation")
    val nextBook = measureTimedValue { transliterator.addHebrewTransliteration(book, hebrewTranslation) }
    log.info { "Hebrew transliteration added to book ${book.id} (${bi.variant}, ${bi.name}). Time: ${nextBook.duration}" }
    lib.copy(
      books = lib.books.map { if (it.id == book.id) nextBook.value else it }.sortedBy { it.id },
      booksInfoByBibleVariant = lib.booksInfoByBibleVariant +
        Pair(
          hebrewTranslation,
          (lib.booksInfoByBibleVariant[hebrewTranslation] ?: emptyList()) + bi.copy(variant = BibleVariant.Transliteration(hebrewTranslation.translationSlug))
        )
    ).also { nextLib ->
      val bkpFile = File(hebrewTransliteratedLibraryFile.absolutePath + "-bkp-${book.id}.bkp")
      log.info { "Save library backup to $bkpFile..." }
      nextLib.saveLibrary(bkpFile)
    }
  }
  log.info { "Hebrew transliteration added. Save to ${hebrewTransliteratedLibraryFile.absolutePath}..." }
  transliteratedLibrary.saveLibrary(hebrewTransliteratedLibraryFile)
  log.info { "Transliterated library saved to ${hebrewTransliteratedLibraryFile.absolutePath}" }
  return transliteratedLibrary
}