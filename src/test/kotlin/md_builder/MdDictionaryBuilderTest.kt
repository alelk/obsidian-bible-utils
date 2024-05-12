package md_builder

import io.github.alelk.obsidian_bible_utils.md_builder.DefinitionPart
import io.github.alelk.obsidian_bible_utils.md_builder.parseDictionaryDefinition
import io.github.alelk.obsidian_bible_utils.model.DictDefinition
import io.github.alelk.obsidian_bible_utils.md_builder.DictReference
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe

class MdDictionaryBuilderTest : FeatureSpec({

  val dictReference = { refTopic: DictDefinition.Topic ->
    DictReference("../Dict/${refTopic.type.signature}/${refTopic.number}", "Text ${refTopic.number}")
  }

  feature("parse dictionary definition") {
    scenario("Parse dictionary definition from html") {
      val html =
        "<he>אַדְמָה</he> <br/>Адма. <br/><df>Оригинал:</df> <b>אַדְמָה</b><br/><df>Произношение:</df> <b>адма</b> <br/><df>Часть речи:</df> Имя собственное и Географическое название <br/><df>Этимология:</df> сокращённая форма <a href='S:H127'>H127</a> <br/><df>Категория:</df> Имя"
      val parsed = html.parseDictionaryDefinition(dictReference)
      parsed shouldBe listOf(
        DefinitionPart.HebrewText("אַדְמָה"),
        DefinitionPart.Text("Адма."),
        DefinitionPart.OriginalText("אַדְמָה"),
        DefinitionPart.Pronunciation("адма"),
        DefinitionPart.PartOfSpeech("Имя собственное и Географическое название"),
        DefinitionPart.Etymology("сокращённая форма [Text 127](../Dict/H/127)"),
        DefinitionPart.Category("Имя")
      )
    }

    scenario("Parse dictionary definition from html (2)") {
      val html =
        "<he>אֲדָמָה</he> <br/>земля, почва, страна; <br/><df>Оригинал:</df> <b>אֲדָמָה</b><br/><df>Произношение:</df> <b>адама</b> <br/><df>Часть речи:</df> Существительное женского рода <br/><df>Этимология:</df> от <a href='S:H119'>H119</a> <br/><df>Синонимы:</df> <a href='S:H776'>H776</a> (אֶרֶץ‎), <a href='S:H7704'>H7704</a> (שָׂדֶה‎)."
      val parsed = html.parseDictionaryDefinition(dictReference)
      parsed shouldBe listOf(
        DefinitionPart.HebrewText("אֲדָמָה"),
        DefinitionPart.Text("земля, почва, страна;"),
        DefinitionPart.OriginalText("אֲדָמָה"),
        DefinitionPart.Pronunciation("адама"),
        DefinitionPart.PartOfSpeech("Существительное женского рода"),
        DefinitionPart.Etymology("от [Text 119](../Dict/H/119)"),
        DefinitionPart.Synonyms("[Text 776](../Dict/H/776) (אֶרֶץ‎), [Text 7704](../Dict/H/7704) (שָׂדֶה‎).")
      )
    }

  }
})