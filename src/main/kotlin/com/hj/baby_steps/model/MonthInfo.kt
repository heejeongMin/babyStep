//package com.hj.baby_steps.model
//
//import jakarta.persistence.*
//
//@Table(name = "monthInfos")
//@Entity
//class MonthInfo (
//    val id: Long?,
//    @Enumerated(EnumType.STRING)
//    val ageLabel: AgeLabel,
//    @OneToMany(mappedBy = "monthInfo")
//    val checkLists: List<CheckList>,
//    val milestonesAndTips: MilestonesAndTips
//)