package com.hj.baby_steps.security

import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/** In-memory brute-force guard: too many failed logins for one email locks it for a while. */
@Component
class LoginRateLimiter {
    private val maxFailures = 5
    private val window: Duration = Duration.ofMinutes(15)
    private val failures = ConcurrentHashMap<String, MutableList<Instant>>()

    fun isBlocked(key: String): Boolean = recent(key).size >= maxFailures

    /** Seconds until [key] is allowed again: when enough old failures have aged out of the window. 0 if not blocked. */
    fun retryAfterSeconds(key: String): Long {
        val recent = recent(key).sorted()
        if (recent.size < maxFailures) return 0
        val unblocksAt = recent[recent.size - maxFailures].plus(window)
        return Duration.between(Instant.now(), unblocksAt).seconds.coerceAtLeast(1)
    }

    fun recordFailure(key: String) {
        failures.compute(key) { _, list -> (list ?: mutableListOf()).also { it.add(Instant.now()) } }
    }

    fun reset(key: String) {
        failures.remove(key)
    }

    private fun recent(key: String): List<Instant> {
        val cutoff = Instant.now().minus(window)
        val list = failures[key] ?: return emptyList()
        synchronized(list) { list.removeIf { it.isBefore(cutoff) } }
        return list.toList()
    }
}
