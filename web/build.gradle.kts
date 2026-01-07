plugins {
    id("java")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.javalin:javalin:6.7.0")
    implementation("io.javalin:javalin-rendering:6.7.0")
    implementation("io.javalin:javalin-micrometer:6.7.0")
    implementation("io.micrometer:micrometer-registry-prometheus:1.16.1")
    implementation("org.thymeleaf:thymeleaf:3.1.3.RELEASE")

    implementation(project(":data"))
    implementation(project(":app"))
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("ch.qos.logback:logback-classic:1.5.24")
    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")
    implementation("com.google.guava:guava:33.5.0-jre")
    implementation("io.avaje:avaje-config:5.0")
    implementation("io.avaje:avaje-applog-slf4j:1.0")

    testImplementation(platform("org.junit:junit-bom:6.0.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("com.microsoft.playwright:playwright:1.57.0")
    testImplementation("com.github.romankh3:image-comparison:4.4.0")
    testImplementation("org.assertj:assertj-core:3.27.6")

}

tasks.test {
    useJUnitPlatform()
    maxHeapSize = "2g"
}