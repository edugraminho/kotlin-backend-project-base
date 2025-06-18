package com.base.domain.exception

/**
 * Exceção customizada para regras de negócio
 */
class BusinessException(
    val exceptionType: ExceptionType,
    val details: String? = null,
    cause: Throwable? = null
) : RuntimeException(exceptionType.message, cause) {

    val code: String = exceptionType.code
    val httpStatus = exceptionType.httpStatus

    constructor(exceptionType: ExceptionType, cause: Throwable) : this(exceptionType, null, cause)

    override fun toString(): String {
        return "BusinessException(code='$code', message='$message', details='$details')"
    }
}