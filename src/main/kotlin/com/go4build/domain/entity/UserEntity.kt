package com.base.domain.entity

import com.base.domain.enums.user.UserStatus
import jakarta.persistence.*
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

@Entity
@Table(name = "users")
class UserEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres")
    var name: String,

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    @Column(unique = true)
    var email: String,

    @NotBlank(message = "Telefone é obrigatório")
    @Size(min = 10, max = 20, message = "Telefone deve ter entre 10 e 20 caracteres")
    var phone: String,

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
    var password: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: UserStatus = UserStatus.PENDING,

    var profileImageUrl: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    // Relacionamento 1:1 com a empresa que o usuário é dono
    @OneToOne(mappedBy = "owner")
    var ownedCompany: CompanyEntity? = null,

    // Relacionamento 1:N com as empresas onde é membro
    @OneToMany(mappedBy = "user")
    val companyMemberships: MutableSet<CompanyMemberEntity> = mutableSetOf(),
) {
    fun updateTimestamp() {
        updatedAt = LocalDateTime.now()
    }
} 