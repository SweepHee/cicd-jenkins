plugins {
    java
    kotlin("jvm") version("1.8.10")
    kotlin("plugin.allopen") version("1.8.10")
    kotlin("plugin.noarg") version("1.8.10")
    kotlin("plugin.spring") version("1.8.10") apply(false)
    id("org.flywaydb.flyway") version("7.10.0") apply(false)
    id("org.jmailen.kotlinter") version("3.3.0") apply(false)
    id("org.springframework.boot") version("2.7.14") apply(false)
    id("io.spring.dependency-management") version("1.0.15.RELEASE") apply(false)
}



allprojects {
    group = "sc2.syrup"
    version = "0.0.1-SNAPSHOT"

    apply(plugin = "kotlin")

    kotlin {
        sourceSets.all {
            languageSettings {
                languageVersion = "1.9"
            }
        }
    }

    tasks.withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-opt-in=kotlin.io.path.ExperimentalPathApi", "-Xallow-result-return-type")
            languageVersion = "1.9"
            jvmTarget = "11"
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    repositories {
        mavenCentral()
    }
}

subprojects {

    apply {
        plugin("org.jetbrains.kotlin.plugin.spring")
        plugin("io.spring.dependency-management")
        plugin("org.springframework.boot")
        plugin("org.jmailen.kotlinter")
        plugin("org.flywaydb.flyway")
        plugin("kotlin")
    }

    tasks.withType<org.flywaydb.gradle.task.FlywayCleanTask>().configureEach {
        isEnabled = false
    }

    configure<org.jmailen.gradle.kotlinter.KotlinterExtension> {
        ignoreFailures = true
        reporters = arrayOf("plain", "checkstyle")
        disabledRules = arrayOf("no-wildcard-imports", "filename", "import-ordering", "string-template")
    }


    dependencies {
        implementation(kotlin("stdlib"))
        implementation(kotlin("reflect"))
        implementation("org.springframework.boot:spring-boot-starter-web")
        testImplementation("org.springframework.boot:spring-boot-starter-test")
    }
}