package com.nexus.shopping.payment.adapter.outbound.provider

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.time.Duration

@Configuration
class RestClientConfig {
    @Bean
    fun restClientBuilder(): RestClient.Builder {
        val httpClient =
            HttpClient
                .newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                // JDK HttpClient defaults to HTTP/2 and attempts a cleartext ("h2c") upgrade on
                // every request; servers that don't support that upgrade for a request with a
                // body (e.g. WireMock's embedded Jetty in tests) reset the connection instead of
                // falling back. Force HTTP/1.1, which every HTTP/1.1-only server (including the
                // real nexus-payment-service) handles unconditionally.
                .version(HttpClient.Version.HTTP_1_1)
                .build()
        val requestFactory =
            JdkClientHttpRequestFactory(httpClient).apply {
                setReadTimeout(READ_TIMEOUT)
            }
        return RestClient.builder().requestFactory(requestFactory)
    }

    private companion object {
        // The payment provider is on the synchronous checkout path and the single-threaded
        // reconciliation scheduler: a hung (not refused) connection must not pin a Tomcat
        // request thread or stall the scheduler indefinitely.
        val CONNECT_TIMEOUT: Duration = Duration.ofSeconds(5)
        val READ_TIMEOUT: Duration = Duration.ofSeconds(5)
    }
}
