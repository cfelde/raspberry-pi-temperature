plugins {
    kotlin("jvm") version "1.9.20"
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.cfelde"
version = "1.0"

repositories {
    mavenCentral()
    maven { url = uri("https://mvn.sigbla.app/repository") }
}

dependencies {
    implementation("sigbla.app:sigbla-app-all:1.+")
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "MainKt"
    }
}

kotlin {
    jvmToolchain(8)
}

application {
    mainClass.set("MainKt")
}
