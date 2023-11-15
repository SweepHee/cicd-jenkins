package dev.example.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@SpringBootApplication
@EnableCaching
class App {
    companion object {
        const val appName = "dev"
    }
}

fun main(args: Array<String>) {
    runApplication<App>(*args)
}