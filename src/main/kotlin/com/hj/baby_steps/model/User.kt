package com.hj.baby_steps.model

import jakarta.persistence.*
import java.time.Instant

@Table(name = "users")
@Entity
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    val email: String,
    var password: String,
    val recoveryQuestion: String,
    val recoveryAnswerHash: String,
    val createdOn: Instant = Instant.now()
)
