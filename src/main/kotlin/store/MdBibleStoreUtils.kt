package io.github.alelk.obsidian_bible_utils.store

import io.github.alelk.obsidian_bible_utils.md_builder.MdBook
import java.io.File

fun MdBook.save(dir: File) {
  val bookDir = File(dir, name)
  bookDir.mkdirs()
  chapters.forEach { chapter ->
    val chapterFile = File(bookDir, "${chapter.name}.md")
    chapterFile.writeText(chapter.text)
  }
}