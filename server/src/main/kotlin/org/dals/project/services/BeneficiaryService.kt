package org.dals.project.services

import org.dals.project.database.*
import org.dals.project.models.*
import org.jetbrains.exposed.sql.*
import java.util.UUID
import java.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class CreateBeneficiaryRequest(
    val customerId: String,
    val name: String,
    val nickname: String? = null,
    val accountNumber: String? = null,
    val bankName: String? = null,
    val bankCode: String? = null,
    val phoneNumber: String? = null,
    val email: String? = null,
    val type: String = "BANK" // BANK, MOBILE, INTERNAL
)

@Serializable
data class UpdateBeneficiaryRequest(
    val name: String? = null,
    val nickname: String? = null,
    val accountNumber: String? = null,
    val bankName: String? = null,
    val bankCode: String? = null,
    val phoneNumber: String? = null,
    val email: String? = null,
    val isFavorite: Boolean? = null,
    val isActive: Boolean? = null
)

@Serializable
data class BeneficiaryResponse(
    val id: String,
    val customerId: String,
    val name: String,
    val nickname: String?,
    val accountNumber: String?,
    val bankName: String?,
    val bankCode: String?,
    val phoneNumber: String?,
    val email: String?,
    val type: String,
    val isFavorite: Boolean,
    val lastUsed: String?,
    val transferCount: Int,
    val isVerified: Boolean,
    val isActive: Boolean,
    val createdAt: String
)

class BeneficiaryService {

    suspend fun getAllBeneficiaries(customerId: UUID): List<BeneficiaryResponse> = DatabaseFactory.dbQuery {
        Beneficiaries.select { Beneficiaries.customerId eq customerId }
            .orderBy(Beneficiaries.isFavorite to SortOrder.DESC, Beneficiaries.lastUsed to SortOrder.DESC)
            .map { row ->
                BeneficiaryResponse(
                    id = row[Beneficiaries.id].toString(),
                    customerId = row[Beneficiaries.customerId].toString(),
                    name = row[Beneficiaries.name],
                    nickname = row[Beneficiaries.nickname],
                    accountNumber = row[Beneficiaries.accountNumber],
                    bankName = row[Beneficiaries.bankName],
                    bankCode = row[Beneficiaries.bankCode],
                    phoneNumber = row[Beneficiaries.phoneNumber],
                    email = row[Beneficiaries.email],
                    type = row[Beneficiaries.type],
                    isFavorite = row[Beneficiaries.isFavorite],
                    lastUsed = row[Beneficiaries.lastUsed]?.toString(),
                    transferCount = row[Beneficiaries.transferCount],
                    isVerified = row[Beneficiaries.isVerified],
                    isActive = row[Beneficiaries.isActive],
                    createdAt = row[Beneficiaries.createdAt].toString()
                )
            }
    }

    suspend fun getBeneficiaryById(beneficiaryId: UUID): BeneficiaryResponse? = DatabaseFactory.dbQuery {
        Beneficiaries.select { Beneficiaries.id eq beneficiaryId }
            .mapNotNull { row ->
                BeneficiaryResponse(
                    id = row[Beneficiaries.id].toString(),
                    customerId = row[Beneficiaries.customerId].toString(),
                    name = row[Beneficiaries.name],
                    nickname = row[Beneficiaries.nickname],
                    accountNumber = row[Beneficiaries.accountNumber],
                    bankName = row[Beneficiaries.bankName],
                    bankCode = row[Beneficiaries.bankCode],
                    phoneNumber = row[Beneficiaries.phoneNumber],
                    email = row[Beneficiaries.email],
                    type = row[Beneficiaries.type],
                    isFavorite = row[Beneficiaries.isFavorite],
                    lastUsed = row[Beneficiaries.lastUsed]?.toString(),
                    transferCount = row[Beneficiaries.transferCount],
                    isVerified = row[Beneficiaries.isVerified],
                    isActive = row[Beneficiaries.isActive],
                    createdAt = row[Beneficiaries.createdAt].toString()
                )
            }.singleOrNull()
    }

    suspend fun getFavoriteBeneficiaries(customerId: UUID): List<BeneficiaryResponse> = DatabaseFactory.dbQuery {
        Beneficiaries.select {
            (Beneficiaries.customerId eq customerId) and
            (Beneficiaries.isFavorite eq true) and
            (Beneficiaries.isActive eq true)
        }
            .orderBy(Beneficiaries.lastUsed to SortOrder.DESC)
            .map { row ->
                BeneficiaryResponse(
                    id = row[Beneficiaries.id].toString(),
                    customerId = row[Beneficiaries.customerId].toString(),
                    name = row[Beneficiaries.name],
                    nickname = row[Beneficiaries.nickname],
                    accountNumber = row[Beneficiaries.accountNumber],
                    bankName = row[Beneficiaries.bankName],
                    bankCode = row[Beneficiaries.bankCode],
                    phoneNumber = row[Beneficiaries.phoneNumber],
                    email = row[Beneficiaries.email],
                    type = row[Beneficiaries.type],
                    isFavorite = row[Beneficiaries.isFavorite],
                    lastUsed = row[Beneficiaries.lastUsed]?.toString(),
                    transferCount = row[Beneficiaries.transferCount],
                    isVerified = row[Beneficiaries.isVerified],
                    isActive = row[Beneficiaries.isActive],
                    createdAt = row[Beneficiaries.createdAt].toString()
                )
            }
    }

