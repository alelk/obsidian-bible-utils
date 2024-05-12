package io.github.alelk.obsidian_bible_utils.model.serialization

import io.github.alelk.obsidian_bible_utils.model.BibleVariant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object BibleVariantSerializer : KSerializer<BibleVariant> {
  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BibleVariant", PrimitiveKind.STRING)
  override fun serialize(encoder: Encoder, value: BibleVariant) = encoder.encodeString(value.toString())
  override fun deserialize(decoder: Decoder): BibleVariant = BibleVariant.parse(decoder.decodeString())
}