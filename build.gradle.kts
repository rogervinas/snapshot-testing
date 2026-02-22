import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED

plugins {
  kotlin("jvm") version "2.3.10"
  id("org.jlleitschuh.gradle.ktlint") version "14.0.1"
}

repositories {
  mavenCentral()
}

val javaSnapshotTestingVersion = "4.0.8"
val selfieVersion = "2.5.5"

dependencies {
  implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

  // com.diffplug.selfie:selfie

  testImplementation("com.diffplug.selfie:selfie-runner-junit5:$selfieVersion")

  // io.github.origin-energy:java-snapshot-testing

  testImplementation("io.github.origin-energy:java-snapshot-testing-junit5:$javaSnapshotTestingVersion")
  testImplementation("io.github.origin-energy:java-snapshot-testing-plugin-jackson:$javaSnapshotTestingVersion")
  testImplementation("org.slf4j:slf4j-simple:2.0.17")

  testImplementation(platform("com.fasterxml.jackson:jackson-bom:2.21.0"))
  testImplementation("com.fasterxml.jackson.core:jackson-core")
  testImplementation("com.fasterxml.jackson.core:jackson-databind")
  testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8")
  testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

  // common

  testImplementation(platform("org.junit:junit-bom:6.0.3"))
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
  testImplementation("org.junit.jupiter:junit-jupiter-api")
  testImplementation("org.junit.jupiter:junit-jupiter-params")

  testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.28.1")
}

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(21)
  }
}

kotlin {
  compilerOptions {
    freeCompilerArgs.addAll("-Xjsr305=strict")
  }
}

tasks.withType<Test> {
  useJUnitPlatform()
  testLogging {
    events(PASSED, SKIPPED, FAILED)
    exceptionFormat = FULL
    showExceptions = true
    showCauses = true
    showStackTraces = true
  }
}
