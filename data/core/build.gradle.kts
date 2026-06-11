plugins {
    id("java")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":data:api"))

    implementation("org.slf4j:slf4j-api:2.0.18")
    implementation("ch.qos.logback:logback-classic:1.5.33")
    compileOnly("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.projectlombok:lombok:1.18.46")
    implementation("com.google.guava:guava:33.6.0-jre")
    implementation("com.google.code.gson:gson:2.14.0")
    implementation("io.micrometer:micrometer-core:1.16.5")
    implementation("io.avaje:avaje-config:5.1")
    implementation("io.avaje:avaje-applog-slf4j:1.2")
    implementation("org.apache.commons:commons-csv:1.14.1")

    implementation("tools.jackson.core:jackson-core:3.1.3")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.21")
    implementation("tools.jackson.core:jackson-databind:3.1.3")

    testImplementation(platform("org.junit:junit-bom:6.1.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:3.27.7")
}

tasks.test {
    useJUnitPlatform()
}
