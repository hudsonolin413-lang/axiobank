package org.dals.project.utils

import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android implementation of contacts manager
 * Fetches real contacts from Android contact provider
 */
actual class PlatformContactsManager : ContactsManager {

    private var context: Context? = null

    fun init(context: Context) {
        this.context = context
    }

    override suspend fun requestPermission(): Boolean = withContext(Dispatchers.Main) {
        // Permission request should be handled in Activity/Fragment
        // This just checks current permission status
        hasPermission()
    }

    override suspend fun fetchContacts(): List<Contact> = withContext(Dispatchers.IO) {
        val contactsList = mutableListOf<Contact>()

        val ctx = context ?: return@withContext emptyList()

        if (!hasPermission()) {
            println("⚠️ No READ_CONTACTS permission")
            return@withContext emptyList()
        }

        try {
            val cursor = ctx.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.PHOTO_URI
                ),
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )

            cursor?.use {
                val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

                while (it.moveToNext()) {
                    val id = it.getString(idIndex)
                    val name = it.getString(nameIndex)
                    val number = it.getString(numberIndex)
                    val photoUri = it.getString(photoIndex)

                    if (!name.isNullOrBlank() && !number.isNullOrBlank()) {
                        contactsList.add(
                            Contact(
                                id = id,
                                name = name,
                                phoneNumber = number.replace("\\s".toRegex(), ""),
                                photoUri = photoUri
                            )
                        )
                    }
                }
            }

            println("✅ Fetched ${contactsList.size} contacts from Android")
        } catch (e: Exception) {
            println("❌ Error fetching contacts: ${e.message}")
            e.printStackTrace()
        }

        return@withContext contactsList.distinctBy { it.phoneNumber }
    }

    override fun hasPermission(): Boolean {
        val ctx = context ?: return false
        return ContextCompat.checkSelfPermission(
            ctx,
            android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }
}
