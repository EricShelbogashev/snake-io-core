import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.google.protobuf.gradle.proto
import com.google.protobuf.gradle.protobuf
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import kotlin.io.path.Path

plugins {
    kotlin("jvm") version "1.9.0"
    application
    id("com.google.protobuf") version "0.8.19"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    `maven-publish`
}

group = "ru.nsu.shelbogashev"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.protobuf:protobuf-kotlin:3.24.3")
    // https://mvnrepository.com/artifact/com.google.protobuf/protobuf-gradle-plugin
    runtimeOnly("com.google.protobuf:protobuf-gradle-plugin:0.9.4")

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

tasks {
    named<ShadowJar>("shadowJar") {
        archiveBaseName.set("snake-io-core")
        destinationDirectory.set(projectDir.resolve(Path("build", "out").toFile()))
        manifest {
            attributes(mapOf("Main-Class" to "MainKt"))
        }
    }
}

tasks {
    build {
        dependsOn(shadowJar)
    }
}

tasks.compileKotlin {
    source("${buildDir.absolutePath}/generated/src/main/java")
}

kotlin {
    compilerOptions {
        apiVersion.set(KotlinVersion.KOTLIN_1_9)
        jvmTarget.set(JvmTarget.JVM_17)
        jvmToolchain(17)
    }
}

tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

protobuf {
    buildDir = File(protobufGenPath)
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/EricShelbogashev/snake-io-core")
            credentials {
                username = project.findProperty("gpr.user") as String?
                password = project.findProperty("gpr.key") as String?
            }
        }
    }
    publications {
        register<MavenPublication>("gpr") {
            from(components["java"])
        }
    }
}