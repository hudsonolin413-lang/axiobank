package org.dals.project.services

import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.minutes

/**
 * Scheduler for automatically detecting and processing M-Pesa reversals
 *
 * This service periodically checks for transactions that have been reversed by Safaricom
 * and updates the system accordingly.
 */
class MpesaReversalScheduler(
    private val mpesaService: MpesaService,
    private val intervalMinutes: Long = 5 // Check every 5 minutes by default
) {
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * Start the automatic reversal detection scheduler
     */
    fun start() {
        if (job?.isActive == true) {
            println("⚠️ M-Pesa reversal scheduler is already running")
            return
        }

        job = scope.launch {
            println("✅ M-Pesa reversal scheduler started (checking every $intervalMinutes minutes)")

            while (isActive) {
                try {
                    println("🔍 Running automatic M-Pesa reversal detection...")
                    mpesaService.detectAndProcessReversals()
                } catch (e: Exception) {
                    println("❌ Error in M-Pesa reversal scheduler: ${e.message}")
                    e.printStackTrace()
                }

                // Wait for the specified interval before next check
                delay(intervalMinutes.minutes)
            }
        }
    }

    /**
     * Stop the automatic reversal detection scheduler
     */
    fun stop() {
        job?.cancel()
        println("🛑 M-Pesa reversal scheduler stopped")
    }

    /**
     * Check if the scheduler is currently running
     */
    fun isRunning(): Boolean = job?.isActive == true

    /**
     * Cleanup resources
     */
    fun cleanup() {
        stop()
        scope.cancel()
    }
}
