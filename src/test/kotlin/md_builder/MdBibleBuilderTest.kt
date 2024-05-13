package md_builder

import io.github.alelk.obsidian_bible_utils.md_builder.skipStrongNumbers
import io.github.alelk.obsidian_bible_utils.md_builder.transliterationToMd
import io.github.alelk.obsidian_bible_utils.model.DictDefinition
import io.github.alelk.obsidian_bible_utils.md_builder.DictReference
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe

class MdBibleBuilderTest : FeatureSpec({

  val dictReference = { refTopic: DictDefinition.Topic ->
    DictReference("../Dict/${refTopic.type.signature}/${refTopic.number}", "Text ${refTopic.number}")
  }

  feature("skip strong numbers") {
    scenario("skip strong numbers from hebrew") {
      "וַיִּקְרָ֧א<S>7121</S> אֱלֹהִ֛ים<S>430</S> לָֽרָקִ֖יעַ<S>7549</S> שָׁמָ֑יִם<S>8064</S> וַֽיְהִי־<S>1961</S>עֶ֥רֶב<S>6153</S> וַֽיְהִי־<S>1961</S>בֹ֖קֶר<S>1242</S> י֥וֹם<S>3117</S> שֵׁנִֽי׃<S>8145</S> פ <br/>"
        .skipStrongNumbers() shouldBe "וַיִּקְרָ֧א אֱלֹהִ֛ים לָֽרָקִ֖יעַ שָׁמָ֑יִם וַֽיְהִי־עֶ֥רֶב וַֽיְהִי־בֹ֖קֶר י֥וֹם שֵׁנִֽי׃ פ"
    }
    scenario("skip strong numbers from russian") {
      "вайа́ас<S>6213</S> элои́м<S>430</S> э́т<S>853</S>аракиа́<S>7549</S> вайавдэ́л<S>914</S> бейн<S>996</S> ама́йим<S>4325</S> ашэ́р<S>834</S> мита́хат<S>8478</S> лараки́а<S>7549</S> увейн<S>996</S> ама́йим<S>4325</S> ашэ́р<S>834</S> мэа́л<S>5921</S> лараки́а<S>7549</S> ва́йи<S>1961</S>хэ́н.<S>3651</S>"
        .skipStrongNumbers() shouldBe "вайа́ас элои́м э́таракиа́ вайавдэ́л бейн ама́йим ашэ́р мита́хат лараки́а увейн ама́йим ашэ́р мэа́л лараки́а ва́йихэ́н."
    }
  }

  feature("convert transliteration to markdown") {
    scenario("convert hebrew text with strong numbers to markdown") {
      "וַיִּקְרָ֧א<S>7121</S> אֱלֹהִ֛ים<S>430</S> לָֽרָקִ֖יעַ<S>7549</S> שָׁמָ֑יִם<S>8064</S> וַֽיְהִי־<S>1961</S>עֶ֥רֶב<S>6153</S> וַֽיְהִי־<S>1961</S>בֹ֖קֶר<S>1242</S> י֥וֹם<S>3117</S> שֵׁנִֽי׃<S>8145</S> פ <br/>"
        .transliterationToMd(dictReference, useWikiLinks = true) shouldBe
        "[[../Dict/H/7121|וַיִּקְרָ֧א]] [[../Dict/H/430|אֱלֹהִ֛ים]] [[../Dict/H/7549|לָֽרָקִ֖יעַ]] [[../Dict/H/8064|שָׁמָ֑יִם]] [[../Dict/H/1961|וַֽיְהִי־]][[../Dict/H/6153|עֶ֥רֶב]] " +
        "[[../Dict/H/1961|וַֽיְהִי־]][[../Dict/H/1242|בֹ֖קֶר]] [[../Dict/H/3117|י֥וֹם]] [[../Dict/H/8145|שֵׁנִֽי׃]] פ"
    }
    scenario("convert russian text with strong numbers to markdown") {
      ("вайа́ас<S>6213</S> элои́м<S>430</S> э́т<S>853</S>аракиа́<S>7549</S> вайавдэ́л<S>914</S> бейн<S>996</S> ама́йим<S>4325</S> ашэ́р<S>834</S> мита́хат<S>8478</S> " +
        "лараки́а<S>7549</S> увейн<S>996</S> ама́йим<S>4325</S> ашэ́р<S>834</S> мэа́л<S>5921</S> лараки́а<S>7549</S> ва́йи<S>1961</S>хэ́н.<S>3651</S>"
        )
        .transliterationToMd(dictReference, useWikiLinks = true) shouldBe
        "[[../Dict/H/6213|вайа́ас]] [[../Dict/H/430|элои́м]] [[../Dict/H/853|э́т]][[../Dict/H/7549|аракиа́]] [[../Dict/H/914|вайавдэ́л]] [[../Dict/H/996|бейн]] " +
        "[[../Dict/H/4325|ама́йим]] [[../Dict/H/834|ашэ́р]] [[../Dict/H/8478|мита́хат]] [[../Dict/H/7549|лараки́а]] [[../Dict/H/996|увейн]] " +
        "[[../Dict/H/4325|ама́йим]] [[../Dict/H/834|ашэ́р]] [[../Dict/H/5921|мэа́л]] [[../Dict/H/7549|лараки́а]] [[../Dict/H/1961|ва́йи]][[../Dict/H/3651|хэ́н]]."
    }
  }
})