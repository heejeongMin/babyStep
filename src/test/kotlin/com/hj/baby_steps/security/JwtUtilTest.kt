package com.hj.baby_steps.security

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class JwtUtilTest {
    private val secret = "unit-test-secret-0123456789-0123456789"

    @Test
    fun `fresh token is not refreshed`() {
        val jwt = JwtUtil(secret, 60_000)
        assertFalse(jwt.shouldRefresh(jwt.generate(1, "a@b.c")))
    }

    @Test
    fun `token past half its life is refreshed`() {
        val jwt = JwtUtil(secret, 2_000)
        val token = jwt.generate(1, "a@b.c")
        Thread.sleep(1_200)
        assertTrue(jwt.validate(token))
        assertTrue(jwt.shouldRefresh(token))
    }

    @Test
    fun `invalid token is never refreshed`() {
        assertFalse(JwtUtil(secret, 60_000).shouldRefresh("garbage"))
    }
}
