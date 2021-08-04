val kspVersion: String by project

plugins {
    kotlin("jvm")
    id("maven-publish")
}

group = "cn.numeron"
version = "1.0.0-alpha1"

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.squareup:kotlinpoet:1.9.0")
    implementation("com.google.devtools.ksp:symbol-processing-api:$kspVersion")
}

publishing {
    publications {
        val mavenJava by creating(MavenPublication::class) {
            from(components["java"])
        }
    }
}