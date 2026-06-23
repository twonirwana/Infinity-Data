plugins {
    id("java")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":data:api"))

    implementation("org.slf4j:slf4j-api:2.0.18")
    implementation("ch.qos.logback:logback-classic:1.5.35")
    compileOnly("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.projectlombok:lombok:1.18.46")
    implementation("com.google.guava:guava:33.6.0-jre")
    implementation("com.google.code.gson:gson:2.14.0")
    implementation("io.micrometer:micrometer-core:1.17.0")
    implementation("io.avaje:avaje-config:5.2")
    implementation("io.avaje:avaje-applog-slf4j:1.2")
    implementation("org.apache.commons:commons-csv:1.14.1")

    implementation("tools.jackson.core:jackson-core:3.2.0")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.22")
    implementation("tools.jackson.core:jackson-databind:3.2.0")

    testImplementation(platform("org.junit:junit-bom:6.1.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:3.27.7")
}

tasks.test {
    useJUnitPlatform()
}
