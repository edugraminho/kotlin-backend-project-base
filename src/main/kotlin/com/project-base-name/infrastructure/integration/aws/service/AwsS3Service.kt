package com.projectbasename.infrastructure.integration.aws.service

import com.projectbasename.infrastructure.integration.aws.config.S3StorageProperties
import com.projectbasename.infrastructure.integration.aws.dto.FileType
import com.projectbasename.infrastructure.integration.aws.dto.FileUploadResponse
import com.projectbasename.domain.exception.BusinessException
import com.projectbasename.domain.exception.ExceptionType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.*
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import java.time.Duration
import java.util.*

/**
 * Serviço principal para operações com AWS S3
 * Responsável por upload, download e gestão de arquivos
 */
@Service
class AwsS3Service(
    private val s3Client: S3Client,
    private val s3Properties: S3StorageProperties
) {
    
    companion object {
        private val log = LoggerFactory.getLogger(AwsS3Service::class.java)
        private const val MAX_FILE_SIZE = 20 * 1024 * 1024L // 20MB
        private const val PRESIGNED_URL_DURATION_HOURS = 1L
    }

    /**
     * Realiza upload de arquivo para S3
     * 
     * @param file Arquivo multipart a ser enviado
     * @param fileType Tipo do arquivo (profile, document, etc.)
     * @param userId ID do usuário (usado no path)
     * @return FileUploadResponse com informações do arquivo
     */
    fun uploadFile(file: MultipartFile, fileType: FileType, userId: Long): FileUploadResponse {
        log.info("Iniciando upload de arquivo. Tipo: ${fileType.name}, UserId: $userId, Nome: ${file.originalFilename}")
        
        validateFile(file, fileType)
        
        val fileName = generateFileName(file.originalFilename ?: "file", fileType)
        val filePath = buildFilePath(fileType, userId, fileName)
        
        try {
            val putObjectRequest = PutObjectRequest.builder()
                .bucket(s3Properties.bucket)
                .key(filePath)
                .contentType(file.contentType)
                .contentLength(file.size)
                .metadata(buildMetadata(file, userId))
                .build()

            val requestBody = RequestBody.fromInputStream(file.inputStream, file.size)
            
            s3Client.putObject(putObjectRequest, requestBody)
            
            val fileUrl = buildPublicUrl(filePath)
            
            log.info("Upload realizado com sucesso. URL: $fileUrl")
            
            return FileUploadResponse(
                fileUrl = fileUrl,
                originalFileName = file.originalFilename ?: "unknown",
                s3FileName = fileName,
                fileSize = file.size,
                contentType = file.contentType ?: "application/octet-stream",
                filePath = filePath
            )
            
        } catch (e: Exception) {
            log.error("Erro ao realizar upload para S3", e)
            throw BusinessException(
                ExceptionType.FILE_UPLOAD_ERROR, 
                "Erro interno ao enviar arquivo para o storage"
            )
        }
    }

    /**
     * Gera URL pré-assinada para download privado
     * 
     * @param filePath Caminho do arquivo no S3
     * @return URL pré-assinada válida por 1 hora
     */
    fun generatePresignedUrl(filePath: String): String {
        try {
            val presigner = S3Presigner.builder()
                .region(software.amazon.awssdk.regions.Region.of(s3Properties.region))
                .build()

            val presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(PRESIGNED_URL_DURATION_HOURS))
                .getObjectRequest(
                    GetObjectRequest.builder()
                        .bucket(s3Properties.bucket)
                        .key(filePath)
                        .build()
                )
                .build()

            val presignedUrl = presigner.presignGetObject(presignRequest)
            presigner.close()
            
            log.debug("URL pré-assinada gerada para: $filePath")
            return presignedUrl.url().toString()
            
        } catch (e: Exception) {
            log.error("Erro ao gerar URL pré-assinada para: $filePath", e)
            throw BusinessException(
                ExceptionType.FILE_ACCESS_ERROR,
                "Erro ao gerar link de acesso ao arquivo"
            )
        }
    }

    /**
     * Remove arquivo do S3
     * 
     * @param filePath Caminho do arquivo no S3
     */
    fun deleteFile(filePath: String) {
        try {
            val deleteRequest = DeleteObjectRequest.builder()
                .bucket(s3Properties.bucket)
                .key(filePath)
                .build()

            s3Client.deleteObject(deleteRequest)
            log.info("Arquivo removido com sucesso: $filePath")
            
        } catch (e: Exception) {
            log.error("Erro ao remover arquivo do S3: $filePath", e)
            throw BusinessException(
                ExceptionType.FILE_DELETE_ERROR,
                "Erro ao remover arquivo do storage"
            )
        }
    }

    /**
     * Verifica se arquivo existe no S3
     */
    fun fileExists(filePath: String): Boolean {
        return try {
            s3Client.headObject(
                HeadObjectRequest.builder()
                    .bucket(s3Properties.bucket)
                    .key(filePath)
                    .build()
            )
            true
        } catch (e: NoSuchKeyException) {
            false
        } catch (e: Exception) {
            log.error("Erro ao verificar existência do arquivo: $filePath", e)
            false
        }
    }

    /**
     * Valida arquivo antes do upload
     */
    private fun validateFile(file: MultipartFile, fileType: FileType) {
        // Validar se arquivo não está vazio
        if (file.isEmpty) {
            throw BusinessException(ExceptionType.INVALID_FILE, "Arquivo não pode estar vazio")
        }

        // Validar tamanho
        if (file.size > MAX_FILE_SIZE) {
            throw BusinessException(
                ExceptionType.FILE_TOO_LARGE,
                "Arquivo muito grande. Tamanho máximo: ${MAX_FILE_SIZE / 1024 / 1024}MB"
            )
        }

        // Validar extensão
        val extension = getFileExtension(file.originalFilename ?: "")
        if (extension.isNotEmpty() && !fileType.allowedExtensions.contains(extension.lowercase())) {
            throw BusinessException(
                ExceptionType.INVALID_FILE_TYPE,
                "Tipo de arquivo não permitido. Extensões válidas: ${fileType.allowedExtensions.joinToString(", ")}"
            )
        }
    }

    /**
     * Gera nome único para o arquivo
     */
    private fun generateFileName(originalName: String, fileType: FileType): String {
        val extension = getFileExtension(originalName)
        val uuid = UUID.randomUUID().toString()
        val prefix = fileType.prefix
        
        return if (extension.isNotEmpty()) {
            "$prefix-$uuid.$extension"
        } else {
            "$prefix-$uuid"
        }
    }

    /**
     * Constrói o caminho completo do arquivo no S3
     */
    private fun buildFilePath(fileType: FileType, userId: Long, fileName: String): String {
        return "${fileType.prefix}/$userId/$fileName"
    }

    /**
     * Constrói URL pública do arquivo
     */
    private fun buildPublicUrl(filePath: String): String {
        return "https://${s3Properties.bucket}.s3.${s3Properties.region}.amazonaws.com/$filePath"
    }

    /**
     * Extrai extensão do arquivo
     */
    private fun getFileExtension(fileName: String): String {
        val lastDot = fileName.lastIndexOf('.')
        return if (lastDot > 0 && lastDot < fileName.length - 1) {
            fileName.substring(lastDot + 1)
        } else {
            ""
        }
    }

    /**
     * Constrói metadados do arquivo
     */
    private fun buildMetadata(file: MultipartFile, userId: Long): Map<String, String> {
        return mapOf(
            "original-filename" to (file.originalFilename ?: "unknown"),
            "uploaded-by" to userId.toString(),
            "upload-timestamp" to System.currentTimeMillis().toString(),
            "content-type" to (file.contentType ?: "application/octet-stream")
        )
    }
} 