package com.projectbasename.application.dto.invitation

import com.projectbasename.domain.enums.invitation.InvitationType
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class CreateInvitationRequest(
    @field:NotBlank(message = "Email é obrigatório")
    @field:Email(message = "Email inválido")
    val email: String,

    @field:NotNull(message = "ID da empresa é obrigatório")
    val companyId: Long,

    @field:NotNull(message = "Tipo de convite é obrigatório")
    val invitationType: InvitationType
) 