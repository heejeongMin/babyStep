package com.hj.baby_steps.api

import com.hj.baby_steps.model.User
import com.hj.baby_steps.service.AuthService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

data class RegisterReq(
    val email: String,
    val password: String,
    val recoveryQuestion: String,
    val recoveryAnswer: String,
)
data class LoginReq(val email: String, val password: String)
data class TokenRes(val token: String)
data class MeRes(val email: String)
data class RecoveryQuestionRes(val question: String)
data class ResetPasswordReq(val email: String, val recoveryAnswer: String, val newPassword: String)

@RestController
@RequestMapping("/api/auth")
class AuthController(private val authService: AuthService) {

    @PostMapping("/register")
    fun register(@RequestBody req: RegisterReq) =
        ResponseEntity.ok(TokenRes(authService.register(req.email, req.password, req.recoveryQuestion, req.recoveryAnswer)))

    @PostMapping("/login")
    fun login(@RequestBody req: LoginReq) =
        ResponseEntity.ok(TokenRes(authService.login(req.email, req.password)))

    @GetMapping("/me")
    fun me(@AuthenticationPrincipal user: User): ResponseEntity<MeRes> =
        ResponseEntity.ok(MeRes(user.email))

    @GetMapping("/recovery-question")
    fun recoveryQuestion(@RequestParam email: String) =
        ResponseEntity.ok(RecoveryQuestionRes(authService.getRecoveryQuestion(email)))

    @PostMapping("/reset-password")
    fun resetPassword(@RequestBody req: ResetPasswordReq): ResponseEntity<Unit> {
        authService.resetPassword(req.email, req.recoveryAnswer, req.newPassword)
        return ResponseEntity.noContent().build()
    }
}
