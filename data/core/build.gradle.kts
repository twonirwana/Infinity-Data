plugins {
    id("java")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":data:api"))

    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("ch.qos.logback:logback-classic:1.5.32")
    compileOnly("org.projectlombok:lombok:1.18.44")
    annotationProcessor("org.projectlombok:lombok:1.18.44")
    implementation("com.google.guava:guava:33.5.0-jre")
    implementation("com.google.code.gson:gson:2.13.2")
    implementation("io.micrometer:micrometer-core:1.16.5")

    implementation("tools.jackson.core:jackson-core:3.1.2")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.21")
    implementation("tools.jackson.core:jackson-databind:3.1.2")

    testImplementation(platform("org.junit:junit-bom:6.0.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:3.27.7")
}

tasks.test {
    useJUnitPlatform()
}
