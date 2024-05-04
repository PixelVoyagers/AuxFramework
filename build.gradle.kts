import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.0.0-RC1"
    id("maven-publish")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

allprojects {
    repositories {
        mavenCentral()
    }

    tasks.withType<KotlinCompile> {
        compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(java.targetCompatibility.toString()))
    }

    version = rootProject.version
    group = rootProject.group


}

group = "pixel.auxframework"
version = "1.0.0"

dependencies {
    api(project(":aux-context"))
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
