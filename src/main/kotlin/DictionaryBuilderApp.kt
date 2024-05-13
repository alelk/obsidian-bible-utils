package io.github.alelk.obsidian_bible_utils

import io.github.alelk.obsidian_bible_utils.client.ALittleHebrewTranslatorClient
import io.github.alelk.obsidian_bible_utils.md_builder.DictReference
import io.github.alelk.obsidian_bible_utils.md_builder.toMdDictionary
import io.github.alelk.obsidian_bible_utils.model.DictDefinition
import io.github.alelk.obsidian_bible_utils.store.MdDictStore
import io.github.alelk.obsidian_bible_utils.store.loadDictionary
import io.github.alelk.obsidian_bible_utils.store.loadOrCreateHebrewTransliteratedDictionary
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

  val findDefinition = { refTopic: DictDefinition.Topic -> transliteratedDict.definitions.find { it.topic == refTopic } }

  val dictionariesTargetDir = File(prop.getProperty("dictionary.target-dir"))
  val dictStore = MdDictStore(dictionariesTargetDir, findDefinition)
  dictStore.storeDictionary(transliteratedDict)

}