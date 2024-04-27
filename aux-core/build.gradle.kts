plugins {
    kotlin("jvm")
}

dependencies {
    testApi(kotlin("test"))
}

dependencies {
    api(kotlin("reflect"))

    api("commons-io:commons-io:2.16.1")
    api("com.google.guava:guava:33.1.0-jre")
    api("org.reflections:reflections:0.10.2")

    runtimeOnly("ch.qos.logback:logback-classic:1.5.6")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
