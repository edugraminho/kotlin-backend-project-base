package com.projectbasename.infrastructure.controller

import com.projectbasename.application.dto.payment.SubscriptionResponse
import com.projectbasename.application.dto.payment.UserSubscriptionStatusResponse
import com.projectbasename.application.mapper.SubscriptionMapper
import com.projectbasename.application.util.SecurityUtils
import com.projectbasename.application.security.service.PermissionService
import com.projectbasename.domain.service.SubscriptionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

/**
 * Controller para gerenciamento e consulta de assinaturas
 * Segue as regras de permissão centralizadas do PermissionService
 */
@RestController
@RequestMapping("/v1/subscriptions")
@Tag(name = "Assinaturas", description = "Endpoints para consulta e gerenciamento de assinaturas")
@SecurityRequirement(name = "bearerAuth")
class SubscriptionController(
    private val subscriptionService: SubscriptionService,
    private val permissionService: PermissionService
) {

    @Operation(
        summary = "Status de assinatura do usuário",
        description = "Retorna informações completas sobre as assinaturas do usuário logado"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Status retornado com sucesso"),
        ApiResponse(responseCode = "401", description = "Usuário não autenticado"),
        ApiResponse(responseCode = "403", description = "Acesso negado")
    ])
    @GetMapping("/my-status")
    @PreAuthorize("isAuthenticated()")
    fun getMySubscriptionStatus(): ResponseEntity<UserSubscriptionStatusResponse> {
        val userId = SecurityUtils.getCurrentUserId()
        
        val hasActiveSubscription = subscriptionService.hasActiveSubscription(userId)
        val activeSubscriptionCount = subscriptionService.findActiveSubscriptionsByUserId(userId).size.toLong()
        val currentSubscription = subscriptionService.findLatestActiveSubscription(userId)
        val allSubscriptions = subscriptionService.findActiveSubscriptionsByUserId(userId)
        
        val response = SubscriptionMapper.toUserSubscriptionStatus(
            hasActiveSubscription = hasActiveSubscription,
            activeSubscriptionCount = activeSubscriptionCount,
            currentSubscription = currentSubscription,
            allSubscriptions = allSubscriptions
        )
        
        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "Detalhes de uma assinatura",
        description = "Retorna detalhes completos de uma assinatura específica (apenas para o proprietário ou admin)"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Assinatura encontrada"),
        ApiResponse(responseCode = "404", description = "Assinatura não encontrada"),
        ApiResponse(responseCode = "403", description = "Acesso negado")
    ])
    @GetMapping("/{subscriptionId}")
    @PreAuthorize("isAuthenticated()")
    fun getSubscriptionDetails(
        @PathVariable subscriptionId: Long,
        authentication: Authentication
    ): ResponseEntity<SubscriptionResponse> {
        val userId = SecurityUtils.getCurrentUserId()
        val subscription = subscriptionService.findById(subscriptionId)
        
        // ✅ Validação usando PermissionService conforme regras do projeto
        if (subscription.user.id != userId && !permissionService.canManageSubscriptions(authentication, subscription.company?.id)) {
            return ResponseEntity.status(403).build()
        }
        
        val response = SubscriptionMapper.toResponse(subscription)
        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "Cancelar assinatura",
        description = "Cancela uma assinatura do usuário (apenas o proprietário ou admin pode cancelar)"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Assinatura cancelada com sucesso"),
        ApiResponse(responseCode = "404", description = "Assinatura não encontrada"),
        ApiResponse(responseCode = "403", description = "Acesso negado"),
        ApiResponse(responseCode = "400", description = "Assinatura não pode ser cancelada")
    ])
    @PutMapping("/{subscriptionId}/cancel")
    @PreAuthorize("isAuthenticated()")
    fun cancelSubscription(
        @PathVariable subscriptionId: Long,
        authentication: Authentication
    ): ResponseEntity<SubscriptionResponse> {
        val userId = SecurityUtils.getCurrentUserId()
        val subscription = subscriptionService.findById(subscriptionId)
        
        // ✅ Validação usando PermissionService conforme regras do projeto
        if (subscription.user.id != userId && !permissionService.canManageSubscriptions(authentication, subscription.company?.id)) {
            return ResponseEntity.status(403).build()
        }
        
        val canceledSubscription = subscriptionService.cancelSubscription(subscriptionId)
        val response = SubscriptionMapper.toResponse(canceledSubscription)
        
        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "Reativar assinatura",
        description = "Reativa uma assinatura cancelada (apenas o proprietário ou admin pode reativar)"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Assinatura reativada com sucesso"),
        ApiResponse(responseCode = "404", description = "Assinatura não encontrada"),
        ApiResponse(responseCode = "403", description = "Acesso negado"),
        ApiResponse(responseCode = "400", description = "Assinatura não pode ser reativada")
    ])
    @PutMapping("/{subscriptionId}/reactivate")
    @PreAuthorize("isAuthenticated()")
    fun reactivateSubscription(
        @PathVariable subscriptionId: Long,
        authentication: Authentication
    ): ResponseEntity<SubscriptionResponse> {
        val userId = SecurityUtils.getCurrentUserId()
        val subscription = subscriptionService.findById(subscriptionId)
        
        // ✅ Validação usando PermissionService conforme regras do projeto
        if (subscription.user.id != userId && !permissionService.canManageSubscriptions(authentication, subscription.company?.id)) {
            return ResponseEntity.status(403).build()
        }
        
        val reactivatedSubscription = subscriptionService.reactivateSubscription(subscriptionId)
        val response = SubscriptionMapper.toResponse(reactivatedSubscription)
        
        return ResponseEntity.ok(response)
    }
} 