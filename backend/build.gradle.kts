import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// ============================================================================
// プラグイン定義
// ============================================================================
plugins {
    java
    id("org.springframework.boot") version "3.3.5"
    id("io.spring.dependency-management") version "1.1.6"
    kotlin("jvm") version "1.9.24"
    kotlin("plugin.spring") version "1.9.24"
    kotlin("plugin.jpa") version "1.9.24"
    jacoco
}

// ============================================================================
// プロジェクト情報
// ============================================================================
group = "com.auratime"
version = "0.0.1-SNAPSHOT"

// ============================================================================
// Java設定
// ============================================================================
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

// ============================================================================
// コンフィグレーション設定
// ============================================================================
configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

// ============================================================================
// リポジトリ設定
// ============================================================================
repositories {
    mavenCentral()
}

// ============================================================================
// 依存関係定義
// ============================================================================
dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-batch")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    implementation("io.jsonwebtoken:jjwt-impl:0.12.6")
    implementation("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // Database
    runtimeOnly("org.postgresql:postgresql")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("com.h2database:h2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// ============================================================================
// タスク設定
// ============================================================================
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += listOf("-Xjsr305=strict")
        jvmTarget = "21"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    // JaCoCoによるカバレッジデータの収集を有効化
    finalizedBy(tasks.jacocoTestReport)

    // コンソール出力のエンコーディングをUTF-8に設定（PowerShellでの文字化け対策）
    systemProperty("file.encoding", "UTF-8")
    systemProperty("console.encoding", "UTF-8")
    // テスト出力のエンコーディングをUTF-8に設定
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

// ============================================================================
// JaCoCo設定
// ============================================================================
tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
    // レポートの出力先を設定
    // HTML: build/reports/jacoco/test/html/index.html
    // XML: build/reports/jacoco/test/jacocoTestReport.xml
}


