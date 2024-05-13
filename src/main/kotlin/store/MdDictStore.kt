package io.github.alelk.obsidian_bible_utils.store

import io.github.alelk.obsidian_bible_utils.md_builder.MdReference
import io.github.alelk.obsidian_bible_utils.md_builder.MdDictDefinition
import io.github.alelk.obsidian_bible_utils.md_builder.MdDictionary
import io.github.alelk.obsidian_bible_utils.md_builder.MdLinkProvider
import io.github.alelk.obsidian_bible_utils.model.DictDefinition
import java.io.File
import java.util.concurrent.ConcurrentHashMap

val MdDictDefinition.fileName: String get() = "$key.md"

class MdDictStore(val dictionariesDir: File, val mdLinkProvider: MdLinkProvider) {
  init {
    if (!dictionariesDir.exists()) dictionariesDir.mkdirs()
    require(dictionariesDir.isDirectory) { "Dictionaries directory must be a directory: $dictionariesDir" }
  }

  private fun relativePath(topic: DictDefinition.Topic): String = "Strong/${topic.type.name.lowercase()}/"

  private val dictDirs = ConcurrentHashMap<DictDefinition.TopicType, File>()
  fun getDictDir(topic: DictDefinition.Topic): File =
    dictDirs.getOrPut(topic.type) {
      val dictDir = File(dictionariesDir, relativePath(topic))
      dictDir.mkdirs()
      dictDir
    }

  fun dictReference(definition: MdDictDefinition): MdReference =
    MdReference(definition.fileName, definition.transliterations[0])

  fun storeDictionary(dict: MdDictionary) {
    dict.definitions.forEach { definition ->
      val mdFile = File(getDictDir(definition.topic), definition.fileName)
      mdFile.writeText(definition.toMd(mdLinkProvider))
    }
  }
}