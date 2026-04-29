plugins {
    kotlin("jvm") version "2.0.0"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.junit.jupiter:junit-jupiter:5.10.0")
    implementation(kotlin("test"))
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