    suspend fun createBeneficiary(request: CreateBeneficiaryRequest): BeneficiaryResponse = DatabaseFactory.dbQuery {
        val beneficiaryId = Beneficiaries.insertAndGetId {
            it[customerId] = UUID.fromString(request.customerId)
            it[name] = request.name
            it[nickname] = request.nickname
            it[accountNumber] = request.accountNumber
            it[bankName] = request.bankName
            it[bankCode] = request.bankCode
            it[phoneNumber] = request.phoneNumber
            it[email] = request.email
            it[type] = request.type
        }

        getBeneficiaryById(UUID.fromString(beneficiaryId.toString()))!!
    }

    suspend fun updateBeneficiary(beneficiaryId: UUID, request: UpdateBeneficiaryRequest): BeneficiaryResponse? = DatabaseFactory.dbQuery {
        val updated = Beneficiaries.update({ Beneficiaries.id eq beneficiaryId }) { stmt ->
            request.name?.let { stmt[name] = it }
            request.nickname?.let { stmt[nickname] = it }
            request.accountNumber?.let { stmt[accountNumber] = it }
            request.bankName?.let { stmt[bankName] = it }
            request.bankCode?.let { stmt[bankCode] = it }
            request.phoneNumber?.let { stmt[phoneNumber] = it }
            request.email?.let { stmt[email] = it }
            request.isFavorite?.let { stmt[isFavorite] = it }
            request.isActive?.let { stmt[isActive] = it }
        }

        if (updated > 0) getBeneficiaryById(beneficiaryId) else null
    }

    suspend fun deleteBeneficiary(beneficiaryId: UUID): Boolean = DatabaseFactory.dbQuery {
        val deleted = Beneficiaries.deleteWhere {
            Op.build { Beneficiaries.id eq beneficiaryId }
        }
        deleted > 0
    }

    suspend fun toggleFavorite(beneficiaryId: UUID): BeneficiaryResponse? = DatabaseFactory.dbQuery {
        val current = getBeneficiaryById(beneficiaryId) ?: return@dbQuery null

        Beneficiaries.update({ Beneficiaries.id eq beneficiaryId }) {
            it[isFavorite] = !current.isFavorite
        }

        getBeneficiaryById(beneficiaryId)
    }

    suspend fun searchBeneficiaries(customerId: UUID, query: String): List<BeneficiaryResponse> = DatabaseFactory.dbQuery {
        val searchPattern = "%${query.lowercase()}%"

        Beneficiaries.select {
            (Beneficiaries.customerId eq customerId) and
            (Beneficiaries.isActive eq true) and
            (
                LowerCase(Beneficiaries.name).like(searchPattern) or
                LowerCase(Beneficiaries.nickname).like(searchPattern) or
                LowerCase(Beneficiaries.accountNumber).like(searchPattern) or
                LowerCase(Beneficiaries.bankName).like(searchPattern)
            )
        }
            .orderBy(Beneficiaries.isFavorite to SortOrder.DESC, Beneficiaries.transferCount to SortOrder.DESC)
            .map { row ->
                BeneficiaryResponse(
                    id = row[Beneficiaries.id].toString(),
                    customerId = row[Beneficiaries.customerId].toString(),
                    name = row[Beneficiaries.name],
                    nickname = row[Beneficiaries.nickname],
                    accountNumber = row[Beneficiaries.accountNumber],
                    bankName = row[Beneficiaries.bankName],
                    bankCode = row[Beneficiaries.bankCode],
                    phoneNumber = row[Beneficiaries.phoneNumber],
                    email = row[Beneficiaries.email],
                    type = row[Beneficiaries.type],
                    isFavorite = row[Beneficiaries.isFavorite],
                    lastUsed = row[Beneficiaries.lastUsed]?.toString(),
                    transferCount = row[Beneficiaries.transferCount],
                    isVerified = row[Beneficiaries.isVerified],
                    isActive = row[Beneficiaries.isActive],
                    createdAt = row[Beneficiaries.createdAt].toString()
                )
            }
    }

    suspend fun recordBeneficiaryUsage(beneficiaryId: UUID) = DatabaseFactory.dbQuery {
        val current = Beneficiaries.select { Beneficiaries.id eq beneficiaryId }.firstOrNull()
        if (current != null) {
            Beneficiaries.update({ Beneficiaries.id eq beneficiaryId }) {
                it[lastUsed] = Instant.now()
                it[transferCount] = current[Beneficiaries.transferCount] + 1
            }
        }
    }
}
