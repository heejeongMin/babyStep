package com.hj.baby_steps.service

import com.hj.baby_steps.model.User
import com.hj.baby_steps.repository.UserRepository
import com.hj.baby_steps.security.JwtUtil
import com.hj.baby_steps.security.LoginRateLimiter
import com.hj.baby_steps.security.TooManyAttemptsException
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: BCryptPasswordEncoder,
    private val jwtUtil: JwtUtil,
    private val rateLimiter: LoginRateLimiter
) {
    @Transactional
    fun register(email: String, password: String, recoveryQuestion: String, recoveryAnswer: String): String {
        val normalized = normalizeEmail(email)
        validateEmail(normalized)
        validatePassword(password)
        if (recoveryQuestion.isBlank() || recoveryAnswer.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Recovery question and answer are required")
        }
        if (userRepository.existsByEmail(normalized)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Email already in use")
        }
        val user = userRepository.save(User(
            email = normalized,
            password = passwordEncoder.encode(password),
            recoveryQuestion = recoveryQuestion.trim(),
            recoveryAnswerHash = passwordEncoder.encode(normalizeAnswer(recoveryAnswer)),
        ))
        return jwtUtil.generate(user.id!!, user.email)
    }

    fun login(email: String, password: String): String {
        val normalized = normalizeEmail(email)
        if (rateLimiter.isBlocked(normalized)) {
            throw TooManyAttemptsException(rateLimiter.retryAfterSeconds(normalized))
        }
        val user = userRepository.findByEmail(normalized)
        if (user == null || !passwordEncoder.matches(password, user.password)) {
            rateLimiter.recordFailure(normalized)
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials")
        }
        rateLimiter.reset(normalized)
        return jwtUtil.generate(user.id!!, user.email)
    }

    fun getRecoveryQuestion(email: String): String =
        (userRepository.findByEmail(normalizeEmail(email))
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "No account with that email")).recoveryQuestion

    @Transactional
    fun resetPassword(email: String, recoveryAnswer: String, newPassword: String) {
        val normalized = normalizeEmail(email)
        if (rateLimiter.isBlocked("reset:$normalized")) {
            throw TooManyAttemptsException(rateLimiter.retryAfterSeconds("reset:$normalized"))
        }
        val user = userRepository.findByEmail(normalized)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "No account with that email")
        if (!passwordEncoder.matches(normalizeAnswer(recoveryAnswer), user.recoveryAnswerHash)) {
            rateLimiter.recordFailure("reset:$normalized")
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "That answer doesn't match")
        }
        validatePassword(newPassword)
        user.password = passwordEncoder.encode(newPassword)
        rateLimiter.reset("reset:$normalized")
    }

    private fun normalizeEmail(email: String) = email.trim().lowercase()

    // Case/whitespace-insensitive so a recovery answer isn't rejected over trivial formatting differences.
    private fun normalizeAnswer(answer: String) = answer.trim().lowercase()

    private fun validateEmail(email: String) {
        if (email.length > 254 || !EMAIL_REGEX.matches(email)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid email")
        }
    }

    // BCrypt ignores everything past 72 bytes, so reject longer passwords instead of silently truncating.
    private fun validatePassword(password: String) {
        if (password.length < 8 || password.toByteArray().size > 72) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be 8-72 characters")
        }
    }

    companion object {
        private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}
