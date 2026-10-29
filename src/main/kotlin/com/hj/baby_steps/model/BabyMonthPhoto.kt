package com.hj.baby_steps.model

import jakarta.persistence.*
import java.time.Instant

@Table(name = "baby_month_photo")
@Entity
class BabyMonthPhoto(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    val babyId: Long,
    val month: Int,
    var contentType: String,
    var data: ByteArray,
    var updatedOn: Instant = Instant.now()
)
