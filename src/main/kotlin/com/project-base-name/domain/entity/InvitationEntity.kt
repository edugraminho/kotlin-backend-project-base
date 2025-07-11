package com.projectbasename.domain.entity

import com.projectbasename.domain.enums.invitation.InvitationStatus
import com.projectbasename.domain.enums.invitation.InvitationType
import jakarta.persistence.*
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime

@Entity
@Table(name = "invitations")
class InvitationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    var email: String,

    @Column(name = "company_id", nullable = false)
    val companyId: Long,

    @Column(name = "inviter_id", nullable = false)
    val inviterId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "invitation_type", nullable = false)
    val invitationType: InvitationType,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: InvitationStatus = InvitationStatus.PENDING,

    @Column(name = "token", nullable = false, unique = true)
    val token: String,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: LocalDateTime,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun updateTimestamp() {
        updatedAt = LocalDateTime.now()
    }
} 