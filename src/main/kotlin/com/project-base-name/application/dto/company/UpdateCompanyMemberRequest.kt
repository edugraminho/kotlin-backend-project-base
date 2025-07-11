package com.projectbasename.application.dto.company

import com.projectbasename.domain.enums.member.MemberStatus
import com.projectbasename.domain.enums.member.MemberType
import jakarta.validation.constraints.NotNull

data class UpdateCompanyMemberRequest(
    @field:NotNull(message = "Tipo do membro é obrigatório")
    val memberType: MemberType? = null,

    @field:NotNull(message = "Status do membro é obrigatório")
    val status: MemberStatus? = null
) 