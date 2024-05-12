plugins {
    kotlin("jvm")
    id("maven-publish")
}

dependencies {
    api(project(":aux-application"))

    api("io.ktor:ktor-server-core-jvm:3.0.0-beta-1")
    api("io.ktor:ktor-server-netty-jvm:3.0.0-beta-1")
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
