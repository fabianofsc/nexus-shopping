package com.nexus.shopping.shared.observability

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class CorrelationIdFilter(private val provider: CorrelationIdProvider) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain
    ) {
        val incomingId = request.getHeader("X-Correlation-ID")
        val resolvedId = provider.resolveCorrelationId(incomingId)

        try {
            MDC.put("correlation.id", resolvedId)
            response.addHeader("X-Correlation-ID", resolvedId)
            chain.doFilter(request, response)
        } finally {
            MDC.remove("correlation.id")
        }
    }
}
