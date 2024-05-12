import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.0.0-RC3"
    id("maven-publish")
}

allprojects {
    repositories {
        mavenCentral()
    }

    tasks.withType<KotlinCompile> {
        kotlin {
            jvmToolchain(21)
        }
    }

    tasks.withType<Jar> {
        manifest {
            attributes("Implementation-Version" to project.version)
        }
    }
}

group = "pixel.auxframework"
version = "1.0.0"

subprojects {
    group = rootProject.group
    version = rootProject.version
}

dependencies {
    subprojects.filter { it.name.startsWith("aux-") }.forEach { api(it) }
}

dependencies {
    testApi(kotlin("test"))
}

tasks.withType<Test> {
    useJUnitPlatform()
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components.getByName("kotlin"))
        artifact(tasks.kotlinSourcesJar)
    }
}
