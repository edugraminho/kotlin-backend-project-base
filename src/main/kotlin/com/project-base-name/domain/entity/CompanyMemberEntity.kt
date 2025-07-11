package com.projectbasename.domain.entity

import com.projectbasename.domain.enums.member.MemberStatus
import com.projectbasename.domain.enums.member.MemberType
import com.projectbasename.domain.enums.member.UserRole
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "company_members",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_company_member_user",
            columnNames = ["company_id", "user_id"]
        )
    ],
    indexes = [
        Index(name = "idx_company_member_user", columnList = "user_id"),
        Index(name = "idx_company_member_company", columnList = "company_id"),
        Index(name = "idx_company_member_status", columnList = "status")
    ]
)
data class CompanyMemberEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    val company: CompanyEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: UserEntity,

    @Enumerated(EnumType.STRING)
    @Column(name = "member_type", nullable = false)
    val memberType: MemberType,

    @Enumerated(EnumType.STRING)
    @Column(name = "user_role", nullable = false)
    var userRole: UserRole,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: MemberStatus = MemberStatus.PENDING,

    // Usuário que fez o convite
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by")
    val invitedBy: UserEntity? = null,

    @Column(name = "joined_at")
    var joinedAt: LocalDateTime? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun updateTimestamp() {
        updatedAt = LocalDateTime.now()
    }

    fun activate() {
        this.status = MemberStatus.ACTIVE
        joinedAt = LocalDateTime.now()
        updateTimestamp()
    }

    fun deactivate() {
        this.status = MemberStatus.INACTIVE
        updateTimestamp()
    }

    fun block() {
        this.status = MemberStatus.BLOCKED
        updateTimestamp()
    }
} 