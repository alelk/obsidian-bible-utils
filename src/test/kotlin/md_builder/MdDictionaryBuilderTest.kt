package md_builder

import io.github.alelk.obsidian_bible_utils.md_builder.DefinitionPart
import io.github.alelk.obsidian_bible_utils.md_builder.MdLinkProvider
import io.github.alelk.obsidian_bible_utils.md_builder.parseDictionaryDefinition
import io.github.alelk.obsidian_bible_utils.model.DictDefinition
import io.github.alelk.obsidian_bible_utils.md_builder.MdReference
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe

class MdDictionaryBuilderTest : FeatureSpec({

  val mdLinkProvider = MdLinkProvider(
    dictReference = { refTopic: DictDefinition.Topic ->
      MdReference("../Dict/${refTopic.type.signature}/${refTopic.number}", "Text ${refTopic.number}")
    },
    bibleReference = { book: Int, chapter: Int, verse: Int, _ ->
      MdReference("../Bible/$book/$chapter/$verse", "Book $book:$chapter:$verse")
    }
  )

  feature("parse dictionary definition for hebrew word") {
    scenario("Parse dictionary definition from html") {
      val html =
        "<he>אַדְמָה</he> <br/>Адма. <br/><df>Оригинал:</df> <b>אַדְמָה</b><br/><df>Произношение:</df> <b>адма</b> <br/>" +
          "<df>Часть речи:</df> Имя собственное и Географическое название <br/>" +
          "<df>Этимология:</df> сокращённая форма <a href='S:H127'>H127</a> <br/><df>Категория:</df> Имя"
      val parsed = html.parseDictionaryDefinition(mdLinkProvider)
      parsed shouldBe listOf(
        DefinitionPart.HebrewText("אַדְמָה"),
        DefinitionPart.Text("Адма."),
        DefinitionPart.OriginalText("אַדְמָה"),
        DefinitionPart.Pronunciation("адма"),
        DefinitionPart.PartOfSpeech("Имя собственное и Географическое название"),
        DefinitionPart.Etymology("сокращённая форма [[../Dict/H/127|Text 127]]"),
        DefinitionPart.Category("Имя")
      )
    }

    scenario("Parse dictionary definition for hebrew word from html (2)") {
      val html =
        "<he>אֲדָמָה</he> <br/>земля, почва, страна; <br/><df>Оригинал:</df> <b>אֲדָמָה</b><br/><df>Произношение:</df> <b>адама</b> <br/>" +
          "<df>Часть речи:</df> Существительное женского рода <br/><df>Этимология:</df> от <a href='S:H119'>H119</a> <br/>" +
          "<df>Синонимы:</df> <a href='S:H776'>H776</a> (אֶרֶץ‎), <a href='S:H7704'>H7704</a> (שָׂדֶה‎)."
      val parsed = html.parseDictionaryDefinition(mdLinkProvider)
      parsed shouldBe listOf(
        DefinitionPart.HebrewText("אֲדָמָה"),
        DefinitionPart.Text("земля, почва, страна;"),
        DefinitionPart.OriginalText("אֲדָמָה"),
        DefinitionPart.Pronunciation("адама"),
        DefinitionPart.PartOfSpeech("Существительное женского рода"),
        DefinitionPart.Etymology("от [[../Dict/H/119|Text 119]]"),
        DefinitionPart.Synonyms("[[../Dict/H/776|Text 776]] (אֶרֶץ‎), [[../Dict/H/7704|Text 7704]] (שָׂדֶה‎).")
      )
    }

    scenario("Parse dictionary definition for greek word from html") {
      val html =
        "<el>Ἰούδας</el> <br/>" +
          "Иуда (<nm>1.</nm> сын патриарха Иакова; <br/>" +
          "<nm>2.</nm> неизвестный человек в родословной Иисуса Христа, <a href='B:490 3:26'>Лк 3:26</a> в некоторых манускриптах; <br/>" +
          "<nm>3.</nm> другой неизвестный человек в родословной Иисуса Христа, <a href='B:490 3:30'>Лк 3:30</a>; <br/>" +
          "<nm>4.</nm> Иуда Галилеянин, поднявший восстание в Галилее во вр. переписи Квириния; <br/>" +
          "<nm>5.</nm> Иуда Дамасский, принявший апостола Павла в свой дом; <br/>" +
          "<nm>6.</nm> Иуда Иаковлев апостол Иисуса Христа; <br/>" +
          "<nm>7.</nm> Иуда Искариот; <br/>" +
          "<nm>8.</nm> Иуда, прозванный Варсавою, христианский пророк из Иер.; <br/>" +
          "<nm>9.</nm> Иуда, брат Иисуса Христа, написавший послание от Иуды);  <br/>" +
          "<df>Оригинал:</df> <b>Ἰούδας</b> <br/>" +
          "<df>Транслитерация:</df> <b>Иоудас</b> <br/>" +
          "<df>Произношение:</df> <b>Иуда́с</b> <br/>" +
          "<df>Часть речи:</df> Существительное мужского рода <br/>" +
          "<df>Этимология:</df> еврейского происхождения <a href='S:H3063'>H3063</a><df>MASOR:</df>  <a href='S:H3063'>H3063</a> (יְהוּדָה‎). <br/><df>Категория:</df> Имя"
      val parsed = html.parseDictionaryDefinition(mdLinkProvider)
      parsed shouldBe listOf(
        DefinitionPart.GreekText("Ἰούδας"),
        DefinitionPart.Text("Иуда (<nm>1.</nm> сын патриарха Иакова;"),
        DefinitionPart.Text("<nm>2.</nm> неизвестный человек в родословной Иисуса Христа, [[../Bible/490/3/26|Book 490:3:26]] в некоторых манускриптах;"),
        DefinitionPart.Text("<nm>3.</nm> другой неизвестный человек в родословной Иисуса Христа, [[../Bible/490/3/30|Book 490:3:30]];"),
        DefinitionPart.Text("<nm>4.</nm> Иуда Галилеянин, поднявший восстание в Галилее во вр. переписи Квириния;"),
        DefinitionPart.Text("<nm>5.</nm> Иуда Дамасский, принявший апостола Павла в свой дом;"),
        DefinitionPart.Text("<nm>6.</nm> Иуда Иаковлев апостол Иисуса Христа;"),
        DefinitionPart.Text("<nm>7.</nm> Иуда Искариот;"),
        DefinitionPart.Text("<nm>8.</nm> Иуда, прозванный Варсавою, христианский пророк из Иер.;"),
        DefinitionPart.Text("<nm>9.</nm> Иуда, брат Иисуса Христа, написавший послание от Иуды);"),
        DefinitionPart.OriginalText("Ἰούδας"),
        DefinitionPart.Transliteration("Иоудас"),
        DefinitionPart.Pronunciation("Иуда́с"),
        DefinitionPart.PartOfSpeech("Существительное мужского рода"),
        DefinitionPart.Etymology("еврейского происхождения [[../Dict/H/3063|Text 3063]]"),
        DefinitionPart.Text("**MASOR:** [[../Dict/H/3063|Text 3063]] (יְהוּדָה\u200E)."),
        DefinitionPart.Category("Имя")
      )
    }

    scenario("Parse dictionary definition for greek word from html (2)") {
      val html =
        "<el>καθάπτω</el> <br/>схватывать, цепляться, вцепиться. <br/><df>Оригинал:</df> <b>καθάπτω</b> <br/><df>Транслитерация:</df> <b>кафапто</b> <br/>" +
          "<df>Произношение:</df> <b>кафа́пто</b> <br/><df>Часть речи:</df> Глагол <br/>" +
          "<df>Этимология:</df> от <a href='S:G2596'>G2596</a> и <a href='S:G680'>G680</a> <br/> <br/>" +
          "<df>Категория:</df> Глагол" +
          "<df>Словарь Дворецкого:</df> <b>καθ-απτω</b> <i>чаще med., ион.</i> καταπτομαι " +
          "1) привязывать, прикреплять Пр.: <i>(τι αμφη σωματος δεσμοις Эврипид (ок. 480-406 до н. э.))</i> " +
          "2) укреплять Пр.: <i>(τι επη τεν γην Ксенофонт (ок. 428-354 до н. э.); τεν πρωρραν εις ακινητον Полибий (ок. 200-120 до н. э.))</i> " +
          "3) связывать, скреплять Пр.: <i>(φανερον …τα οστεα καθαπτειν τα νευρα Аристотель (384-322 до н. э.))</i> " +
          "4) набрасывать, накидывать, надевать Пр.: <i>(ωμοις τινος αμφιβληστρον Софокл (ок. 496-406 до н. э.)) βροχω καθημμενη Софокл (ок. 496-406 до н. э.) " +
          "— <i>(Антигона</i>) с накинутой (на шею) петлей; σκευη πρεποντως σωμ΄ εμον καθαψομαι Эврипид (ок. 480-406 до н. э.) " +
          "— я надену на себя соответствующую одежду</i> " +
          "5) преимущ. <i>med.</i> схватывать Пр.: <i>(βρεφεος χειρεσσι Феокрит (ок. 300-ок. 260 до н. э.)) εχιδνα καθηψε της χειρος αυτου Новый Завет — " +
          "гадюка вцепилась ему в руку; καθαπτεσθαιτυραννιδος Солон (ок. 640-560 до н. э.) Апулей (1 в.) Плутарх (ок. 46-126) — захватывать царскую власть</i> " +
          "6) <i>med.</i> простираться, проникать, доходить, достигать Пр.: <i>(εις <i>или</i> προς τι Аристотель (384-322 до н. э.))</i> " +
          "7) <i>med.</i> подходить, приближаться (к <i>кому-л.</i> с речью), обращаться Пр.: <i>(τινα επεεσσι μαλακοισιν Гомер (X-IX вв. до н. э.); " +
          "λογω δηκτη Плутарх (ок. 46-126)) αντιβιοις επεεσσι καθαπτομενος Гомер (X-IX вв. до н. э.) — обратившись с неприязненной речью</i> " +
          "8) <i>med.</i> нападать, бранить, порицать Пр.: <i>(πικρως τινος Плутарх (ок. 46-126)) επειδη μου Νικιας καθηψατο Лисий (ок. 445-380 до н. э.) — " +
          "поскольку Никий выступил с нападками на меня</i> 9) <i>med.</i> просить <i>чьим-л.</i> именем, призывать в свидетели Пр.: " +
          "<i>(θεων των αλλων καταπτομενος ικετευω καη του Διος τουδε Геродот (ок. 484-425 до н. э.)) Δημαρατου τε καη αλλων μαρτυρων καταπτομενος Геродот " +
          "(ок. 484-425 до н. э.) — ссылаясь на Демарата и других свидетелей</i>."
      val parsed = html.parseDictionaryDefinition(mdLinkProvider)
      parsed shouldBe listOf(
        DefinitionPart.GreekText("καθάπτω"),
        DefinitionPart.Text("схватывать, цепляться, вцепиться."),
        DefinitionPart.OriginalText("καθάπτω"),
        DefinitionPart.Transliteration("кафапто"),
        DefinitionPart.Pronunciation("кафа́пто"),
        DefinitionPart.PartOfSpeech("Глагол"),
        DefinitionPart.Etymology(text = "от [[../Dict/G/2596|Text 2596]] и [[../Dict/G/680|Text 680]]"),
        DefinitionPart.Text(),
        DefinitionPart.Category("Глагол"),
        DefinitionPart.Text(
          "**Словарь Дворецкого:** <b>καθ-απτω</b> <i>чаще med., ион.</i> καταπτομαι 1) привязывать, прикреплять Пр.: " +
            "<i>(τι αμφη σωματος δεσμοις Эврипид (ок. 480-406 до н. э.))</i> " +
            "2) укреплять Пр.: <i>(τι επη τεν γην Ксенофонт (ок. 428-354 до н. э.); τεν πρωρραν εις ακινητον Полибий (ок. 200-120 до н. э.))</i> " +
            "3) связывать, скреплять Пр.: <i>(φανερον …τα οστεα καθαπτειν τα νευρα Аристотель (384-322 до н. э.))</i> " +
            "4) набрасывать, накидывать, надевать Пр.: <i>(ωμοις τινος αμφιβληστρον Софокл (ок. 496-406 до н. э.)) βροχω καθημμενη Софокл " +
            "(ок. 496-406 до н. э.) — <i>(Антигона</i>) с накинутой (на шею) петлей; σκευη πρεποντως σωμ΄ εμον καθαψομαι Эврипид (ок. 480-406 до н. э.) " +
            "— я надену на себя соответствующую одежду</i> " +
            "5) преимущ. <i>med.</i> схватывать Пр.: <i>(βρεφεος χειρεσσι Феокрит (ок. 300-ок. 260 до н. э.)) εχιδνα καθηψε της χειρος αυτου Новый Завет " +
            "— гадюка вцепилась ему в руку; καθαπτεσθαι" +
            "τυραννιδος Солон (ок. 640-560 до н. э.) Апулей (1 в.) Плутарх (ок. 46-126) — захватывать царскую власть</i> " +
            "6) <i>med.</i> простираться, проникать, доходить, достигать Пр.: <i>(εις <i>или</i> προς τι Аристотель (384-322 до н. э.))</i> " +
            "7) <i>med.</i> подходить, приближаться (к <i>кому-л.</i> с речью), обращаться Пр.: <i>(τινα επεεσσι μαλακοισιν Гомер (X-IX вв. до н. э.); " +
            "λογω δηκτη Плутарх (ок. 46-126)) αντιβιοις επεεσσι καθαπτομενος Гомер (X-IX вв. до н. э.) — обратившись с неприязненной речью</i> " +
            "8) <i>med.</i> нападать, бранить, порицать Пр.: <i>(πικρως τινος Плутарх (ок. 46-126)) επειδη μου Νικιας καθηψατο Лисий (ок. 445-380 до н. э.) " +
            "— поскольку Никий выступил с нападками на меня</i> " +
            "9) <i>med.</i> просить <i>чьим-л.</i> именем, призывать в свидетели Пр.: <i>(θεων των αλλων καταπτομενος ικετευω καη του Διος τουδε Геродот " +
            "(ок. 484-425 до н. э.)) Δημαρατου τε καη αλλων μαρτυρων καταπτομενος Геродот (ок. 484-425 до н. э.) — ссылаясь на Демарата и других свидетелей</i>."
        )
      )
    }
  }
})