package org.dals.project.repository

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.dals.project.model.*
import org.dals.project.storage.PreferencesStorage
import org.dals.project.storage.PreferencesKeys
import org.dals.project.storage.createPreferencesStorage
import org.dals.project.API_BASE_URL
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class ServerLoginResponse(
    val success: Boolean,
    val message: String,
    val user: ServerUserData? = null,
    val sessionId: String? = null
)

@Serializable
data class ServerUserData(
    val id: String,
    val username: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: String,
    val status: String,
    val branchId: String?,
    val employeeId: String?,
    val permissions: List<String>,
    val lastLoginAt: String?
)

@Serializable
data class CustomerLoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class CustomerRegisterRequest(
    val username: String,
    val password: String,
    val confirmPassword: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String
)

@Serializable
data class CustomerLoginResponse(
    val success: Boolean,
    val message: String,
    val data: CustomerLoginData? = null,
    val token: String? = null,
    val error: String? = null
)

@Serializable
data class CustomerLoginData(
    val id: String,
    val username: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val phoneNumber: String,
    val customerNumber: String,
    val status: String,
    val kycStatus: String,
    val creditScore: Int?,
    val createdAt: String,
    val lastLoginAt: String?,
    val totalBorrowed: String = "0.0",
    val totalRepaid: String = "0.0",
    val activeLoans: Int = 0
)

@Serializable
data class AccountBalanceResponse(
    val success: Boolean,
    val message: String,
    val data: List<ServerAccountData>? = null,
    val total: Int = 0,
    val page: Int = 1,
    val pageSize: Int = 10,
    val timestamp: String? = null
)

@Serializable
data class KYCDocumentsResponse(
    val success: Boolean,
    val message: String,
    val data: List<Map<String, String>>? = null
)

@Serializable
data class ServerAccountData(
    val id: String,
    val accountNumber: String,
    val customerId: String,
    val type: String,
    val status: String,
    val balance: Double,
    val availableBalance: Double,
    val minimumBalance: Double,
    val interestRate: Double,
    val branchId: String,
    val openedDate: String,
    val closedDate: String? = null
)

