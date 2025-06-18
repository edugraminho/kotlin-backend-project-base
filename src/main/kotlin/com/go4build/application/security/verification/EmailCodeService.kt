package com.base.application.security.verification

import org.springframework.stereotype.Service

/**
 * Serviço específico para códigos de email
 */
@Service
class EmailCodeService {

    /**
     * Envia código de verificação por email
     */
//    fun sendVerificationCode(email: String, userId: Long): EmailVerificationResponse {
//        val code = generateCode()
//
//        // TODO: Implementar persistência e envio real
//        println("📧 Email Code para $email: $code")
//
//        return EmailVerificationResponse(
//            success = true,
//            message = "Código email enviado",
//            expiresIn = 600
//        )
//    }
//
//    /**
//     * Verifica código de email
//     */
//    fun verifyCode(email: String, code: String): EmailVerificationResult {
//        // TODO: Implementar verificação real
//
//        return EmailVerificationResult(
//            success = code.length == 6,
//            message = if (code.length == 6) "Código válido" else "Código inválido"
//        )
//    }
//
//    private fun generateCode(): String {
//        return Random.nextInt(100000, 999999).toString()
//    }
}
