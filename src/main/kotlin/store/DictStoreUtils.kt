package io.github.alelk.obsidian_bible_utils.store

import io.github.alelk.obsidian_bible_utils.client.ALittleHebrewTranslatorClient
import io.github.alelk.obsidian_bible_utils.md_builder.MdDictionary
import io.github.alelk.obsidian_bible_utils.model.DictDefinition
import io.github.alelk.obsidian_bible_utils.model.Dictionary
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.serializer
import mu.KotlinLogging
import java.io.File
import kotlin.time.measureTimedValue

private val log = KotlinLogging.logger { }

@OptIn(ExperimentalSerializationApi::class)
fun loadDictionary(file: File, dictSignature: String): Dictionary? =
  file.takeIf { it.exists() }?.inputStream()?.use { Json.decodeFromStream(serializer<List<DictDefinition>>(), it) }?.let { Dictionary(dictSignature, it) }

@OptIn(ExperimentalSerializationApi::class)
fun Dictionary.save(file: File) =
  file.outputStream().use { Json.encodeToStream(serializer<List<DictDefinition>>(), definitions, it) }

@OptIn(ExperimentalSerializationApi::class)
fun loadTransliteratedDictionary(file: File): MdDictionary? =
  file.takeIf { it.exists() }?.inputStream()?.use { Json.decodeFromStream(MdDictionary.serializer(), it) }

@OptIn(ExperimentalSerializationApi::class)
fun MdDictionary.save(file: File) =
  file.outputStream().use { Json.encodeToStream(MdDictionary.serializer(), this, it) }

const val fetchDefinitionsChunkSize = 100

suspend fun loadOrCreateHebrewTransliteratedDictionary(
  file: File, dictionaryLoader: () -> MdDictionary, transliterator: ALittleHebrewTranslatorClient
): MdDictionary {
  val existing = loadTransliteratedDictionary(file)
  if (existing != null) return existing
  val dictionary = dictionaryLoader()
  log.info { "Perform dictionary transliteration..." }
  val countDefinitions = dictionary.definitions.size

  val nextDefinitions = measureTimedValue {
    dictionary.definitions.withIndex().chunked(fetchDefinitionsChunkSize).flatMap { chunk ->
      val result = measureTimedValue {
        transliterator
          .getTransliteration(chunk.joinToString("\n") { (_, definition) -> definition.lexeme.trim() })
          .split('\n')
          .map { it.trim() }
      }
      require(result.value.size == chunk.size) { "Transliteration result count mismatch" }
      val transliterated =
        chunk
          .map { it.value }
          .zip(result.value)
          .map { (def, transliteration) ->
            def.copy(transliterations = if (def.lexeme.trim() == transliteration) def.transliterations else listOf(transliteration) + def.transliterations)
          }
      log.info("[${chunk.maxOfOrNull { it.index + 1 }} of $countDefinitions] - ${result.duration} - Transliterated: ${result.value.joinToString("; ")}")
      transliterated
    }
  }

  return dictionary.copy(definitions = nextDefinitions.value)
    .also { it.save(file) }
    .also { log.info { "Dictionary transliterated (time: ${nextDefinitions.duration}). Save to ${file.absolutePath}" } }
}