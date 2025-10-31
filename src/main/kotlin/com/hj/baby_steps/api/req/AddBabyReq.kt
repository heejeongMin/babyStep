package com.hj.baby_steps.api.req

import java.time.LocalDate

class AddBabyReq (
    val name: String,
    val birthdate: LocalDate
)