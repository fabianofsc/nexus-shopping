package com.nexus.shopping.customer.application.usecase

import org.slf4j.Logger
import org.slf4j.MDC

internal fun Logger.infoWithContext(
    message: String,
    vararg context: Pair<String, Any?>,
) {
    logWithContext(context) {
        info(message)
    }
}

internal fun Logger.warnWithContext(
    message: String,
    vararg context: Pair<String, Any?>,
) {
    logWithContext(context) {
        warn(message)
    }
}

private fun logWithContext(
    context: Array<out Pair<String, Any?>>,
    log: () -> Unit,
) {
    try {
        context.forEach { (key, value) ->
            if (value != null) {
                MDC.put(key, value.toString())
            }
        }
        log()
    } finally {
        context.forEach { (key, _) ->
            MDC.remove(key)
        }
    }
}
