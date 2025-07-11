package com.projectbasename.infrastructure.controller

import com.projectbasename.application.dto.payment.SubscriptionListResponse
import com.projectbasename.application.mapper.SubscriptionMapper
import com.projectbasename.application.security.service.PermissionService
import com.projectbasename.domain.enums.payment.PaymentProvider
import com.projectbasename.domain.service.SubscriptionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

/**
 * Controller administrativo para gerenciamento de assinaturas
 * OWNER e ADMIN podem acessar estes endpoints para suas empresas
 * SUPER_USER pode acessar todos os endpoints
 */
@RestController
@RequestMapping("/v1/admin/subscriptions")
@Tag(name = "Admin - Assinaturas", description = "Endpoints administrativos para gerenciamento de assinaturas")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("@permissionService.canAccessSystemAdmin(authentication)")
class AdminPaymentController(
    private val subscriptionService: SubscriptionService,
    private val permissionService: PermissionService
) {

    @Operation(
        summary = "Processar assinaturas expiradas",
        description = "Executa o processo de expiração de assinaturas vencidas (job administrativo)"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Processamento executado com sucesso"),
        ApiResponse(responseCode = "403", description = "Acesso negado - requer role ADMIN ou OWNER")
    ])
    @PostMapping("/process-expired")
    fun processExpiredSubscriptions(): ResponseEntity<Map<String, Any>> {
        subscriptionService.processExpiredSubscriptions()
        
        return ResponseEntity.ok(mapOf<String, Any>(
            "success" to true,
            "message" to "Processamento de assinaturas expiradas executado",
            "timestamp" to System.currentTimeMillis()
        ))
    }

    @Operation(
        summary = "Buscar assinaturas por usuário",
        description = "Lista todas as assinaturas de um usuário específico (admin/owner only)"
    )
    @GetMapping("/user/{userId}")
    fun getUserSubscriptions(@PathVariable userId: Long): ResponseEntity<List<SubscriptionListResponse>> {
        val subscriptions = subscriptionService.findActiveSubscriptionsByUserId(userId)
        val response = SubscriptionMapper.toListResponses(subscriptions)
        
        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "Estatísticas de assinaturas",
        description = "Retorna estatísticas gerais do sistema de assinaturas"
    )
    @GetMapping("/stats")
    fun getSubscriptionStats(): ResponseEntity<Map<String, Any>> {
        // TODO: Implementar estatísticas de assinaturas
        val stats = mapOf<String, Any>(
            "totalSubscriptions" to 0,
            "activeSubscriptions" to 0,
            "expiredSubscriptions" to 0,
            "timestamp" to System.currentTimeMillis()
        )
        
        return ResponseEntity.ok(stats)
    }
} 