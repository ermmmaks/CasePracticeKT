plugins {
    kotlin("jvm") version "2.0.0"
    id("org.jetbrains.compose") version "1.6.11"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

    implementation("org.junit.jupiter:junit-jupiter:5.10.0")
    implementation(kotlin("test"))
}

compose.desktop {
    application {
        mainClass = "GameApplauncherKt"
        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.applicationdsl.TargetFormat.Dmg, org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi, org.jetbrains.compose.desktop.application/dsl.TargetFormat.Deb)
            packageName = "BuckshotRouletteAdmin"
            packageVersion = "1.0.0"
        }
    }
}

sourceSets {
    main {
        kotlin.setSrcDirs(listOf("Game"))
    }
    test {
        kotlin.setSrcDirs(listOf("Game"))
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

application {
    mainClass.set("Game.AppLauncherKt") 
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}
