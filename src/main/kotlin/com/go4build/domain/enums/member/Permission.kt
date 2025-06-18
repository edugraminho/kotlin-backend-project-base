package com.base.domain.enums.member

/**
 * Permissões específicas no sistema
 */
enum class Permission {
    // Gestão de usuários
    MANAGE_USERS,
    INVITE_USERS,
    REMOVE_USERS,

    // Gestão de projetos
    CREATE_PROJECTS,
    EDIT_PROJECTS,
    DELETE_PROJECTS,
    VIEW_PROJECTS,

    // Gestão financeira
    VIEW_FINANCIAL,
    MANAGE_FINANCIAL,

    // Gestão de empresa
    MANAGE_COMPANY,
    MANAGE_SETTINGS,

    // Relatórios
    VIEW_REPORTS,
    EXPORT_REPORTS,

    // Super User - Permissões especiais
    SUPER_ACCESS,           // Acesso total ao sistema
    BYPASS_VALIDATIONS,     // Pular validações de negócio
    VIEW_ALL_COMPANIES,     // Ver todas as empresas
    MANAGE_ANY_COMPANY,     // Gerenciar qualquer empresa
    SYSTEM_ADMINISTRATION   // Administração do sistema
}