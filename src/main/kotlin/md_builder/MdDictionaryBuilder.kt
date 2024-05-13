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

  fun toMd(dictReference: (refTopic: DictDefinition.Topic, from: MdDictDefinition) -> DictReference?): String =
    buildString {
      val definition = definition.parseDictionaryDefinition { refTopic -> dictReference(refTopic, this@MdDictDefinition) }

      val category = definition.find<DefinitionPart.Category>()?.text?.trim()
      val partOfSpeech = definition.find<DefinitionPart.PartOfSpeech>()?.text?.trim()

      appendLine("### $topic: $shortDefinition")
      appendLine()
      val hebrewText = definition.find<DefinitionPart.HebrewText>()?.text ?: lexeme
      appendLine("> $hebrewText")
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
      val pronunciation = listOfNotNull(definition.find<DefinitionPart.Pronunciation>()?.text, pronunciation, *transliterations.toTypedArray()).distinct()
      pronunciation.forEach { appendLine("- $it") }
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
      appendLine("#dictionary #dictionary_definition #generated #unchanged")
    }
}

sealed interface DefinitionPart {
  data class HebrewText(val text: String) : DefinitionPart
  data class OriginalText(val text: String) : DefinitionPart
  data class Pronunciation(val text: String) : DefinitionPart
  data class PartOfSpeech(val text: String) : DefinitionPart
  data class Etymology(val text: String) : DefinitionPart
  data class Synonyms(val text: String) : DefinitionPart
  data class Category(val text: String) : DefinitionPart
  data class Text(val text: String) : DefinitionPart
}

inline fun <reified D : DefinitionPart> List<DefinitionPart>.find(): D? = find { it is D } as? D

private val referenceRegex = Regex("""<a href='S:(?<reference>[HG]\d+)'>(?<text>[\w\s\p{L}\p{InCombiningDiacriticalMarks}\u0590-\u05fe]*)</a>""")


fun String.replaceDictReferences(dictReference: (refTopic: DictDefinition.Topic) -> DictReference?) =
  referenceRegex.replace(this) { matchResult ->
    val ref = dictReference(DictDefinition.Topic.parse(matchResult.groups["reference"]!!.value))
    val text = ref?.text ?: matchResult.groups["text"]!!.value
    ref?.toMdLink(text = text, wiki = true) ?: text
  }

fun String.parseDictionaryDefinition(dictReference: (refTopic: DictDefinition.Topic) -> DictReference?) =
  this.split("<br/>").map { it.trim() }
    .map { line ->
      Regex("^<he>(?<hebrewText>.*)</he>$").matchEntire(line)?.let { DefinitionPart.HebrewText(it.groups["hebrewText"]!!.value) }
        ?: Regex("""^<df>\s*Оригинал:\s*</df>\s*<b>\s*(?<originalText>.*)\s*</b>$""")
          .matchEntire(line)?.let { DefinitionPart.OriginalText(it.groups["originalText"]!!.value) }
        ?: Regex("""^<df>\s*Произношение:\s*</df>\s*<b>\s*(?<pronunciation>.*)\s*</b>$""")
          .matchEntire(line)?.let { DefinitionPart.Pronunciation(it.groups["pronunciation"]!!.value) }
        ?: Regex("""^<df>\s*Часть речи:\s*</df>\s*(?<partOfSpeech>.*)\s*$""")
          .matchEntire(line)?.let { DefinitionPart.PartOfSpeech(it.groups["partOfSpeech"]!!.value) }
        ?: Regex("""^<df>\s*Этимология:\s*</df>\s*(?<etymology>.*)\s*$""")
          .matchEntire(line)?.let { DefinitionPart.Etymology(it.groups["etymology"]!!.value.replaceDictReferences(dictReference)) }
        ?: Regex("""^<df>\s*Синонимы:\s*</df>\s*(?<synonyms>.*)\s*$""")
          .matchEntire(line)?.let { DefinitionPart.Synonyms(it.groups["synonyms"]!!.value.replaceDictReferences(dictReference)) }
        ?: Regex("""^<df>\s*Категория:\s*</df>\s*(?<category>.*)\s*$""")
          .matchEntire(line)?.let { DefinitionPart.Category(it.groups["category"]!!.value) }
        ?: DefinitionPart.Text(line)
    }

