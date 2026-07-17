package com.nexus.shopping.notification.domain

data class RenderedMessage(
    val subject: String,
    val body: String,
)

object NotificationMessageRenderer {
    private data class MessageTemplate(
        val subject: String,
        val body: String,
    )

    private val placeholderPattern = Regex("\\{(\\w+)\\}")

    private val templates =
        mapOf(
            NotificationType.ORDER_CONFIRMED to
                MessageTemplate(
                    subject = "Pedido {orderId} confirmado",
                    body = "Seu pedido {orderId} no valor de {amount} foi confirmado.",
                ),
            NotificationType.ORDER_PAYMENT_FAILED to
                MessageTemplate(
                    subject = "Falha no pagamento do pedido {orderId}",
                    body = "O pagamento do pedido {orderId} no valor de {amount} falhou.",
                ),
            NotificationType.ORDER_CANCELLED to
                MessageTemplate(
                    subject = "Pedido {orderId} cancelado",
                    body = "Seu pedido {orderId} foi cancelado.",
                ),
        )

    fun requiredPlaceholders(type: NotificationType): Set<String> {
        val template = templates.getValue(type)
        return placeholderPattern
            .findAll("${template.subject} ${template.body}")
            .map { it.groupValues[1] }
            .toSet()
    }

    fun render(
        type: NotificationType,
        params: Map<String, String>,
    ): RenderedMessage {
        val template = templates.getValue(type)
        return RenderedMessage(
            subject = interpolate(template.subject, params),
            body = interpolate(template.body, params),
        )
    }

    private fun interpolate(
        template: String,
        params: Map<String, String>,
    ): String =
        placeholderPattern.replace(template) { match ->
            params[match.groupValues[1]] ?: match.value
        }
}
