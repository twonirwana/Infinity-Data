plugins {
    id("java")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.javalin:javalin:7.2.2")
    implementation("io.javalin:javalin-rendering-thymeleaf:7.2.2")
    implementation("io.javalin:javalin-micrometer:7.2.2")
    implementation("io.micrometer:micrometer-registry-prometheus:1.17.0")
    implementation("org.thymeleaf:thymeleaf:3.1.5.RELEASE")

    implementation(project(":data"))
    implementation(project(":app"))
    implementation("org.slf4j:slf4j-api:2.0.18")
    implementation("ch.qos.logback:logback-classic:1.5.36")
    compileOnly("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.projectlombok:lombok:1.18.46")
    implementation("com.google.guava:guava:33.6.0-jre")
    implementation("io.avaje:avaje-config:5.2")
    implementation("io.avaje:avaje-applog-slf4j:1.2")

    testImplementation(platform("org.junit:junit-bom:6.1.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("com.microsoft.playwright:playwright:1.60.0")
    testImplementation("com.github.romankh3:image-comparison:4.4.0")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation("org.testcontainers:testcontainers:2.0.5")
    testImplementation("org.testcontainers:junit-jupiter:1.21.4")

}

tasks.test {
    useJUnitPlatform()
    maxHeapSize = "2g"
}