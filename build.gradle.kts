plugins {
    id("org.springframework.boot") version "3.2.4"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "1.9.24"
    kotlin("plugin.spring") version "1.9.23"
    kotlin("plugin.jpa") version "1.9.23"
    kotlin("plugin.serialization") version "1.9.23"
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1"
}

group = "com.base"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_21

val mockkVersion = "1.13.10"
val testcontainersVersion = "1.19.7"

repositories {
    mavenCentral()
}
dependencies {
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("redis.clients:jedis")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")

    // OAuth2 Resource Server (JWT validation)
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

    // OAuth2 Client (Social login - Google/Apple/Microsoft)
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

    // Spring Security OAuth2 JOSE
    implementation("org.springframework.security:spring-security-oauth2-jose")

    // Google OAuth2 validation (se precisar validar tokens Google diretamente)
    implementation("com.google.auth:google-auth-library-oauth2-http:1.23.0")

    // Apple JWT validation (se precisar validar tokens Apple diretamente)
    implementation("com.nimbusds:nimbus-jose-jwt:9.37.3")

    // Kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json")

    // Database
    runtimeOnly("org.postgresql:postgresql")

    // Flyway
    implementation("org.flywaydb:flyway-core")

    // Hibernate Utils
    implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.7.3")

    // AWS SDK v2
    implementation(platform("software.amazon.awssdk:bom:2.29.34"))
    implementation("software.amazon.awssdk:sns")
    implementation("software.amazon.awssdk:s3")
    implementation("software.amazon.awssdk:regions")
    implementation("software.amazon.awssdk:auth")

    // Spring Cloud AWS
    implementation("io.awspring.cloud:spring-cloud-aws-starter-sqs:3.1.1")

    // IBM MQ
    implementation("com.ibm.mq:mq-jms-spring-boot-starter:3.2.4")

    // Google APIs
    implementation("com.google.api-client:google-api-client:2.4.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // Firebase
    implementation("com.google.firebase:firebase-admin:9.2.0")

    // OpenAPI/Swagger
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0")

    // Monitoring
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("com.newrelic.agent.java:newrelic-api:8.10.0")

    // Twilio
    implementation("com.twilio.sdk:twilio:10.1.2")

    // Reports
    implementation("net.sf.jasperreports:jasperreports:6.21.3")
    implementation("org.apache.poi:poi:5.2.5")
    implementation("org.apache.poi:poi-ooxml:5.2.5")
    implementation("org.xhtmlrenderer:flying-saucer-pdf:9.7.2")

    // Utilities
    implementation("org.apache.commons:commons-lang3")
    implementation("commons-validator:commons-validator:1.8.0")

    // Configuration Processor
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // Test Dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("com.ninja-squad:springmockk:4.0.2")
    testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
    testImplementation("org.testcontainers:testcontainers:$testcontainersVersion")

    // Mac compatibility
    val osName = System.getProperty("os.name").lowercase()
    if (osName.contains("mac")) {
        implementation("io.netty:netty-resolver-dns-native-macos:4.1.108.Final:osx-aarch_64")
    }
}

ktlint {
    version.set("1.0.1")
    android.set(false)
    ignoreFailures.set(false)
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
    }
    filter {
        exclude("**/generated/**")
        include("**/kotlin/**")
    }
}

