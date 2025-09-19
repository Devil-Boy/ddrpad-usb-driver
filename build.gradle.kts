import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.21"
    application
}

group = "in.kerv.ddrpad.usbdriver"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.usb4java:usb4java:1.3.0")
    // The native libraries are included in the usb4java dependency,
    // but if needed they can be added explicitly like this:
    // val osName = System.getProperty("os.name").toLowerCase()
    // val osArch = System.getProperty("os.arch")
    // val platform = when {
    //     osName.contains("linux") -> when (osArch) {
    //         "amd64" -> "linux-x86_64"
    //         "x86_64" -> "linux-x86_64"
    //         "aarch64" -> "linux-aarch64"
    //         "arm" -> "linux-arm"
    //         else -> "linux-x86"
    //     }
    //     osName.contains("windows") -> when (osArch) {
    //         "amd64" -> "win32-x86-64"
    //         else -> "win32-x86"
    //     }
    //     osName.contains("mac") -> "darwin-x86-64"
    //     else -> throw GradleException("Unsupported OS: $osName")
    // }
    // runtimeOnly("org.usb4java:libusb4java:1.3.0:$platform")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("in.kerv.ddrpad.usbdriver.MainKt")
}

kotlin {
    jvmToolchain(21)
}
