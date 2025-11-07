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
    implementation("io.micrometer:micrometer-registry-prometheus:1.16.0")
    implementation("org.thymeleaf:thymeleaf:3.1.3.RELEASE")

    implementation(project(":data"))
    implementation(project(":app"))
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("ch.qos.logback:logback-classic:1.5.20")
    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")
    implementation("com.google.guava:guava:33.5.0-jre")
    implementation("io.avaje:avaje-config:4.2")
    implementation("io.avaje:avaje-applog-slf4j:1.0")

    testImplementation(platform("org.junit:junit-bom:6.0.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}