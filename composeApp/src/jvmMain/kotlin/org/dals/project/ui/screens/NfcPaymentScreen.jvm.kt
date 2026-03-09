package org.dals.project.ui.screens

// Desktop NFC implementation (simulated for testing)
actual fun checkNfcAvailability(callback: (available: Boolean, enabled: Boolean) -> Unit) {
    // Desktop doesn't typically have NFC hardware
    // Return false to indicate NFC is not available
    callback(false, false)

    // For testing purposes, you could uncomment the line below to simulate NFC availability:
    // callback(true, true)
}

actual fun enableNfcForegroundDispatch() {
    // No-op on desktop
    // In a production environment, you might integrate with external NFC readers via USB
    println("NFC foreground dispatch not supported on desktop platform")
}

// Desktop simulation functions for testing
object DesktopNfcSimulator {
    private var isSimulationEnabled = false
    private var simulatedTagCallback: ((tagId: String, merchantName: String, amount: Double) -> Unit)? = null

    fun enableSimulation(enabled: Boolean) {
        isSimulationEnabled = enabled
    }

    fun setTagDetectedCallback(callback: (tagId: String, merchantName: String, amount: Double) -> Unit) {
        simulatedTagCallback = callback
    }

    fun simulateNfcTag(merchantName: String, amount: Double) {
        if (isSimulationEnabled) {
            val simulatedTagId = "SIM${System.currentTimeMillis()}"
            simulatedTagCallback?.invoke(simulatedTagId, merchantName, amount)
        }
    }

    fun simulateMerchantTerminal(
        merchantName: String,
        merchantId: String,
        amount: Double
    ): SimulatedNfcTerminal {
        return SimulatedNfcTerminal(merchantName, merchantId, amount)
    }
}

data class SimulatedNfcTerminal(
    val merchantName: String,
    val merchantId: String,
    val amount: Double
) {
    val terminalId: String = "TERM${System.currentTimeMillis()}"
    val nfcTagId: String = merchantId.hashCode().toString(16)

    fun getPaymentData(): Map<String, String> {
        return mapOf(
            "merchantName" to merchantName,
            "merchantId" to merchantId,
            "amount" to amount.toString(),
            "terminalId" to terminalId,
            "nfcTagId" to nfcTagId
        )
    }
}

// Example test merchants for desktop simulation
object TestMerchants {
    val starbucks = DesktopNfcSimulator.simulateMerchantTerminal(
        merchantName = "Starbucks",
        merchantId = "STBX001",
        amount = 5.99
    )

    val walmart = DesktopNfcSimulator.simulateMerchantTerminal(
        merchantName = "Walmart",
        merchantId = "WMT001",
        amount = 45.50
    )

    val mcdonalds = DesktopNfcSimulator.simulateMerchantTerminal(
        merchantName = "McDonald's",
        merchantId = "MCD001",
        amount = 12.75
    )

    val amazonGo = DesktopNfcSimulator.simulateMerchantTerminal(
        merchantName = "Amazon Go",
        merchantId = "AMZGO001",
        amount = 28.99
    )

    val shell = DesktopNfcSimulator.simulateMerchantTerminal(
        merchantName = "Shell Gas Station",
        merchantId = "SHELL001",
        amount = 65.00
    )

    fun getAllTestMerchants(): List<SimulatedNfcTerminal> {
        return listOf(starbucks, walmart, mcdonalds, amazonGo, shell)
    }
}
