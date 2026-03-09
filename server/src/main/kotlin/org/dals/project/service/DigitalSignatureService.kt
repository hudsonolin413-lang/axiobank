package org.dals.project.service

import org.dals.project.database.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

data class CreateSignatureRequest(
    val customerId: String,
    val documentId: String,
    val documentType: String,
    val documentName: String,
    val signatureData: String, // Base64 encoded signature image
    val ipAddress: String? = null,
    val deviceInfo: String? = null
)

data class DigitalSignatureDto(
    val id: String,
    val customerId: String,
    val documentId: String,
    val documentType: String,
    val documentName: String,
    val signatureData: String,
    val signedAt: String,
    val ipAddress: String?,
    val deviceInfo: String?,
    val isValid: Boolean,
    val verificationHash: String
)

class DigitalSignatureService {

    /**
     * Create a new digital signature
     */
    fun createSignature(request: CreateSignatureRequest): Result<DigitalSignatureDto> {
        return try {
            val customerId = UUID.fromString(request.customerId)

            // Generate verification hash
            val verificationHash = generateVerificationHash(
                request.customerId,
                request.documentId,
                request.signatureData,
                Instant.now().toString()
            )

            val signatureId = transaction {
                DigitalSignatures.insert {
                    it[DigitalSignatures.customerId] = customerId
                    it[documentId] = request.documentId
                    it[documentType] = request.documentType
                    it[documentName] = request.documentName
                    it[signatureData] = request.signatureData
                    it[ipAddress] = request.ipAddress
                    it[deviceInfo] = request.deviceInfo
                    it[isValid] = true
                    it[DigitalSignatures.verificationHash] = verificationHash
                } get DigitalSignatures.id
            }

            println("✅ Digital signature created: $signatureId")

            getSignatureById(signatureId.value.toString())
        } catch (e: Exception) {
            println("❌ Error creating digital signature: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Get signature by ID
     */
    fun getSignatureById(signatureId: String): Result<DigitalSignatureDto> {
        return try {
            val id = UUID.fromString(signatureId)

            val signature = transaction {
                DigitalSignatures.select { DigitalSignatures.id eq id }.firstOrNull()
            } ?: return Result.failure(Exception("Signature not found"))

            Result.success(
                DigitalSignatureDto(
                    id = signatureId,
                    customerId = transaction { signature[DigitalSignatures.customerId].toString() },
                    documentId = transaction { signature[DigitalSignatures.documentId] },
                    documentType = transaction { signature[DigitalSignatures.documentType] },
                    documentName = transaction { signature[DigitalSignatures.documentName] },
                    signatureData = transaction { signature[DigitalSignatures.signatureData] },
                    signedAt = transaction { signature[DigitalSignatures.signedAt].toString() },
                    ipAddress = transaction { signature[DigitalSignatures.ipAddress] },
                    deviceInfo = transaction { signature[DigitalSignatures.deviceInfo] },
                    isValid = transaction { signature[DigitalSignatures.isValid] },
                    verificationHash = transaction { signature[DigitalSignatures.verificationHash] }
                )
            )
        } catch (e: Exception) {
            println("❌ Error getting signature: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Get all signatures for a customer
     */
    fun getCustomerSignatures(customerId: String): Result<List<DigitalSignatureDto>> {
        return try {
            val custId = UUID.fromString(customerId)

            val signatures = transaction {
                DigitalSignatures.select { DigitalSignatures.customerId eq custId }
                    .orderBy(DigitalSignatures.signedAt, SortOrder.DESC)
                    .map { row ->
                        DigitalSignatureDto(
                            id = row[DigitalSignatures.id].value.toString(),
                            customerId = customerId,
                            documentId = row[DigitalSignatures.documentId],
                            documentType = row[DigitalSignatures.documentType],
                            documentName = row[DigitalSignatures.documentName],
                            signatureData = row[DigitalSignatures.signatureData],
                            signedAt = row[DigitalSignatures.signedAt].toString(),
                            ipAddress = row[DigitalSignatures.ipAddress],
                            deviceInfo = row[DigitalSignatures.deviceInfo],
                            isValid = row[DigitalSignatures.isValid],
                            verificationHash = row[DigitalSignatures.verificationHash]
                        )
                    }
            }

            println("✅ Retrieved ${signatures.size} signatures for customer")
            Result.success(signatures)
        } catch (e: Exception) {
            println("❌ Error getting customer signatures: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Verify a signature
     */
    fun verifySignature(signatureId: String): Result<Boolean> {
        return try {
            val id = UUID.fromString(signatureId)

            val isValid = transaction {
                val signature = DigitalSignatures.select { DigitalSignatures.id eq id }.firstOrNull()
                    ?: return@transaction false

                signature[DigitalSignatures.isValid]
            }

            println("✅ Signature verification: $isValid")
            Result.success(isValid)
        } catch (e: Exception) {
            println("❌ Error verifying signature: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Invalidate a signature
     */
    fun invalidateSignature(signatureId: String): Result<Boolean> {
        return try {
            val id = UUID.fromString(signatureId)

            transaction {
                DigitalSignatures.update({ DigitalSignatures.id eq id }) {
                    it[isValid] = false
                }
            }

            println("✅ Signature invalidated: $signatureId")
            Result.success(true)
        } catch (e: Exception) {
            println("❌ Error invalidating signature: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Get signatures by document ID
     */
    fun getSignaturesByDocument(documentId: String): Result<List<DigitalSignatureDto>> {
        return try {
            val signatures = transaction {
                DigitalSignatures.select { DigitalSignatures.documentId eq documentId }
                    .orderBy(DigitalSignatures.signedAt, SortOrder.DESC)
                    .map { row ->
                        DigitalSignatureDto(
                            id = row[DigitalSignatures.id].value.toString(),
                            customerId = row[DigitalSignatures.customerId].toString(),
                            documentId = row[DigitalSignatures.documentId],
                            documentType = row[DigitalSignatures.documentType],
                            documentName = row[DigitalSignatures.documentName],
                            signatureData = row[DigitalSignatures.signatureData],
                            signedAt = row[DigitalSignatures.signedAt].toString(),
                            ipAddress = row[DigitalSignatures.ipAddress],
                            deviceInfo = row[DigitalSignatures.deviceInfo],
                            isValid = row[DigitalSignatures.isValid],
                            verificationHash = row[DigitalSignatures.verificationHash]
                        )
                    }
            }

            println("✅ Retrieved ${signatures.size} signatures for document")
            Result.success(signatures)
        } catch (e: Exception) {
            println("❌ Error getting document signatures: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Generate verification hash for signature
     */
    private fun generateVerificationHash(
        customerId: String,
        documentId: String,
        signatureData: String,
        timestamp: String
    ): String {
        val data = "$customerId:$documentId:$signatureData:$timestamp"
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
}
