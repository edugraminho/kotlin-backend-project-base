package com.projectbasename.application.dto.user

import org.springframework.data.domain.Page

/**
 * DTO de resposta para lista paginada de usuários
 */
data class UserListResponse(
    val users: List<UserResponse>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int,
    val size: Int
) {
    companion object {
        fun from(page: Page<UserResponse>): UserListResponse {
            return UserListResponse(
                users = page.content,
                totalElements = page.totalElements,
                totalPages = page.totalPages,
                currentPage = page.number,
                size = page.size
            )
        }
    }
}