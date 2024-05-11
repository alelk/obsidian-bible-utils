package io.github.alelk.obsidian_bible_utils.transliterator

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable

class ALittleHebrewTranslatorClient(val sessionId: String) {

  val client = HttpClient(CIO) {
    defaultRequest {
      url("https://alittlehebrew.com/")
    }
    engine {
      maxConnectionsCount = 10
    }
    install(Logging) {
      level = LogLevel.INFO
    }
    install(ContentNegotiation) {
      json()
    }
  }

  suspend fun getToken() =
    client.get("transliterate/") {
      accept(ContentType.Text.Html)
      cookie("PHPSESSID", sessionId)
    }.body<String>().let {
      Regex("""<input\s+type="hidden"\s+name="token"\s+value="([^"]+)">""").find(it)?.groupValues?.get(1) ?: throw IllegalStateException("Token not found")
    }

  private var tokenMutex = Mutex()
  private var token: Deferred<String>? = null

  suspend fun <T> withToken(block: suspend (String) -> T): T = coroutineScope {
    val t = tokenMutex.withLock { token ?: async { getToken() }.also { token = it } }.await()
    runCatching {
      block(t)
    }.onFailure {
      tokenMutex.withLock { token = null }
    }.getOrThrow()
  }

  @Serializable
  data class TranslationResult(val success: Boolean, val result: String? = null, val message: String? = null)

  suspend fun getTranslation(text: String): String = withToken { token ->
    client.get("transliterate/get.php") {
      parameter("token", token)
      parameter("style", "230_russian")
      parameter("syllable", "auto")
      parameter("accent", "auto")
      parameter("hebrew_text", text)
      cookie("PHPSESSID", sessionId)
      accept(ContentType.Application.Json)
      header("X-Requested-With", "XMLHttpRequest")
    }.body<TranslationResult>().let {
      if (it.success) it.result ?: throw IllegalStateException("Translation not found")
      else throw IllegalStateException(it.message)
    }
  }
}