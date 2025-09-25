import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  kotlin("jvm") version "2.1.20"
  application
}

group = "in.kerv.ddrpad.usbdriver"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
}

dependencies {
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
  implementation("net.codecrete.usb:java-does-usb:1.2.1")
  implementation("uk.co.bithatch:linuxio4j:2.1")
  implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")
  implementation("org.slf4j:slf4j-simple:2.0.3")
}

tasks.test {
  useJUnitPlatform()
}

application {
  mainClass.set("in.kerv.ddrpad.usbdriver.MainKt")
}

kotlin {
  compilerOptions {
    jvmTarget = JvmTarget.JVM_23
  }
  jvmToolchain(23)
}