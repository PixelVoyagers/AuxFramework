rootProject.name = "aux-framework"

for (directory in rootProject.projectDir.listFiles()!!) {
    if (!directory.isDirectory) continue
    if (!directory.name.startsWith("aux-")) continue
    if (directory.listFiles()?.firstOrNull { it.name == "build.gradle.kts" || it.name == "build.gradle" } != null) {
        include(directory.name)
    }
}
