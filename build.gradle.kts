import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
plugins {
    id("com.gradleup.shadow") version "9.2.2"
    id("java")
}

group = "de.twonirwana"
version = "0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.slf4j:slf4j-api:2.0.13")
    implementation("ch.qos.logback:logback-classic:1.5.18")
    implementation("org.apache.commons:commons-text:1.14.0")
    implementation("org.apache.commons:commons-csv:1.8")
    implementation("com.fasterxml.jackson.core:jackson-core:2.15.0")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.11.3")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.4.2")
    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")
    implementation("com.google.guava:guava:33.4.8-jre")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

apply(plugin = "com.gradleup.shadow")

dependencies {
    implementation(project(":app"))
}

tasks {
    named<ShadowJar>("shadowJar") {
        manifest {
            attributes(mapOf("Main-Class" to "de.twonirwana.infinity.ExportArmyCode"))
        }
    }
}

