package io.github.alelk.obsidian_bible_utils.md_builder

data class DictReference(val link: String, val text: String? = null)

fun DictReference.toMdLink(text: String? = null): String {
  val linkText = text ?: this.text
  return if (linkText == null) link else "[$linkText]($link)"
}