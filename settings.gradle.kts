rootProject.name = "SignManager"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {
    repositories {
        mavenCentral()

        // Spigot
        maven("https://hub.spigotmc.org/nexus/content/groups/public/")

        // Paper, Velocity
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}
