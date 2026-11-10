package com.hj.baby_steps.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date

@Component
class JwtUtil(
    @Value("\${app.jwt.secret}") private val secret: String,
    @Value("\${app.jwt.expiration-ms}") private val expirationMs: Long
) {
    init {
        require(secret.toByteArray().size >= 32) { "app.jwt.secret must be at least 32 bytes" }
    }

    private val key by lazy { Keys.hmacShaKeyFor(secret.toByteArray()) }

    fun generate(userId: Long, email: String): String =
        Jwts.builder()
            .subject(email)
            .claim("userId", userId)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + expirationMs))
            .signWith(key)
            .compact()

    fun validate(token: String): Boolean = runCatching {
        Jwts.parser().verifyWith(key).build().parseSignedClaims(token)
        true
    }.getOrDefault(false)

    /** True once less than half of the token's lifetime is left, so active users get a fresh token (rolling session). */
    fun shouldRefresh(token: String): Boolean = runCatching {
        val exp = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload.expiration.time
        exp - System.currentTimeMillis() < expirationMs / 2
    }.getOrDefault(false)

    fun getEmail(token: String): String =
        Jwts.parser().verifyWith(key).build()
            .parseSignedClaims(token).payload.subject
}
