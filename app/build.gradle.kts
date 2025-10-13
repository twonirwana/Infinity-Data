plugins {
    id("java")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":data"))

    implementation("org.slf4j:slf4j-api:2.0.13")
    implementation("ch.qos.logback:logback-classic:1.5.18")
    implementation("org.apache.commons:commons-csv:1.8")
    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")
    implementation("com.google.guava:guava:33.5.0-jre")
    implementation("org.thymeleaf:thymeleaf:3.1.3.RELEASE")
    implementation("com.twelvemonkeys.imageio:imageio-core:3.10.0")
    implementation("com.twelvemonkeys.imageio:imageio-metadata:3.10.0")
    implementation("com.twelvemonkeys.imageio:imageio-webp:3.10.0")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:3.27.6")
}

tasks.test {
    useJUnitPlatform()
}
