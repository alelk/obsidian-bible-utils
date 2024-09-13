package io.github.alelk.obsidian_bible_utils.model

import io.github.alelk.obsidian_bible_utils.model.serialization.BibleVariantSerializer
import kotlinx.serialization.Serializable
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

@Serializable
data class Bible(val books: List<Book>, val booksInfoByBibleVariant: Map<BibleVariant, List<BookInfo>> = emptyMap())

fun Bible.merge(other: Bible, strict: Boolean = true): Bible {
  fun List<Book>.merge(other: List<Book>): List<Book> {
    require(this.map { it.id }.let { it == it.distinct() }) { "Book ids are not unique: ${this.map { it.id }.joinToString(",")}" }
    require(other.map { it.id }.let { it == it.distinct() }) { "Book ids are not unique: ${other.map { it.id }.joinToString(",")}" }
    return (this + other).groupBy { it.id }.map { (_, books) ->
      books.reduce { acc, book -> acc.merge(book, strict) }
    }.sortedBy { it.id }
  }

  val nextBookInfo = (this.booksInfoByBibleVariant.keys + other.booksInfoByBibleVariant.keys).associateWith { variant ->
    ((this.booksInfoByBibleVariant[variant] ?: emptyList()) + (other.booksInfoByBibleVariant[variant] ?: emptyList()))
      .distinct()
      .sortedBy { it.id }
  }
  return kotlin.runCatching {
    Bible(this.books.merge(other.books), nextBookInfo)
  }.onFailure { e -> throw UnsupportedOperationException("Error merging books: ${e.message}", e) }.getOrThrow()
}

@Serializable
data class BookInfo(val id: Int, val name: String, val variant: BibleVariant)

@Serializable
data class Book(val id: Int, val chapters: List<Chapter>, val attrs: Map<String, String> = emptyMap()) {
  fun withAttr(attr: Pair<String, String>): Book = this.copy(attrs = this.attrs + attr)
}

fun Book.merge(other: Book, strict: Boolean = true): Book {
  require(this.id == other.id) { "Book ids are not equal: ${this.id} != ${other.id}" }
  fun List<Chapter>.merge(other: List<Chapter>): List<Chapter> {
    require(this.map { it.number }.let { it == it.distinct() }) { "Chapter numbers are not unique: ${this.map { it.number }.joinToString(",")}" }
    require(other.map { it.number }.let { it == it.distinct() }) { "Chapter numbers are not unique: ${other.map { it.number }.joinToString(",")}" }
    return (this + other).groupBy { it.number }.map { (_, chapters) ->
      chapters.reduce { acc, chapter -> acc.merge(chapter, strict) }
    }.sortedBy { it.number }
  }
  return kotlin.runCatching {
    require(this.chapters.size == other.chapters.size) { "Chapters count are not equal: ${this.chapters.size} != ${other.chapters.size}" }
    require(this.chapters.map { it.number }.toSet() == other.chapters.map { it.number }.toSet()) {
      "Chapter numbers numbers are not equal: ${this.chapters.map { it.number }.joinToString(",")}"
    }
    Book(this.id, this.chapters.merge(other.chapters))
  }.recover { e ->
    if (strict) throw UnsupportedOperationException("Error merging books ${this.id}: ${e.message}", e)
    else {
      val msg = "Error merging books ${this.id}: ${e.message}. Skip merging."
      log.warn { msg }
      this.withAttr("error" to msg)
    }
  }.getOrThrow()
}

@Serializable
data class Chapter(val number: Int, val verses: List<Verse>, val attrs: Map<String, String> = emptyMap()) {
  fun withAttr(attr: Pair<String, String>): Chapter = this.copy(attrs = this.attrs + attr)
}

fun Chapter.merge(other: Chapter, strict: Boolean = true): Chapter {
  require(this.number == other.number) { "Chapter numbers are not equal: ${this.number} != ${other.number}" }
  fun List<Verse>.merge(other: List<Verse>): List<Verse> {
    require(this.map { it.number }.let { it == it.distinct() }) { "Verse numbers are not unique: ${this.map { it.number }.joinToString(",")}" }
    require(other.map { it.number }.let { it == it.distinct() }) { "Verse numbers are not unique: ${other.map { it.number }.joinToString(",")}" }
    val thisVariants = this.flatMap { it.text.keys }.toSet()
    val otherVariants = other.flatMap { it.text.keys }.toSet()
    if (strict)
      require(thisVariants.none { it in otherVariants }) { "Verse variants are not unique: ${thisVariants.intersect(otherVariants).joinToString(",")}" }
    return (this + other).groupBy { it.number }.map { (_, verses) ->
      verses.reduce(Verse::plus)
    }.sortedBy { it.number }
  }
  return kotlin.runCatching {
    require(this.verses.size == other.verses.size) { "Verses count are not equal: ${this.verses.size} != ${other.verses.size}" }
    require(this.verses.map { it.number }.toSet() == other.verses.map { it.number }.toSet()) { "Verse numbers are not equal" }
    Chapter(this.number, this.verses.merge(other.verses))
  }.recover { e ->
    if (strict) throw UnsupportedOperationException("Error merging chapters ${this.number}: ${e.message}", e)
    else {
      val msg = "Error merging chapters ${this.number}: ${e.message}. Please recheck verses mapping!!!"
      log.warn { msg }
      Chapter(this.number, this.verses.merge(other.verses)).withAttr("error" to msg)
    }
  }.getOrThrow()
}

@Serializable(with = BibleVariantSerializer::class)
sealed interface BibleVariant {
  data class Translation(val translationSlug: String) : BibleVariant {
    override fun toString(): String = "translation-${this.translationSlug}"
  }

  data class Transliteration(val signature: String) : BibleVariant {
    override fun toString(): String = "transliteration-${this.signature}"
  }

  companion object {
    fun parse(str: String): BibleVariant = when {
      str.startsWith("translation-") -> Translation(str.removePrefix("translation-"))
      str.startsWith("transliteration-") -> Transliteration(str.removePrefix("transliteration-"))
      else -> throw IllegalArgumentException("Unknown BibleVariant: $str")
    }
  }
}

@Serializable
data class Verse(val number: Int, val text: Map<BibleVariant, Data>, val attrs: Map<String, String> = emptyMap()) {

  @Serializable
  data class Data(val text: String, val comment: String? = null)

  operator fun plus(other: Verse): Verse {
    require(this.number == other.number) { "Verse numbers are not equal: ${this.number} != ${other.number}" }
    return Verse(this.number, this.text + other.text)
  }

  fun withAttr(attr: Pair<String, String>): Verse = this.copy(attrs = this.attrs + attr)
}