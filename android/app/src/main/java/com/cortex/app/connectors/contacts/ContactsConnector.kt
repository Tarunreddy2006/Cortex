package com.cortex.app.connectors.contacts

import android.Manifest
import android.content.Context
import android.provider.ContactsContract
import com.cortex.app.database.CortexDatabase
import com.cortex.app.database.entities.CollectedRecordEntity
import com.cortex.app.permissions.PermissionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class ContactItem(
    val contactId: String,
    val displayName: String,
    val phoneNumbers: List<String>,
    val emailAddresses: List<String>
)

class ContactsConnector(private val context: Context) {

    private val db = CortexDatabase.getInstance(context)

    suspend fun syncContacts(maxLimit: Int = 200): List<ContactItem> = withContext(Dispatchers.IO) {
        if (!PermissionManager.isPermissionGranted(context, Manifest.permission.READ_CONTACTS)) {
            return@withContext emptyList()
        }

        val contactsMap = mutableMapOf<String, ContactItem>()

        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
        )

        try {
            val cursor = context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} ASC LIMIT $maxLimit"
            )

            cursor?.use {
                val idIdx = it.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
                val nameIdx = it.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)

                while (it.moveToNext()) {
                    val id = it.getString(idIdx) ?: continue
                    val name = it.getString(nameIdx) ?: "Unknown"

                    val phones = getPhonesForContact(id)
                    val emails = getEmailsForContact(id)

                    val item = ContactItem(
                        contactId = id,
                        displayName = name,
                        phoneNumbers = phones,
                        emailAddresses = emails
                    )
                    contactsMap[id] = item

                    // Save to Room
                    val payload = JSONObject().apply {
                        put("id", "contact_$id")
                        put("display_name", name)
                        put("phone_numbers", JSONArray(phones))
                        put("email_addresses", JSONArray(emails))
                    }

                    db.collectedRecordDao().insert(
                        CollectedRecordEntity(
                            id = "contact_$id",
                            sourceConnector = "contacts",
                            packageName = null,
                            recordType = "CONTACT",
                            title = name,
                            summary = "Phone: ${phones.firstOrNull() ?: "None"}, Email: ${emails.firstOrNull() ?: "None"}",
                            payloadJson = payload.toString(),
                            dedupHash = null,
                            timestampMs = System.currentTimeMillis()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        contactsMap.values.toList()
    }

    private fun getPhonesForContact(contactId: String): List<String> {
        val phones = mutableListOf<String>()
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
            arrayOf(contactId),
            null
        )
        cursor?.use {
            val numIdx = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                it.getString(numIdx)?.let { num -> phones.add(num.trim()) }
            }
        }
        return phones
    }

    private fun getEmailsForContact(contactId: String): List<String> {
        val emails = mutableListOf<String>()
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS),
            "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
            arrayOf(contactId),
            null
        )
        cursor?.use {
            val addrIdx = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.ADDRESS)
            while (it.moveToNext()) {
                it.getString(addrIdx)?.let { email -> emails.add(email.trim().lowercase()) }
            }
        }
        return emails
    }
}
