plugins {
    kotlin("jvm") version "2.0.0"
    id("org.jetbrains.compose") version "1.6.11"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
}

group = "org.example"
version = "1.0.0"

repositories {
    google()
    mavenCentral()
}

sourceSets {
    main {
        kotlin {
            setSrcDirs(listOf("./Game"))
        }
    }
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

    implementation("org.junit.jupiter:junit-jupiter:5.10.0")
    implementation(kotlin("test"))
}


compose.desktop {
    application {
        mainClass = "AppLauncher"
        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg)
        }
    }
}