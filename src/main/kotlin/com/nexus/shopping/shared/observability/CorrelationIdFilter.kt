package com.nexus.shopping.shared.observability

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class CorrelationIdFilter : OncePerRequestFilter() {

    private val logger = LoggerFactory.getLogger(CorrelationIdFilter::class.java)
    private val provider = CorrelationIdProvider()

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
            MDC.put("http.request.method", request.method)
            MDC.put("url.path", request.requestURI)
            MDC.put("http.response.status_code", statusCode.toString())
            MDC.put("event.duration", duration.toString())
            if (statusCode >= 500) {
                logger.error("http.request.completed")
            } else {
                logger.info("http.request.completed")
            }
            MDC.remove("http.request.method")
            MDC.remove("url.path")
            MDC.remove("http.response.status_code")
            MDC.remove("event.duration")
            MDC.remove("correlation.id")
        }
    }
}
