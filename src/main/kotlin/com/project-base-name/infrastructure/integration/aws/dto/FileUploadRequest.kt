package com.projectbasename.infrastructure.integration.aws.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import org.springframework.web.multipart.MultipartFile

/**
 * DTO para request de upload de arquivo
 */
@Schema(description = "Request para upload de arquivo")
data class FileUploadRequest(
    @field:NotBlank(message = "Tipo de arquivo é obrigatório")
    @field:Pattern(
        regexp = "^(profile|document|image|task|company)$",
        message = "Tipo deve ser um dos valores válidos: profile, document, image, task, company"
    )
    @Schema(description = "Tipo do arquivo", example = "profile", allowableValues = ["profile", "document", "image", "task", "company"])
    val fileType: String,

    @Schema(description = "Arquivo a ser enviado", required = true)
    val file: MultipartFile
)

/**
 * DTO para response de upload de arquivo
 */
@Schema(description = "Response do upload de arquivo")
data class FileUploadResponse(
    @Schema(description = "URL do arquivo no S3", example = "https://projectbasename-files-dev.s3.us-east-1.amazonaws.com/profile/123/profile-image-uuid.jpg")
    val fileUrl: String,

    @Schema(description = "Nome original do arquivo", example = "profile-picture.jpg")
    val originalFileName: String,

    @Schema(description = "Nome do arquivo no S3", example = "profile-image-uuid.jpg")
    val s3FileName: String,

    @Schema(description = "Tamanho do arquivo em bytes", example = "1024000")
    val fileSize: Long,

    @Schema(description = "Tipo MIME do arquivo", example = "image/jpeg")
    val contentType: String,

    @Schema(description = "Caminho do arquivo no bucket", example = "profile/123/profile-image-uuid.jpg")
    val filePath: String
)

/**
 * Enum com tipos de arquivo suportados
 */
enum class FileType(val prefix: String, val allowedExtensions: Set<String>) {
    PROFILE("profile", setOf("jpg", "jpeg", "png", "webp")),
    DOCUMENT("document", setOf("pdf", "doc", "docx", "xls", "xlsx", "txt")),
    IMAGE("image", setOf("jpg", "jpeg", "png", "gif", "webp", "svg")),
    // TODO: cada feature deve ter seu set de extensoes, verificar quais vao ser necessarios
    TASK("task", setOf("jpg", "jpeg", "png", "pdf", "doc", "docx", "xls", "xlsx")),
    COMPANY("company", setOf("jpg", "jpeg", "png", "webp", "pdf"));

    companion object {
        fun fromString(type: String): FileType? {
            return values().find { it.name.lowercase() == type.lowercase() }
        }
    }
} 