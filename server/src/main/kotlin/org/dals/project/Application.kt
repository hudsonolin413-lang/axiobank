package org.dals.project

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.dals.project.database.DatabaseFactory
import org.dals.project.plugins.*
import org.dals.project.services.MpesaService
import org.dals.project.services.MpesaReversalScheduler

// Global reference to the scheduler for cleanup
private var reversalScheduler: MpesaReversalScheduler? = null

fun main() {
    val server = embeddedServer(Netty, port = 8081, host = "0.0.0.0", module = Application::module)

    // Add shutdown hook to cleanup scheduler
    Runtime.getRuntime().addShutdownHook(Thread {
        println("🛑 Shutting down M-Pesa reversal scheduler...")
        reversalScheduler?.cleanup()
    })

    server.start(wait = true)
}

fun Application.module() {
    // Initialize database
    DatabaseFactory.init()

    // Configure JSON serialization
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        })
    }

    // Configure CORS - Restrict to specific domains
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Get)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowCredentials = true

        // Allow public website domains
        allowHost("axionbank.com", schemes = listOf("http", "https"))
        allowHost("www.axionbank.com", schemes = listOf("http", "https"))
        allowHost("axiobank.com", schemes = listOf("http", "https"))
        allowHost("www.axiobank.com", schemes = listOf("http", "https"))

        // Allow localhost for development
        allowHost("localhost:8081", schemes = listOf("http", "https"))
        allowHost("127.0.0.1:8081", schemes = listOf("http", "https"))
        allowHost("localhost:3000", schemes = listOf("http", "https"))
        allowHost("localhost:8080", schemes = listOf("http", "https"))
    }

    // Configure routing
    configureRouting()

    // Start M-Pesa reversal detection scheduler (optional - enable if you want automatic detection)
    // Uncomment the lines below to enable automatic reversal detection
    /*
    val mpesaService = MpesaService(DatabaseFactory.database)
    reversalScheduler = MpesaReversalScheduler(
        mpesaService = mpesaService,
        intervalMinutes = 5 // Check every 5 minutes
    )
    reversalScheduler?.start()
    println("✅ M-Pesa automatic reversal detection enabled")
    */
}