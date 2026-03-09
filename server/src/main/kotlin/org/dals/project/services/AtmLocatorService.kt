package org.dals.project.services

import org.dals.project.database.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.util.*
import kotlin.math.*

data class CreateAtmRequest(
    val atmId: String,
    val branchId: String? = null,
    val name: String,
    val address: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val country: String = "USA",
    val latitude: Double,
    val longitude: Double,
    val atmType: String = "FULL_SERVICE",
    val features: List<String>? = null,
    val availability: String = "24/7"
)

data class AtmDto(
    val id: String,
    val atmId: String,
    val branchId: String?,
    val name: String,
    val address: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val atmType: String,
    val features: List<String>,
    val availability: String,
    val status: String,
    val cashAvailable: Boolean,
    val distance: Double? = null,
    val createdAt: String
)

data class AtmSearchResultDto(
    val atms: List<AtmDto>,
    val totalCount: Int,
    val nearestAtm: AtmDto?
)

object AtmLocatorService {

    fun createAtm(request: CreateAtmRequest): Result<AtmDto> {
        return try {
            transaction {
                val atmId = ATMLocations.insert {
                    it[ATMLocations.atmId] = request.atmId
                    it[branchId] = request.branchId?.let { id -> UUID.fromString(id) }
                    it[name] = request.name
                    it[address] = request.address
                    it[city] = request.city
                    it[state] = request.state
                    it[zipCode] = request.zipCode
                    it[country] = request.country
                    it[latitude] = BigDecimal(request.latitude)
                    it[longitude] = BigDecimal(request.longitude)
                    it[atmType] = request.atmType
                    it[features] = request.features?.joinToString(",")
                    it[availability] = request.availability
                }[ATMLocations.id].value

                val atm = ATMLocations.select { ATMLocations.id eq atmId }.single()
                Result.success(mapToAtmDto(atm))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun findNearbyAtms(
        latitude: Double,
        longitude: Double,
        radiusMiles: Double = 10.0,
        limit: Int = 20
    ): Result<AtmSearchResultDto> {
        return try {
            transaction {
                val allAtms = ATMLocations.select { ATMLocations.status eq "OPERATIONAL" }
                    .map { mapToAtmDto(it) }

                val atmsWithDistance = allAtms.map { atm ->
                    atm.copy(distance = calculateDistance(latitude, longitude, atm.latitude, atm.longitude))
                }.filter { it.distance!! <= radiusMiles }
                    .sortedBy { it.distance }
                    .take(limit)

                val nearestAtm = atmsWithDistance.firstOrNull()

                Result.success(
                    AtmSearchResultDto(
                        atms = atmsWithDistance,
                        totalCount = atmsWithDistance.size,
                        nearestAtm = nearestAtm
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun searchAtms(
        city: String? = null,
        state: String? = null,
        zipCode: String? = null,
        atmType: String? = null,
        features: List<String>? = null
    ): Result<List<AtmDto>> {
        return try {
            transaction {
                var query = ATMLocations.selectAll()

                city?.let { query = query.andWhere { ATMLocations.city eq it } }
                state?.let { query = query.andWhere { ATMLocations.state eq it } }
                zipCode?.let { query = query.andWhere { ATMLocations.zipCode eq it } }
                atmType?.let { query = query.andWhere { ATMLocations.atmType eq it } }

                var atms = query.map { mapToAtmDto(it) }

                // Filter by features if provided
                features?.let { requiredFeatures ->
                    atms = atms.filter { atm ->
                        requiredFeatures.all { feature -> atm.features.contains(feature) }
                    }
                }

                Result.success(atms)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getAtmById(atmId: String): Result<AtmDto> {
        return try {
            transaction {
                val atm = ATMLocations.select { ATMLocations.atmId eq atmId }.singleOrNull()
                    ?: return@transaction Result.failure(Exception("ATM not found"))

                Result.success(mapToAtmDto(atm))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun updateAtmStatus(atmId: String, status: String, cashAvailable: Boolean? = null): Result<AtmDto> {
        return try {
            transaction {
                ATMLocations.update({ ATMLocations.atmId eq atmId }) {
                    it[ATMLocations.status] = status
                    cashAvailable?.let { cash -> it[ATMLocations.cashAvailable] = cash }
                    it[updatedAt] = java.time.Instant.now()
                }

                val atm = ATMLocations.select { ATMLocations.atmId eq atmId }.single()
                Result.success(mapToAtmDto(atm))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusMiles = 3959.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusMiles * c
    }

    private fun mapToAtmDto(row: ResultRow) = AtmDto(
        id = row[ATMLocations.id].value.toString(),
        atmId = row[ATMLocations.atmId] ?: "",
        branchId = row[ATMLocations.branchId]?.toString(),
        name = row[ATMLocations.name],
        address = row[ATMLocations.address],
        city = row[ATMLocations.city],
        state = row[ATMLocations.state] ?: "",
        zipCode = row[ATMLocations.zipCode] ?: "",
        country = row[ATMLocations.country],
        latitude = row[ATMLocations.latitude].toDouble(),
        longitude = row[ATMLocations.longitude].toDouble(),
        atmType = row[ATMLocations.atmType],
        features = row[ATMLocations.features]?.split(",") ?: emptyList(),
        availability = row[ATMLocations.availability],
        status = row[ATMLocations.status],
        cashAvailable = row[ATMLocations.cashAvailable],
        createdAt = row[ATMLocations.createdAt].toString()
    )
}
