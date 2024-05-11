import kotlinx.coroutines.runBlocking


fun main() {
  runBlocking {

    val client = ALittleHebrewTranslatorClient("6debb86ba3d3ad61a53846e39025a6f0")

    val r = client.getTranslation("כִּֽי־אָ֛ז אֶהְפֹּ֥ךְ אֶל־עַמִּ֖ים שָׂפָ֣ה בְרוּרָ֑ה לִקְרֹ֤א כֻלָּם֙ בְּשֵׁ֣ם יְהוָ֔ה לְעָבְד֖וֹ שְׁכֶ֥ם אֶחָֽד׃")

    println(r)

  }
}