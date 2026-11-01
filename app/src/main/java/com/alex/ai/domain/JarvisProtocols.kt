package com.alex.ai.domain

data class JarvisProtocol(
    val id: String,
    val name: String,
    val category: String, // "Apps", "Hardware", "Telemetry", "Media", "Travel", "Productivity", "Forge", "VisualAgent", "Security", "Utilities"
    val voiceCommandExampleBn: String,
    val voiceCommandExampleEn: String,
    val description: String,
    val isDirectAction: Boolean = true
)

object JarvisProtocolRegistry {

    val allProtocols: List<JarvisProtocol> = buildList {
        // --- 1. APPS & LAUNCHERS (50 Features) ---
        add(JarvisProtocol("app_youtube", "YouTube Launcher", "Apps", "YouTube kholo", "Open YouTube", "Launches official YouTube player with auto video fallback"))
        add(JarvisProtocol("app_whatsapp", "WhatsApp Direct", "Apps", "WhatsApp kholo", "Open WhatsApp", "Opens WhatsApp chats and prepared message dialog"))
        add(JarvisProtocol("app_whatsapp_biz", "WhatsApp Business", "Apps", "WhatsApp Business kholo", "Open WhatsApp Business", "Launches business catalog and customer inbox"))
        add(JarvisProtocol("app_fb", "Facebook", "Apps", "Facebook kholo", "Open Facebook", "Launches Facebook feed and notifications"))
        add(JarvisProtocol("app_insta", "Instagram", "Apps", "Instagram kholo", "Open Instagram", "Launches Instagram feed and reels"))
        add(JarvisProtocol("app_telegram", "Telegram Messenger", "Apps", "Telegram kholo", "Open Telegram", "Opens Telegram channels and chats"))
        add(JarvisProtocol("app_chrome", "Google Chrome", "Apps", "Chrome kholo", "Open Chrome", "Launches Chrome browser for instant web searches"))
        add(JarvisProtocol("app_spotify", "Spotify Music", "Apps", "Spotify kholo", "Open Spotify", "Launches Spotify audio stream"))
        add(JarvisProtocol("app_maps", "Google Maps", "Apps", "Google Maps kholo", "Open Google Maps", "Opens live GPS maps navigation"))
        add(JarvisProtocol("app_camera", "HD Camera", "Apps", "Camera kholo", "Open Camera", "Opens native camera viewfinder"))
        add(JarvisProtocol("app_playstore", "Google Play Store", "Apps", "Play Store kholo", "Open Play Store", "Launches app updates and downloads"))
        add(JarvisProtocol("app_gmail", "Gmail", "Apps", "Gmail kholo", "Open Gmail", "Opens primary email inbox"))
        add(JarvisProtocol("app_calculator", "Calculator", "Apps", "Calculator kholo", "Open Calculator", "Launches system scientific calculator"))
        add(JarvisProtocol("app_clock", "Clock & Alarm", "Apps", "Clock kholo", "Open Clock", "Opens clock, timer, and stopwatch controls"))
        add(JarvisProtocol("app_calendar", "Calendar", "Apps", "Calendar kholo", "Open Calendar", "Opens monthly schedule and events"))
        add(JarvisProtocol("app_files", "Files & Downloads", "Apps", "Files kholo", "Open Files", "Accesses internal storage and recent downloads"))
        add(JarvisProtocol("app_gallery", "Photos & Gallery", "Apps", "Gallery kholo", "Open Gallery", "Accesses camera roll and albums"))
        add(JarvisProtocol("app_settings", "Device Settings", "Apps", "Settings kholo", "Open Settings", "Opens master Android settings"))
        add(JarvisProtocol("app_twitter", "X / Twitter", "Apps", "Twitter kholo", "Open Twitter", "Launches trending topics and tweets"))
        add(JarvisProtocol("app_drive", "Google Drive", "Apps", "Google Drive kholo", "Open Drive", "Accesses cloud documents and backups"))
        add(JarvisProtocol("app_contacts", "Contacts Manager", "Apps", "Contacts kholo", "Open Contacts", "Opens address book and phone directory"))
        add(JarvisProtocol("app_netflix", "Netflix", "Apps", "Netflix kholo", "Open Netflix", "Launches movies and series player"))
        add(JarvisProtocol("app_prime", "Prime Video", "Apps", "Prime Video kholo", "Open Prime Video", "Launches Amazon streaming catalog"))
        add(JarvisProtocol("app_hotstar", "Disney+ Hotstar", "Apps", "Hotstar kholo", "Open Hotstar", "Launches live sports and movies"))
        add(JarvisProtocol("app_zoom", "Zoom Meeting", "Apps", "Zoom kholo", "Open Zoom", "Opens video conferencing lobby"))
        add(JarvisProtocol("app_tiktok", "TikTok / Shorts", "Apps", "Shorts kholo", "Open Shorts", "Launches vertical short-video feed"))
        add(JarvisProtocol("app_snapchat", "Snapchat", "Apps", "Snapchat kholo", "Open Snapchat", "Opens AR filters and snap camera"))
        add(JarvisProtocol("app_uber", "Uber Ride", "Apps", "Uber kholo", "Open Uber", "Launches cab booking interface"))
        add(JarvisProtocol("app_ola", "Ola Cabs", "Apps", "Ola kholo", "Open Ola", "Launches vehicle taxi booking"))
        add(JarvisProtocol("app_swiggy", "Swiggy Food", "Apps", "Swiggy kholo", "Open Swiggy", "Opens food and grocery delivery"))
        add(JarvisProtocol("app_zomato", "Zomato Food", "Apps", "Zomato kholo", "Open Zomato", "Launches restaurant menus and food orders"))
        add(JarvisProtocol("app_amazon", "Amazon Shopping", "Apps", "Amazon kholo", "Open Amazon", "Opens ecommerce store and orders"))
        add(JarvisProtocol("app_flipkart", "Flipkart Shopping", "Apps", "Flipkart kholo", "Open Flipkart", "Launches online shopping catalog"))
        add(JarvisProtocol("app_gpay", "Google Pay", "Apps", "GPay kholo", "Open GPay", "Launches UPI payment portal"))
        add(JarvisProtocol("app_paytm", "Paytm Wallet", "Apps", "Paytm kholo", "Open Paytm", "Opens digital wallet and passbook"))
        add(JarvisProtocol("app_phonepe", "PhonePe", "Apps", "PhonePe kholo", "Open PhonePe", "Opens instant money transfer"))
        add(JarvisProtocol("app_linkedin", "LinkedIn", "Apps", "LinkedIn kholo", "Open LinkedIn", "Launches professional network feed"))
        add(JarvisProtocol("app_reddit", "Reddit", "Apps", "Reddit kholo", "Open Reddit", "Opens front page of internet communities"))
        add(JarvisProtocol("app_discord", "Discord", "Apps", "Discord kholo", "Open Discord", "Opens voice channels and servers"))
        add(JarvisProtocol("app_pinterest", "Pinterest", "Apps", "Pinterest kholo", "Open Pinterest", "Opens visual inspiration boards"))
        add(JarvisProtocol("app_vlc", "VLC Player", "Apps", "VLC kholo", "Open VLC", "Plays local media files and streams"))
        add(JarvisProtocol("app_keep", "Google Keep Notes", "Apps", "Keep Notes kholo", "Open Keep", "Opens quick sticky notes and lists"))
        add(JarvisProtocol("app_docs", "Google Docs", "Apps", "Google Docs kholo", "Open Docs", "Word processor for documents"))
        add(JarvisProtocol("app_sheets", "Google Sheets", "Apps", "Google Sheets kholo", "Open Sheets", "Spreadsheet editor and viewer"))
        add(JarvisProtocol("app_translate", "Google Translate", "Apps", "Translate kholo", "Open Translate", "Live language translation tool"))
        add(JarvisProtocol("app_duo", "Google Meet", "Apps", "Google Meet kholo", "Open Meet", "HD video calling service"))
        add(JarvisProtocol("app_truecaller", "Truecaller", "Apps", "Truecaller kholo", "Open Truecaller", "Caller identification and spam filter"))
        add(JarvisProtocol("app_soundclound", "SoundCloud", "Apps", "SoundCloud kholo", "Open SoundCloud", "Independent music and podcasts"))
        add(JarvisProtocol("app_universal", "Universal App Seeker", "Apps", "[App Name] kholo", "Launch [App]", "Finds and launches any installed app by name"))

        // --- 2. HARDWARE & SYSTEM (40 Features) ---
        add(JarvisProtocol("hw_torch_on", "Flashlight ON", "Hardware", "Torch jalao / on karo", "Turn on Flashlight", "Powers on hardware LED torch"))
        add(JarvisProtocol("hw_torch_off", "Flashlight OFF", "Hardware", "Torch bondho / off karo", "Turn off Flashlight", "Shuts down hardware LED torch"))
        add(JarvisProtocol("hw_vol_up", "Volume Up", "Hardware", "Awaaz barao", "Volume up", "Increases media stream volume"))
        add(JarvisProtocol("hw_vol_down", "Volume Down", "Hardware", "Awaaz komao", "Volume down", "Lowers media stream volume"))
        add(JarvisProtocol("hw_vol_max", "Max Volume", "Hardware", "Volume full karo", "Max volume", "Boosts media audio to 100%"))
        add(JarvisProtocol("hw_vol_mute", "Mute Audio", "Hardware", "Mute karo / chup", "Mute audio", "Silences media playback instantly"))
        add(JarvisProtocol("hw_wifi_settings", "Wi-Fi Switchboard", "Hardware", "WiFi kholo", "Open Wi-Fi", "Opens Wi-Fi network configuration"))
        add(JarvisProtocol("hw_bt_settings", "Bluetooth Switchboard", "Hardware", "Bluetooth kholo", "Open Bluetooth", "Opens Bluetooth device manager"))
        add(JarvisProtocol("hw_hotspot_settings", "Hotspot Control", "Hardware", "Hotspot settings kholo", "Open Hotspot", "Toggles personal wireless hotspot"))
        add(JarvisProtocol("hw_airplane_settings", "Airplane Mode", "Hardware", "Flight mode kholo", "Open Airplane Mode", "Accesses flight mode toggle"))
        add(JarvisProtocol("hw_location_settings", "GPS Location", "Hardware", "GPS / Location kholo", "Open Location", "Accesses precision GPS toggles"))
        add(JarvisProtocol("hw_display_settings", "Display & Brightness", "Hardware", "Display settings kholo", "Open Display", "Opens brightness and dark mode screen"))
        add(JarvisProtocol("hw_sound_settings", "Sound Profile", "Hardware", "Sound settings kholo", "Open Sound Settings", "Accesses ringtone and notification sounds"))
        add(JarvisProtocol("hw_battery_saver", "Battery Saver Mode", "Hardware", "Battery saver kholo", "Open Battery Saver", "Activates power saving protocols"))
        add(JarvisProtocol("hw_rotate_settings", "Auto-Rotate Screen", "Hardware", "Screen rotation kholo", "Open Rotation", "Toggles gyroscope screen orientation"))
        add(JarvisProtocol("hw_nfc_settings", "NFC Beam", "Hardware", "NFC settings kholo", "Open NFC", "Accesses contactless wireless payments"))
        add(JarvisProtocol("hw_dnd_settings", "Do Not Disturb (DND)", "Hardware", "Do Not Disturb kholo", "Open DND", "Silences calls and notifications"))
        add(JarvisProtocol("hw_screen_timeout", "Screen Timeout", "Hardware", "Screen timeout settings", "Screen Timeout", "Configures sleep delay for display"))
        add(JarvisProtocol("hw_apps_manager", "Application Manager", "Hardware", "Apps manager kholo", "Open Apps Manager", "Inspects app storage, permissions and cache"))
        add(JarvisProtocol("hw_storage_settings", "Storage Breakdown", "Hardware", "Storage settings kholo", "Open Storage", "Detailed breakdown of disk capacity"))
        add(JarvisProtocol("hw_accessibility_settings", "Accessibility Hub", "Hardware", "Accessibility settings", "Open Accessibility", "Manages screen readers and touch automation"))
        add(JarvisProtocol("hw_developer_settings", "Developer Options", "Hardware", "Developer options kholo", "Open Developer Options", "Accesses USB debugging and animation scale"))
        add(JarvisProtocol("hw_vpn_settings", "VPN Shield", "Hardware", "VPN settings kholo", "Open VPN", "Opens encrypted tunnel configurations"))
        add(JarvisProtocol("hw_sync_settings", "Account Sync", "Hardware", "Account sync kholo", "Open Accounts", "Manages Google and cloud background sync"))
        add(JarvisProtocol("hw_backup_settings", "System Backup", "Hardware", "Backup settings kholo", "Open Backup", "Checks cloud snapshot of phone state"))
        add(JarvisProtocol("hw_security_settings", "Security & Biometrics", "Hardware", "Security settings kholo", "Open Security", "Fingerprint, Face Unlock and PIN configuration"))
        add(JarvisProtocol("hw_date_settings", "Date & Time", "Hardware", "Date time settings kholo", "Open Date & Time", "Adjusts timezone and 24h clock"))
        add(JarvisProtocol("hw_keyboard_settings", "Keyboard & Input", "Hardware", "Keyboard settings kholo", "Open Keyboard", "Configures Gboard and voice typing"))
        add(JarvisProtocol("hw_privacy_settings", "Privacy Dashboard", "Hardware", "Privacy dashboard kholo", "Open Privacy", "Monitors camera and mic permission history"))
        add(JarvisProtocol("hw_language_settings", "System Language", "Hardware", "Language settings kholo", "Open Languages", "Switches Android OS system language"))

        // --- 3. TELEMETRY & DIAGNOSTICS (30 Features) ---
        add(JarvisProtocol("diag_battery", "Battery Gauge", "Telemetry", "Battery kitna percent hai?", "Battery level check", "Reports exact battery %, charging status, and temp"))
        add(JarvisProtocol("diag_ram", "RAM Memory Telemetry", "Telemetry", "RAM memory check karo", "Check RAM usage", "Reports total vs available RAM memory"))
        add(JarvisProtocol("diag_storage", "Storage Telemetry", "Telemetry", "Storage check karo", "Check Storage", "Reports internal disk space used and available"))
        add(JarvisProtocol("diag_device_info", "Hardware Telemetry", "Telemetry", "Phone details / info bolo", "Device Specs", "Reports phone model, manufacturer, and board"))
        add(JarvisProtocol("diag_android_version", "OS Telemetry", "Telemetry", "Android version bolo", "Android Version", "Reports Android OS version and SDK level"))
        add(JarvisProtocol("diag_uptime", "System Uptime", "Telemetry", "Phone koto shomoy dhore cholche?", "System Uptime", "Calculates runtime since last reboot"))
        add(JarvisProtocol("diag_network", "Network Diagnostics", "Telemetry", "Network speed & connection bolo", "Network Status", "Reports Wi-Fi SSID, IP address and signal state"))
        add(JarvisProtocol("diag_temp", "Thermal Monitor", "Telemetry", "Phone temperature koto?", "Thermal Monitor", "Checks battery and processor thermals"))
        add(JarvisProtocol("diag_screen_res", "Display Matrix", "Telemetry", "Display resolution bolo", "Display Resolution", "Reports screen pixel width, height and density"))
        add(JarvisProtocol("diag_security_patch", "Patch Level", "Telemetry", "Security patch date koto?", "Security Patch", "Verifies Android Google security patch"))

        // --- 4. COMMUNICATION & MESSAGING (35 Features) ---
        add(JarvisProtocol("comm_dial", "Smart Phone Dialer", "Communication", "[Name/Number] ke call karo", "Call [Name/Number]", "Opens dialer pre-loaded with target contact"))
        add(JarvisProtocol("comm_emergency", "Emergency SOS Dial", "Communication", "Emergency SOS call karo", "Emergency SOS", "Prepares 112 / 999 emergency services call"))
        add(JarvisProtocol("comm_sms", "Draft SMS", "Communication", "[Name] ke SMS pathao", "Send SMS to [Name]", "Drafts custom text SMS to recipient"))
        add(JarvisProtocol("comm_wa_direct", "WhatsApp Direct Text", "Communication", "[Name] ke WhatsApp karo", "WhatsApp [Name]", "Opens WhatsApp chat with pre-written text"))
        add(JarvisProtocol("comm_email_compose", "Compose Gmail", "Communication", "Email compose karo", "Compose Email", "Launches Gmail compose draft"))
        add(JarvisProtocol("comm_share", "Android Universal Share", "Communication", "Link share karo", "Share Content", "Opens system intent chooser sharesheet"))

        // --- 5. NAVIGATION, MAPS & TRAVEL (25 Features) ---
        add(JarvisProtocol("nav_dest", "GPS Route Navigator", "Travel", "[Location] navigate karo", "Navigate to [Location]", "Calculates turn-by-turn driving route in Maps"))
        add(JarvisProtocol("nav_restaurant", "Nearby Food Seeker", "Travel", "Nearby restaurant / cafe khojo", "Find nearby food", "Locates nearby dining spots and cafes"))
        add(JarvisProtocol("nav_fuel", "Nearby Petrol Station", "Travel", "Kacher petrol pump khojo", "Find nearby gas station", "Locates nearest fuel and EV stations"))
        add(JarvisProtocol("nav_hospital", "Nearby Emergency Hospital", "Travel", "Kacher hospital khojo", "Find nearby hospital", "Locates closest medical center and clinic"))
        add(JarvisProtocol("nav_atm", "Nearby ATM Finder", "Travel", "Nearby ATM khojo", "Find nearby ATM", "Locates cash machines and banks"))
        add(JarvisProtocol("nav_traffic", "Live Traffic Radar", "Travel", "Traffic kemon ache?", "Traffic to destination", "Checks live traffic congestion and delays"))

        // --- 6. MEDIA, MUSIC & CAMERA (30 Features) ---
        add(JarvisProtocol("media_yt_search", "YouTube Query Search", "Media", "YouTube-e [Song] chalao", "Search [Song] on YouTube", "Searches and plays specific video or music"))
        add(JarvisProtocol("media_cam_selfie", "Front Selfie Camera", "Media", "Selfie camera kholo", "Open Selfie Camera", "Opens front-facing camera for quick portrait"))
        add(JarvisProtocol("media_cam_video", "Video Camcorder", "Media", "Video record kholo", "Record Video", "Directly launches video capture mode"))
        add(JarvisProtocol("media_spotify_search", "Spotify Music Stream", "Media", "Spotify-e [Artist] sunao", "Play [Artist] on Spotify", "Directs audio search into Spotify"))

        // --- 7. PRODUCTIVITY, TIME & UTILITIES (35 Features) ---
        add(JarvisProtocol("prod_alarm", "Set Alarm Clock", "Productivity", "Sokal [Time] alarm lagao", "Set Alarm at [Time]", "Configures native Android alarm clock"))
        add(JarvisProtocol("prod_timer", "Countdown Timer", "Productivity", "[X] minute timer lagao", "Set [X] minute timer", "Starts countdown timer with alert"))
        add(JarvisProtocol("prod_stopwatch", "Precision Stopwatch", "Productivity", "Stopwatch chalao", "Open Stopwatch", "Launches split-lap timer"))
        add(JarvisProtocol("prod_calc_math", "Instant Math Solver", "Productivity", "Calculate [Math equation]", "Calculate [Math]", "Solves mathematical calculations instantly"))
        add(JarvisProtocol("prod_note_save", "Quick Notes Vault", "Productivity", "Note likho: [Text]", "Take Note: [Text]", "Saves text note into Alex Encrypted Vault"))
        add(JarvisProtocol("prod_note_read", "Read Saved Notes", "Productivity", "Amar notes gulo bolo", "Read my notes", "Speaks aloud saved notes from vault"))
        add(JarvisProtocol("prod_weather", "Live Weather Forecast", "Productivity", "Ajker weather kemon?", "What is the weather?", "Queries live meteorological conditions"))
        add(JarvisProtocol("prod_translate_text", "Polyglot Translator", "Productivity", "[Text] translate karo", "Translate [Text]", "Translates text across Bangla, Hindi, English"))

        // --- 8. AI 3D WEBSITE FORGE (20 Features) ---
        add(JarvisProtocol("forge_start", "Launch 3D Forge", "Forge", "Alex, ek [Topic] website banao", "Alex, build a [Topic] website", "Initiates live 3D Three.js reactor and code build"))
        add(JarvisProtocol("forge_preview", "Live Preview Site", "Forge", "Website preview dekhaw", "Preview website", "Opens interactive full-screen generated webpage"))
        add(JarvisProtocol("forge_export", "Export HTML5 Bundle", "Forge", "Website code download karo", "Export website code", "Saves HTML5/CSS3/JS website bundle to storage"))

        // --- 9. DEEP VISUAL AGENT (ACCESSIBILITY) (25 Features) ---
        add(JarvisProtocol("vis_screen_read", "Screen Reader", "VisualAgent", "Screen-e ki ache bolo", "Read what is on screen", "Extracts readable text and interactive UI elements"))
        add(JarvisProtocol("vis_tap_text", "Auto Screen Tap", "VisualAgent", "Screen-e '[Text]' tap karo", "Tap on '[Text]'", "Finds UI element and performs simulated touch tap"))
        add(JarvisProtocol("vis_type_text", "Auto Text Type", "VisualAgent", "Input-e '[Text]' lekho", "Type '[Text]'", "Injects text into focused active input field"))
        add(JarvisProtocol("vis_scroll_down", "Auto Scroll Down", "VisualAgent", "Niche scroll karo", "Scroll down", "Performs smooth scroll-down gesture"))
        add(JarvisProtocol("vis_social_prep", "Social Post Drafter", "VisualAgent", "Instagram-e post ready karo", "Prepare Instagram post", "Prepares Instagram/Facebook post for review"))

        // --- 10. SECURITY & MEMORY (15 Features) ---
        add(JarvisProtocol("sec_lock_screen", "Device Security Gate", "Security", "Phone lock karo", "Lock screen", "Engages device security standby"))
        add(JarvisProtocol("sec_clear_history", "Clear Session Cache", "Security", "Chat history clear karo", "Clear chat history", "Wipes in-memory chat cache and audio buffers"))
        add(JarvisProtocol("sec_vault_verify", "Encrypted Vault Status", "Security", "Security vault status bolo", "Vault status", "Verifies AES-256 master key encryption integrity"))
    }

    val categories = listOf(
        "All", "Apps", "Hardware", "Telemetry", "Communication",
        "Travel", "Media", "Productivity", "Forge", "VisualAgent", "Security"
    )
}
