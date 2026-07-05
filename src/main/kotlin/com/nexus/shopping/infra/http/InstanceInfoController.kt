package com.nexus.shopping.infra.http

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.net.InetAddress
import java.time.Instant

/**
 * Endpoint operacional para validar visualmente o balanceamento de carga:
 * retorna o hostname do container que atendeu a requisicao e um timestamp.
 */
@RestController
class InstanceInfoController {
    @GetMapping("/instance-info")
    fun instanceInfo(): Map<String, String> {
        val hostname =
            runCatching { InetAddress.getLocalHost().hostName }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
                ?: System.getenv("HOSTNAME")
                ?: "unknown"
        return mapOf(
            "hostname" to hostname,
            "timestamp" to Instant.now().toString(),
        )
    }
}
