package io.github.alelk.obsidian_bible_utils.model

import io.github.alelk.obsidian_bible_utils.model.serialization.DictDefinitionTopicSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Dictionary(
  val signature: String,
  val definitions: List<DictDefinition>
)

@Serializable
data class DictDefinition(
  val topic: Topic,
  val lexeme: String,
  val definition: String,
  @SerialName("short_definition") val shortDefinition: String,
  val transliteration: String,
  val pronunciation: String
) {
  enum class TopicType(val signature: String) {
    HEBREW("H"),
    GREEK("G");

    companion object {
      fun parse(str: String): TopicType = when (str) {
        "H" -> HEBREW
        "G" -> GREEK
        else -> throw IllegalArgumentException("Unknown topic type: $str")
      }
    }
  }

  @Serializable(with = DictDefinitionTopicSerializer::class)
  data class Topic(val type: TopicType, val number: Int) {
    override fun toString(): String = "${type.signature}$number"

    companion object {
      fun parse(str: String): Topic = Topic(type = TopicType.parse(str.take(1)), number = str.substring(1).toInt())
    }
  }
}
