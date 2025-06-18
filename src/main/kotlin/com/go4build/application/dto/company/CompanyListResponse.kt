package com.base.application.dto.company

import org.springframework.data.domain.Page

/**
 * DTO de resposta para lista paginada de empresas
 */
data class CompanyListResponse(
    val companies: List<CompanyResponse>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int,
    val size: Int
) {
    companion object {
        fun from(page: Page<CompanyResponse>): CompanyListResponse {
            return CompanyListResponse(
                companies = page.content,
                totalElements = page.totalElements,
                totalPages = page.totalPages,
                currentPage = page.number,
                size = page.size
            )
        }
    }
}

