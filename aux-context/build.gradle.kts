plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":aux-core"))
}

dependencies {
    testApi(kotlin("test"))
}

tasks.withType<Test> {
    useJUnitPlatform()
}
