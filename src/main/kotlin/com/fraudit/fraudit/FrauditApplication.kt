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
    val springApplication = SpringApplication(FrauditApplication::class.java)
    val properties = Properties()

    // Set the port from environment variable
    val port = System.getenv("PORT") ?: "8080"
    properties.setProperty("server.port", port)
    properties.setProperty("server.address", "0.0.0.0")

    springApplication.setDefaultProperties(properties)
    springApplication.run(*args)
}
