plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.serialization)
}

group = "io.github.alelk.obsidian-utils"
version = "1.0.0-SNAPSHOT"

repositories {
  mavenCentral()
}

dependencies {
  implementation(libs.ktor.client.core)
  implementation(libs.ktor.client.cio)
  implementation(libs.ktor.client.content.negotiation)
  implementation(libs.ktor.serialization.koilinx.json)
  implementation(libs.ktor.client.logging)

  implementation(libs.logback.classic)

  testImplementation(libs.kotest.runner.junit5)
  testImplementation(libs.kotest.assertions.core)
  testImplementation(libs.kotest.property)
}

tasks.test {
  useJUnitPlatform()
}
kotlin {
  jvmToolchain(21)
}