package io.github.alelk.obsidian_bible_utils.store

import io.github.alelk.obsidian_bible_utils.model.DictDefinition
import io.github.alelk.obsidian_bible_utils.model.Dictionary
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.serializer
import java.io.File

@OptIn(ExperimentalSerializationApi::class)
fun loadDictionary(file: File, dictSignature: String): Dictionary? =
  file.takeIf { it.exists() }?.inputStream()?.use { Json.decodeFromStream(serializer<List<DictDefinition>>(), it) }?.let { Dictionary(dictSignature, it) }

fun Dictionary.save(file: File) =
  file.outputStream().use { Json.encodeToStream(serializer<List<DictDefinition>>(), definitions, it) }
