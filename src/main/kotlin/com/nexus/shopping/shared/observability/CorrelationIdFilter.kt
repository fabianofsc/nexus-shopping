package com.nexus.shopping.shared.observability

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class CorrelationIdFilter(private val provider: CorrelationIdProvider) : OncePerRequestFilter() {

    private val logger = LoggerFactory.getLogger(CorrelationIdFilter::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain
    ) {
        val incomingId = request.getHeader("X-Correlation-ID")
        val resolvedId = provider.resolveCorrelationId(incomingId)
        val startTime = System.currentTimeMillis()

        try {
            MDC.put("correlation.id", resolvedId)
            response.addHeader("X-Correlation-ID", resolvedId)
            chain.doFilter(request, response)
        } finally {
            val duration = System.currentTimeMillis() - startTime
            val statusCode = response.status
            val message = "http.request.completed method=${request.method} path=${request.requestURI} status=$statusCode duration=${duration}ms"

            if (statusCode >= 500) {
                logger.error(message)
            } else {
                logger.info(message)
            }

            MDC.remove("correlation.id")
        }
    }
}
