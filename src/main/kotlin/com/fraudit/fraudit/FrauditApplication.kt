package com.fraudit.fraudit

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.runApplication
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer

@SpringBootApplication
class FrauditApplication : SpringBootServletInitializer() {
    override fun configure(application: SpringApplicationBuilder): SpringApplicationBuilder {
        return application.sources(FrauditApplication::class.java)
    }
}


fun main(args: Array<String>) {
    // Get PORT environment variable with default fallback to 8080
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080

    // Set the server port programmatically
    System.setProperty("server.port", port.toString())

    // Log the port being used
    println("Starting application on port $port")

    runApplication<FrauditApplication>(*args)
}
