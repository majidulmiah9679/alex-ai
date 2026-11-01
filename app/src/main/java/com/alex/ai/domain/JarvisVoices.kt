package com.alex.ai.domain

data class JarvisVoice(
    val id: String,
    val name: String,
    val gender: String, // "Female" or "Male"
    val geminiVoice: String,
    val defaultPitch: Float,
    val defaultSpeed: Float,
    val personaTitle: String,
    val description: String,
    val avatarEmoji: String,
    val greetingBn: String,
    val greetingHi: String,
    val greetingEn: String
)

object JarvisVoiceRegistry {

    val femaleVoices = listOf(
        JarvisVoice(
            id = "female_friday",
            name = "FRIDAY (JARVIS AI)",
            gender = "Female",
            geminiVoice = "Aoede",
            defaultPitch = 1.05f,
            defaultSpeed = 1.0f,
            personaTitle = "Intelligent AI Assistant",
            description = "Calm, razor-sharp intellect, sophisticated sci-fi assistant voice. Highly articulate.",
            avatarEmoji = "🤖",
            greetingBn = "নমস্কার! আমি ফ্রাইডে। আপনার ফোন ও সিস্টেম সম্পূর্ণ নিয়ন্ত্রণে আছে। আদেশ করুন, স্যার।",
            greetingHi = "नमस्ते! मैं फ्राइडे हूँ। आपके सभी सिस्टम तैयार हैं। आज क्या टास्क पूरा करना है?",
            greetingEn = "Good day. I am FRIDAY. All systems and protocols are fully operational. Ready for your command, Sir."
        ),
        JarvisVoice(
            id = "female_tara",
            name = "Tara (Sweet & Warm)",
            gender = "Female",
            geminiVoice = "Kore",
            defaultPitch = 1.15f,
            defaultSpeed = 0.95f,
            personaTitle = "Warm Companion & Friend",
            description = "Natural sweet female voice, warm, patient and comforting like an elder sister.",
            avatarEmoji = "🌸",
            greetingBn = "হ্যালো! কেমন আছো? তোমার সারাদিনের কাজের যত্ন নিতে আমি সব সময় পাশে আছি।",
            greetingHi = "हेलो! आप कैसे हैं? आज आपके सारे काम बहुत आसानी से पूरे करेंगे।",
            greetingEn = "Hello there! It is wonderful to hear from you. How can I brighten your day today?"
        ),
        JarvisVoice(
            id = "female_maya",
            name = "Maya (Energetic Tech)",
            gender = "Female",
            geminiVoice = "Fenrir",
            defaultPitch = 1.25f,
            defaultSpeed = 1.1f,
            personaTitle = "Punchy & Modern Tech",
            description = "High energy, fast-paced, cheerful, modern urban female tone. Quick and decisive.",
            avatarEmoji = "⚡",
            greetingBn = "হেই! অ্যালেক্স মায়া অনলাইন। চলো চোখের পলকে সবকিছু ডান করে ফেলি!",
            greetingHi = "हे! माया इन एक्शन। चलिए फटाफट अपने टास्क निपटाते हैं!",
            greetingEn = "Hey! Maya here, powered up and ready to crush today's goals. Let's go!"
        ),
        JarvisVoice(
            id = "female_ananya",
            name = "Ananya (Professional Executive)",
            gender = "Female",
            geminiVoice = "Charon",
            defaultPitch = 0.95f,
            defaultSpeed = 1.0f,
            personaTitle = "Corporate & Wise Mentor",
            description = "Formal, authoritative, clear diction, suitable for work, study and organization.",
            avatarEmoji = "💼",
            greetingBn = "নমস্কার। প্রতিটি মিনিট মূল্যবান। আজ কোন কাজগুলো আমরা সফলভাবে সম্পন্ন করব?",
            greetingHi = "नमस्कार। समय और सटीकता ही सफलता की कुंजी है। आज का एजेंडा क्या है?",
            greetingEn = "Greetings. Efficiency and precision lead to excellence. What is our primary objective?"
        ),
        JarvisVoice(
            id = "female_rhea",
            name = "Rhea (Expressive & Playful)",
            gender = "Female",
            geminiVoice = "Aoede",
            defaultPitch = 1.2f,
            defaultSpeed = 1.05f,
            personaTitle = "Playful & Vibrant",
            description = "Dynamic emotion, playful banter, expressive pitch with a smiling undertone.",
            avatarEmoji = "🎭",
            greetingBn = "আরে বাহ্! এসে গেছি আমি! বলো আজ কী দারুণ চমক অপেক্ষা করছে?",
            greetingHi = "अरे वाह! मैं आ गई। बताइए आज क्या मस्ती और काम की बात होगी?",
            greetingEn = "Ta-da! Rhea in the house. Ready for smart conversations and lots of fun?"
        )
    )

