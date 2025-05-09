package com.fraudit.fraudit.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class RootController {
    @GetMapping("/")
    fun root(): ResponseEntity<String> {
        return ResponseEntity.ok("FraudIt API is running")
    }
}