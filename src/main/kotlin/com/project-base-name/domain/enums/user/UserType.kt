package com.projectbasename.domain.enums.user

/**
 * Tipo do usuário baseado na origem do cadastro
 */
enum class UserType {
    OWNER,     // Pode criar e gerenciar empresa própria
    INVITED    // Membro convidado de empresas
}