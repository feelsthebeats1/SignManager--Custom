plugins {
    id("java")
    id("java-library")
    id("xyz.jpenilla.run-paper") version "2.3.0"
    id("com.modrinth.minotaur") version "2.+"
}

val supportedVersions = listOf("1.20", "1.20.1", "1.20.2", "1.20.3", "1.20.4", "1.20.5", "1.20.6", "1.21")

group = "me.redned"
version = "1.0.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

repositories {
    mavenCentral()

    maven("https://hub.spigotmc.org/nexus/content/groups/public/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/releases/")
}

dependencies {
    compileOnly(libs.spigot.api)
    compileOnly(libs.adventure.platform.bukkit)
    compileOnly(libs.adventure.text.minimessage)
    compileOnly(libs.placeholderapi)
}

tasks {
    runServer {
        minecraftVersion("1.20.4")
    }

    processResources {
        expand("version" to rootProject.version)
    }
}
