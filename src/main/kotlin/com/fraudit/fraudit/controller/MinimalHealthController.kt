package com.fraudit.fraudit.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.info.BuildProperties
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.availability.AvailabilityChangeEvent
import org.springframework.boot.availability.LivenessState
import org.springframework.boot.availability.ReadinessState
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.Scheduled
import java.time.OffsetDateTime
import java.lang.management.ManagementFactory
import java.util.concurrent.atomic.AtomicInteger

@RestController
class MinimalHealthController(
    @Autowired(required = false) val buildProperties: BuildProperties?,
    @Autowired val publisher: ApplicationEventPublisher
) {

    @Value("\${spring.application.name:fraudit-application}")
    private val appName: String = "fraudit-application"

    private val startTime = OffsetDateTime.now()
    private val requestCounter = AtomicInteger(0)

    @GetMapping("/")
    fun home(): Map<String, Any> {
        requestCounter.incrementAndGet()
        val runtime = ManagementFactory.getRuntimeMXBean()

        return mapOf(
            "status" to "UP",
            "application" to appName,
            "timestamp" to OffsetDateTime.now().toString(),
            "uptime_ms" to runtime.uptime,
            "request_count" to requestCounter.get(),
            "build_info" to (buildProperties?.version ?: "unknown"),
            "memory" to mapOf(
                "free_memory" to Runtime.getRuntime().freeMemory(),
                "total_memory" to Runtime.getRuntime().totalMemory(),
                "max_memory" to Runtime.getRuntime().maxMemory()
            )
        )
    }

    @GetMapping("/health")
    fun health(): Map<String, Any> {
        val runtime = ManagementFactory.getRuntimeMXBean()

        return mapOf(
            "status" to "UP",
            "start_time" to startTime.toString(),
            "uptime_seconds" to runtime.uptime / 1000
        )
    }

    @GetMapping("/readiness")
    fun ready(): Map<String, String> {
        return mapOf("status" to "READY")
    }

    @GetMapping("/liveness")
    fun liveness(): Map<String, String> {
        return mapOf("status" to "ALIVE")
    }

    @GetMapping("/mark-ready")
    fun markReady(): Map<String, String> {
        AvailabilityChangeEvent.publish(publisher, this, ReadinessState.ACCEPTING_TRAFFIC)
        return mapOf("status" to "READY")
    }

    @GetMapping("/mark-not-ready")
    fun markNotReady(): Map<String, String> {
        AvailabilityChangeEvent.publish(publisher, this, ReadinessState.REFUSING_TRAFFIC)
        return mapOf("status" to "NOT_READY")
    }

    @Scheduled(fixedRate = 60000) // Every minute
    fun reportHealth() {
        val runtime = ManagementFactory.getRuntimeMXBean()
        println("=== HEALTH CHECK ===")
        println("Uptime: ${runtime.uptime / 1000} seconds")
        println("Free memory: ${Runtime.getRuntime().freeMemory() / (1024 * 1024)} MB")
        println("Total memory: ${Runtime.getRuntime().totalMemory() / (1024 * 1024)} MB")
        println("Max memory: ${Runtime.getRuntime().maxMemory() / (1024 * 1024)} MB")
        println("====================")
    }
}