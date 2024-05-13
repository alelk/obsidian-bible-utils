package io.github.alelk.obsidian_bible_utils.md_builder

import io.github.alelk.obsidian_bible_utils.model.DictDefinition
import io.github.alelk.obsidian_bible_utils.model.Dictionary
import kotlinx.serialization.Serializable

fun Dictionary.toMdDictionary(signature: String) =
  MdDictionary(
    name = signature,
    definitions = definitions.map { it.toMdDictDefinition() }
  )

fun DictDefinition.toMdDictDefinition() =
  MdDictDefinition(
    topic = topic,
    lexeme = lexeme,
    definition = definition,
    shortDefinition = shortDefinition,
    transliterations = listOf(transliteration),
    pronunciation = pronunciation
  )

@Serializable
data class MdDictionary(
  val name: String,
  val definitions: List<MdDictDefinition>
)

@Serializable
data class MdDictDefinition(
  val topic: DictDefinition.Topic,
  val lexeme: String,
  val definition: String,
  val shortDefinition: String,
  val transliterations: List<String>,
  val pronunciation: String
) {
  init {
    require(transliterations.isNotEmpty()) { "Transliterations must not be empty" }
  }

  val key: String = "$shortDefinition (${transliterations[0]})"

  fun toMd(mdLinkProvider: MdLinkProvider): String =
    buildString {
      val definition = definition.parseDictionaryDefinition(mdLinkProvider)

      val category = definition.find<DefinitionPart.Category>()?.text?.trim()
      val partOfSpeech = definition.find<DefinitionPart.PartOfSpeech>()?.text?.trim()

      appendLine("### $topic: $shortDefinition")
      appendLine()
      val lexeme = definition.find<DefinitionPart.HebrewText>()?.text ?: definition.find<DefinitionPart.GreekText>() ?: lexeme
      appendLine("> $lexeme")
      appendLine()

      if (partOfSpeech != null) {
        appendLine("Часть речи: *$partOfSpeech*")
        appendLine()
      }

      if (category != null) {
        appendLine("Категория: *${category}*")
        appendLine()
      }

      appendLine("###### Произношение")
      val pronunciation = listOfNotNull(definition.find<DefinitionPart.Pronunciation>()?.text, pronunciation).distinct()
      pronunciation.forEach { appendLine("- $it") }
      appendLine()

      appendLine("###### Транслитерация")
      val transliteration = listOfNotNull(definition.find<DefinitionPart.Transliteration>()?.text, *transliterations.toTypedArray()).distinct()
      transliteration.forEach { appendLine("- $it") }
      appendLine()

      definition.forEach { part ->
        when (part) {
          is DefinitionPart.OriginalText -> {
            appendLine("\n###### Оригинал")
            appendLine(part.text)
          }

          is DefinitionPart.Etymology -> {
            appendLine("\n###### Этимология")
            appendLine(part.text)
          }

          is DefinitionPart.Synonyms -> {
            appendLine("\n###### Синонимы")
            appendLine(part.text)
          }

          is DefinitionPart.Text -> appendLine(part.text)
          else -> {}
        }
      }

      appendLine()
      appendLine("---")
      appendLine("#dictionary #${topic.type.name.lowercase()} #dictionary_definition #generated #unchanged")
    }
}

sealed interface DefinitionPart {
  data class HebrewText(val text: String) : DefinitionPart
  data class GreekText(val text: String) : DefinitionPart
  data class OriginalText(val text: String) : DefinitionPart
  data class Pronunciation(val text: String) : DefinitionPart
  data class Transliteration(val text: String) : DefinitionPart
  data class PartOfSpeech(val text: String) : DefinitionPart
  data class Etymology(val text: String) : DefinitionPart
  data class Synonyms(val text: String) : DefinitionPart
  data class Category(val text: String) : DefinitionPart
  data class Text(val text: String) : DefinitionPart
}

inline fun <reified D : DefinitionPart> List<DefinitionPart>.find(): D? = find { it is D } as? D

