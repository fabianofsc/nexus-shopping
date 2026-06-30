package com.nexus.shopping.platform.adapter.inbound.http

import com.nexus.shopping.platform.application.exception.NotFoundException
import com.nexus.shopping.platform.application.exception.ValidationException
import jakarta.servlet.http.HttpServletRequest
import java.net.URI
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.ErrorResponse
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(ValidationException::class)
    fun handleValidation(
        exception: ValidationException,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> =
        problemDetailResponse(
            status = HttpStatus.BAD_REQUEST,
            detail = exception.message ?: "Validation failed.",
            request = request,
        )

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFound(
        exception: NotFoundException,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> =
        problemDetailResponse(
            status = HttpStatus.NOT_FOUND,
            detail = exception.message ?: "Resource not found.",
            request = request,
        )

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleMessageNotReadable(
        exception: HttpMessageNotReadableException,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> {
        logger.warn("Malformed request body", exception)
        return problemDetailResponse(
            status = HttpStatus.BAD_REQUEST,
            detail = "Malformed request body.",
            request = request,
        )
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotSupported(
        exception: HttpRequestMethodNotSupportedException,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> {
        val detail = buildProblemDetail(
            status = HttpStatus.METHOD_NOT_ALLOWED,
            detail = "Request method is not supported.",
            request = request,
        )
        return ResponseEntity
            .status(HttpStatus.METHOD_NOT_ALLOWED)
            .allow(*exception.supportedHttpMethods.orEmpty().toTypedArray())
            .body(detail)
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    fun handleMediaTypeNotSupported(
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> =
        problemDetailResponse(
            status = HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            detail = "Content type is not supported.",
            request = request,
        )

    @ExceptionHandler(Exception::class)
    fun handleUnhandled(
        exception: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> {
        if (exception is ErrorResponse && exception.statusCode.is4xxClientError) {
            return problemDetailResponse(
                status = exception.statusCode,
                detail = exception.body.detail?.takeIf { it.isNotBlank() } ?: "Invalid request.",
                request = request,
            )
        }

        if (exception is MethodArgumentTypeMismatchException) {
            return problemDetailResponse(
                status = HttpStatus.BAD_REQUEST,
                detail = "Invalid request.",
                request = request,
            )
        }

        logger.error("Unhandled exception while processing request", exception)
        return problemDetailResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR,
            detail = "Unexpected server error.",
            request = request,
        )
    }

    private fun problemDetailResponse(
        status: HttpStatusCode,
        detail: String,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> {
        val problemDetail = buildProblemDetail(status, detail, request)
        return ResponseEntity.status(status).body(problemDetail)
    }

    private fun buildProblemDetail(
        status: HttpStatusCode,
        detail: String,
        request: HttpServletRequest,
    ): ProblemDetail {
        val problemDetail = ProblemDetail.forStatusAndDetail(status, detail)
        problemDetail.type = URI.create("about:blank")
        problemDetail.title = HttpStatus.resolve(status.value())?.reasonPhrase ?: status.toString()
        problemDetail.instance = URI.create(request.requestURI)
        return problemDetail
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(ApiExceptionHandler::class.java)
    }
}
