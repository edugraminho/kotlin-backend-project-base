package com.base.domain.entity

import com.base.domain.enums.company.CompanyStatus
import com.base.domain.enums.company.CompanyType
import jakarta.persistence.*
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

@Entity
@Table(name = "companies")
class CompanyEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres")
    var name: String,

    @NotBlank(message = "Documento é obrigatório")
    @Size(min = 11, max = 14, message = "Documento deve ser CPF ou CNPJ válido")
    @Column(unique = true)
    var document: String,

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    var email: String,

    @NotBlank(message = "Telefone é obrigatório")
    @Size(min = 10, max = 20, message = "Telefone deve ter entre 10 e 20 caracteres")
    var phone: String,

    var address: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "company_type", nullable = false)
    var companyType: CompanyType = CompanyType.PERSONAL,

    // Relacionamento 1:1 com o dono da empresa
    @OneToOne
    @JoinColumn(name = "owner_id")
    val owner: UserEntity,

    var activePlanId: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: CompanyStatus = CompanyStatus.ACTIVE,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    // Relacionamento 1:N com os membros da empresa
    @OneToMany(mappedBy = "company")
    val members: MutableSet<CompanyMemberEntity> = mutableSetOf()
) {
    fun updateTimestamp() {
        updatedAt = LocalDateTime.now()
    }
} 