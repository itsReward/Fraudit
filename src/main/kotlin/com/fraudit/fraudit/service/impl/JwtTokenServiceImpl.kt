package com.fraudit.fraudit.service.impl

import com.fraudit.fraudit.domain.entity.User
import com.fraudit.fraudit.service.JwtTokenService
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*
import javax.crypto.SecretKey

@Service
class JwtTokenServiceImpl(
    @Value("\${jwt.secret:frauditDefaultSecretKey12345678901234567890}")
    private val jwtSecret: String,

    @Value("\${jwt.expiration:86400000}") // 24 hours in milliseconds
    private val jwtExpiration: Long,

    @Value("\${jwt.refresh-expiration:604800000}") // 7 days in milliseconds
    private val refreshExpiration: Long
) : JwtTokenService {

    private val secretKey: SecretKey by lazy {
        // Generate a secure key from the JWT secret
        Keys.hmacShaKeyFor(jwtSecret.toByteArray())
    }

    override fun generateAccessToken(user: User): String {
        val now = Date()
        val expiryDate = Date(now.time + jwtExpiration)

        return Jwts.builder()
            .setSubject(user.id.toString())
            .claim("username", user.username)
            .claim("email", user.email)
            .claim("role", user.role.name)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(secretKey)
            .compact()
    }

    override fun generateRefreshToken(user: User): String {
        val now = Date()
        val expiryDate = Date(now.time + refreshExpiration)

        return Jwts.builder()
            .setSubject(user.id.toString())
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(secretKey)
            .compact()
    }

    override fun validateToken(token: String): Boolean {
        try {
            Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
            return true
        } catch (e: Exception) {
            // Token validation failed
            return false
        }
    }

    override fun getUserIdFromToken(token: String): UUID? {
        try {
            val claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .body

            return UUID.fromString(claims.subject)
        } catch (e: Exception) {
            return null
        }
    }

    override fun getExpirationFromToken(token: String): Date? {
        try {
            val claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .body

            return claims.expiration
        } catch (e: Exception) {
            return null
        }
    }

    override fun getAllClaimsFromToken(token: String): Claims? {
        try {
            return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .body
        } catch (e: Exception) {
            return null
        }
    }
}