package org.dals.project.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * iOS implementation of contacts manager
 * TODO: Integrate with CNContactStore from Contacts framework
 */
actual class PlatformContactsManager : ContactsManager {

    override suspend fun requestPermission(): Boolean = withContext(Dispatchers.Main) {
        // TODO: Request CNContactStore authorization
        println("⚠️ iOS contacts not yet implemented")
        false
    }

    override suspend fun fetchContacts(): List<Contact> = withContext(Dispatchers.Default) {
        // TODO: Fetch from CNContactStore
        println("⚠️ iOS contacts not yet implemented")
        emptyList()
    }

    override fun hasPermission(): Boolean {
        // TODO: Check CNContactStore authorization status
        return false
    }
}