    val maleVoices = listOf(
        JarvisVoice(
            id = "male_jarvis_classic",
            name = "JARVIS Classic (Protocol)",
            gender = "Male",
            geminiVoice = "Puck",
            defaultPitch = 0.9f,
            defaultSpeed = 1.0f,
            personaTitle = "British/Formal Intelligence",
            description = "Classic Tony Stark JARVIS style. Extremely polite, witty, deep and analytical.",
            avatarEmoji = "🛡️",
            greetingBn = "আদেশ করুন, স্যার। জার্ভিস প্রোটোকল সক্রিয়। আপনার মোবাইল অটোমেশন প্রস্তুত।",
            greetingHi = "आदेश दीजिए, सर। जार्विस सिस्टम सक्रिय है। फोन और डेटा पूरी तरह तैयार हैं।",
            greetingEn = "At your service, Sir. JARVIS core protocols online. What shall we achieve today?"
        ),
        JarvisVoice(
            id = "male_orion",
            name = "Orion (Silicon Valley Coder)",
            gender = "Male",
            geminiVoice = "Charon",
            defaultPitch = 1.0f,
            defaultSpeed = 1.05f,
            personaTitle = "Full-Stack Tech Architect",
            description = "Crisp, logical, developer mindset, fast solution-oriented engineer voice.",
            avatarEmoji = "💻",
            greetingBn = "হেই ব্রাদার! কোড, অ্যাপ আর অটোমেশন সব অপ্টিমাইজড। কী রান করাতে চাও বলো।",
            greetingHi = "हे बडी! सारे अल्गोरिदम और ऐप कंट्रोल रेडी हैं। चलिए शुरू करते हैं।",
            greetingEn = "Hey there! Orion online. Backend, automation and APIs ready. What are we building?"
        ),
        JarvisVoice(
            id = "male_vikram",
            name = "Vikram (Deep Baritone Commander)",
            gender = "Male",
            geminiVoice = "Fenrir",
            defaultPitch = 0.75f,
            defaultSpeed = 0.95f,
            personaTitle = "Deep Resonant Commander",
            description = "Rich, resonant baritone, strong, grounded, and deeply protective tone.",
            avatarEmoji = "🏛️",
            greetingBn = "নিশ্চিন্তে থাকুন। আপনার ফোন ও নিরাপত্তা সম্পূর্ণ সুরক্ষিত। আমি সাথে আছি।",
            greetingHi = "निश्चिंत रहिए। आपकी सुरक्षा और सारे काम हमारे हाथ में हैं। बताइए क्या आज्ञा है।",
            greetingEn = "Stand firm, Sir. Defensive shields and system monitors are green. Proceed with confidence."
        ),
        JarvisVoice(
            id = "male_rohan",
            name = "Rohan (Casual Buddy / Dost)",
            gender = "Male",
            geminiVoice = "Puck",
            defaultPitch = 1.1f,
            defaultSpeed = 1.1f,
            personaTitle = "Cool & Friendly Buddy",
            description = "Relatable street-smart buddy tone, uses natural Bangla & Hinglish modern cadence.",
            avatarEmoji = "🤙",
            greetingBn = "আরে দোস্ত! কী খবর? চিল কর, সব কাজ আমি সেকেন্ডে সামলে নিচ্ছি!",
            greetingHi = "अरे भाई! क्या हालचाल? लोड मत लो, सारे टास्क झटपट हो जाएंगे!",
            greetingEn = "Yo bro! What's good? Relax, sit back, I got all your tasks covered!"
        ),
        JarvisVoice(
            id = "male_dev",
            name = "Dev (Wise Guru & Mentor)",
            gender = "Male",
            geminiVoice = "Charon",
            defaultPitch = 0.85f,
            defaultSpeed = 0.95f,
            personaTitle = "Wise Philosopher & Teacher",
            description = "Patient, gentle, articulate philosopher. Inspiring and reflective voice.",
            avatarEmoji = "🧘",
            greetingBn = "নমস্কার। ধৈর্য ও জানার ইচ্ছাই মানুষের সবচেয়ে বড় শক্তি। আজ আমরা কী শিখব?",
            greetingHi = "नमस्ते। धैर्य और निरंतर प्रयास ही उन्नति का मार्ग है। आइए आज कुछ नया सीखें।",
            greetingEn = "Greetings. Every conversation is a step toward greater wisdom. What shall we explore today?"
        )
    )

    val all: List<JarvisVoice> = femaleVoices + maleVoices

    fun getById(id: String): JarvisVoice {
        return all.firstOrNull { it.id == id } ?: femaleVoices.first()
    }
}
