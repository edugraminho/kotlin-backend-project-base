package com.projectbasename.application.dto.invitation

import com.projectbasename.domain.enums.invitation.InvitationStatus
import com.projectbasename.domain.enums.invitation.InvitationType
import java.time.LocalDateTime

data class InvitationResponse(
    val id: Long,
    val email: String,
    val companyId: Long,
    val companyName: String,
    val inviterId: Long,
    val inviterName: String,
    val invitationType: InvitationType,
    val status: InvitationStatus,
    val token: String,
    val expiresAt: LocalDateTime,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) 