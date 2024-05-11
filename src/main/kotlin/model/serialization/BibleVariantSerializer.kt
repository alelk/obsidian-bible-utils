package io.github.alelk.obsidian_bible_utils.model.serialization

import io.github.alelk.obsidian_bible_utils.model.BibleTranslation
import io.github.alelk.obsidian_bible_utils.model.BibleTransliteration
import io.github.alelk.obsidian_bible_utils.model.BibleVariant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object BibleVariantSerializer : KSerializer<BibleVariant> {
  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BibleVariant", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: BibleVariant) {
    when (value) {
      is BibleTranslation -> encoder.encodeString("translation-${value.translationSlug}")
      is BibleTransliteration -> encoder.encodeString("transliteration-${value.signature}")
    }
  }

  override fun deserialize(decoder: Decoder): BibleVariant {
    val value = decoder.decodeString()
    return when {
      value.startsWith("translation-") -> BibleTranslation(value.removePrefix("translation-"))
      value.startsWith("transliteration-") -> BibleTransliteration(value.removePrefix("transliteration-"))
      else -> throw IllegalArgumentException("Unknown BibleVariant: $value")
    }
  }
}