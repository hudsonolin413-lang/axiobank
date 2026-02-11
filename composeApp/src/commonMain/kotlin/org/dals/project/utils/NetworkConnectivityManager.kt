package org.dals.project.utils

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * Network connectivity manager for monitoring network status and auto-refreshing data
 */
class NetworkConnectivityManager(
    private val baseUrl: String = "http://localhost:8081",
    private val checkIntervalMs: Long = 5000L // Check every 5 seconds
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _lastCheckTime = MutableStateFlow(0L)
    val lastCheckTime: StateFlow<Long> = _lastCheckTime.asStateFlow()

    private var connectivityJob: Job? = null
    private val dataRefreshCallbacks = mutableListOf<suspend () -> Unit>()

    /**
     * Start monitoring network connectivity
     */
    fun startMonitoring() {
        if (connectivityJob?.isActive == true) {
            return
        }

        connectivityJob = scope.launch {
            while (isActive) {
                checkConnectivity()
                delay(checkIntervalMs)
            }
        }
    }

    /**
     * Stop monitoring network connectivity
     */
    fun stopMonitoring() {
        connectivityJob?.cancel()
        connectivityJob = null
    }

    /**
     * Register a callback to be called when network connectivity is restored
     */
    fun registerDataRefreshCallback(callback: suspend () -> Unit) {
        dataRefreshCallbacks.add(callback)
    }

    /**
     * Unregister a data refresh callback
     */
    fun unregisterDataRefreshCallback(callback: suspend () -> Unit) {
        dataRefreshCallbacks.remove(callback)
    }

    /**
     * Check network connectivity by pinging the server
     */
    private suspend fun checkConnectivity() {
        val wasConnected = _isConnected.value

        try {
            val response = httpClient.get("$baseUrl/api/v1/health")
            val isNowConnected = response.status.value in 200..299

            _isConnected.value = isNowConnected
            _lastCheckTime.value = System.currentTimeMillis()

            // If we just reconnected, trigger all data refresh callbacks
            if (!wasConnected && isNowConnected) {
                onConnectivityRestored()
            }
        } catch (e: Exception) {
            _isConnected.value = false
            _lastCheckTime.value = System.currentTimeMillis()
        }
    }

    /**
     * Force a connectivity check immediately
     */
    suspend fun forceCheck() {
        checkConnectivity()
    }

    /**
     * Called when connectivity is restored
     */
    private suspend fun onConnectivityRestored() {
        println("📡 Network connectivity restored - refreshing data")

        // Execute all registered data refresh callbacks
        dataRefreshCallbacks.forEach { callback ->
            try {
                callback()
            } catch (e: Exception) {
                println("⚠️ Error refreshing data: ${e.message}")
            }
        }
    }

    /**
     * Clean up resources
     */
    fun dispose() {
        stopMonitoring()
        dataRefreshCallbacks.clear()
        scope.cancel()
        httpClient.close()
    }

    companion object {
        private var instance: NetworkConnectivityManager? = null

        fun getInstance(
            baseUrl: String = "http://localhost:8081",
            checkIntervalMs: Long = 5000L
        ): NetworkConnectivityManager {
            return instance ?: synchronized(this) {
                instance ?: NetworkConnectivityManager(baseUrl, checkIntervalMs).also {
                    instance = it
                }
            }
        }
    }
}
