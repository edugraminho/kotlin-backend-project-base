package com.projectbasename.application.security.service

import com.projectbasename.application.dto.auth.SocialLoginRequest
import com.projectbasename.application.dto.auth.SocialUserInfo
import com.projectbasename.domain.enums.user.SocialNetworkType
import com.projectbasename.domain.exception.BusinessException
import com.projectbasename.domain.exception.ExceptionType
// Removido import que não existe
import com.google.gson.Gson
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.security.SignatureException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.util.UriComponentsBuilder
import java.math.BigInteger
import java.net.URI
import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.RSAPublicKeySpec
import java.util.*

/**
 * Serviço para validação de tokens de login social
 * Baseado na implementação simples do projeto antigo
 */
@Service
class SocialLoginService {
    
    private val log = LoggerFactory.getLogger(this::class.java)
    
    /**
     * Valida token social e extrai informações do usuário
     */
    fun validateSocialToken(request: SocialLoginRequest): SocialUserInfo {
        return when (request.socialNetworkType) {
            SocialNetworkType.GOOGLE -> {
                validateGoogleToken(request.token)
                    ?: throw BusinessException(ExceptionType.INVALID_TOKEN, "Token do Google inválido")
            }
            SocialNetworkType.APPLE -> {
                validateAppleToken(request.token, request.receivedName, request.receivedEmail)
                    ?: throw BusinessException(ExceptionType.INVALID_TOKEN, "Token da Apple inválido")
            }
            SocialNetworkType.FACEBOOK -> {
                validateFacebookToken(request.token)
                    ?: throw BusinessException(ExceptionType.INVALID_TOKEN, "Token do Facebook inválido")
            }
        }
    }
    
    /**
     * Valida token do Google via API
     */
    private fun validateGoogleToken(token: String): SocialUserInfo? {
        val request: HttpRequest = HttpRequest.newBuilder()
            .uri(URI.create("https://www.googleapis.com/oauth2/v3/userinfo?access_token=$token"))
            .build()
            
        return try {
            val response: HttpResponse<String> = 
                HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString())
                
            val gson = Gson().fromJson(response.body(), HashMap::class.java)
            if (gson["email_verified"] != true) return null
            
            SocialUserInfo(
                email = gson["email"].toString(),
                name = gson["given_name"]?.toString(),
                completeName = gson["name"]?.toString(),
                providerId = gson["sub"].toString(),
                socialNetworkType = SocialNetworkType.GOOGLE
            )
        } catch (e: Exception) {
            log.error("Erro ao validar token do Google: ${e.message}", e)
            null
        }
    }
    
    /**
     * Valida token do Facebook via API
     */
    private fun validateFacebookToken(token: String): SocialUserInfo? {
        val uri = URI.create("https://graph.facebook.com/v12.0/me")
        val params: MultiValueMap<String, String> = LinkedMultiValueMap()
        params.add("fields", "id,name,first_name,last_name,middle_name,email")
        params.add("access_token", token)
        
        val uriWithParams = UriComponentsBuilder.fromUri(uri)
            .queryParams(params)
            .build()
            .toUri()
            
        val request: HttpRequest = HttpRequest.newBuilder()
            .uri(uriWithParams)
            .build()
            
        return try {
            val response: HttpResponse<String> = 
                HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString())
                
            val gson = Gson().fromJson(response.body(), HashMap::class.java)
            gson["error"]?.let { return null }
            
            SocialUserInfo(
                email = gson["email"].toString(),
                name = gson["first_name"]?.toString(),
                completeName = gson["name"]?.toString(),
                providerId = gson["id"].toString(),
                socialNetworkType = SocialNetworkType.FACEBOOK
            )
        } catch (e: Exception) {
            log.error("Erro ao validar token do Facebook: ${e.message}", e)
            null
        }
    }
    
    /**
     * Valida token da Apple via JWT com chaves públicas
     */
    private fun validateAppleToken(
        token: String, 
        receivedName: String?, 
        receivedEmail: String?
    ): SocialUserInfo? {
        val appleKeysUrl = URL("https://appleid.apple.com/auth/keys")
        val appleKeysJson = appleKeysUrl.readText()
        val appleKeys = Gson().fromJson(appleKeysJson, AppleKeysResponse::class.java)
        
        return appleKeys.keys.asSequence().mapNotNull { key ->
            val decodedN = Base64.getUrlDecoder().decode(key.n)
            val decodedE = Base64.getUrlDecoder().decode(key.e)
            val publicKeySpec = RSAPublicKeySpec(BigInteger(1, decodedN), BigInteger(1, decodedE))
            val keyFactory = KeyFactory.getInstance("RSA")
            val publicKey = keyFactory.generatePublic(publicKeySpec) as RSAPublicKey
            
            try {
                val claims = Jwts.parser()
                    .setSigningKey(publicKey)
                    .parseClaimsJws(token)
                    .body
                    
                val newEmail = receivedEmail ?: claims["email"]?.toString() ?: ""
                val completeNameSplitEmail = receivedName ?: newEmail.split("@")[0]
                
                val delimiters = arrayOf(".", "-", "_")
                val nameSplit = completeNameSplitEmail.split(*delimiters)
                
                SocialUserInfo(
                    email = newEmail,
                    name = cleanAndKeepLetters(nameSplit.firstOrNull()) 
                        ?: receivedName 
                        ?: completeNameSplitEmail,
                    completeName = cleanAndKeepLetters(completeNameSplitEmail),
                    providerId = claims["sub"].toString(),
                    socialNetworkType = SocialNetworkType.APPLE
                )
            } catch (e: SignatureException) {
                log.info("JWT signature error: ${e.message}")
                null
            } catch (e: ExpiredJwtException) {
                log.info("Expired JWT Token: ${e.message}")
                null
            } catch (e: MalformedJwtException) {
                log.info("Malformed JWT Token: ${e.message}")
                null
            } catch (e: Exception) {
                log.info("Error processing JWT token: ${e.message}")
                null
            }
        }.firstOrNull()
    }
    
    /**
     * Limpa string mantendo apenas letras
     */
    private fun cleanAndKeepLetters(input: String?): String {
        return input?.replace("[^a-zA-ZÀ-ÿ\\s]".toRegex(), "")?.trim() ?: ""
    }
}

/**
 * Resposta das chaves públicas da Apple
 */
private data class AppleKeysResponse(
    val keys: List<AppleKey>
)

/**
 * Chave pública da Apple
 */
private data class AppleKey(
    val kty: String,
    val kid: String,
    val use: String,
    val alg: String,
    val n: String,
    val e: String
) 