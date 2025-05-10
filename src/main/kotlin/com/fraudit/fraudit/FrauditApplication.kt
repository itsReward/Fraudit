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
    // Get PORT environment variable with default fallback to 8081
    val port = System.getenv("SERVER_PORT")?.toIntOrNull() ?: 8081

    println("=====================================")
    println("Starting Fraudit application on port: $port")
    println("Setting server.port system property to: $port")
    println("=====================================")

    // Force port configuration
    System.setProperty("server.port", port.toString())
    System.setProperty("spring.main.web-application-type", "servlet")

    // Start application
    runApplication<FrauditApplication>(*args) {
        setDefaultProperties(mapOf(
            "server.port" to port.toString(),
            "spring.main.web-application-type" to "servlet"
        ))
    }
}