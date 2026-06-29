package com.nexus.shopping.product.adapter.inbound.http

import com.nexus.shopping.product.application.usecase.ProductNotFoundException
import com.nexus.shopping.product.application.usecase.ProductValidationException
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
import org.springframework.web.server.ResponseStatusException

@RestControllerAdvice
class ProductExceptionHandler {

    @ExceptionHandler(ProductValidationException::class)
    fun handleValidation(
        exception: ProductValidationException,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> =
        problem(
            status = HttpStatus.BAD_REQUEST,
            detail = exception.message ?: "Validation failed.",
            request = request,
        )

    @ExceptionHandler(ProductNotFoundException::class)
    fun handleNotFound(
        exception: ProductNotFoundException,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> =
        problem(
            status = HttpStatus.NOT_FOUND,
            detail = exception.message ?: "Resource not found.",
            request = request,
        )

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleMessageNotReadable(
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> =
        problem(
            status = HttpStatus.BAD_REQUEST,
            detail = "Malformed request body.",
            request = request,
        )

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotSupported(
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> =
        problem(
            status = HttpStatus.METHOD_NOT_ALLOWED,
            detail = "Request method is not supported.",
            request = request,
        )

    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    fun handleMediaTypeNotSupported(
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> =
        problem(
            status = HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            detail = "Content type is not supported.",
            request = request,
        )

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatus(
        exception: ResponseStatusException,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> {
        if (exception.statusCode.is5xxServerError) {
            logger.error("Unhandled response status exception while processing request", exception)
            return problem(
                status = HttpStatus.INTERNAL_SERVER_ERROR,
                detail = "Unexpected server error.",
                request = request,
            )
        }

        return problem(
            status = exception.statusCode,
            detail = exception.reason ?: exception.statusCode.toString(),
            request = request,
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleUnhandled(
        exception: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> {
        if (exception is ErrorResponse && exception.statusCode.is4xxClientError) {
            return problem(
                status = exception.statusCode,
                detail = exception.body.detail?.takeIf { it.isNotBlank() } ?: "Invalid request.",
                request = request,
            )
        }

        if (exception is MethodArgumentTypeMismatchException) {
            return problem(
                status = HttpStatus.BAD_REQUEST,
                detail = "Invalid request.",
                request = request,
            )
        }

        logger.error("Unhandled exception while processing request", exception)
        return problem(
            status = HttpStatus.INTERNAL_SERVER_ERROR,
            detail = "Unexpected server error.",
            request = request,
        )
    }

    private fun problem(
        status: HttpStatusCode,
        detail: String,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> {
        val problem = ProblemDetail.forStatusAndDetail(status, detail)
        problem.type = URI.create("about:blank")
        problem.title = HttpStatus.resolve(status.value())?.reasonPhrase ?: status.toString()
        problem.instance = URI.create(request.requestURI)
        return ResponseEntity.status(status).body(problem)
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(ProductExceptionHandler::class.java)
    }
}