private val dictRefRegex = Regex("""<a\s+href\s*=\s*'S:(?<reference>[HG]\d+)'\s*>(?<text>[\w\s\p{L}\p{InCombiningDiacriticalMarks}\u0590-\u05fe]*)</a>""")
private val bibleRefRegex =
  Regex("""<a\s+href\s*=\s*'B:(?<bookId>\d+)\s+(?<chapter>\d+):(?<verse>\d+)'\s*>(?<text>[\w\s\p{L}\p{InCombiningDiacriticalMarks}\u0590-\u05fe:.]{0,30})</a>""")


fun String.replaceReferences(linkProvider: MdLinkProvider) =
  this.replace(dictRefRegex) { matchResult ->
    val ref = linkProvider.dictReference(DictDefinition.Topic.parse(matchResult.groups["reference"]!!.value))
    val text = ref?.text ?: matchResult.groups["text"]!!.value
    ref?.toMdLink(text = text, wiki = true) ?: text
  }.replace(bibleRefRegex) { matchResult ->
    val bookId = matchResult.groups["bookId"]!!.value.toInt()
    val chapter = matchResult.groups["chapter"]!!.value.toInt()
    val verse = matchResult.groups["verse"]!!.value.toInt()
    val originalText = matchResult.groups["text"]!!.value
    val ref = linkProvider.bibleReference(bookId, chapter, verse, originalText)
    val text = ref?.text ?: originalText
    ref?.toMdLink(text = text, wiki = true) ?: text
  }

fun String.parseDictionaryDefinition(linkProvider: MdLinkProvider) =
  this.split("<br/>").map { it.trim() }
    .map { line ->
      Regex("^<he>(?<hebrewText>.*)</he>$").matchEntire(line)?.let { DefinitionPart.HebrewText(it.groups["hebrewText"]!!.value) }
        ?: Regex("^<el>(?<greekText>.*)</el>$").matchEntire(line)?.let { DefinitionPart.GreekText(it.groups["greekText"]!!.value) }
        ?: Regex("""^<df>\s*Оригинал:\s*</df>\s*<b>\s*(?<originalText>.*)\s*</b>$""")
          .matchEntire(line)?.let { DefinitionPart.OriginalText(it.groups["originalText"]!!.value) }
        ?: Regex("""^<df>\s*Произношение:\s*</df>\s*<b>\s*(?<pronunciation>.*)\s*</b>$""")
          .matchEntire(line)?.let { DefinitionPart.Pronunciation(it.groups["pronunciation"]!!.value) }
        ?: Regex("""^<df>\s*Транслитерация:\s*</df>\s*<b>\s*(?<transliteration>.*)\s*</b>$""")
          .matchEntire(line)?.let { DefinitionPart.Transliteration(it.groups["transliteration"]!!.value) }
        ?: Regex("""^<df>\s*Часть речи:\s*</df>\s*(?<partOfSpeech>.*)\s*$""")
          .matchEntire(line)?.let { DefinitionPart.PartOfSpeech(it.groups["partOfSpeech"]!!.value) }
        ?: Regex("""^<df>\s*Этимология:\s*</df>\s*(?<etymology>.*)\s*$""")
          .matchEntire(line)?.let { DefinitionPart.Etymology(it.groups["etymology"]!!.value.replaceReferences(linkProvider)) }
        ?: Regex("""^<df>\s*Синонимы:\s*</df>\s*(?<synonyms>.*)\s*$""")
          .matchEntire(line)?.let { DefinitionPart.Synonyms(it.groups["synonyms"]!!.value.replaceReferences(linkProvider)) }
        ?: Regex("""^<df>\s*Категория:\s*</df>\s*(?<category>.*)\s*$""")
          .matchEntire(line)?.let { DefinitionPart.Category(it.groups["category"]!!.value) }
        ?: DefinitionPart.Text(line.replaceReferences(linkProvider))
    }

