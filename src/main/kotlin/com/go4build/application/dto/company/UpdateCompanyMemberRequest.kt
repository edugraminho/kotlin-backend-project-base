package com.base.application.dto.company

import com.base.domain.enums.member.MemberStatus
import com.base.domain.enums.member.MemberType
import jakarta.validation.constraints.NotNull

data class UpdateCompanyMemberRequest(
    @field:NotNull(message = "Tipo do membro é obrigatório")
    val memberType: MemberType? = null,

    @field:NotNull(message = "Status do membro é obrigatório")
    val status: MemberStatus? = null
) 