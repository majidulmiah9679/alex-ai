package com.alex.ai.actions

import android.content.Context
import com.alex.ai.accessibility.AlexAccessibilityService
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

interface ActionExecutor {
    suspend fun execute(actionName: String, args: JsonObject?): ActionResult
}

/**
 * Store Flavor Action Executor:
 * Delegates to the robust PhoneActionExecutor.
 */
class StoreActionExecutor(private val context: Context) : ActionExecutor {
    private val phoneActionExecutor = PhoneActionExecutor(context)

    override suspend fun execute(actionName: String, args: JsonObject?): ActionResult {
        return phoneActionExecutor.execute(actionName, args)
    }
}

/**
 * Direct Flavor Action Executor:
 * Combines PhoneActionExecutor with deep autonomous device control via AlexAccessibilityService
 * (screen reading, auto-tapping, typing into inputs, and social media posting).
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
                return ActionResult(true, "Prepared $platform post with caption: '$caption'. Confirm to publish.")
            }
            else -> return storeExecutor.execute(actionName, args)
        }
    }
}
