package org.dals.project.utils

import org.dals.project.model.PaymentMethod

/**
 * Utility function to get the logo emoji for a given payment method
 */
fun getPaymentMethodLogo(paymentMethod: PaymentMethod): String {
    return ""
}

/**
 * Utility function to get the display name for a given payment method
 */
fun getPaymentMethodName(paymentMethod: PaymentMethod): String {
    return when (paymentMethod) {
        PaymentMethod.MPESA -> "M-Pesa"
        PaymentMethod.AIRTEL -> "Airtel Money"
        PaymentMethod.CREDIT_CARD -> "Credit Card"
        PaymentMethod.DEBIT_CARD -> "Debit Card"
    }
}

/**
 * Utility function to get the description for a given payment method
 */
fun getPaymentMethodDescription(paymentMethod: PaymentMethod): String {
    return when (paymentMethod) {
        PaymentMethod.MPESA -> "Mobile money via M-Pesa"
        PaymentMethod.AIRTEL -> "Mobile money via Airtel"
        PaymentMethod.CREDIT_CARD -> "Pay with credit card"
        PaymentMethod.DEBIT_CARD -> "Pay with debit card"
    }
}