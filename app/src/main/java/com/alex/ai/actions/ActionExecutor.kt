package com.alex.ai.actions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.alex.ai.accessibility.AlexAccessibilityService
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

interface ActionExecutor {
    suspend fun execute(actionName: String, args: JsonObject?): ActionResult
}

/**
 * Store Flavor Action Executor:
 * Standard Android Intents (Dialer, SMS, WhatsApp, App Launcher).
 * 100% compliant with Google Play Store policies (no accessibility/screen reading).
 */
class StoreActionExecutor(private val context: Context) : ActionExecutor {
    private val tag = "StoreActionExecutor"

    override suspend fun execute(actionName: String, args: JsonObject?): ActionResult {
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
            Log.e(tag, "Error executing $actionName", e)
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
            ActionResult(false, "WhatsApp not installed")
        }
    }

    private fun openApp(appName: String): ActionResult {
        val pm = context.packageManager
        val query = appName.lowercase().trim()

        if (query == "youtube") {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            return tryLaunch(intent, "YouTube")
        }

        val launchIntent = pm.getInstalledApplications(0).firstOrNull { appInfo ->
            val label = pm.getApplicationLabel(appInfo).toString().lowercase()
            label.contains(query)
        }?.let { pm.getLaunchIntentForPackage(it.packageName) }

        return if (launchIntent != null) {
            launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(launchIntent)
            ActionResult(true, "Opened $appName")
        } else {
            ActionResult(false, "App '$appName' not found")
        }
    }

    private fun tryLaunch(intent: Intent, appName: String): ActionResult {
        return try {
            context.startActivity(intent)
            ActionResult(true, "Opened $appName")
        } catch (e: Exception) {
            ActionResult(false, "Could not open $appName")
        }
    }
}

/**
 * Direct Flavor Action Executor:
 * Full autonomous device control via AlexAccessibilityService (screen reading,
 * auto-tapping, typing into inputs, and social media posting).
 */
class DirectActionExecutor(
    private val context: Context,
    private val storeExecutor: StoreActionExecutor = StoreActionExecutor(context)
) : ActionExecutor {

    override suspend fun execute(actionName: String, args: JsonObject?): ActionResult {
        val accessibility = AlexAccessibilityService.instance

        when (actionName) {
            "screen_tap_text" -> {
                val text = args?.get("text")?.jsonPrimitive?.content ?: ""
                val success = accessibility?.clickText(text) ?: false
                return ActionResult(success, if (success) "Tapped '$text'" else "Text '$text' not found on screen")
            }
            "screen_type_text" -> {
                val text = args?.get("text")?.jsonPrimitive?.content ?: ""
                val success = accessibility?.typeText(text) ?: false
                return ActionResult(success, if (success) "Typed '$text'" else "No editable input found")
            }
            "screen_scroll" -> {
                val success = accessibility?.scrollDown() ?: false
                return ActionResult(success, if (success) "Scrolled down" else "Could not scroll")
            }
            "post_social" -> {
                val platform = args?.get("platform")?.jsonPrimitive?.content ?: "Instagram"
                val caption = args?.get("caption")?.jsonPrimitive?.content ?: ""
                // Safety gate: Requires user confirmation before irrevocable action
                return ActionResult(true, "Prepared $platform post with caption: '$caption'. Confirm to publish.")
            }
            else -> return storeExecutor.execute(actionName, args)
        }
    }
}
