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
  implementation("net.codecrete.usb:java-does-usb:1.2.1")
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