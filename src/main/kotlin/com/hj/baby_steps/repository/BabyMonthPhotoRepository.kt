package com.hj.baby_steps.repository

import com.hj.baby_steps.model.BabyMonthPhoto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface BabyMonthPhotoRepository : JpaRepository<BabyMonthPhoto, Long> {
    fun findByBabyIdAndMonth(babyId: Long, month: Int): BabyMonthPhoto?

    // Months only, so listing doesn't load every image's bytes
    @Query("select p.month from BabyMonthPhoto p where p.babyId = :babyId order by p.month")
    fun findMonthsByBabyId(babyId: Long): List<Int>

    @Modifying
    @Query("delete from BabyMonthPhoto p where p.babyId = :babyId and p.month = :month")
    fun deleteByBabyIdAndMonth(babyId: Long, month: Int)
}
