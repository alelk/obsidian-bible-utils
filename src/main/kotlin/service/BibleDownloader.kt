package io.github.alelk.obsidian_bible_utils.service

import io.github.alelk.bolls_api_client.BollsApiClient
import io.github.alelk.obsidian_bible_utils.model.*
import io.github.alelk.obsidian_bible_utils.utils.retryOnException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class BibleDownloader(private val bollsApiClient: BollsApiClient, private val parallelism: Int = 10) {
  suspend fun downloadBible(translationSlug: String): Bible = coroutineScope {
    val books = retryOnException { bollsApiClient.getBooksInfoByTranslation(translationSlug) }.map { bi ->
      val chapters = (1..bi.countChapters).chunked(parallelism).flatMap { chapterNumbers ->
        chapterNumbers.map { chapter ->
          async {
            val verses = retryOnException {
              bollsApiClient.getChapterText(translationSlug, bi.bookId, chapter, withTranslatorComments = true)
            }
            Chapter(
              number = chapter,
              verses = verses.map { v -> Verse(v.verse, mapOf(BibleVariant.Translation(translationSlug) to Verse.Data(v.text, v.comment))) }
            )
          }
        }.awaitAll()
      }
      Book(id = bi.bookId, chapters = chapters) to BookInfo(name = bi.name, id = bi.bookId, variant = BibleVariant.Translation(translationSlug))
    }
    Bible(
      books = books.map { it.first }.sortedBy { it.id },
      booksInfoByBibleVariant = books.map { it.second }.groupBy { it.variant }.mapValues { e -> e.value.sortedBy { it.id } }
    )
  }
}