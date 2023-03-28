plugins {
    id("java")
    id("com.google.cloud.tools.jib") version "3.3.1"
}

repositories {
    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }
}

dependencies {
    val springBootVersion = "2.7.10"
    implementation("org.springframework.boot:spring-boot-starter-web:${springBootVersion}")
    implementation("org.springframework.boot:spring-boot-starter-validation:${springBootVersion}")
    implementation("org.springframework.boot:spring-boot-starter-actuator:${springBootVersion}")
    implementation("com.google.gerrit:gerrit-extension-api:3.4.4")
    implementation("org.sonarsource.sonarqube:sonar-ws:8.9.5.50698")
    implementation("org.apache.commons:commons-text:1.10.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testImplementation("org.springframework.boot:spring-boot-starter-test:${springBootVersion}")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
}