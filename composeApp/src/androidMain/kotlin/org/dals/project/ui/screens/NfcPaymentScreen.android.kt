package org.dals.project.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.NfcManager
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.NfcA
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// Global reference to current activity for NFC foreground dispatch
private var currentActivity: Activity? = null
private var nfcAdapter: NfcAdapter? = null

actual fun checkNfcAvailability(callback: (available: Boolean, enabled: Boolean) -> Unit) {
    val context = getCurrentContext() ?: return callback(false, false)

    val nfcManager = context.getSystemService(Context.NFC_SERVICE) as? NfcManager
    val adapter = nfcManager?.defaultAdapter
    nfcAdapter = adapter

    if (adapter == null) {
        callback(false, false)
    } else {
        callback(true, adapter.isEnabled)
    }
}

actual fun enableNfcForegroundDispatch() {
    val activity = currentActivity ?: return
    val adapter = nfcAdapter ?: return

    if (adapter.isEnabled) {
        try {
            val intent = Intent(activity, activity.javaClass).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            val pendingIntent = android.app.PendingIntent.getActivity(
                activity,
                0,
                intent,
                android.app.PendingIntent.FLAG_MUTABLE
            )

            adapter.enableForegroundDispatch(
                activity,
                pendingIntent,
                null,
                null
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

fun disableNfcForegroundDispatch() {
    val activity = currentActivity ?: return
    val adapter = nfcAdapter ?: return

    try {
        adapter.disableForegroundDispatch(activity)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun handleNfcIntent(intent: Intent, onTagDetected: (tagId: String, tagData: ByteArray?) -> Unit) {
    if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action ||
        NfcAdapter.ACTION_TECH_DISCOVERED == intent.action ||
        NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action
    ) {
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)

        if (tag != null) {
            val tagId = tag.id.toHexString()

            // Try to read NFC data
            val data = try {
                readNfcTag(tag)
            } catch (e: Exception) {
                null
            }

            onTagDetected(tagId, data)
        }
    }
}

private fun readNfcTag(tag: Tag): ByteArray? {
    // Try IsoDep first (for contactless cards)
    val isoDep = IsoDep.get(tag)
    if (isoDep != null) {
        try {
            isoDep.connect()
            // Send APDU command to read card data (simplified)
            val response = isoDep.transceive(
                byteArrayOf(
                    0x00.toByte(), // CLA
                    0xA4.toByte(), // INS (SELECT)
                    0x04.toByte(), // P1
                    0x00.toByte(), // P2
                    0x00.toByte()  // Le
                )
            )
            isoDep.close()
            return response
        } catch (e: Exception) {
            try {
                isoDep.close()
            } catch (ignored: Exception) {
            }
        }
    }

    // Try NfcA
    val nfcA = NfcA.get(tag)
    if (nfcA != null) {
        try {
            nfcA.connect()
            val atqa = nfcA.atqa
            nfcA.close()
            return atqa
        } catch (e: Exception) {
            try {
                nfcA.close()
            } catch (ignored: Exception) {
            }
        }
    }

    return null
}

private fun ByteArray.toHexString(): String {
    return joinToString("") { "%02x".format(it) }
}

// Helper to get current context (needs to be called from Composable)
@Composable
private fun getCurrentContext(): Context? {
    return LocalContext.current
}

// Initialize activity reference (should be called from MainActivity)
fun initializeNfcPayment(activity: Activity) {
    currentActivity = activity
    val nfcManager = activity.getSystemService(Context.NFC_SERVICE) as? NfcManager
    nfcAdapter = nfcManager?.defaultAdapter
}

// Clean up activity reference
fun cleanupNfcPayment() {
    disableNfcForegroundDispatch()
    currentActivity = null
    nfcAdapter = null
}

// Open NFC settings
fun openNfcSettings(context: Context) {
    val intent = Intent(Settings.ACTION_NFC_SETTINGS)
    context.startActivity(intent)
}
