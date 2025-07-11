package com.projectbasename.domain.exception

import org.springframework.http.HttpStatus

/**
 * Enum com tipos de exceções de negócio
 */
enum class ExceptionType(
    val code: String,
    val message: String,
    val httpStatus: HttpStatus
) {
    // User exceptions
    USER_NOT_FOUND("USER_001", "Usuário não encontrado", HttpStatus.NOT_FOUND),
    EMAIL_ALREADY_EXISTS("USER_002", "Email já está em uso", HttpStatus.CONFLICT),
    DOCUMENT_ALREADY_EXISTS("USER_003", "Documento já está em uso", HttpStatus.CONFLICT),
    INVALID_CREDENTIALS("USER_004", "Credenciais inválidas", HttpStatus.UNAUTHORIZED),
    USER_INACTIVE("USER_005", "Usuário inativo", HttpStatus.FORBIDDEN),
    USER_ALREADY_MEMBER("USER_006", "Usuário já é membro", HttpStatus.CONFLICT),
    INVALID_EMAIL_FORMAT("USER_007", "Formato de email inválido", HttpStatus.BAD_REQUEST),
    INVALID_PHONE_FORMAT("USER_008", "Formato de telefone inválido", HttpStatus.BAD_REQUEST),
    TOO_MANY_ATTEMPTS(
        "USER_009",
        "Muitas tentativas de login. Tente novamente em 15 minutos",
        HttpStatus.TOO_MANY_REQUESTS
    ),
    USER_ALREADY_HAS_COMPANY("USER_010", "Usuário já possui uma empresa", HttpStatus.CONFLICT),
    INVALID_USER_STATUS("USER_011", "Status do usuário inválido", HttpStatus.BAD_REQUEST),
    INVITATION_INVALID_STATUS("USER_012", "Status do convidado inválido", HttpStatus.BAD_REQUEST),
    PROFILE_ALREADY_COMPLETE("USER_013", "Perfil já foi completado", HttpStatus.BAD_REQUEST),

    // Company exceptions
    COMPANY_NOT_FOUND("COMPANY_001", "Empresa não encontrada", HttpStatus.NOT_FOUND),
    COMPANY_NAME_REQUIRED("COMPANY_002", "Nome da empresa é obrigatório para tipo BUSINESS", HttpStatus.BAD_REQUEST),
    COMPANY_DOCUMENT_REQUIRED("COMPANY_003", "CNPJ é obrigatório para tipo BUSINESS", HttpStatus.BAD_REQUEST),
    INVALID_CNPJ("COMPANY_004", "CNPJ inválido", HttpStatus.BAD_REQUEST),
    COMPANY_DOCUMENT_EXISTS("COMPANY_005", "CNPJ já está em uso", HttpStatus.CONFLICT),
    COMPANY_ACCESS_DENIED("COMPANY_006", "Acesso negado à empresa", HttpStatus.FORBIDDEN),
    COMPANY_DATA_REQUIRED("COMPANY_007", "Dados da empresa são obrigatórios", HttpStatus.FORBIDDEN),


    // CompanyMember exceptions
    MEMBER_NOT_FOUND("MEMBER_001", "Membro não encontrado", HttpStatus.NOT_FOUND),
    MEMBER_ALREADY_EXISTS("MEMBER_002", "Usuário já é membro desta empresa", HttpStatus.CONFLICT),
    OWNER_CANNOT_BE_MEMBER("MEMBER_003", "Proprietário da empresa não pode ser membro", HttpStatus.BAD_REQUEST),
    CANNOT_REMOVE_OWNER("MEMBER_004", "Não é possível remover o proprietário da empresa", HttpStatus.FORBIDDEN),
    CANNOT_CHANGE_OWNER_ROLE("MEMBER_005", "Não é possível alterar o papel do proprietário", HttpStatus.FORBIDDEN),
    CANNOT_DEACTIVATE_OWNER("MEMBER_006", "Não é possível desativar o proprietário da empresa", HttpStatus.FORBIDDEN),
    CANNOT_BLOCK_OWNER("MEMBER_007", "Não é possível bloquear o proprietário da empresa", HttpStatus.FORBIDDEN),

    // Invitation exceptions
    INVITATION_NOT_FOUND("INVITATION_001", "Convite não encontrado", HttpStatus.NOT_FOUND),
    INVITATION_EXPIRED("INVITATION_002", "Convite expirado", HttpStatus.BAD_REQUEST),
    INVITATION_ALREADY_ACCEPTED("INVITATION_003", "Convite já foi aceito", HttpStatus.BAD_REQUEST),
    INVITATION_ALREADY_EXISTS("INVITATION_004", "Já existe convite pendente para este email", HttpStatus.CONFLICT),
    INVALID_INVITATION_TOKEN("INVITATION_005", "Token de convite inválido", HttpStatus.BAD_REQUEST),

    // Generic exceptions
    VALIDATION_ERROR("GENERIC_001", "Erro de validação", HttpStatus.BAD_REQUEST),
    INTERNAL_SERVER_ERROR("GENERIC_002", "Erro interno do servidor", HttpStatus.INTERNAL_SERVER_ERROR),
    ACCESS_DENIED("GENERIC_003", "Acesso negado", HttpStatus.FORBIDDEN),
    UNAUTHORIZED("GENERIC_004", "Não autorizado", HttpStatus.UNAUTHORIZED),
    RESOURCE_NOT_FOUND("GENERIC_005", "Recurso não encontrado", HttpStatus.NOT_FOUND),
    BAD_REQUEST("GENERIC_006", "Requisição inválida", HttpStatus.BAD_REQUEST),
    FORBIDDEN("GENERIC_007", "Acesso negado", HttpStatus.FORBIDDEN),
    NOT_FOUND("GENERIC_008", "Recurso não encontrado", HttpStatus.NOT_FOUND),
    OPERATION_NOT_ALLOWED("GENERIC_009", "Operation not permitted", HttpStatus.NOT_ACCEPTABLE),

    // Authentication exceptions
    TOKEN_EXPIRED("AUTH_001", "Token expirado", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN("AUTH_002", "Token inválido", HttpStatus.UNAUTHORIZED),
    TOKEN_REQUIRED("AUTH_003", "Token de autenticação obrigatório", HttpStatus.UNAUTHORIZED),
    PHONE_REQUIRED("AUTH_004", "Telefone é obrigatório para login", HttpStatus.BAD_REQUEST),
    INVALID_VERIFICATION_CODE("AUTH_005", "Código de verificação inválido", HttpStatus.BAD_REQUEST),
    RATE_LIMIT_EXCEEDED("AUTH_006", "Limite de requisições excedido", HttpStatus.BAD_REQUEST),

    // SMS
    SMS_SEND_ERROR("SMS_001", "Erro ao enviar SMS", HttpStatus.INTERNAL_SERVER_ERROR),
    SMS_STATUS_ERROR("SMS_002", "Erro ao verificar status do SMS", HttpStatus.INTERNAL_SERVER_ERROR),
    SMS_COOLDOWN_ACTIVE("SMS_003", "Aguarde antes de solicitar novo código", HttpStatus.TOO_MANY_REQUESTS),
    INVALID_SMS_CONTENT("SMS_004", "Conteúdo do SMS inválido", HttpStatus.BAD_REQUEST),
    SMS_TOO_LONG("SMS_005", "SMS excede limite de caracteres", HttpStatus.BAD_REQUEST),
    INVALID_MESSAGE_ID("SMS_006", "ID da mensagem inválido", HttpStatus.BAD_REQUEST),

    // EMAIL
    EMAIL_SEND_ERROR("EMAIL_001", "Erro ao enviar email", HttpStatus.INTERNAL_SERVER_ERROR),
    EMAIL_TEMPLATE_ERROR("EMAIL_002", "Erro no template de email", HttpStatus.INTERNAL_SERVER_ERROR),
    EMAIL_COOLDOWN_ACTIVE("EMAIL_003", "Aguarde antes de solicitar novo código", HttpStatus.TOO_MANY_REQUESTS),
    INVALID_EMAIL_CODE("EMAIL_004", "Código de email inválido", HttpStatus.BAD_REQUEST),

    // FILE UPLOAD/STORAGE
    FILE_UPLOAD_ERROR("FILE_001", "Erro ao fazer upload do arquivo", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_ACCESS_ERROR("FILE_002", "Erro ao acessar arquivo", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_DELETE_ERROR("FILE_003", "Erro ao remover arquivo", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_FILE("FILE_004", "Arquivo inválido", HttpStatus.BAD_REQUEST),
    FILE_TOO_LARGE("FILE_005", "Arquivo muito grande", HttpStatus.BAD_REQUEST),
    INVALID_FILE_TYPE("FILE_006", "Tipo de arquivo não permitido", HttpStatus.BAD_REQUEST),

    // SERVIÇOS EXTERNOS
    EXTERNAL_SERVICE_ERROR("EXT_001", "Erro no serviço externo", HttpStatus.SERVICE_UNAVAILABLE),
    TWILIO_API_ERROR("EXT_002", "Erro na API do Twilio", HttpStatus.SERVICE_UNAVAILABLE),
    REDIS_CONNECTION_ERROR("EXT_003", "Erro de conexão com Redis", HttpStatus.SERVICE_UNAVAILABLE),

    // Subscription/Payment related
    SUBSCRIPTION_NOT_FOUND("PAYMENT_001", "Assinatura não encontrada", HttpStatus.NOT_FOUND),
    SUBSCRIPTION_ALREADY_EXISTS("PAYMENT_002", "Assinatura já existe", HttpStatus.CONFLICT),
    INVALID_SUBSCRIPTION_STATUS("PAYMENT_003", "Status de assinatura inválido", HttpStatus.BAD_REQUEST),
    PAYMENT_PROCESSING_ERROR("PAYMENT_004", "Erro no processamento de pagamento", HttpStatus.INTERNAL_SERVER_ERROR),
}