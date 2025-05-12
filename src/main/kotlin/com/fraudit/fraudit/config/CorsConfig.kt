package com.fraudit.fraudit.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class CorsConfig {

    @Bean
    fun corsFilter(): CorsFilter {
        val source = UrlBasedCorsConfigurationSource()
        val config = CorsConfiguration()

        // Allow requests from your frontend domain
        config.allowedOrigins = listOf("https://fraudit-frontend.onrender.com")

        // Allow common HTTP methods
        config.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")

        // Allow all headers
        config.allowedHeaders = listOf("Authorization", "Content-Type", "Accept")

        // Allow cookies if necessary for your authentication flow
        config.allowCredentials = true

        // Apply this configuration to all paths
        source.registerCorsConfiguration("/**", config)

        return CorsFilter(source)
    }

    // Additional method to ensure global CORS configuration
    @Bean
    fun corsConfigurer(): WebMvcConfigurer {
        return object : WebMvcConfigurer {
            override fun addCorsMappings(registry: CorsRegistry) {
                registry.addMapping("/**")
                    .allowedOrigins("https://fraudit-frontend.onrender.com")
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    .allowedHeaders("Authorization", "Content-Type", "Accept")
                    .allowCredentials(true)
            }
        }
    }
}