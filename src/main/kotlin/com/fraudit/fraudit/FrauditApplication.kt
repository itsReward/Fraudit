package com.fraudit.fraudit

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.runApplication
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer
import java.util.Properties

@SpringBootApplication
class FrauditApplication : SpringBootServletInitializer() {
    override fun configure(application: SpringApplicationBuilder): SpringApplicationBuilder {
        return application.sources(FrauditApplication::class.java)
    }
}


fun main(args: Array<String>) {
    // Get PORT environment variable with default fallback to 8080
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080

    println("=====================================")
    println("Starting application with PORT: $port")
    println("All environment variables:")
    System.getenv().forEach { (key, value) ->
        println("$key: ${if (key.contains("PASSWORD", ignoreCase = true)) "***REDACTED***" else value}")
    }
    println("=====================================")

    // Set server port
    System.setProperty("server.port", port.toString())

    runApplication<FrauditApplication>(*args)
}