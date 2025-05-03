import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.kotlin.dsl.named

plugins {
    kotlin("jvm") version "2.1.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "cancelcloud"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://repo.purpurmc.org/snapshots")
}

dependencies {
    // Verwende die aktuelle Purpur API – beachte: die Gruppe ist "io.purpurmc"
    compileOnly("org.purpurmc.purpur:purpur-api:1.21.5-R0.1-SNAPSHOT")
    implementation("net.dv8tion:JDA:5.5.0")
    implementation("club.minnced:jda-ktx:0.12.0")
    // Aktuelle Utility-Libraries von TheFruxz (aktualisierte Versionsnummern; ersetze ggf. durch neueste):
    implementation("com.github.TheFruxz:Stacked:2025.3-b66c374")
    implementation("com.github.TheFruxz:Ascend:2025.3-c861242")

    implementation(kotlin("stdlib"))
}

// Füge Dependency Substitution hinzu, um Anfragen nach "dev.fruxz:ascend" umzuleiten:
configurations.all {
    resolutionStrategy.dependencySubstitution {
        substitute(module("dev.fruxz:ascend")).using(module("com.github.TheFruxz:Ascend:2025.3-c861242"))
    }
}

tasks {
    shadowJar {
        archiveBaseName.set("PurpurStats")
        archiveClassifier.set("")
        archiveVersion.set(version.toString())
    }
}

kotlin {
    jvmToolchain(21)
}

java {
    withSourcesJar()
}

// Konfiguration des ShadowJar-Tasks:
// Dieser Task erstellt die "geshadete" Jar, in der neben deinen Klassen auch alle notwendigen Abhängigkeiten (z. B. kotlin-stdlib) enthalten sind.
tasks.named<ShadowJar>("shadowJar").configure {
    archiveBaseName.set("PurpurInsight")
    archiveClassifier.set("")  // Keine zusätzliche Klassifizierung im Dateinamen
    manifest {
        // Hier setzen wir das Main-Class-Attribut:
        attributes(mapOf("Main-Class" to "cancelcloud.PurpurInsightPlugin"))
    }
}

// Task zum Kopieren des erstellten JAR in den Zielordner:
tasks.register<Copy>("copyJar") {
    dependsOn(tasks.named<ShadowJar>("shadowJar"))
    from(tasks.named<ShadowJar>("shadowJar").get().archiveFile.get().asFile)
    into(file("/Users/cancelcloud/Developer/Minecraft/purpur21-4/plugins/"))
    rename { "PurpurInsight-${version}.jar" }
}

// Finalisiere den Build-Task, sodass nach dem Build automatisch das JAR kopiert wird:
tasks.named("build") {
    finalizedBy("copyJar")
}