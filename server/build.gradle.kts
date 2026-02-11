plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ktor)
    application
}

group = "org.dals.project"
version = "1.0.0"
application {
    mainClass.set("org.dals.project.ApplicationKt")
    
    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

tasks.withType<JavaCompile> {
    targetCompatibility = "21"
    sourceCompatibility = "21"
}

dependencies {
    implementation(projects.shared)
    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    implementation(libs.ktor.serverContentNegotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)
    implementation("io.ktor:ktor-server-html-builder:3.3.1")
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.11.0")

    // CORS support
    implementation("io.ktor:ktor-server-cors:3.3.1")

    // Database dependencies
    implementation("org.postgresql:postgresql:42.7.2")
    implementation("com.h2database:h2:2.2.224")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.jetbrains.exposed:exposed-core:0.46.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.46.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.46.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.46.0")
    implementation("org.jetbrains.exposed:exposed-json:0.46.0")

    // Kotlinx DateTime
    implementation(libs.kotlinx.datetime)

    // Authentication and JWT
    implementation("io.ktor:ktor-server-auth:3.3.1")
    implementation("io.ktor:ktor-server-auth-jwt:3.3.1")

    // BCrypt for password hashing
    implementation("org.mindrot:jbcrypt:0.4")

    // PDF generation (OpenPDF)
    implementation("com.github.librepdf:openpdf:2.0.3")

    // BouncyCastle for PDF encryption
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")
    implementation("org.bouncycastle:bcpkix-jdk15on:1.70")

    // JavaMail for email notifications
    implementation("com.sun.mail:javax.mail:1.6.2")

    // Status pages for error handling
    implementation("io.ktor:ktor-server-status-pages:3.3.1")

    // Call logging
    implementation("io.ktor:ktor-server-call-logging:3.3.1")

    // Default headers
    implementation("io.ktor:ktor-server-default-headers:3.3.1")

    // HTTP Client for M-Pesa API calls
    implementation("io.ktor:ktor-client-core:3.3.1")
    implementation("io.ktor:ktor-client-cio:3.3.1")
    implementation("io.ktor:ktor-client-content-negotiation:3.3.1")
    implementation("io.ktor:ktor-client-logging:3.3.1")
    implementation("io.ktor:ktor-client-serialization:3.3.1")

    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.kotlin.testJunit)
}

tasks.withType<JavaExec> {
    if (System.getProperty("os.name").lowercase().contains("windows")) {
        // Handle spaces in path for Windows
        jvmArgs("-Dfile.encoding=UTF-8")
    }
}