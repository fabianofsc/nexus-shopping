package com.nexus.shopping.infra.http

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.time.Duration

@Configuration
class ConfigurableRestClientFactory {
    @Bean
    fun objectMapper(): ObjectMapper = jacksonObjectMapper()

    fun builder(
        connectTimeout: Duration,
        readTimeout: Duration,
    ): RestClient.Builder {
        val httpClient =
            HttpClient
                .newBuilder()
                .connectTimeout(connectTimeout)
                .version(HttpClient.Version.HTTP_1_1)
                .build()
        val requestFactory =
            JdkClientHttpRequestFactory(httpClient).apply {
                setReadTimeout(readTimeout)
            }
        return RestClient.builder().requestFactory(requestFactory)
    }
}
