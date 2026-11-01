package com.alex.ai.actions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

data class ActionResult(
    val success: Boolean,
    val message: String
)

class PhoneActionExecutor(private val context: Context) {

    private val tag = "PhoneActionExecutor"

    fun execute(actionName: String, args: JsonObject?): ActionResult {
        return try {
            when (actionName) {
                "make_call" -> {
                    val phone = args?.get("phoneNumber")?.jsonPrimitive?.content ?: ""
                    val contact = args?.get("contactName")?.jsonPrimitive?.content ?: ""
                    makeCall(phone, contact)
                }
                "send_sms" -> {
                    val phone = args?.get("phoneNumber")?.jsonPrimitive?.content ?: ""
                    val message = args?.get("message")?.jsonPrimitive?.content ?: ""
                    sendSms(phone, message)
                }
                "open_whatsapp" -> {
                    val phone = args?.get("phoneNumber")?.jsonPrimitive?.content ?: ""
                    val message = args?.get("message")?.jsonPrimitive?.content ?: ""
                    openWhatsApp(phone, message)
                }
                "open_app" -> {
                    val appName = args?.get("appName")?.jsonPrimitive?.content ?: ""
                    openApp(appName)
                }
                else -> ActionResult(false, "Unknown action: $actionName")
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to execute $actionName", e)
            ActionResult(false, "Error executing $actionName: ${e.message}")
        }
    }

    private fun makeCall(phoneNumber: String, contactName: String): ActionResult {
        val cleanNumber = phoneNumber.replace("[^0-9+]".toRegex(), "")
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$cleanNumber")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)

        val target = if (contactName.isNotBlank()) "$contactName ($cleanNumber)" else cleanNumber
        return ActionResult(true, "Dialer opened for $target")
    }

    private fun sendSms(phoneNumber: String, message: String): ActionResult {
        val cleanNumber = phoneNumber.replace("[^0-9+]".toRegex(), "")
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$cleanNumber")
            putExtra("sms_body", message)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return ActionResult(true, "SMS drafted for $cleanNumber")
    }

    private fun openWhatsApp(phoneNumber: String, message: String): ActionResult {
        val cleanNumber = phoneNumber.replace("[^0-9]".toRegex(), "")
        val encodedMsg = Uri.encode(message)
        val uri = if (cleanNumber.isNotBlank()) {
            Uri.parse("https://api.whatsapp.com/send?phone=$cleanNumber&text=$encodedMsg")
        } else {
            Uri.parse("https://api.whatsapp.com/send?text=$encodedMsg")
        }

        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            ActionResult(true, "WhatsApp chat opened")
        } catch (e: Exception) {
            ActionResult(false, "WhatsApp not installed on device")
        }
    }

    private fun openApp(appName: String): ActionResult {
        val pm = context.packageManager
        val query = appName.lowercase().trim()

        // Known direct intents
        when (query) {
            "youtube" -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                return tryLaunch(intent, "YouTube")
            }
            "camera" -> {
                val intent = Intent("android.media.action.IMAGE_CAPTURE").apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                return tryLaunch(intent, "Camera")
            }
            "maps", "google maps" -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                return tryLaunch(intent, "Google Maps")
            }
        }

        // Search installed packages
        val launchIntent = pm.getInstalledApplications(0).firstOrNull { appInfo ->
            val label = pm.getApplicationLabel(appInfo).toString().lowercase()
            label.contains(query)
        }?.let { pm.getLaunchIntentForPackage(it.packageName) }

        return if (launchIntent != null) {
            launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(launchIntent)
            ActionResult(true, "Opened $appName")
        } else {
            ActionResult(false, "Could not find app '$appName' on device")
        }
    }

    private fun tryLaunch(intent: Intent, appName: String): ActionResult {
        return try {
            context.startActivity(intent)
            ActionResult(true, "Opened $appName")
        } catch (e: Exception) {
            ActionResult(false, "Could not launch $appName")
        }
    }
}
