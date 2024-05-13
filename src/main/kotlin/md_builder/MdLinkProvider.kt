package io.github.alelk.obsidian_bible_utils.md_builder

import io.github.alelk.obsidian_bible_utils.model.DictDefinition

data class MdLinkProvider(
  val dictReference: (refTopic: DictDefinition.Topic) -> MdReference? = { null },
  val bibleReference: (book: Int, chapter: Int, verse: Int, text: String?) -> MdReference? = { _, _, _, _ -> null }
)