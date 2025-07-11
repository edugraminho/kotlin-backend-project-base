package com.projectbasename.application.dto.user

import com.projectbasename.application.dto.company.CompanyResponse

/**
 * Response do complete profile
 */
data class ProfileResponse(
    val user: UserResponse,
    val company: CompanyResponse? = null,
    val isComplete: Boolean,
    val needsCompany: Boolean = false
) 