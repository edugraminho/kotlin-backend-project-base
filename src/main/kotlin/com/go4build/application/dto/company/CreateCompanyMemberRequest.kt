package com.base.application.dto.company

import com.base.domain.enums.member.MemberType
import jakarta.validation.constraints.NotNull

data class CreateCompanyMemberRequest(
    @field:NotNull(message = "ID do usuário é obrigatório")
    val userId: Long,

    @field:NotNull(message = "Tipo do membro é obrigatório")
    val memberType: MemberType
) 