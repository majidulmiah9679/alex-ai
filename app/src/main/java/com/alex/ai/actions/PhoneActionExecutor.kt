package com.alex.ai.actions

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
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
                "toggle_flashlight" -> {
                    val enable = args?.get("enable")?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: true
                    toggleFlashlight(enable)
                }
                "control_volume" -> {
                    val direction = args?.get("direction")?.jsonPrimitive?.content ?: "up"
                    controlVolume(direction)
                }
                "open_settings" -> {
                    val type = args?.get("type")?.jsonPrimitive?.content ?: "general"
                    openSettings(type)
                }
                else -> ActionResult(false, "Unknown action: $actionName")
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to execute $actionName", e)
            ActionResult(false, "Error executing $actionName: ${e.message}")
        }
    }

    fun makeCall(phoneNumber: String, contactName: String = ""): ActionResult {
        val cleanNumber = phoneNumber.replace("[^0-9+]".toRegex(), "")
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$cleanNumber")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)

        val target = if (contactName.isNotBlank()) "$contactName ($cleanNumber)" else cleanNumber
        return ActionResult(true, "Dialer opened for $target")
    }

    fun sendSms(phoneNumber: String, message: String): ActionResult {
        val cleanNumber = phoneNumber.replace("[^0-9+]".toRegex(), "")
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$cleanNumber")
            putExtra("sms_body", message)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return ActionResult(true, "SMS drafted for $cleanNumber")
    }

    fun openWhatsApp(phoneNumber: String = "", message: String = ""): ActionResult {
        val pm = context.packageManager
        val cleanNumber = phoneNumber.replace("[^0-9]".toRegex(), "")

        // If phone number or message is specified, use WhatsApp API deep link
        if (cleanNumber.isNotBlank() || message.isNotBlank()) {
            val encodedMsg = Uri.encode(message)
            val uri = if (cleanNumber.isNotBlank()) {
                Uri.parse("https://api.whatsapp.com/send?phone=$cleanNumber&text=$encodedMsg")
            } else {
                Uri.parse("https://api.whatsapp.com/send?text=$encodedMsg")
            }
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            return tryLaunch(intent, "WhatsApp")
        }

        // Otherwise open WhatsApp application directly
        val waPackages = listOf("com.whatsapp", "com.whatsapp.w4b")
        for (pkg in waPackages) {
            val launchIntent = pm.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(launchIntent)
                return ActionResult(true, "Opened WhatsApp")
            }
        }

        // Web fallback
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://web.whatsapp.com")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return tryLaunch(webIntent, "WhatsApp Web")
    }

    fun openApp(appName: String): ActionResult {
        val pm = context.packageManager
        val query = appName.lowercase().trim()

        // 1. Direct package mappings for top apps
        val directPackage = when {
            query.contains("youtube") || query.contains("ইউটিউব") || query == "yt" -> "com.google.android.youtube"
            query.contains("whatsapp") || query.contains("হোয়াটসঅ্যাপ") || query == "wa" -> "com.whatsapp"
            query.contains("facebook") || query.contains("ফেসবুক") || query == "fb" -> "com.facebook.katana"
            query.contains("instagram") || query.contains("ইনস্টাগ্রাম") || query.contains("insta") -> "com.instagram.android"
            query.contains("chrome") || query.contains("ক্রোম") -> "com.android.chrome"
            query.contains("spotify") || query.contains("স্পটিফাই") -> "com.spotify.music"
            query.contains("telegram") || query.contains("টেলিগ্রাম") -> "org.telegram.messenger"
            query.contains("map") || query.contains("ম্যাপ") -> "com.google.android.apps.maps"
            query.contains("play store") || query.contains("প্লে স্টোর") -> "com.android.vending"
            query.contains("calculator") || query.contains("ক্যালকুলেটর") -> "com.google.android.calculator"
            query.contains("gmail") || query.contains("ইমেইল") -> "com.google.android.gm"
            else -> null
        }

        if (directPackage != null) {
            val launchIntent = pm.getLaunchIntentForPackage(directPackage)
            if (launchIntent != null) {
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(launchIntent)
                return ActionResult(true, "Opened $appName")
            }
            // If primary WhatsApp package not found, try Business
            if (directPackage == "com.whatsapp") {
                val waBizIntent = pm.getLaunchIntentForPackage("com.whatsapp.w4b")
                if (waBizIntent != null) {
                    waBizIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(waBizIntent)
                    return ActionResult(true, "Opened WhatsApp Business")
                }
            }
            // URL fallbacks
            if (directPackage == "com.google.android.youtube") {
                val ytWeb = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                return tryLaunch(ytWeb, "YouTube")
            }
        }

        // 2. Camera Action
        if (query.contains("camera") || query.contains("ক্যামেরা")) {
            val camIntent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            return tryLaunch(camIntent, "Camera")
        }

        // 3. Settings Action
        if (query.contains("setting") || query.contains("সেটিংস")) {
            val setIntent = Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            return tryLaunch(setIntent, "Settings")
        }

        // 4. Dial / Phone
        if (query.contains("dial") || query.contains("phone") || query.contains("ফোন")) {
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            return tryLaunch(dialIntent, "Phone Dialer")
        }

        // 5. Search all installed applications using PackageManager
        try {
            val installedApps = pm.getInstalledApplications(0)
            val matchedApp = installedApps.firstOrNull { appInfo ->
                val label = pm.getApplicationLabel(appInfo).toString().lowercase()
                label.contains(query) || appInfo.packageName.lowercase().contains(query)
            }
            if (matchedApp != null) {
                val launchIntent = pm.getLaunchIntentForPackage(matchedApp.packageName)
                if (launchIntent != null) {
                    launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(launchIntent)
                    val label = pm.getApplicationLabel(matchedApp).toString()
                    return ActionResult(true, "Opened $label")
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Package search warning: ${e.message}")
        }

        // 6. Final fallback: Search on Google or Play Store
        val searchIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=$query")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return tryLaunch(searchIntent, "Play Store for $appName")
    }

    fun toggleFlashlight(enable: Boolean): ActionResult {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return ActionResult(false, "No flashlight available")
            cameraManager.setTorchMode(cameraId, enable)
            ActionResult(true, if (enable) "Flashlight turned ON" else "Flashlight turned OFF")
        } catch (e: Exception) {
            ActionResult(false, "Flashlight error: ${e.message}")
        }
    }

    fun controlVolume(direction: String): ActionResult {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val adjust = if (direction == "up") AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, adjust, AudioManager.FLAG_SHOW_UI)
            ActionResult(true, "Volume adjusted")
        } catch (e: Exception) {
            ActionResult(false, "Volume error: ${e.message}")
        }
    }

    fun openSettings(type: String): ActionResult {
        val action = when (type.lowercase()) {
            "wifi" -> Settings.ACTION_WIFI_SETTINGS
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "sound" -> Settings.ACTION_SOUND_SETTINGS
            "display" -> Settings.ACTION_DISPLAY_SETTINGS
            "accessibility" -> Settings.ACTION_ACCESSIBILITY_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }
        val intent = Intent(action).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        context.startActivity(intent)
        return ActionResult(true, "Opened $type settings")
    }

    private fun tryLaunch(intent: Intent, appName: String): ActionResult {
        return try {
            context.startActivity(intent)
            ActionResult(true, "Opened $appName")
        } catch (e: Exception) {
            ActionResult(false, "Could not open $appName: ${e.message}")
        }
    }
}
