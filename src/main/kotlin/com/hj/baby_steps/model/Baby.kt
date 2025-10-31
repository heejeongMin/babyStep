package com.hj.baby_steps.model

import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Table(name = "babies")
@Entity
class Baby (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    val uuid: UUID,
    val name: String,
    val birthdate: LocalDate,
    var isActive: Boolean = true,
    val createdOn: Instant,
    val updatedOn: Instant
) {


    companion object {
        fun create(name: String, birthdate: LocalDate): Baby {
            val now = Instant.now()
            return Baby(
                uuid = UUID.randomUUID(),
                name = name,
                birthdate = birthdate,
                createdOn = now,
                updatedOn = now
            )
        }
    }
}