package io.github.alelk.obsidian_bible_utils.utils

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retry
import mu.KotlinLogging
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val log = KotlinLogging.logger { }

suspend fun <T> retryOnException(
  countRetries: Long = 10,
  delay: Duration = 10.seconds,
  excFilter: (exc: Throwable) -> Boolean = { true },
  block: suspend () -> T
): T =
  flow {
    emit(block())
  }.retry(countRetries) { e ->
    if (excFilter(e)) {
      log.error { "Error occurred: ${e.message}. Retry after $delay..." }
      delay(delay)
      true
    } else false
  }.first()