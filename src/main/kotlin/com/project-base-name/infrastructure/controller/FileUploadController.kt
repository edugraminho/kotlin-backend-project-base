package com.projectbasename.infrastructure.controller

import com.projectbasename.application.security.service.JwtService
import com.projectbasename.infrastructure.integration.aws.dto.FileType
import com.projectbasename.infrastructure.integration.aws.dto.FileUploadResponse
import com.projectbasename.infrastructure.integration.aws.service.AwsS3Service
import com.projectbasename.domain.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

/**
 * Controller responsável pelo upload de arquivos para S3
 * Inclui upload de foto do perfil do usuário
 */
@RestController
@RequestMapping("/v1/files")
@Tag(name = "File Upload", description = "Endpoints para upload de arquivos")
@SecurityRequirement(name = "bearerAuth")
class FileUploadController(
    private val awsS3Service: AwsS3Service,
    private val userService: UserService,
    private val jwtService: JwtService
) {

    companion object {
        private val log = LoggerFactory.getLogger(FileUploadController::class.java)
    }

    @Operation(
        summary = "Upload de foto do perfil",
        description = "Realiza upload da foto do perfil do usuário autenticado e atualiza automaticamente o campo profileImageUrl"
    )
    @PostMapping(
        "/profile-image",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @PreAuthorize("isAuthenticated()")
    fun uploadProfileImage(
        @RequestHeader("Authorization") authHeader: String,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<ProfileImageUploadResponse> {
        log.info("Iniciando upload de foto do perfil. Tamanho: ${file.size} bytes, Nome: ${file.originalFilename}")

        val token = authHeader.substring(7) // Remove "Bearer "
        val userId = jwtService.validateAccessToken(token)

        // Upload do arquivo para S3
        val uploadResponse = awsS3Service.uploadFile(file, FileType.PROFILE, userId)

        // Atualizar profileImageUrl do usuário
        userService.updateProfileImage(userId, uploadResponse.fileUrl)

        log.info("Upload de foto do perfil concluído com sucesso para usuário: $userId")

        val response = ProfileImageUploadResponse(
            profileImageUrl = uploadResponse.fileUrl,
            uploadInfo = uploadResponse
        )

        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "Upload genérico de arquivo",
        description = "Realiza upload de arquivo para S3 com tipo específico"
    )
    @PostMapping(
        "/upload",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @PreAuthorize("isAuthenticated()")
    fun uploadFile(
        @RequestHeader("Authorization") authHeader: String,
        @RequestParam("file") file: MultipartFile,
        @RequestParam("fileType") fileType: String
    ): ResponseEntity<FileUploadResponse> {
        log.info("Iniciando upload de arquivo. Tipo: $fileType, Tamanho: ${file.size} bytes")

        val token = authHeader.substring(7) // Remove "Bearer "
        val userId = jwtService.validateAccessToken(token)

        // Validar tipo de arquivo
        val fileTypeEnum = FileType.fromString(fileType)
            ?: throw IllegalArgumentException("Tipo de arquivo inválido: $fileType")

        // Upload do arquivo para S3
        val uploadResponse = awsS3Service.uploadFile(file, fileTypeEnum, userId)

        log.info("Upload de arquivo concluído com sucesso para usuário: $userId")

        return ResponseEntity.ok(uploadResponse)
    }

    @Operation(
        summary = "Gerar URL pré-assinada",
        description = "Gera URL pré-assinada para download privado de arquivo"
    )
    @GetMapping("/presigned-url")
    @PreAuthorize("isAuthenticated()")
    fun generatePresignedUrl(
        @RequestParam("filePath") filePath: String
    ): ResponseEntity<PresignedUrlResponse> {
        log.info("Gerando URL pré-assinada para: $filePath")

        val presignedUrl = awsS3Service.generatePresignedUrl(filePath)

        val response = PresignedUrlResponse(
            presignedUrl = presignedUrl,
            expirationHours = 1
        )

        return ResponseEntity.ok(response)
    }
}

/**
 * Response específico para upload de foto do perfil
 */
@Schema(description = "Response do upload de foto do perfil")
data class ProfileImageUploadResponse(
    @Schema(description = "URL da nova foto do perfil")
    val profileImageUrl: String,

    @Schema(description = "Informações detalhadas do upload")
    val uploadInfo: FileUploadResponse
)

/**
 * Response para URL pré-assinada
 */
@Schema(description = "Response da URL pré-assinada")
data class PresignedUrlResponse(
    @Schema(description = "URL pré-assinada para download")
    val presignedUrl: String,

    @Schema(description = "Tempo de expiração em horas")
    val expirationHours: Int
) 