plugins {
    kotlin("jvm")
    id("maven-publish")
}

dependencies {
    testApi(kotlin("test"))
}

dependencies {
    api(kotlin("reflect"))

    api("commons-io:commons-io:2.16.1")
    api("com.google.guava:guava:33.1.0-jre")
    api("org.reflections:reflections:0.10.2")
    api("io.arrow-kt:arrow-core:1.2.4")
    api("io.arrow-kt:arrow-fx-coroutines:1.2.4")
    api("net.bytebuddy:byte-buddy:1.14.14")
    api("org.quartz-scheduler:quartz:2.5.0-rc1")

    implementation("ch.qos.logback:logback-classic:1.5.6")
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
