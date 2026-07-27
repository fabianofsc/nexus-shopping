package com.nexus.shopping.payment.application.port.inbound

import com.nexus.shopping.payment.application.command.ValidatePaymentInputCommand

interface ValidatePaymentInputPort {
    fun validate(command: ValidatePaymentInputCommand)
}
