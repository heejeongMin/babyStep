//package com.hj.baby_steps.model
//
//import jakarta.persistence.*
//
//@Table(name = "checkList")
//@Entity
//class CheckList (
//    val id: Long?,
//    @ManyToOne
//    @JoinColumn(name="month_info_id", nullable=false)
//    val monthInfo: MonthInfo,
//    val checkListId: String,
//    val title: String,
//    val description: String,
//    val completed: Boolean,
//    @Enumerated(EnumType.STRING)
//    val category: CheckListCategory
//)