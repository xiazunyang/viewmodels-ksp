plugins {
    id("com.google.devtools.ksp")
    kotlin("jvm")
}

dependencies {
    compileOnly(fileTree("dir" to "libs", "include" to listOf("*.jar")))
    implementation(kotlin("stdlib"))
    ksp(project(":plugin"))
}

