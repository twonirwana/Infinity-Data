plugins {
    id("java-library")
}

repositories {
    mavenCentral()
}

dependencies {
    api(project(":data:api"))
    implementation(project(":data:core"))

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
