plugins {
    id("java")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":data"))

    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("ch.qos.logback:logback-classic:1.5.29")
    implementation("org.apache.commons:commons-csv:1.14.1")
    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")
    implementation("com.google.guava:guava:33.5.0-jre")
    implementation("org.thymeleaf:thymeleaf:3.1.3.RELEASE")

    implementation("com.twelvemonkeys.imageio:imageio-core:3.13.0")
    implementation("com.twelvemonkeys.imageio:imageio-metadata:3.13.0")
    implementation("com.twelvemonkeys.imageio:imageio-webp:3.13.0")

    testImplementation(project(":data:core"))
    testImplementation(platform("org.junit:junit-bom:6.0.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:3.27.7")
}

tasks.test {
    useJUnitPlatform()
}
