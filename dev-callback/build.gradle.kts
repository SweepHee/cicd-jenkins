import org.jooq.meta.jaxb.Logging

plugins {
    java
    kotlin("jvm") version("1.8.10")
    kotlin("plugin.allopen") version("1.8.10")
    kotlin("plugin.noarg") version("1.8.10")
    kotlin("plugin.spring") version("1.8.10")
    id("org.flywaydb.flyway") version("7.10.0")
    id("nu.studer.jooq") version("5.2.1")
    id("org.springframework.boot") version("2.7.14")
    id("io.spring.dependency-management") version("1.0.15.RELEASE")
}

tasks {
    bootJar {
        launchScript()
    }
}


apply {
    plugin("org.jetbrains.kotlin.plugin.spring")
    plugin("io.spring.dependency-management")
    plugin("org.springframework.boot")
    plugin("org.flywaydb.flyway")
    plugin("nu.studer.jooq")
    plugin("kotlin")
}

allOpen {
    annotation("javax.persistence.Entity")
    annotation("cg.tax.infra.core.NoArgs")
}

noArg {
    annotation("javax.persistence.Entity")
    annotation("cg.tax.infra.core.NoArgs")
}

tasks.withType<org.flywaydb.gradle.task.FlywayCleanTask>().configureEach {
    isEnabled = true
}

val jdbcUrl = "jdbc:postgresql://localhost:5432/sc2syrup?useUnicode=true&characterEncoding=utf8"
val jdbcDriver = "org.postgresql.Driver"
val jdbcUser = "sc2syrup"
val jdbcPassword = "#tlfjq6"

flyway {
    url = jdbcUrl
    user = jdbcUser
    password = jdbcPassword
}

jooq {
    version.set("3.15.4")
    edition.set(nu.studer.gradle.jooq.JooqEdition.OSS)

    configurations {
        create("main") {
            generateSchemaSourceOnCompilation.set(false)

            jooqConfiguration.apply {
                logging = Logging.INFO
                jdbc.apply {
                    driver = jdbcDriver
                    url = jdbcUrl
                    user = jdbcUser
                    password = jdbcPassword
                }

                generator.apply {
                    name = "org.jooq.codegen.DefaultGenerator"
                    database.apply {
                        name = "org.jooq.meta.postgres.PostgresDatabase"
                        inputSchema = "public"
                        excludes = """flyway_schema_history"""
                        schemaVersionProvider = """select max(flyway_schema_history.version) from flyway_schema_history"""
                    }

                    generate.apply {
                        isGlobalObjectReferences = true
                        isDeprecated = true
                        isRecords = true
                        isImmutablePojos = true
                        isRelations = true
                        isFluentSetters = true
                        isPojos = true
                    }
                    target.apply {
                        directory = "build/generated-src/jooq"
                        packageName = "sc2.syrup.web.jooq"
                    }
                    strategy.name = "org.jooq.codegen.DefaultGeneratorStrategy"

                }
            }
        }
    }

}


dependencies {
    implementation(project(":common"))
    implementation("org.springframework.security:spring-security-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.thymeleaf.extras:thymeleaf-extras-springsecurity5")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("com.github.vladimir-bukhtoyarov:bucket4j-core:7.1.0")
    implementation("org.springframework.boot:spring-boot-starter-jooq")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    implementation("org.apache.httpcomponents:httpclient:4.5.13")
    implementation("io.github.microutils:kotlin-logging:2.0.6")
    implementation("com.github.ua-parser:uap-java:1.5.4")
    implementation("org.jetbrains.kotlin:kotlin-allopen")
    implementation("org.jetbrains.kotlin:kotlin-noarg")
    implementation("org.apache.tika:tika-core:2.4.1")
    implementation("org.apache.poi:poi-ooxml:4.1.2")
    implementation("org.flywaydb:flyway-core")
    implementation("org.yaml:snakeyaml:2.1")
    implementation("org.postgresql:postgresql")
    implementation("org.apache.commons:commons-text:1.8")

    implementation ("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation ("com.fasterxml.jackson.core:jackson-databind")
    implementation ("com.slack.api:slack-api-client:1.18.0")

    implementation("com.slack.api:bolt:1.18.0")
    implementation("com.slack.api:bolt-servlet:1.18.0")
    implementation("com.slack.api:bolt-jetty:1.18.0")

    


    implementation("io.netty:netty-resolver-dns-native-macos:4.1.68.Final:osx-aarch_64")
    implementation ("com.fasterxml.jackson.module:jackson-module-kotlin")

    jooqGenerator("org.postgresql:postgresql:42.5.1")
    jooqGenerator("jakarta.xml.bind:jakarta.xml.bind-api:3.0.1")
}