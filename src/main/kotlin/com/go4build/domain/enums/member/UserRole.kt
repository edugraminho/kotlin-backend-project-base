package com.base.domain.enums.member

/**
 * Papel/função do usuário dentro da empresa
 */
enum class UserRole {
    SUPER_USER,  // Super usuário (acesso total)
    OWNER,       // Proprietário da empresa
    ADMIN,       // Administrador
    MANAGER,     // Gerente
    EMPLOYEE,    // Funcionário
    CLIENT,      // Cliente
    SUPPLIER,    // Fornecedor
    GUEST        // Convidado (acesso limitado)
}