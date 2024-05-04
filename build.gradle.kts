import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.0.0-RC1"
    id("maven-publish")
}

allprojects {
    repositories {
        mavenCentral()
    }

    version = rootProject.version
    group = rootProject.group

    tasks.withType<KotlinCompile> {
        kotlin {
            jvmToolchain(21)
        }
    }
}

group = "pixel.auxframework"
version = "1.0.0"

dependencies {
    subprojects.filter { it.name.startsWith("aux-") }.forEach { api(it) }
}

dependencies {
    testApi(kotlin("test"))
}

tasks.withType<Test> {
    useJUnitPlatform()
}

configure<PublishingExtension> {
    publications.create<MavenPublication>("maven") {
        from(components.getByName("kotlin"))
        artifact(tasks.kotlinSourcesJar)
    }
}
