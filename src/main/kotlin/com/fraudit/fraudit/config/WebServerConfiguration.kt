package com.fraudit.fraudit.config

import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class WebServerConfiguration {
    @Bean
    fun webServerFactoryCustomizer(): WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {
        return WebServerFactoryCustomizer { factory ->
            // Get PORT from environment variable with default to 8080
            val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
            factory.setPort(port)
        }
    }
}