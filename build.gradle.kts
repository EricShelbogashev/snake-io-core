import com.google.protobuf.gradle.proto
import com.google.protobuf.gradle.protobuf

plugins {
    kotlin("jvm") version "1.9.0"
    kotlin("plugin.lombok") version "1.8.10"
    application
    id("com.google.protobuf") version "0.8.19"
}

group = "ru.nsu.shelbogashev"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // https://mvnrepository.com/artifact/com.google.protobuf/protobuf-kotlin
    implementation("com.google.protobuf:protobuf-kotlin:3.24.3")
    // https://mvnrepository.com/artifact/com.google.protobuf/protobuf-gradle-plugin
    runtimeOnly("com.google.protobuf:protobuf-gradle-plugin:0.9.4")
    // https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-maven-lombok
    implementation("org.jetbrains.kotlin:kotlin-maven-lombok:1.9.10")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}

application {
    mainClass.set("MainKt")
}

val protobufGenPath = "build/generated/source/proto"

sourceSets {
    main {
        proto {
            srcDir("src/main/proto")
        }
        kotlin.srcDirs += File(protobufGenPath)
    }
}

tasks.compileKotlin {
    source("${buildDir.absolutePath}/generated/src/main/java")
}

tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

protobuf {
    buildDir = File(protobufGenPath)
}