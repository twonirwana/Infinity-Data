import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.gradleup.shadow") version "9.2.2"
    id("com.palantir.git-version") version "4.1.0"
    id("java")
}

group = "de.twonirwana"
val gitVersion: groovy.lang.Closure<String> by extra
version = gitVersion()

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

apply(plugin = "com.gradleup.shadow")

dependencies {
    implementation(project(":app"))
    implementation(project(":web"))
}

tasks {
    named<ShadowJar>("shadowJar") {
        archiveFileName.set("${archiveBaseName.get()}-${archiveVersion.get()}.jar")

        manifest {
            attributes(mapOf("Main-Class" to "de.twonirwana.infinity.WebApp"))
        }
    }
}

