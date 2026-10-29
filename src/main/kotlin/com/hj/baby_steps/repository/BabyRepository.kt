package com.hj.baby_steps.repository

import com.hj.baby_steps.model.Baby
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BabyRepository: JpaRepository<Baby, Long> {
    fun findByUuidAndOwnerIdAndIsActiveIsTrue(uuid: UUID, ownerId: Long): Baby?
    fun findByOwnerIdAndIsActiveIsTrue(ownerId: Long): List<Baby>
}