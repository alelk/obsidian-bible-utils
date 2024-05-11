plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.serialization)
}

group = "io.github.alelk.obsidian-utils"
version = "1.0.0-SNAPSHOT"

repositories {
  mavenCentral()
  maven {
    url = uri("https://maven.pkg.github.com/alelk/bolls-api-client")
    credentials {
      username = project.findProperty("gpr.user") as? String? ?: System.getenv("USERNAME") ?: "alelk"
      password = project.findProperty("gpr.key") as? String? ?: System.getenv("TOKEN")
    }
  }
}

dependencies {
  implementation("io.github.alelk.bolls-api-client:bolls-api-client:0.3.0")

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