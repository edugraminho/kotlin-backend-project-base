package com.base.domain.service

import com.base.application.dto.company.member.CompanyMemberResponse
import com.base.domain.entity.CompanyEntity
import com.base.domain.entity.CompanyMemberEntity
import com.base.domain.entity.UserEntity
import com.base.domain.enums.member.MemberStatus
import com.base.domain.enums.member.MemberType
import com.base.domain.enums.member.UserRole
import com.base.domain.exception.BusinessException
import com.base.domain.exception.ExceptionType
import com.base.domain.repository.CompanyMemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CompanyMemberService(
    private val companyMemberRepository: CompanyMemberRepository,
    private val userService: UserService,
    private val companyService: CompanyService
) {

    /**
     * Adiciona membro à empresa
     */
    fun addMember(
        companyId: Long,
        userId: Long,
        memberType: MemberType,
        userRole: UserRole,
        invitedBy: UserEntity? = null
    ): CompanyMemberResponse {
        val company = companyService.findCompanyEntityById(companyId)
        val user = userService.findUserEntityById(userId)

        // Validar se já é membro
        if (companyMemberRepository.existsByUserIdAndCompanyId(userId, companyId)) {
            throw BusinessException(ExceptionType.USER_ALREADY_MEMBER)
        }

        // Validar se usuário é dono da empresa
        if (user.ownedCompany?.id == companyId) {
            throw BusinessException(ExceptionType.OWNER_CANNOT_BE_MEMBER)
        }

        val member = CompanyMemberEntity(
            user = user,
            company = company,
            memberType = memberType,
            userRole = userRole,
            invitedBy = invitedBy
        )

        val savedMember = companyMemberRepository.save(member)
        return mapToResponse(savedMember)
    }

    /**
     * Remove membro da empresa
     */
    fun removeMember(companyId: Long, userId: Long) {
        val member = companyMemberRepository.findByUserIdAndCompanyId(userId, companyId)
            ?: throw BusinessException(ExceptionType.MEMBER_NOT_FOUND)

        // Validar se não é o dono da empresa
        if (member.user.ownedCompany?.id == companyId) {
            throw BusinessException(ExceptionType.CANNOT_REMOVE_OWNER)
        }

        companyMemberRepository.delete(member)
    }

    /**
     * Atualiza role do membro
     */
    fun updateMemberRole(
        companyId: Long,
        userId: Long,
        newRole: UserRole
    ): CompanyMemberResponse {
        val member = companyMemberRepository.findByUserIdAndCompanyId(userId, companyId)
            ?: throw BusinessException(ExceptionType.MEMBER_NOT_FOUND)

        // Validar se não é o dono da empresa
        if (member.user.ownedCompany?.id == companyId) {
            throw BusinessException(ExceptionType.CANNOT_CHANGE_OWNER_ROLE)
        }

        member.userRole = newRole
        member.updateTimestamp()
        val updatedMember = companyMemberRepository.save(member)

        return mapToResponse(updatedMember)
    }

    /**
     * Busca membro por ID
     */
    fun findById(id: Long): CompanyMemberResponse {
        val member = companyMemberRepository.findById(id)
            .orElseThrow { BusinessException(ExceptionType.MEMBER_NOT_FOUND) }

        return mapToResponse(member)
    }

    /**
     * Lista membros da empresa
     */
    fun listCompanyMembers(companyId: Long): List<CompanyMemberResponse> {
        val members = companyMemberRepository.findByCompanyId(companyId)
        return members.map { mapToResponse(it) }
    }

    fun activateMember(companyId: Long, userId: Long): CompanyMemberResponse {
        val member = companyMemberRepository.findByUserIdAndCompanyId(userId, companyId)
            ?: throw BusinessException(ExceptionType.MEMBER_NOT_FOUND)

        member.activate()
        val updatedMember = companyMemberRepository.save(member)

        return mapToResponse(updatedMember)
    }

    fun deactivateMember(companyId: Long, userId: Long): CompanyMemberResponse {
        val member = companyMemberRepository.findByUserIdAndCompanyId(userId, companyId)
            ?: throw BusinessException(ExceptionType.MEMBER_NOT_FOUND)

        // Validar se não é o dono da empresa
        if (member.user.ownedCompany?.id == companyId) {
            throw BusinessException(ExceptionType.CANNOT_DEACTIVATE_OWNER)
        }

        member.deactivate()
        val updatedMember = companyMemberRepository.save(member)

        return mapToResponse(updatedMember)
    }

    fun blockMember(companyId: Long, userId: Long): CompanyMemberResponse {
        val member = companyMemberRepository.findByUserIdAndCompanyId(userId, companyId)
            ?: throw BusinessException(ExceptionType.MEMBER_NOT_FOUND)

        // Validar se não é o dono da empresa
        if (member.user.ownedCompany?.id == companyId) {
            throw BusinessException(ExceptionType.CANNOT_BLOCK_OWNER)
        }

        member.block()
        val updatedMember = companyMemberRepository.save(member)

        return mapToResponse(updatedMember)
    }

    /**
     * Mapeia entidade para DTO de resposta
     */
    private fun mapToResponse(member: CompanyMemberEntity): CompanyMemberResponse {
        return CompanyMemberResponse(
            id = member.id!!,
            companyId = member.company.id!!,
            companyName = member.company.name,
            userId = member.user.id!!,
            userName = member.user.name,
            userEmail = member.user.email,
            memberType = member.memberType,
            userRole = member.userRole,
            status = member.status,
            invitedBy = member.invitedBy?.id,
            joinedAt = member.joinedAt,
            createdAt = member.createdAt,
            updatedAt = member.updatedAt
        )
    }
} 