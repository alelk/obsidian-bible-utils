package io.github.alelk.obsidian_bible_utils

import io.github.alelk.obsidian_bible_utils.client.ALittleHebrewTranslatorClient
import io.github.alelk.obsidian_bible_utils.md_builder.MdLinkProvider
import io.github.alelk.obsidian_bible_utils.md_builder.MdReference
import io.github.alelk.obsidian_bible_utils.md_builder.toMdDictionary
import io.github.alelk.obsidian_bible_utils.store.MdDictStore
import io.github.alelk.obsidian_bible_utils.store.fileName
import io.github.alelk.obsidian_bible_utils.store.loadDictionary
import io.github.alelk.obsidian_bible_utils.store.loadOrCreateHebrewTransliteratedDictionary
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import mu.KotlinLogging
import java.io.File
import java.util.*

private val log = KotlinLogging.logger { }

suspend fun main(args: Array<String>) {
  val prop = Properties().apply { File(args.getOrNull(0) ?: "config/obu-dictionary-builder.properties").inputStream().use { load(it) } }

  val strongDictFile = File(prop.getProperty("dictionary.strong-dict.file"))
  val strongDictTransliteratedFile = File(prop.getProperty("dictionary.strong-dict.transliterated.file"))

  val dict =
    loadDictionary(strongDictFile, prop.getProperty("dictionary.strong-dict.signature"))
      ?: throw IllegalStateException("Dictionary not found: ${strongDictFile.absolutePath}")

  val transliterator = ALittleHebrewTranslatorClient(sessionId = prop.getProperty("alittlehebrew.translator.client.sessionId"))

  val transliteratedDict =
    loadOrCreateHebrewTransliteratedDictionary(
      strongDictTransliteratedFile,
      { dict.toMdDictionary(prop.getProperty("dictionary.strong-dict.signature")) },
      transliterator
    )

  val bookNameById: Map<Int, String> = File(prop.getProperty("bible.bookNameById.file")).inputStream().let { Json.decodeFromStream(it) }

  val mdLinkProvider = MdLinkProvider(
    dictReference = { refTopic ->
      val ref = transliteratedDict.definitions.find { it.topic == refTopic }?.let { MdReference(link = it.fileName, text = it.transliterations[0]) }
      if (ref == null) log.warn { "Dictionary reference not found: $refTopic" }
      ref
    },
    bibleReference = { book, chapter, verse, text ->
      val ref = bookNameById[book]?.let { MdReference(link = "$it $chapter#$verse") }
      if (ref == null) log.warn { "Bible reference not found: $book $chapter:$verse ($text)" }
      ref
    }
  )

  val dictionariesTargetDir = File(prop.getProperty("dictionary.target-dir"))
  val dictStore = MdDictStore(dictionariesTargetDir, mdLinkProvider)
  dictStore.storeDictionary(transliteratedDict)

}