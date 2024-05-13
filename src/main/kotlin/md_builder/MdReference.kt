package io.github.alelk.obsidian_bible_utils.md_builder

data class MdReference(val link: String, val text: String? = null)

fun MdReference.toMdLink(text: String? = null, wiki: Boolean = false): String {
  val linkText = text ?: this.text
  return if (wiki) "[[${link}|${linkText ?: link}]]" else "[${linkText ?: link}]($link)"
}