package org.dals.project.model

import kotlinx.serialization.Serializable

@Serializable
data class Advertisement(
    val id: String,
    val title: String,
    val description: String? = null,
    val imageUrl: String,
    val linkUrl: String? = null,
    val displayOrder: Int = 0,
    val isActive: Boolean = true,
    val startDate: String,
    val endDate: String? = null,
    val createdBy: String,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class AdvertisementsResponse(
    val success: Boolean,
    val message: String,
    val data: List<Advertisement>? = null
)
