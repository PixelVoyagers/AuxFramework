plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":aux-context"))
}

dependencies {
    testApi(kotlin("test"))
}

tasks.withType<Test> {
    useJUnitPlatform()
}
