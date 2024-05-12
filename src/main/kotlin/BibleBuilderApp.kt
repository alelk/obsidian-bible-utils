package io.github.alelk.obsidian_bible_utils

import io.github.alelk.bolls_api_client.BollsApiClient
import io.github.alelk.bolls_api_client.BollsApiClientConfig
import io.github.alelk.obsidian_bible_utils.client.ALittleHebrewTranslatorClient
import io.github.alelk.obsidian_bible_utils.md_builder.DictReference
import io.github.alelk.obsidian_bible_utils.md_builder.toMd
import io.github.alelk.obsidian_bible_utils.model.*
import io.github.alelk.obsidian_bible_utils.service.BibleTransliterator
import io.github.alelk.obsidian_bible_utils.store.*
import mu.KotlinLogging
import java.io.File
import java.util.*
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTimedValue

private val log = KotlinLogging.logger { }

suspend fun main(args: Array<String>) {
  val prop = Properties().apply { File(args.getOrNull(0) ?: "config/obu-bible-builder.properties").inputStream().use { load(it) } }

  val bollsClient = BollsApiClient(BollsApiClientConfig(connectTimeout = 30.seconds, requestTimeout = 30.seconds, socketTimeout = 30.seconds))
  val transliterator = BibleTransliterator(ALittleHebrewTranslatorClient(sessionId = prop.getProperty("alittlehebrew.translator.client.sessionId")))

  val mainTranslation = BibleVariant.Translation(prop.getProperty("bible.translations.main").trim())
  val hebrewTranslation = BibleVariant.Translation(prop.getProperty("bible.translations.hebrew").trim())
  val hebrewTransliteration = BibleVariant.Transliteration(hebrewTranslation.translationSlug)

  val targetLibraryFile = File("${prop.getProperty("output-path")}/bible-library.json")
  val mainLibraryFile = File("${prop.getProperty("output-path")}/bible-library-$mainTranslation.json")
  val hebrewLibraryFile = File("${prop.getProperty("output-path")}/bible-library-$hebrewTranslation.json")
  val hebrewTransliteratedLibraryFile = File("${prop.getProperty("output-path")}/bible-library-$hebrewTransliteration.json")

  val transliterateHebrew = prop.getProperty("bible.translations.hebrew.transliterate").trim().toBoolean()

  val lib = loadLibrary(targetLibraryFile)
    ?.also { log.info { "Library already exists in ${targetLibraryFile.absolutePath}. Variants: ${it.booksInfoByBibleVariant.keys.joinToString(", ")}" } }
    ?: run {
      val targetLib = measureTimedValue {
        val mainLib = loadOrCreateLibrary(mainLibraryFile, bollsClient, mainTranslation)
        val hebrewLib = loadOrCreateLibrary(hebrewLibraryFile, bollsClient, hebrewTranslation)

        val secondLib =
          if (transliterateHebrew) {
            val hebrewTransliteratedLib =
              loadOrCreateHebrewTransliteratedLibrary(hebrewTransliteratedLibraryFile, { hebrewLib }, hebrewTranslation, transliterator)
            hebrewTransliteratedLib
          } else {
            hebrewLib
          }

        mainLib.merge(secondLib, strict = false)
      }
      log.info { "Library built (time: ${targetLib.duration}). Save to ${targetLibraryFile.absolutePath}..." }
      targetLib.value.saveLibrary(targetLibraryFile)
      log.info { "Library saved to ${targetLibraryFile.absolutePath}" }
      targetLib.value
    }

  log.info { "Library loaded from ${targetLibraryFile.absolutePath}. Variants: ${lib.booksInfoByBibleVariant.keys.joinToString(", ")}" }
  val mdBibleDir = File("${prop.getProperty("output-path")}/md-bible/")
  log.info { "Build markdown bible library in ${mdBibleDir.absolutePath}..." }
  mdBibleDir.mkdir()
  val dictReference = { refTopic: DictDefinition.Topic ->
    DictReference("../Dict/${refTopic.type.signature}/${refTopic.number}", "Text ${refTopic.number}")
  }
  lib.toMd(mainTranslation, dictReference).forEach { book ->
    log.info { "Build markdown for book ${book.name}..." }
    book.save(mdBibleDir)
    log.info { "Book ${book.name} saved to ${mdBibleDir.absolutePath}" }
  }
}