package org.dals.project.utils

/**
 * Contact data model
 */
data class Contact(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val email: String? = null,
    val photoUri: String? = null
)

/**
 * Platform-specific contacts manager
 */
interface ContactsManager {
    suspend fun requestPermission(): Boolean
    suspend fun fetchContacts(): List<Contact>
    fun hasPermission(): Boolean
}

expect class PlatformContactsManager() : ContactsManager
