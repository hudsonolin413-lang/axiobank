package org.dals.project.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Desktop implementation of contacts manager
 * For desktop, we'll use mock contacts since there's no native contact book
 * In production, this could integrate with Outlook, Google Contacts API, etc.
 */
actual class PlatformContactsManager : ContactsManager {

    override suspend fun requestPermission(): Boolean = withContext(Dispatchers.IO) {
        // Desktop doesn't need runtime permissions
        true
    }

    override suspend fun fetchContacts(): List<Contact> = withContext(Dispatchers.IO) {
        // For desktop, return sample contacts
        // In production, integrate with system contacts or cloud contacts
        listOf(
            Contact(
                id = "1",
                name = "John Doe",
                phoneNumber = "+254712345678",
                email = "john.doe@example.com"
            ),
            Contact(
                id = "2",
                name = "Jane Smith",
                phoneNumber = "+254723456789",
                email = "jane.smith@example.com"
            ),
            Contact(
                id = "3",
                name = "Michael Johnson",
                phoneNumber = "+254734567890",
                email = "michael.j@example.com"
            ),
            Contact(
                id = "4",
                name = "Sarah Williams",
                phoneNumber = "+254745678901",
                email = "sarah.w@example.com"
            ),
            Contact(
                id = "5",
                name = "David Brown",
                phoneNumber = "+254756789012",
                email = "david.b@example.com"
            ),
            Contact(
                id = "6",
                name = "Emma Davis",
                phoneNumber = "+254767890123",
                email = "emma.d@example.com"
            ),
            Contact(
                id = "7",
                name = "Robert Miller",
                phoneNumber = "+254778901234",
                email = "robert.m@example.com"
            ),
            Contact(
                id = "8",
                name = "Lisa Anderson",
                phoneNumber = "+254789012345",
                email = "lisa.a@example.com"
            ),
            Contact(
                id = "9",
                name = "James Wilson",
                phoneNumber = "+254790123456",
                email = "james.w@example.com"
            ),
            Contact(
                id = "10",
                name = "Emily Taylor",
                phoneNumber = "+254701234567",
                email = "emily.t@example.com"
            )
        )
    }

    override fun hasPermission(): Boolean = true
}
