package io.github.alelk.obsidian_bible_utils.model.serialization

import io.github.alelk.obsidian_bible_utils.model.DictDefinition
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class DictDefinitionTopicSerializer : KSerializer<DictDefinition.Topic> {
  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("DictDefinitionTopic", PrimitiveKind.STRING)
  override fun deserialize(decoder: Decoder): DictDefinition.Topic = DictDefinition.Topic.parse(decoder.decodeString())
  override fun serialize(encoder: Encoder, value: DictDefinition.Topic) = encoder.encodeString(value.toString())
}