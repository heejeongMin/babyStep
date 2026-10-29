package com.hj.baby_steps.security

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

/** 429 that tells the client how long to wait, via the standard Retry-After header. */
class TooManyAttemptsException(private val retryAfterSeconds: Long) :
    ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many attempts, try again later") {

    override fun getHeaders(): HttpHeaders =
        HttpHeaders().apply { set(HttpHeaders.RETRY_AFTER, retryAfterSeconds.toString()) }
}
