package com.alex.ai.actions

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.provider.AlarmClock
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
                // Communications
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

                // Hardware Controls
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

                // Travel & Navigation
                "navigate_maps" -> {
                    val query = args?.get("query")?.jsonPrimitive?.content ?: "nearby restaurants"
                    navigateMaps(query)
                }

                // Media & Search
                "search_youtube" -> {
                    val query = args?.get("query")?.jsonPrimitive?.content ?: ""
                    searchYouTube(query)
                }

                // Productivity
                "set_alarm" -> {
                    val hour = args?.get("hour")?.jsonPrimitive?.content?.toIntOrNull() ?: 7
                    val minute = args?.get("minute")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                    val message = args?.get("message")?.jsonPrimitive?.content ?: "Alex Alarm"
                    setAlarm(hour, minute, message)
                }
                "set_timer" -> {
                    val seconds = args?.get("seconds")?.jsonPrimitive?.content?.toIntOrNull() ?: 300
                    val message = args?.get("message")?.jsonPrimitive?.content ?: "Alex Timer"
                    setTimer(seconds, message)
                }

                // Diagnostics & Telemetry
                "get_battery_status" -> getBatteryStatus()
                "get_ram_status" -> getRamStatus()
                "get_storage_status" -> getStorageStatus()
                "get_device_specs" -> getDeviceSpecs()

                // In-App Gestures & Autonomous Accessibility
                "click_screen", "screen_tap_text", "click_element" -> {
                    val target = args?.get("target")?.jsonPrimitive?.content ?: args?.get("text")?.jsonPrimitive?.content ?: ""
                    clickOnScreen(target)
                }
                "scroll_screen", "screen_scroll" -> {
                    val direction = args?.get("direction")?.jsonPrimitive?.content ?: "down"
                    scrollScreen(direction)
                }
                "go_back" -> goBack()
                "go_home" -> goHome()
                "open_recents", "recent_apps" -> openRecentApps()
                "open_notifications" -> openNotifications()
                "open_quick_settings" -> openQuickSettings()
                "open_accessibility_settings" -> openAccessibilitySettings()
                "open_app_details", "allow_restricted_settings" -> openAppDetailsSettings()

                else -> ActionResult(false, "Unknown action: $actionName")
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to execute $actionName", e)
            ActionResult(false, "Error executing $actionName: ${e.message}")
        }
    }

    // --- Communication Methods ---

    fun makeCall(phoneNumber: String, contactName: String = ""): ActionResult {
        val cleanNumber = phoneNumber.replace("[^0-9+]".toRegex(), "")
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = if (cleanNumber.isNotBlank()) Uri.parse("tel:$cleanNumber") else Uri.parse("tel:")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)

        val target = if (contactName.isNotBlank()) "$contactName ($cleanNumber)" else if (cleanNumber.isNotBlank()) cleanNumber else "Phone Dialer"
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
            return tryLaunch(intent, "WhatsApp Chat")
        }

        val waPackages = listOf("com.whatsapp", "com.whatsapp.w4b")
        for (pkg in waPackages) {
            val launchIntent = pm.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(launchIntent)
                return ActionResult(true, "Opened WhatsApp")
            }
        }

        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://web.whatsapp.com")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return tryLaunch(webIntent, "WhatsApp Web")
    }

    // --- App Launching Hub ---

    fun openApp(appName: String): ActionResult {
        val pm = context.packageManager
        val query = appName.lowercase().trim()

        val directPackage = when {
            query.contains("youtube") || query.contains("ইউটিউব") || query == "yt" -> "com.google.android.youtube"
            query.contains("whatsapp") || query.contains("হোয়াটসঅ্যাপ") || query.contains("ওয়াটসঅ্যাপ") || query == "wa" -> "com.whatsapp"
            query.contains("facebook") || query.contains("ফেসবুক") || query == "fb" -> "com.facebook.katana"
            query.contains("instagram") || query.contains("ইনস্টাগ্রাম") || query.contains("ইন্সটাগ্রাম") || query.contains("insta") -> "com.instagram.android"
            query.contains("tiktok") || query.contains("টিকটক") -> "com.zhiliaoapp.musically"
            query.contains("messenger") || query.contains("মেসেঞ্জার") -> "com.facebook.orca"
            query.contains("snapchat") || query.contains("স্ন্যাপচ্যাট") -> "com.snapchat.android"
            query.contains("bkash") || query.contains("বিকাশ") -> "com.bKash.customerapp"
            query.contains("nagad") || query.contains("নগদ") -> "com.konasl.nagad"
            query.contains("rocket") || query.contains("রকেট") -> "com.dbbl.mbs.apps.holder"
            query.contains("chrome") || query.contains("ক্রোম") || query == "browser" -> "com.android.chrome"
            query.contains("spotify") || query.contains("স্পটিফাই") || query.contains("গান") -> "com.spotify.music"
            query.contains("telegram") || query.contains("টেলিগ্রাম") -> "org.telegram.messenger"
            query.contains("map") || query.contains("ম্যাপ") -> "com.google.android.apps.maps"
            query.contains("play store") || query.contains("প্লে স্টোর") || query.contains("প্লেস্টোর") -> "com.android.vending"
            query.contains("calculator") || query.contains("ক্যালকুলেটর") -> "com.google.android.calculator"
            query.contains("gmail") || query.contains("ইমেইল") || query.contains("জিমেইল") -> "com.google.android.gm"
            query.contains("clock") || query.contains("ঘড়ি") || query.contains("ঘড়ি") || query.contains("alarm") || query.contains("অ্যালার্ম") -> "com.google.android.deskclock"
            query.contains("file") || query.contains("ফাইল") -> "com.google.android.apps.nbu.files"
            query.contains("drive") || query.contains("ড্রাইভ") -> "com.google.android.apps.docs"
            query.contains("twitter") || query == "x" || query.contains("টুইটার") -> "com.twitter.android"
            query.contains("netflix") || query.contains("নেটফ্লিক্স") -> "com.netflix.mediaclient"
            query.contains("prime") -> "com.amazon.avod.thirdpartyclient"
            query.contains("hotstar") -> "in.startv.hotstar"
            query.contains("truecaller") -> "com.truecaller"
            query.contains("uber") || query.contains("pathao") || query.contains("পাঠাও") -> "com.ubercab"
            query.contains("swiggy") -> "in.swiggy.android"
            query.contains("zomato") -> "com.application.zomato"
            else -> null
        }

        if (directPackage != null) {
            val launchIntent = pm.getLaunchIntentForPackage(directPackage)
            if (launchIntent != null) {
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(launchIntent)
                return ActionResult(true, "Opened $appName")
            }
            if (directPackage == "com.whatsapp") {
                val waBizIntent = pm.getLaunchIntentForPackage("com.whatsapp.w4b")
                if (waBizIntent != null) {
                    waBizIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(waBizIntent)
                    return ActionResult(true, "Opened WhatsApp Business")
                }
            }
            if (directPackage == "com.google.android.youtube") {
                val ytWeb = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                return tryLaunch(ytWeb, "YouTube")
            }
        }

        if (query.contains("camera") || query.contains("ক্যামেরা")) {
            val camIntent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            return tryLaunch(camIntent, "Camera")
        }

        if (query.contains("setting") || query.contains("সেটিংস")) {
            val setIntent = Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            return tryLaunch(setIntent, "Settings")
        }

        if (query.contains("dial") || query.contains("phone") || query.contains("ফোন")) {
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            return tryLaunch(dialIntent, "Phone Dialer")
        }

        try {
            val installedApps = pm.getInstalledApplications(0)
            val matchedApp = installedApps.firstOrNull { appInfo ->
                val label = pm.getApplicationLabel(appInfo).toString().lowercase().trim()
                val pkg = appInfo.packageName.lowercase()
                label == query || label.contains(query) || (query.isNotBlank() && query.contains(label)) || pkg.contains(query)
            }
            if (matchedApp != null) {
                val launchIntent = pm.getLaunchIntentForPackage(matchedApp.packageName)
                if (launchIntent != null) {
                    launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(launchIntent)
                    val label = pm.getApplicationLabel(matchedApp).toString()
                    return ActionResult(true, "$label khulchhi, Sir!")
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Package search warning: ${e.message}")
        }

        val searchIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=$query")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return tryLaunch(searchIntent, "Play Store for $appName")
    }

    fun cleanAppQuery(raw: String): String {
        var q = raw.lowercase().trim()
        val removeWords = listOf(
            "kholo", "khulbe", "khol", "khulo", "chalao", "open", "launch", "start",
            "khule daw", "khule dao", "chalaw", "খোল", "খুলবে", "খোলো", "চালাও", "অন করো", "খুলে দাও"
        )
        for (w in removeWords) {
            q = q.replace(w, "").trim()
        }
        return q
    }

    // --- In-App Visual Agent Actions (Click, Scroll, Back, Home, Recents) ---

    fun clickOnScreen(targetText: String): ActionResult {
        val service = com.alex.ai.accessibility.AlexAccessibilityService.instance
        if (service == null) {
            openAccessibilitySettings()
            return ActionResult(false, "Accessibility permission proyojon. Settings khulchhi!")
        }
        val success = service.clickElement(targetText)
        return if (success) {
            ActionResult(true, "'$targetText'-e click korechhi!")
        } else {
            ActionResult(false, "'$targetText' screen-e khuje paini.")
        }
    }

    fun scrollScreen(direction: String): ActionResult {
        val service = com.alex.ai.accessibility.AlexAccessibilityService.instance
        if (service == null) {
            openAccessibilitySettings()
            return ActionResult(false, "Accessibility permission proyojon. Settings khulchhi!")
        }
        val success = when (direction.lowercase().trim()) {
            "up", "উপরে" -> service.scrollUp()
            "left", "বামে" -> service.swipeLeft()
            "right", "ডানে" -> service.swipeRight()
            else -> service.scrollDown()
        }
        return ActionResult(success, if (success) "Scroll korechhi!" else "Scroll failed.")
    }

    fun goBack(): ActionResult {
        val service = com.alex.ai.accessibility.AlexAccessibilityService.instance
        if (service != null && service.goBack()) {
            return ActionResult(true, "Back-e gechhi, Sir!")
        }
        return ActionResult(false, "Accessibility permission proyojon.")
    }

    fun goHome(): ActionResult {
        val service = com.alex.ai.accessibility.AlexAccessibilityService.instance
        if (service != null && service.goHome()) {
            return ActionResult(true, "Home screen-e gechhi!")
        }
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(homeIntent)
        return ActionResult(true, "Home screen-e gechhi!")
    }

    fun openRecentApps(): ActionResult {
        val service = com.alex.ai.accessibility.AlexAccessibilityService.instance
        if (service != null && service.openRecents()) {
            return ActionResult(true, "Recent apps dekhachhi!")
        }
        return ActionResult(false, "Accessibility permission proyojon.")
    }

    fun openNotifications(): ActionResult {
        val service = com.alex.ai.accessibility.AlexAccessibilityService.instance
        if (service != null && service.openNotifications()) {
            return ActionResult(true, "Notification bar namiechhi!")
        }
        return ActionResult(false, "Accessibility permission proyojon.")
    }

    fun openAccessibilitySettings(): ActionResult {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return tryLaunch(intent, "Accessibility Settings")
    }

    fun openAppDetailsSettings(): ActionResult {
        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:" + context.packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ActionResult(true, "App Info khulchhi. 3-dot (⋮) a giye 'Allow restricted settings' select korun.")
        } catch (e: Exception) {
            ActionResult(false, "Could not open App Info: ${e.message}")
        }
    }

    fun openQuickSettings(): ActionResult {
        val service = com.alex.ai.accessibility.AlexAccessibilityService.instance
        if (service != null && service.openQuickSettings()) {
            return ActionResult(true, "Quick Settings khulchhi!")
        }
        return openSettings("general")
    }

    // --- Hardware Controls ---

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
            when (direction.lowercase()) {
                "max" -> {
                    val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, AudioManager.FLAG_SHOW_UI)
                    ActionResult(true, "Volume set to 100% Maximum")
                }
                "mute" -> {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI)
                    ActionResult(true, "Media audio muted")
                }
                "down" -> {
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                    ActionResult(true, "Volume decreased")
                }
                else -> {
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                    ActionResult(true, "Volume increased")
                }
            }
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
            "battery" -> Intent.ACTION_POWER_USAGE_SUMMARY
            "apps" -> Settings.ACTION_APPLICATION_SETTINGS
            "storage" -> Settings.ACTION_INTERNAL_STORAGE_SETTINGS
            "location" -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
            "developer" -> Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS
            "date" -> Settings.ACTION_DATE_SETTINGS
            "nfc" -> Settings.ACTION_NFC_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }
        val intent = Intent(action).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        return tryLaunch(intent, "$type settings")
    }

    // --- Travel & Navigation ---

    fun navigateMaps(query: String): ActionResult {
        val gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(query))
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return tryLaunch(mapIntent, "Maps navigation for '$query'")
    }

    // --- Media & Entertainment ---

    fun searchYouTube(query: String): ActionResult {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(query))).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return tryLaunch(intent, "YouTube search for '$query'")
    }

    // --- Time & Productivity ---

    fun setAlarm(hour: Int, minute: Int, message: String): ActionResult {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, message)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return tryLaunch(intent, "Alarm for %02d:%02d".format(hour, minute))
    }

    fun setTimer(seconds: Int, message: String): ActionResult {
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            putExtra(AlarmClock.EXTRA_MESSAGE, message)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return tryLaunch(intent, "Timer for $seconds seconds")
    }

    // --- System Telemetry & Diagnostics ---

    fun getBatteryStatus(): ActionResult {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val isCharging = bm.isCharging
            val status = if (isCharging) "Charging ⚡" else "Discharging"
            ActionResult(true, "Battery level is $level% ($status)")
        } catch (e: Exception) {
            ActionResult(false, "Could not read battery: ${e.message}")
        }
    }

    fun getRamStatus(): ActionResult {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            am.getMemoryInfo(memInfo)
            val totalGb = String.format("%.1f", memInfo.totalMem / (1024.0 * 1024 * 1024))
            val availGb = String.format("%.1f", memInfo.availMem / (1024.0 * 1024 * 1024))
            ActionResult(true, "RAM Telemetry: Available $availGb GB out of $totalGb GB Total")
        } catch (e: Exception) {
            ActionResult(false, "Could not read RAM: ${e.message}")
        }
    }

    fun getStorageStatus(): ActionResult {
        return try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong
            val totalGb = String.format("%.1f", (totalBlocks * blockSize) / (1024.0 * 1024 * 1024))
            val freeGb = String.format("%.1f", (availableBlocks * blockSize) / (1024.0 * 1024 * 1024))
            ActionResult(true, "Storage Telemetry: Free $freeGb GB of $totalGb GB Internal Space")
        } catch (e: Exception) {
            ActionResult(false, "Could not read storage: ${e.message}")
        }
    }

    fun getDeviceSpecs(): ActionResult {
        val model = Build.MODEL
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        val release = Build.VERSION.RELEASE
        val sdk = Build.VERSION.SDK_INT
        val uptimeHours = SystemClock.elapsedRealtime() / (1000 * 60 * 60)
        return ActionResult(true, "$manufacturer $model running Android $release (API $sdk). System uptime: $uptimeHours hours.")
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
