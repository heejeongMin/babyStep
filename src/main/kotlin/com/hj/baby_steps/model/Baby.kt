package com.hj.baby_steps.model

import jakarta.persistence.*
import org.hibernate.annotations.Check
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
    @OneToOne(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(
        name = "checklist_id",
        referencedColumnName = "id")
    var checklistProgress: ChecklistProgress? = null,
    var isActive: Boolean = true,
    var createdOn: Instant,
    var updatedOn: Instant
) {
    @PrePersist
    fun prePersist() {
        this.createdOn = Instant.now()
    }

    @PreUpdate
    fun preUpdate() {
        this.updatedOn = Instant.now()
    }

    fun updateCheckList(checklistId: String, completed: Boolean) {
        val checklistProgress = this.checklistProgress?: ChecklistProgress.create()
        if (completed) {
            checklistProgress.checklistItemId.add(checklistId)
        } else {
            checklistProgress.checklistItemId.remove(checklistId)
        }
    }

    companion object {
        fun create(name: String, birthdate: LocalDate): Baby {
            val now = Instant.now()
            val baby = Baby(
                uuid = UUID.randomUUID(),
                name = name,
                birthdate = birthdate,
                createdOn = now,
                updatedOn = now,
            )
            val checklistProgress = ChecklistProgress.create()
            baby.checklistProgress = checklistProgress
            return baby
        }
    }
}