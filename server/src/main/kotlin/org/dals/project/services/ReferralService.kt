package org.dals.project.services

import org.dals.project.database.*
import org.dals.project.models.ApiResponse
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.Instant
import java.util.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SortOrder

@Serializable
data class ReferralDto(
    val id: String,
    val referrerId: String,
    val referredId: String? = null,
    val referralCode: String,
    val referredEmail: String? = null,
    val referredPhone: String? = null,
    val status: String,
    val rewardAmount: String,
    val rewardPaid: Boolean,
    val rewardPaidAt: String? = null,
    val expiresAt: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val referredName: String? = null // For UI
)

@Serializable
data class InviteFriendRequest(
    val referrerId: String,
    val friendEmail: String,
    val referralCode: String
)

class ReferralService {
    private val emailService = EmailService()

    suspend fun getReferralsByReferrer(referrerId: UUID): List<ReferralDto> {
        return DatabaseFactory.dbQuery {
            Referrals.select { Referrals.referrerId eq referrerId }
                .orderBy(Referrals.createdAt, SortOrder.DESC)
                .map { row ->
                    ReferralDto(
                        id = row[Referrals.id].value.toString(),
                        referrerId = row[Referrals.referrerId].toString(),
                        referredId = row[Referrals.referredId]?.toString(),
                        referralCode = row[Referrals.referralCode],
                        referredEmail = row[Referrals.referredEmail],
                        referredPhone = row[Referrals.referredPhone],
                        status = row[Referrals.status],
                        rewardAmount = row[Referrals.rewardAmount].toString(),
                        rewardPaid = row[Referrals.rewardPaid],
                        rewardPaidAt = row[Referrals.rewardPaidAt]?.toString(),
                        expiresAt = row[Referrals.expiresAt]?.toString(),
                        createdAt = row[Referrals.createdAt].toString(),
                        updatedAt = row[Referrals.updatedAt].toString(),
                        referredName = row[Referrals.referredEmail]?.split("@")?.get(0) ?: "Friend"
                    )
                }
        }
    }

    suspend fun inviteFriend(request: InviteFriendRequest): ApiResponse<String> {
        return DatabaseFactory.dbQuery {
            try {
                val referrerId = UUID.fromString(request.referrerId)
                
                // Get referrer name
                val referrer = Customers.select { Customers.id eq referrerId }.singleOrNull()
                val referrerName = if (referrer != null) {
                    "${referrer[Customers.firstName]} ${referrer[Customers.lastName]}"
                } else {
                    "Your friend"
                }

                // Check if already referred
                val existing = Referrals.select { 
                    (Referrals.referrerId eq referrerId) and (Referrals.referredEmail eq request.friendEmail)
                }.firstOrNull()

                if (existing != null) {
                    // Just resend email
                    emailService.sendReferralInvitation(
                        toEmail = request.friendEmail,
                        referrerName = referrerName,
                        referralCode = request.referralCode
                    )
                    return@dbQuery ApiResponse(true, "Invitation resent to ${request.friendEmail}")
                }

                // Create referral record
                val referralId = UUID.randomUUID()
                Referrals.insert {
                    it[id] = referralId
                    it[Referrals.referrerId] = referrerId
                    it[referralCode] = request.referralCode
                    it[referredEmail] = request.friendEmail
                    it[status] = "PENDING"
                    it[rewardAmount] = java.math.BigDecimal("25.00")
                    it[expiresAt] = Instant.now().plus(java.time.Duration.ofDays(30))
                }

                // Send real email
                val emailResult = emailService.sendReferralInvitation(
                    toEmail = request.friendEmail,
                    referrerName = referrerName,
                    referralCode = request.referralCode
                )

                if (emailResult.isSuccess) {
                    ApiResponse(true, "Invitation sent successfully to ${request.friendEmail}")
                } else {
                    val errorMsg = emailResult.exceptionOrNull()?.message ?: "Unknown error"
                    ApiResponse(false, errorMsg)
                }
            } catch (e: Exception) {
                ApiResponse(false, "Failed to invite friend: ${e.message}")
            }
        }
    }
}