class AuthRepository {
    private val baseUrl = API_BASE_URL

    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }

    private val _authState = MutableStateFlow(AuthState.LOGGED_OUT)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // Storage for platform-specific implementations
    private var storage: PreferencesStorage? = null

    // Store auth token/session
    private var sessionId: String? = null
    private var authToken: String? = null

    init {
        // Initialize storage
        try {
            storage = createPreferencesStorage()
        } catch (e: Exception) {
            println("Storage not initialized yet: ${e.message}")
        }
    }

    fun setStorage(preferencesStorage: PreferencesStorage) {
        storage = preferencesStorage
    }

    suspend fun tryAutoLogin(): Boolean {
        val storage = storage ?: return false

        return try {
            val savedUsername = storage.getString(PreferencesKeys.SAVED_USERNAME)
            val savedPassword = storage.getString(PreferencesKeys.SAVED_PASSWORD)
            val autoLogin = storage.getBoolean(PreferencesKeys.AUTO_LOGIN)

            if (autoLogin && !savedUsername.isNullOrEmpty() && !savedPassword.isNullOrEmpty()) {
                val response = login(savedUsername, savedPassword, rememberCredentials = false)
                response.success
            } else {
                false
            }
        } catch (e: Exception) {
            println("Auto-login failed: ${e.message}")
            false
        }
    }

    suspend fun getSavedCredentials(): Pair<String?, String?> {
        val storage = storage ?: return Pair(null, null)

        return try {
            val username = storage.getString(PreferencesKeys.SAVED_USERNAME)
            val password = storage.getString(PreferencesKeys.SAVED_PASSWORD)
            Pair(username, password)
        } catch (e: Exception) {
            Pair(null, null)
        }
    }

    suspend fun getLastUsername(): String? {
        val storage = storage ?: return null
        return try {
            storage.getString(PreferencesKeys.LAST_USERNAME)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun isRememberMeEnabled(): Boolean {
        val storage = storage ?: return false

        return try {
            storage.getBoolean(PreferencesKeys.REMEMBER_ME)
        } catch (e: Exception) {
            false
        }
    }

    suspend fun login(
        username: String,
        password: String,
        rememberCredentials: Boolean = false
    ): AuthResponse {
        _authState.value = AuthState.LOADING

        return try {
            // Trim whitespace from inputs
            val trimmedUsername = username.trim()
            val trimmedPassword = password.trim()

            println("Attempting login to server: $baseUrl/auth/customer/login")
            println("Username: $trimmedUsername")

            val response = httpClient.post("$baseUrl/auth/customer/login") {
                contentType(ContentType.Application.Json)
                setBody(
                    CustomerLoginRequest(
                        username = trimmedUsername,
                        password = trimmedPassword
                    )
                )
            }

            println("Server response status: ${response.status}")

            if (response.status == HttpStatusCode.OK) {
                val serverResponse = response.body<CustomerLoginResponse>()
                println("Server response: $serverResponse")

                if (serverResponse.success && serverResponse.data != null) {
                    // Convert server customer data to app user model
                    val customerData = serverResponse.data

                    println("📋 Login: Customer KYC Status from server: ${customerData.kycStatus}")

                    // Fetch account number if KYC is verified
                    val accountNumber = if (customerData.kycStatus == "VERIFIED") {
                        fetchAccountNumber(customerData.id)
                    } else null

                    val user = User(
                        id = customerData.id,
                        username = customerData.username,
                        walletAddress = customerData.id, // Use customer ID as identifier
                        fullName = "${customerData.firstName} ${customerData.lastName}",
                        email = customerData.email,
                        phoneNumber = customerData.phoneNumber,
                        customerNumber = customerData.customerNumber,
                        accountNumber = accountNumber,
                        creditScore = customerData.creditScore ?: 0,
                        kycStatus = parseKycStatus(customerData.kycStatus).also {
                            println("📋 Login: Parsed KYC Status: $it")
                        },
                        totalBorrowed = customerData.totalBorrowed.toDoubleOrNull() ?: 0.0,
                        totalRepaid = customerData.totalRepaid.toDoubleOrNull() ?: 0.0,
                        activeLoans = customerData.activeLoans,
                        defaultedLoans = 0,
                        joinedDate = customerData.createdAt
                    )

                    // Store auth token
                    authToken = serverResponse.token

                    _currentUser.value = user
                    _authState.value = AuthState.LOGGED_IN

                    // Save last username
                    saveLastUsername(trimmedUsername)

                    // Save credentials if requested
                    if (rememberCredentials) {
                        saveCredentials(trimmedUsername, trimmedPassword)
                    }

                    println("Login successful for customer: ${user.fullName} (Customer #: ${customerData.customerNumber})")

                    AuthResponse(
                        success = true,
                        message = "Welcome back, ${user.fullName}!",
                        user = user,
                        token = serverResponse.token ?: "no_token"
                    )
                } else {
                    _authState.value = AuthState.LOGGED_OUT
                    val errorMsg = serverResponse.message
                    println("Login failed: $errorMsg")

                    AuthResponse(
                        success = false,
                        message = errorMsg
                    )
                }
            } else {
                _authState.value = AuthState.LOGGED_OUT
                println("Login failed with status: ${response.status}")

                // Try to get error message from response body
                try {
                    val errorResponse = response.body<CustomerLoginResponse>()
                    AuthResponse(
                        success = false,
                        message = errorResponse.message
                    )
                } catch (e: Exception) {
                    AuthResponse(
                        success = false,
                        message = when (response.status) {
                            HttpStatusCode.Unauthorized -> "Invalid username or password"
                            HttpStatusCode.NotFound -> "Customer account not found"
                            else -> "Login failed. Please try again."
                        }
                    )
                }
            }

        } catch (e: Exception) {
            _authState.value = AuthState.LOGGED_OUT
            val errorMessage = "Network error: ${e.message}"
            println("Login error: $errorMessage")
            e.printStackTrace()

            AuthResponse(
                success = false,
                message = "Unable to connect to server. Please check your network connection and ensure the server is running on localhost:8081"
            )
        }
    }

    suspend fun register(
        username: String,
        password: String,
        confirmPassword: String,
        fullName: String,
        email: String,
        phoneNumber: String
    ): AuthResponse {
        _authState.value = AuthState.LOADING

        return try {
            // Parse full name into first and last name
            val nameParts = fullName.trim().split(" ", limit = 2)
            val firstName = nameParts.getOrNull(0) ?: ""
            val lastName = nameParts.getOrNull(1) ?: ""

            println("Attempting to register new customer")
            println("Email: $email")
            println("Phone: $phoneNumber")
            println("Name: $firstName $lastName")

            val response = httpClient.post("$baseUrl/auth/customer/register") {
                contentType(ContentType.Application.Json)
                setBody(
                    CustomerRegisterRequest(
                        username = username.trim(),
                        password = password,
                        confirmPassword = confirmPassword,
                        firstName = firstName,
                        lastName = lastName,
                        email = email.trim(),
                        phoneNumber = phoneNumber.trim()
                    )
                )
            }

            println("Registration response status: ${response.status}")

            if (response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK) {
                val serverResponse = response.body<CustomerLoginResponse>()
                println("Registration response: $serverResponse")

                if (serverResponse.success && serverResponse.data != null) {
                    // Convert server customer data to app user model
                    val customerData = serverResponse.data

                    // Fetch account number if KYC is verified (unlikely for new registration)
                    val accountNumber = if (customerData.kycStatus == "VERIFIED") {
                        fetchAccountNumber(customerData.id)
                    } else null

                    val user = User(
                        id = customerData.id,
                        username = customerData.username,
                        walletAddress = customerData.id, // Use customer ID as identifier
                        fullName = "${customerData.firstName} ${customerData.lastName}",
                        email = customerData.email,
                        phoneNumber = customerData.phoneNumber,
                        customerNumber = customerData.customerNumber,
                        accountNumber = accountNumber,
                        creditScore = customerData.creditScore ?: 0,
                        kycStatus = parseKycStatus(customerData.kycStatus),
                        totalBorrowed = customerData.totalBorrowed.toDoubleOrNull() ?: 0.0,
                        totalRepaid = customerData.totalRepaid.toDoubleOrNull() ?: 0.0,
                        activeLoans = customerData.activeLoans,
                        defaultedLoans = 0,
                        joinedDate = customerData.createdAt
                    )

                    // Store auth token
                    authToken = serverResponse.token

                    _currentUser.value = user
                    _authState.value = AuthState.LOGGED_IN

                    // Save last username
                    saveLastUsername(username.trim())

                    println("Registration successful for: ${user.fullName} (Customer #: ${customerData.customerNumber})")

                    AuthResponse(
                        success = true,
                        message = "Account created successfully! Welcome to AxionBank, ${user.fullName}!",
                        user = user,
                        token = serverResponse.token ?: "no_token"
                    )
                } else {
                    _authState.value = AuthState.LOGGED_OUT
                    AuthResponse(
                        success = false,
                        message = serverResponse.message
                    )
                }
            } else {
                _authState.value = AuthState.LOGGED_OUT

                // Try to get error message from response body
                try {
                    val errorResponse = response.body<CustomerLoginResponse>()
                    AuthResponse(
                        success = false,
                        message = errorResponse.message
                    )
                } catch (e: Exception) {
                    AuthResponse(
                        success = false,
                        message = when (response.status) {
                            HttpStatusCode.Conflict -> "An account with this username, email, or phone number already exists"
                            HttpStatusCode.BadRequest -> "Please check all fields and try again"
                            else -> "Registration failed. Please try again."
                        }
                    )
                }
            }

        } catch (e: Exception) {
            _authState.value = AuthState.LOGGED_OUT
            val errorMessage = "Network error: ${e.message}"
            println("Registration error: $errorMessage")
            e.printStackTrace()

            AuthResponse(
                success = false,
                message = "Unable to connect to server. Please check your network connection and ensure the server is running on localhost:8081"
            )
        }
    }

    private suspend fun saveCredentials(username: String, password: String) {
        val storage = storage ?: return

        try {
            storage.saveString(PreferencesKeys.SAVED_USERNAME, username)
            storage.saveString(PreferencesKeys.SAVED_PASSWORD, password)
            storage.saveBoolean(PreferencesKeys.REMEMBER_ME, true)
        } catch (e: Exception) {
            println("Failed to save credentials: ${e.message}")
        }
    }

    private suspend fun saveLastUsername(username: String) {
        val storage = storage ?: return
        try {
            storage.saveString(PreferencesKeys.LAST_USERNAME, username)
        } catch (e: Exception) {
            println("Failed to save last username: ${e.message}")
        }
    }

    suspend fun setAutoLogin(enabled: Boolean) {
        val storage = storage ?: return

        try {
            storage.saveBoolean(PreferencesKeys.AUTO_LOGIN, enabled)
        } catch (e: Exception) {
            println("Failed to set auto-login: ${e.message}")
        }
    }

    suspend fun logout() {
        _authState.value = AuthState.LOADING

        try {
            // Call server logout endpoint if we have a token (customers don't have sessions like employees)
            if (authToken != null) {
                println("Logging out customer...")
                // Note: Customer logout is mostly client-side since they use tokens
                // We could implement token invalidation on server side if needed
            }
        } catch (e: Exception) {
            println("Logout error: ${e.message}")
            // Continue with local logout even if server call fails
        }

        delay(500) // Simulate logout delay

        // Preserve the last username for quick re-login
        val lastUsername = _currentUser.value?.username

        // Clear user session
        authToken = null
        _currentUser.value = null
        _authState.value = AuthState.LOGGED_OUT

        // Clear all saved credentials and preferences
        clearSavedCredentials()

        // Restore the last username so user can quickly sign back in
        if (lastUsername != null) {
            saveLastUsername(lastUsername)
        }
    }

    suspend fun logoutWithoutClearingCredentials() {
        _authState.value = AuthState.LOADING

        try {
            if (authToken != null) {
                println("Logging out customer (keeping credentials)...")
            }
        } catch (e: Exception) {
            println("Logout error: ${e.message}")
        }

        delay(500) // Simulate logout delay
        authToken = null
        _currentUser.value = null
        _authState.value = AuthState.LOGGED_OUT
    }

    suspend fun lock() {
        println("🔒 Locking app due to inactivity (preserving user session)")
        _authState.value = AuthState.LOCKED
    }

    suspend fun clearSavedCredentials() {
        val storage = storage ?: return

        try {
            storage.remove(PreferencesKeys.SAVED_USERNAME)
            storage.remove(PreferencesKeys.SAVED_PASSWORD)
            storage.remove(PreferencesKeys.LAST_USERNAME)
            storage.saveBoolean(PreferencesKeys.REMEMBER_ME, false)
            storage.saveBoolean(PreferencesKeys.AUTO_LOGIN, false)
        } catch (e: Exception) {
            println("Failed to clear credentials: ${e.message}")
        }
    }

    fun isLoggedIn(): Boolean {
        return _authState.value == AuthState.LOGGED_IN && _currentUser.value != null
    }

    suspend fun verifyPassword(password: String): Boolean {
        // In a real app, this would verify with the server or a secure local storage
        // For now, we'll check against the saved password if available, or simulate a check
        val storage = storage ?: return true
        val savedPassword = storage.getString(PreferencesKeys.SAVED_PASSWORD)
        
        return if (savedPassword != null) {
            savedPassword == password
        } else {
            // If no password saved (e.g. didn't "Remember me"), we might need to call server
            // Simulation for now:
            delay(500)
            password.isNotEmpty()
        }
    }

    fun getAuthToken(): String? {
        return authToken
    }

    private fun parseKycStatus(status: String): KycStatus {
        return when (status.uppercase()) {
            "VERIFIED" -> KycStatus.VERIFIED
            "PENDING" -> KycStatus.PENDING
            "REJECTED" -> KycStatus.REJECTED
            else -> KycStatus.PENDING
        }
    }

    private suspend fun fetchAccountNumber(customerId: String): String? {
        return try {
            println("🔍 Fetching account number for verified customer: $customerId")

            val response = httpClient.get("$baseUrl/customer-care/accounts/customer/$customerId") {
                contentType(ContentType.Application.Json)
                headers {
                    authToken?.let { token ->
                        append("Authorization", "Bearer $token")
                    }
                }
            }

            if (response.status == HttpStatusCode.OK) {
                val accountResponse = response.body<AccountBalanceResponse>()

                if (accountResponse.success && !accountResponse.data.isNullOrEmpty()) {
                    val firstAccount = accountResponse.data.first()
                    println("✅ Account number fetched: ${firstAccount.accountNumber}")
                    firstAccount.accountNumber
                } else {
                    println("⚠️ No accounts found for verified customer")
                    null
                }
            } else {
                println("❌ Failed to fetch account number: ${response.status}")
                null
            }
        } catch (e: Exception) {
            println("❌ Error fetching account number: ${e.message}")
            null
        }
    }

    suspend fun uploadKYCDocument(
        customerId: String,
        documentType: String,
        fileName: String,
        fileData: ByteArray
    ): AuthResponse {
        return try {
            println("📤 Uploading KYC document...")
            println("   Customer ID: $customerId")
            println("   Document type: $documentType")
            println("   File name: $fileName")
            println("   File size: ${fileData.size} bytes")

            // Convert file data to Base64
            val base64Data = fileData.encodeBase64()

            val response = httpClient.post("$baseUrl/auth/customer/kyc/upload") {
                contentType(ContentType.Application.Json)
                headers {
                    authToken?.let { token ->
                        append("Authorization", "Bearer $token")
                    }
                }
                setBody(
                    mapOf(
                        "customerId" to customerId,
                        "documentType" to documentType,
                        "fileName" to fileName,
                        "fileData" to base64Data
                    )
                )
            }

            println("   Upload response status: ${response.status}")

            if (response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK) {
                @Serializable
                data class UploadResponse(
                    val success: Boolean,
                    val message: String,
                    val data: Map<String, String>? = null,
                    val error: String? = null
                )

                val uploadResponse = response.body<UploadResponse>()
                println("   ✅ Upload successful: ${uploadResponse.message}")

                AuthResponse(
                    success = true,
                    message = uploadResponse.message,
                    user = null,
                    token = null
                )
            } else {
                println("   ❌ Upload failed with status: ${response.status}")

                AuthResponse(
                    success = false,
                    message = "Failed to upload document. Please try again.",
                    user = null,
                    token = null
                )
            }
        } catch (e: Exception) {
            println("❌ KYC upload error: ${e.message}")
            e.printStackTrace()

            AuthResponse(
                success = false,
                message = "Unable to upload document. Please check your connection and try again.",
                user = null,
                token = null
            )
        }
    }

    suspend fun getKYCDocuments(customerId: String): List<Map<String, String>> {
        return try {
            println("📋 Fetching KYC documents from API...")
            println("   Customer ID: $customerId")

            val response = httpClient.get("$baseUrl/auth/customer/kyc/documents/$customerId") {
                contentType(ContentType.Application.Json)
            }

            println("   Response status: ${response.status}")

            if (response.status == HttpStatusCode.OK) {
                val apiResponse = response.body<KYCDocumentsResponse>()
                println("   ✅ Fetched ${apiResponse.data?.size ?: 0} documents")
                apiResponse.data ?: emptyList()
            } else {
                println("   ❌ Failed to fetch documents")
                emptyList()
            }
        } catch (e: Exception) {
            println("❌ Exception fetching KYC documents: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    // Extension function to encode ByteArray to Base64
    private fun ByteArray.encodeBase64(): String {
        val base64Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
        val output = StringBuilder()
        var i = 0
        while (i < size) {
            val b1 = this[i++].toInt() and 0xFF
            val b2 = if (i < size) this[i++].toInt() and 0xFF else 0
            val b3 = if (i < size) this[i++].toInt() and 0xFF else 0

            val encoded = (b1 shl 16) or (b2 shl 8) or b3

            output.append(base64Chars[(encoded shr 18) and 0x3F])
            output.append(base64Chars[(encoded shr 12) and 0x3F])
            output.append(if (i > size + 1) '=' else base64Chars[(encoded shr 6) and 0x3F])
            output.append(if (i > size) '=' else base64Chars[encoded and 0x3F])
        }
        return output.toString()
    }

    suspend fun sendPasswordResetCode(email: String): AuthResponse {
        return try {
            println("📧 Sending password reset code to: $email")

            val response = httpClient.post("$baseUrl/auth/customer/forgot-password/send-code") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("email" to email))
            }

            println("Response status: ${response.status}")

            if (response.status == HttpStatusCode.OK) {
                @Serializable
                data class SendCodeResponse(
                    val success: Boolean,
                    val message: String,
                    val error: String? = null
                )

                val sendCodeResponse = response.body<SendCodeResponse>()
                println("✅ Code sent successfully: ${sendCodeResponse.message}")

                AuthResponse(
                    success = true,
                    message = sendCodeResponse.message,
                    user = null,
                    token = null
                )
            } else {
                println("❌ Failed to send code: ${response.status}")
                AuthResponse(
                    success = false,
                    message = "Failed to send verification code. Please try again.",
                    user = null,
                    token = null
                )
            }
        } catch (e: Exception) {
            println("❌ Error sending reset code: ${e.message}")
            e.printStackTrace()

            AuthResponse(
                success = false,
                message = "Unable to send verification code. Please check your connection.",
                user = null,
                token = null
            )
        }
    }

    suspend fun verifyPasswordResetCode(email: String, code: String): AuthResponse {
        return try {
            println("🔍 Verifying reset code for: $email")

            val response = httpClient.post("$baseUrl/auth/customer/forgot-password/verify-code") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("email" to email, "code" to code))
            }

            println("Response status: ${response.status}")

            if (response.status == HttpStatusCode.OK) {
                @Serializable
                data class VerifyCodeResponse(
                    val success: Boolean,
                    val message: String,
                    val error: String? = null
                )

                val verifyCodeResponse = response.body<VerifyCodeResponse>()
                println("✅ Code verified successfully")

                AuthResponse(
                    success = true,
                    message = verifyCodeResponse.message,
                    user = null,
                    token = null
                )
            } else {
                println("❌ Invalid verification code")
                AuthResponse(
                    success = false,
                    message = "Invalid or expired verification code. Please try again.",
                    user = null,
                    token = null
                )
            }
        } catch (e: Exception) {
            println("❌ Error verifying code: ${e.message}")
            e.printStackTrace()

            AuthResponse(
                success = false,
                message = "Unable to verify code. Please try again.",
                user = null,
                token = null
            )
        }
    }

    suspend fun resetPassword(email: String, code: String, newPassword: String): AuthResponse {
        return try {
            println("🔒 Resetting password for: $email")

            val response = httpClient.post("$baseUrl/auth/customer/forgot-password/reset") {
                contentType(ContentType.Application.Json)
                setBody(mapOf(
                    "email" to email,
                    "code" to code,
                    "newPassword" to newPassword
                ))
            }

            println("Response status: ${response.status}")

            if (response.status == HttpStatusCode.OK) {
                @Serializable
                data class ResetPasswordResponse(
                    val success: Boolean,
                    val message: String,
                    val error: String? = null
                )

                val resetResponse = response.body<ResetPasswordResponse>()
                println("✅ Password reset successfully")

                AuthResponse(
                    success = true,
                    message = resetResponse.message,
                    user = null,
                    token = null
                )
            } else {
                println("❌ Failed to reset password")
                AuthResponse(
                    success = false,
                    message = "Failed to reset password. Please try again.",
                    user = null,
                    token = null
                )
            }
        } catch (e: Exception) {
            println("❌ Error resetting password: ${e.message}")
            e.printStackTrace()

            AuthResponse(
                success = false,
                message = "Unable to reset password. Please try again.",
                user = null,
                token = null
            )
        }
    }

    /**
     * Refresh current user data from server (e.g., to check KYC status updates)
     */
    suspend fun refreshUserData(): Boolean {
        val currentUserId = _currentUser.value?.id ?: return false

        return try {
            println("🔄 Refreshing user data for customer: $currentUserId")

            val response = httpClient.get("$baseUrl/auth/customer/profile/$currentUserId") {
                contentType(ContentType.Application.Json)
                authToken?.let { token ->
                    header("Authorization", "Bearer $token")
                }
            }

            if (response.status == HttpStatusCode.OK) {
                val profileResponse = response.body<CustomerLoginResponse>()

                if (profileResponse.success && profileResponse.data != null) {
                    val customerData = profileResponse.data

                    // Fetch account number if KYC is verified
                    val accountNumber = if (customerData.kycStatus == "VERIFIED") {
                        fetchAccountNumber(customerData.id)
                    } else null

                    val updatedUser = User(
                        id = customerData.id,
                        username = customerData.username,
                        walletAddress = customerData.id,
                        fullName = "${customerData.firstName} ${customerData.lastName}",
                        email = customerData.email,
                        phoneNumber = customerData.phoneNumber,
                        customerNumber = customerData.customerNumber,
                        accountNumber = accountNumber,
                        creditScore = customerData.creditScore ?: 0,
                        kycStatus = parseKycStatus(customerData.kycStatus),
                        totalBorrowed = customerData.totalBorrowed.toDoubleOrNull() ?: 0.0,
                        totalRepaid = customerData.totalRepaid.toDoubleOrNull() ?: 0.0,
                        activeLoans = customerData.activeLoans,
                        defaultedLoans = 0,
                        joinedDate = customerData.createdAt
                    )

                    _currentUser.value = updatedUser
                    println("✅ User data refreshed. KYC Status: ${updatedUser.kycStatus}, Account: ${updatedUser.accountNumber}")

                    // Show notification if KYC status changed to VERIFIED
                    if (updatedUser.kycStatus == KycStatus.VERIFIED && updatedUser.accountNumber != null) {
                        println("🎉 KYC verified! Account number: ${updatedUser.accountNumber}")
                    }

                    true
                } else {
                    println("❌ Failed to refresh user data: ${profileResponse.message}")
                    false
                }
            } else {
                println("❌ Failed to refresh user data. Status: ${response.status}")
                false
            }
        } catch (e: Exception) {
            println("❌ Error refreshing user data: ${e.message}")
            false
        }
    }

    fun cleanup() {
        httpClient.close()
    }
}