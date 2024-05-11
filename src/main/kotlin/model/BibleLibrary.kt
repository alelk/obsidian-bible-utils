package io.github.alelk.obsidian_bible_utils.model

import io.github.alelk.obsidian_bible_utils.model.serialization.BibleVariantSerializer
import kotlinx.serialization.Serializable

@Serializable
data class Bible(val books: List<Book>, val bookInfoByBibleVariant: Map<BibleVariant, BookInfo>)

fun Bible.merge(other: Bible): Bible {
  fun List<Book>.merge(other: List<Book>): List<Book> {
    require(this.map { it.id }.let { it == it.distinct() }) { "Book ids are not unique: ${this.map { it.id }.joinToString(",")}" }
    require(other.map { it.id }.let { it == it.distinct() }) { "Book ids are not unique: ${other.map { it.id }.joinToString(",")}" }
    return (this + other).groupBy { it.id }.map { (_, books) ->
      books.reduce(Book::merge)
    }.sortedBy { it.id }
  }
  return kotlin.runCatching {
    Bible(books.merge(other.books), bookInfoByBibleVariant + other.bookInfoByBibleVariant)
  }.onFailure { e -> throw UnsupportedOperationException("Error merging books: ${e.message}", e) }.getOrThrow()
}

@Serializable
data class BookInfo(val id: Int, val name: String, val variant: BibleVariant)

@Serializable
data class Book(val id: Int, val chapters: List<Chapter>)

fun Book.merge(other: Book): Book {
  require(id == other.id) { "Book ids are not equal: $id != ${other.id}" }
  require(chapters.size == other.chapters.size) { "Book $id chapters count are not equal: ${chapters.size} != ${other.chapters.size}" }
  require(chapters.map { it.number }.toSet() == other.chapters.map { it.number }.toSet()) {
    "Book $id chapter numbers numbers are not equal: ${chapters.map { it.number }.joinToString(",")}"
  }
  fun List<Chapter>.merge(other: List<Chapter>): List<Chapter> {
    require(this.map { it.number }.let { it == it.distinct() }) { "Chapter numbers are not unique: ${this.map { it.number }.joinToString(",")}" }
    require(other.map { it.number }.let { it == it.distinct() }) { "Chapter numbers are not unique: ${other.map { it.number }.joinToString(",")}" }
    return (this + other).groupBy { it.number }.map { (_, chapters) ->
      chapters.reduce(Chapter::merge)
    }.sortedBy { it.number }
  }
  return kotlin.runCatching {
    Book(id, chapters.merge(other.chapters))
  }.onFailure { e -> throw UnsupportedOperationException("Error merging books $id: ${e.message}", e) }.getOrThrow()
}

@Serializable
data class Chapter(val number: Int, val verses: List<Verse>)

fun Chapter.merge(other: Chapter): Chapter {
  require(number == other.number) { "Chapter numbers are not equal: $number != ${other.number}" }
  require(verses.size == other.verses.size) { "Chapter $number verses count are not equal: ${verses.size} != ${other.verses.size}" }
  require(verses.map { it.number }.toSet() == other.verses.map { it.number }.toSet()) { "Chapter $number verse numbers are not equal" }
  fun List<Verse>.merge(other: List<Verse>): List<Verse> {
    require(this.map { it.number }.let { it == it.distinct() }) { "Verse numbers are not unique: ${this.map { it.number }.joinToString(",")}" }
    require(other.map { it.number }.let { it == it.distinct() }) { "Verse numbers are not unique: ${other.map { it.number }.joinToString(",")}" }
    val thisVariants = this.flatMap { it.text.keys }.toSet()
    val otherVariants = other.flatMap { it.text.keys }.toSet()
    require(thisVariants.none { it in otherVariants }) { "Verse variants are not unique: ${thisVariants.intersect(otherVariants).joinToString(",")}" }
    return (this + other).groupBy { it.number }.map { (_, verses) ->
      verses.reduce(Verse::plus)
    }.sortedBy { it.number }
  }
  return kotlin.runCatching {
    Chapter(number, verses.merge(other.verses))
  }.onFailure { e -> throw UnsupportedOperationException("Error merging chapters $number: ${e.message}", e) }.getOrThrow()
}

@Serializable(with = BibleVariantSerializer::class)
sealed interface BibleVariant
data class BibleTranslation(val translationSlug: String) : BibleVariant
data class BibleTransliteration(val signature: String) : BibleVariant

@Serializable
data class Verse(val number: Int, val text: Map<BibleVariant, Data>) {

  @Serializable
  data class Data(val text: String, val comment: String? = null)

  operator fun plus(other: Verse): Verse {
    require(number == other.number) { "Verse numbers are not equal: $number != ${other.number}" }
    return Verse(number, text + other.text)
  }
}